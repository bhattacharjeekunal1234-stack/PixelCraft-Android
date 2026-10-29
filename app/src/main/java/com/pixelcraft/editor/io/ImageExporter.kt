package com.pixelcraft.editor.io

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves an edited bitmap into the device gallery.
 *
 * - Android 10+ (API 29): MediaStore, into Pictures/PixelCraft.
 *   No storage permission is required for this.
 * - Android 9 and below: the app-specific Pictures directory, which also
 *   needs no permission, then media-scanner registration so Gallery apps
 *   see the file.
 */
object ImageExporter {

    enum class Format { JPEG, PNG }

    /** What the caller needs to confirm a successful save to the user. */
    data class ExportResult(
        val uri: Uri,
        val displayName: String,
        val sizeBytes: Long
    )

    /** Thrown with a user-facing message when saving fails. */
    class ImageExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun export(context: Context, bitmap: Bitmap, format: Format, jpegQuality: Int): ExportResult {
        val bytes = encode(bitmap, format, jpegQuality)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(context, bytes, format)
        } else {
            exportToAppDirectory(context, bytes, format)
        }
    }

    // Compresses once into memory so a failure happens before any file/row
    // is created, leaving no broken artifacts in the gallery.
    private fun encode(bitmap: Bitmap, format: Format, jpegQuality: Int): ByteArray {
        val compressFormat = when (format) {
            Format.JPEG -> Bitmap.CompressFormat.JPEG
            Format.PNG -> Bitmap.CompressFormat.PNG
        }
        val stream = ByteArrayOutputStream()
        val ok = bitmap.compress(compressFormat, jpegQuality, stream)
        if (!ok) {
            throw ImageExportException("The image could not be encoded.")
        }
        return stream.toByteArray()
    }

    private fun exportViaMediaStore(
        context: Context,
        bytes: ByteArray,
        format: Format
    ): ExportResult {
        val displayName = buildFileName(format)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeTypeOf(format))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/PixelCraft"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val rowUri = resolver.insert(collection, values)
            ?: throw ImageExportException("The gallery rejected the new image.")

        try {
            resolver.openOutputStream(rowUri)?.use { output ->
                output.write(bytes)
            } ?: throw ImageExportException("Could not open the gallery for writing.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(rowUri, values, null, null)
            }
        } catch (error: Exception) {
            resolver.delete(rowUri, null, null) // remove the half-written row
            throw ImageExportException("Saving failed: ${error.message}", error)
        }

        return ExportResult(rowUri, displayName, bytes.size.toLong())
    }

    private fun exportToAppDirectory(
        context: Context,
        bytes: ByteArray,
        format: Format
    ): ExportResult {
        val picturesDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "PixelCraft"
        )
        if (!picturesDir.exists() && !picturesDir.mkdirs()) {
            throw ImageExportException("Could not create the save folder.")
        }

        val file = File(picturesDir, buildFileName(format))
        try {
            FileOutputStream(file).use { output -> output.write(bytes) }
        } catch (error: Exception) {
            throw ImageExportException("Saving failed: ${error.message}", error)
        }

        // Let gallery apps notice the new file.
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeTypeOf(format)),
            null
        )

        return ExportResult(Uri.fromFile(file), file.name, bytes.size.toLong())
    }

    // Example: PixelCraft_20261029_183045.jpg
    private fun buildFileName(format: Format): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (format == Format.JPEG) "jpg" else "png"
        return "PixelCraft_$stamp.$extension"
    }

    private fun mimeTypeOf(format: Format): String =
        if (format == Format.JPEG) "image/jpeg" else "image/png"
}
