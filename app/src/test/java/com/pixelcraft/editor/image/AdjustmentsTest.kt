package com.pixelcraft.editor.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the adjustment value object: ranges, defaults and the copy API. */
class AdjustmentsTest {

    @Test
    fun `new adjustments are neutral`() {
        val adjustments = Adjustments()
        assertTrue(adjustments.isDefault)
        assertEquals(0, adjustments.brightness)
        assertEquals(0, adjustments.contrast)
        assertEquals(0, adjustments.saturation)
        assertEquals(0, adjustments.exposure)
        assertEquals(0, adjustments.whiteBalance)
        assertEquals(FilterType.ORIGINAL, adjustments.filter)
    }

    @Test
    fun `changing one slider keeps the others untouched`() {
        val adjusted = Adjustments().withBrightness(30).withContrast(-10)
        assertEquals(30, adjusted.brightness)
        assertEquals(-10, adjusted.contrast)
        assertEquals(0, adjusted.saturation)
        assertEquals(0, adjusted.exposure)
        assertEquals(0, adjusted.whiteBalance)
        assertEquals(FilterType.ORIGINAL, adjusted.filter)
        assertFalse(adjusted.isDefault)
    }

    @Test
    fun `values outside -100 and 100 are clamped`() {
        assertEquals(-100, Adjustments().withBrightness(-250).brightness)
        assertEquals(100, Adjustments().withBrightness(250).brightness)
        assertEquals(-100, Adjustments().withExposure(-999).exposure)
        assertEquals(100, Adjustments().withWhiteBalance(1234).whiteBalance)
    }

    @Test
    fun `filter changes break the default state`() {
        val filtered = Adjustments().withFilter(FilterType.GRAYSCALE)
        assertFalse(filtered.isDefault)
        assertEquals(FilterType.GRAYSCALE, filtered.filter)
    }

    @Test
    fun `defaults companion matches a fresh instance`() {
        assertEquals(Adjustments(), Adjustments.DEFAULTS)
        assertTrue(Adjustments.DEFAULTS.isDefault)
    }
}
