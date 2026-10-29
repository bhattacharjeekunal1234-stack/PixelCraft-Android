package com.pixelcraft.editor.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the per-pixel color math.
 *
 * Pixel packing: (alpha shl 24) or (red shl 16) or (green shl 8) or blue.
 * Helper functions below build and unpack such ints.
 */
class ColorTransformTest {

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun red(pixel: Int) = (pixel shr 16) and 0xFF
    private fun green(pixel: Int) = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int) = pixel and 0xFF

    // ---------- brightness ----------

    @Test
    fun `zero brightness leaves pixel unchanged`() {
        val pixel = argb(120, 60, 200)
        assertEquals(pixel, ColorTransform.brightness(pixel, 0))
    }

    @Test
    fun `positive brightness lightens every channel`() {
        val result = ColorTransform.brightness(argb(10, 10, 10), 50)
        assertTrue(red(result) in 51..77)
        assertTrue(green(result) in 51..77)
        assertTrue(blue(result) in 51..77)
    }

    @Test
    fun `brightness clamps white at 255`() {
        val result = ColorTransform.brightness(argb(250, 250, 250), 100)
        assertEquals(255, red(result))
        assertEquals(255, green(result))
        assertEquals(255, blue(result))
    }

    @Test
    fun `brightness clamps black at 0`() {
        val result = ColorTransform.brightness(argb(5, 5, 5), -100)
        assertEquals(0, red(result))
        assertEquals(0, green(result))
        assertEquals(0, blue(result))
    }

    // ---------- contrast ----------

    @Test
    fun `zero contrast leaves pixel unchanged`() {
        val pixel = argb(120, 60, 200)
        assertEquals(pixel, ColorTransform.contrast(pixel, 0))
    }

    @Test
    fun `midpoint 128 is a fixed point of contrast`() {
        // Channel value 128 must never move, at any contrast amount.
        val pixel = argb(128, 128, 128)
        for (amount in -100..100 step 10) {
            assertEquals(128, red(ColorTransform.contrast(pixel, amount)))
        }
    }

    @Test
    fun `high contrast pushes dark pixel darker and light pixel lighter`() {
        val dark = red(ColorTransform.contrast(argb(100, 100, 100), 100))
        val light = red(ColorTransform.contrast(argb(200, 200, 200), 100))
        assertTrue(dark < 100)
        assertTrue(light > 200)
    }

    // ---------- exposure ----------

    @Test
    fun `zero exposure is identity`() {
        val pixel = argb(30, 140, 250)
        assertEquals(pixel, ColorTransform.exposure(pixel, 0))
    }

    @Test
    fun `exposure doubles brightness around zero`() {
        // +50 units means 2^(50/50) = 2x multiplier.
        val result = ColorTransform.exposure(argb(40, 40, 40), 50)
        assertEquals(80, red(result))
        assertEquals(80, green(result))
        assertEquals(80, blue(result))
    }

    @Test
    fun `strong negative exposure quarters the brightness`() {
        // -100 units means 2^(-2) = 0.25x multiplier: 120 * 0.25 = 30.
        val result = ColorTransform.exposure(argb(120, 120, 120), -100)
        assertEquals(30, red(result))
    }

    // ---------- saturation ----------

    @Test
    fun `zero saturation leaves pixel unchanged`() {
        val pixel = argb(120, 60, 200)
        assertEquals(pixel, ColorTransform.saturation(pixel, 0))
    }

    @Test
    fun `minimum saturation yields grayscale`() {
        val result = ColorTransform.saturation(argb(120, 60, 200), -100)
        assertEquals(red(result), green(result))
        assertEquals(green(result), blue(result))
    }

    @Test
    fun `already gray pixels do not change with saturation`() {
        // Grays have no chroma to amplify.
        val gray = argb(100, 100, 100)
        for (amount in -100..100 step 20) {
            assertEquals(gray, ColorTransform.saturation(gray, amount))
        }
    }

    @Test
    fun `high saturation increases distance from gray`() {
        val pixel = argb(120, 60, 200)
        val boosted = ColorTransform.saturation(pixel, 100)
        val originalSpread = red(pixel) - green(pixel)
        val boostedSpread = red(boosted) - green(boosted)
        assertTrue(boostedSpread > originalSpread)
    }

    // ---------- white balance ----------

    @Test
    fun `zero white balance is identity`() {
        val pixel = argb(120, 60, 200)
        assertEquals(pixel, ColorTransform.whiteBalance(pixel, 0))
    }

    @Test
    fun `warming raises red and lowers blue`() {
        val result = ColorTransform.whiteBalance(argb(128, 128, 128), 100)
        assertTrue(red(result) > 128)
        assertTrue(blue(result) < 128)
    }

    @Test
    fun `cooling is the mirror of warming`() {
        val pixel = argb(128, 128, 128)
        val warm = ColorTransform.whiteBalance(pixel, 80)
        val cool = ColorTransform.whiteBalance(pixel, -80)
        // Symmetric amounts on a neutral gray swap the red/blue offsets.
        assertEquals(blue(cool), red(warm))
        assertEquals(red(cool), blue(warm))
    }

    // ---------- alpha preservation ----------

    @Test
    fun `transformations preserve the alpha byte`() {
        val translucent = argb(10, 20, 30, a = 0x7F)
        val results = listOf(
            ColorTransform.brightness(translucent, 100),
            ColorTransform.contrast(translucent, -100),
            ColorTransform.exposure(translucent, 100),
            ColorTransform.saturation(translucent, -100),
            ColorTransform.whiteBalance(translucent, -100),
            ColorTransform.grayscale(translucent),
            ColorTransform.warm(translucent),
            ColorTransform.cool(translucent)
        )
        results.forEach { result ->
            assertEquals(0x7F, (result ushr 24) and 0xFF)
        }
    }

    // ---------- grayscale / filters ----------

    @Test
    fun `grayscale produces equal channels`() {
        val result = ColorTransform.grayscale(argb(30, 200, 90))
        assertEquals(red(result), green(result))
        assertEquals(green(result), blue(result))
    }

    @Test
    fun `warm filter lifts red above blue`() {
        val result = ColorTransform.warm(argb(128, 128, 128))
        assertTrue(red(result) > blue(result))
    }

    @Test
    fun `cool filter lifts blue above red`() {
        val result = ColorTransform.cool(argb(128, 128, 128))
        assertTrue(blue(result) > red(result))
    }

    @Test
    fun `warm and cool keep channels inside 0-255`() {
        val extremes = listOf(argb(0, 0, 0), argb(255, 255, 255), argb(255, 0, 255))
        extremes.forEach { pixel ->
            listOf(ColorTransform.warm(pixel), ColorTransform.cool(pixel)).forEach { result ->
                assertTrue(red(result) in 0..255)
                assertTrue(green(result) in 0..255)
                assertTrue(blue(result) in 0..255)
            }
        }
    }
}
