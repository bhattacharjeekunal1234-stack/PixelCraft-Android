package com.pixelcraft.editor.image

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure-Kotlin per-pixel color math for PixelCraft.
 *
 * Every function takes a packed Android ARGB int and an adjustment in the
 * range -100..100 (except grayscale, which needs no amount) and returns the
 * new packed ARGB int. Keeping the math free of android.* classes lets us
 * unit test it on the JVM with plain JUnit.
 *
 * All channels are clamped to 0..255 after each operation, and the original
 * alpha byte is always preserved, so stacking adjustments can never corrupt
 * neighboring pixels or make the photo transparent.
 */
object ColorTransform {

    /** Lifts or darkens every channel by a flat offset (max ±127). */
    fun brightness(argb: Int, amount: Int): Int {
        val offset = (amount * 127) / 100
        val r = ((argb shr 16) and 0xFF) + offset
        val g = ((argb shr 8) and 0xFF) + offset
        val b = (argb and 0xFF) + offset
        return combine(argb, r, g, b)
    }

    /** Stretches or compresses channel values around the midpoint 128. */
    fun contrast(argb: Int, amount: Int): Int {
        val factor = (amount + 100) / 100f // 0.0f at -100, 2.0f at +100
        val r = ((argb shr 16) and 0xFF)
        val g = ((argb shr 8) and 0xFF)
        val b = (argb and 0xFF)
        return combine(
            argb,
            ((r - 128) * factor + 128).roundToInt(),
            ((g - 128) * factor + 128).roundToInt(),
            ((b - 128) * factor + 128).roundToInt()
        )
    }

    /** Multiplies every channel — emulates camera exposure stops. */
    fun exposure(argb: Int, amount: Int): Int {
        val multiplier = 2f.pow(amount / 50f) // 0.25x at -100, 4x at +100
        val r = ((argb shr 16) and 0xFF) * multiplier
        val g = ((argb shr 8) and 0xFF) * multiplier
        val b = (argb and 0xFF) * multiplier
        return combine(argb, r.roundToInt(), g.roundToInt(), b.roundToInt())
    }

    /** Scales the distance of each channel from its luminance (0 = b&w). */
    fun saturation(argb: Int, amount: Int): Int {
        val factor = (amount + 100) / 100f // 0.0f at -100, 2.0f at +100
        val r = ((argb shr 16) and 0xFF)
        val g = ((argb shr 8) and 0xFF)
        val b = (argb and 0xFF)
        val luma = luminance(r, g, b)
        return combine(
            argb,
            (luma + (r - luma) * factor).roundToInt(),
            (luma + (g - luma) * factor).roundToInt(),
            (luma + (b - luma) * factor).roundToInt()
        )
    }

    /**
     * White balance: positive warms (adds red, removes blue), negative cools.
     * Green moves a quarter as much so mid-tones stay believable.
     */
    fun whiteBalance(argb: Int, amount: Int): Int {
        val shift = amount * 30 / 100 // max ±30 levels
        val r = ((argb shr 16) and 0xFF) + shift
        val g = ((argb shr 8) and 0xFF) + shift / 4
        val b = (argb and 0xFF) - shift
        return combine(argb, r, g, b)
    }

    /** ITU-R BT.601 luma, the standard grayscale recipe for photos. */
    fun luminance(r: Int, g: Int, b: Int): Int =
        (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()

    /** Full desaturation using the luma formula above. */
    fun grayscale(argb: Int): Int {
        val gray = luminance((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
        return combine(argb, gray, gray, gray)
    }

    /** One-tap warm look: lift reds, sink blues, nudge green. */
    fun warm(argb: Int): Int {
        val r = ((argb shr 16) and 0xFF) + 14
        val g = ((argb shr 8) and 0xFF) + 4
        val b = (argb and 0xFF) - 14
        return combine(argb, r, g, b)
    }

    /** One-tap cool look: the exact mirror of [warm]. */
    fun cool(argb: Int): Int {
        val r = ((argb shr 16) and 0xFF) - 14
        val g = ((argb shr 8) and 0xFF) + 2
        val b = (argb and 0xFF) + 14
        return combine(argb, r, g, b)
    }

    // Rebuilds the pixel with the original alpha and clamped fresh channels.
    private fun combine(argb: Int, r: Int, g: Int, b: Int): Int =
        (argb and 0xFF000000.toInt()) or
            (clamp(r) shl 16) or
            (clamp(g) shl 8) or
            clamp(b)

    private fun clamp(value: Int): Int = max(0, min(255, value))
}
