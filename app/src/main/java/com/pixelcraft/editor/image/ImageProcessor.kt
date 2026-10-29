package com.pixelcraft.editor.image

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Applies a full [Adjustments] set to a bitmap in one pass over the pixel
 * buffer. A single pass is much faster than running each adjustment as its
 * own bitmap copy, which matters because sliders re-render continuously.
 *
 * The source bitmap is never modified: a new bitmap is always returned.
 *
 * Pipeline order (chosen to match how desktop editors behave):
 * exposure -> brightness -> contrast -> saturation -> white balance -> filter.
 */
object ImageProcessor {

    fun apply(source: Bitmap, adjustments: Adjustments): Bitmap {
        if (adjustments.isDefault) {
            // Nothing to do: hand back a copy so callers can treat the
            // result as an independent bitmap.
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val hasFilter = adjustments.filter != FilterType.ORIGINAL
        for (i in pixels.indices) {
            var pixel = pixels[i]

            if (adjustments.exposure != 0) pixel = ColorTransform.exposure(pixel, adjustments.exposure)
            if (adjustments.brightness != 0) pixel = ColorTransform.brightness(pixel, adjustments.brightness)
            if (adjustments.contrast != 0) pixel = ColorTransform.contrast(pixel, adjustments.contrast)
            if (adjustments.saturation != 0) pixel = ColorTransform.saturation(pixel, adjustments.saturation)
            if (adjustments.whiteBalance != 0) pixel = ColorTransform.whiteBalance(pixel, adjustments.whiteBalance)

            if (hasFilter) {
                pixel = when (adjustments.filter) {
                    FilterType.GRAYSCALE -> ColorTransform.grayscale(pixel)
                    FilterType.WARM -> ColorTransform.warm(pixel)
                    FilterType.COOL -> ColorTransform.cool(pixel)
                    FilterType.ORIGINAL -> pixel
                }
            }

            pixels[i] = pixel
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /** Rotates a bitmap by ±90 degrees and returns the new bitmap. */
    fun rotate90(source: Bitmap, clockwise: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(if (clockwise) 90f else -90f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
