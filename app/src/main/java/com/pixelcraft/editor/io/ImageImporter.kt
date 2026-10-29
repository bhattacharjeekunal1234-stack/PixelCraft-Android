package com.pixelcraft.editor.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

/**
 * Loads a photo from a gallery URI into a display-sized bitmap.
 *
 * Two details handled here are the difference between "works in the demo"
 * and "works on real photos":
 * 1. Large camera photos are downsampled with inSampleSize so a 48 MP shot
 *    does not exhaust the heap.
 * 2. EXIF orientation is applied, because phones store pixels sideways and
 *    expect the viewer to rotate them; skipping this shows sideways photos.
 */
object ImageImporter {

    /** Largest edge of the bitmap we keep in memory for editing. */
    private const val MAX_DIMENSION = 2048

    /**
     * Decodes the image at [uri], downsamples it if needed and corrects the
     * EXIF rotation. Throws [ImageImportException] with a user-facing message
     * on any failure.
     */
    fun decodeFromUri(context: Context, uri: Uri): Bitmap {
        val downsampled = try {
            decodeDownsampled(context, uri)
        } catch (error: OutOfMemoryError) {
            throw ImageImportException(
                context.getString(com.pixelcraft.editor.R.string.error_oom),
                error
            )
        } catch (error: Exception) {
            throw ImageImportException(
                context.getString(com.pixelcraft.editor.R.string.error_unreadable_image),
                error
            )
        }

        return try {
            applyExifOrientation(context, uri, downsampled)
        } catch (error: Exception) {
            // Orientation is a nice-to-have: if EXIF cannot be read we still
            // have a decodable bitmap, so keep it instead of failing.
            downsampled
        }
    }

    // First pass reads only the size, second pass decodes at a reduced scale.
    private fun decodeDownsampled(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ImageImportException(
                context.getString(com.pixelcraft.editor.R.string.error_unreadable_image)
            )
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = openStream(context, uri).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw ImageImportException(
            context.getString(com.pixelcraft.editor.R.string.error_unreadable_image)
        )
        return bitmap
    }

    // Largest power-of-two divisor that brings both edges under the limit.
    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var largestEdge = max(width, height)
        while (largestEdge / 2 >= MAX_DIMENSION / 2) {
            sampleSize *= 2
            largestEdge /= 2
        }
        return sampleSize
    }

    // Reads EXIF orientation and rotates/flips the bitmap to upright pixels.
    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = openStream(context, uri).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun openStream(context: Context, uri: Uri) =
        context.contentResolver.openInputStream(uri)
            ?: throw ImageImportException(
                context.getString(com.pixelcraft.editor.R.string.error_unreadable_image)
            )
}
