package com.sweak.qralarm.features.theme

import com.sweak.qralarm.features.theme.model.ThemeUi

sealed class ThemeScreenUserEvent {
    data object OnBackClicked : ThemeScreenUserEvent()
    data class OnDynamicThemeToggled(val isChecked: Boolean) : ThemeScreenUserEvent()
    data class OnCustomThemeToggled(val isChecked: Boolean) : ThemeScreenUserEvent()
    data class OnCustomThemeSelected(val theme: ThemeUi.Custom) : ThemeScreenUserEvent()
    data object OnColorPickerOpened : ThemeScreenUserEvent()
    data object OnColorPickerDismissed : ThemeScreenUserEvent()
    data class OnColorPicked(val color: String) : ThemeScreenUserEvent()
}
