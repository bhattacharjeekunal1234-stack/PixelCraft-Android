package com.pixelcraft.editor.image

/**
 * One-tap looks available in the Filters tab.
 *
 * ORIGINAL leaves colors untouched; the other presets are implemented as
 * short color recipes in [ColorTransform] and [ImageProcessor].
 */
enum class FilterType {
    ORIGINAL,
    GRAYSCALE,
    WARM,
    COOL
}
