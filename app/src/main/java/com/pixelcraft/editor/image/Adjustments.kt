package com.pixelcraft.editor.image

/**
 * The complete set of user-editable parameters for one editing session.
 *
 * Every slider value uses the range -100..100 where 0 means "no change".
 * This class is pure Kotlin (no Android imports) so it can be unit tested
 * with plain JUnit.
 */
data class Adjustments(
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val exposure: Int = 0,
    val whiteBalance: Int = 0,
    val filter: FilterType = FilterType.ORIGINAL
) {

    /** True when nothing has been changed from the neutral state. */
    val isDefault: Boolean
        get() = this == DEFAULTS

    fun withBrightness(value: Int) = copy(brightness = value.coerceIn(MIN, MAX))
    fun withContrast(value: Int) = copy(contrast = value.coerceIn(MIN, MAX))
    fun withSaturation(value: Int) = copy(saturation = value.coerceIn(MIN, MAX))
    fun withExposure(value: Int) = copy(exposure = value.coerceIn(MIN, MAX))
    fun withWhiteBalance(value: Int) = copy(whiteBalance = value.coerceIn(MIN, MAX))
    fun withFilter(value: FilterType) = copy(filter = value)

    companion object {
        const val MIN = -100
        const val MAX = 100

        /** The neutral state every new editing session starts from. */
        val DEFAULTS = Adjustments()
    }
}
