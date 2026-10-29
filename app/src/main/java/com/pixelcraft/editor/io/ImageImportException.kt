package com.pixelcraft.editor.io

/**
 * Thrown when a photo cannot be read from the gallery. The message is
 * user-facing, so it must always be a readable sentence, not a stack trace.
 */
class ImageImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
