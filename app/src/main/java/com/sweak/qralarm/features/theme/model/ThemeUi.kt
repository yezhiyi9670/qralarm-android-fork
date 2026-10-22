package com.sweak.qralarm.features.theme.model

sealed class ThemeUi {
    data object Default : ThemeUi()
    data object Dynamic : ThemeUi()

    /**
     * [customColor] is false when [primaryColor] is one of the preset themes and true when the
     * user picked an arbitrary color.
     */
    data class Custom(
        val primaryColor: String,
        val customColor: Boolean
    ) : ThemeUi()
}
