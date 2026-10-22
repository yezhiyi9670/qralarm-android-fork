package com.sweak.qralarm.features.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweak.qralarm.core.domain.user.UserDataRepository
import com.sweak.qralarm.core.domain.user.model.Theme
import com.sweak.qralarm.features.theme.model.ThemeUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ThemeScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userDataRepository.theme.collect { theme ->
                val themeUi = when (theme) {
                    is Theme.Default -> ThemeUi.Default
                    is Theme.Dynamic -> ThemeUi.Dynamic
                    is Theme.Custom -> {
                        val matchingPreset = _state.value.availableCustomThemes.firstOrNull {
                            it.primaryColor.equals(theme.primaryColor, ignoreCase = true)
                        }

                        matchingPreset ?: ThemeUi.Custom(
                            primaryColor = theme.primaryColor,
                            customColor = true
                        )
                    }
                }

                _state.update { currentState ->
                    currentState.copy(theme = themeUi)
                }
            }
        }
    }

    fun onEvent(event: ThemeScreenUserEvent) {
        when (event) {
            is ThemeScreenUserEvent.OnDynamicThemeToggled -> viewModelScope.launch {
                val dynamicThemeEnabled = event.isChecked

                userDataRepository.setTheme(
                    theme = if (dynamicThemeEnabled) Theme.Dynamic else Theme.Default
                )

                _state.update { currentState ->
                    currentState.copy(
                        theme = if (dynamicThemeEnabled) ThemeUi.Dynamic else ThemeUi.Default
                    )
                }
            }

            is ThemeScreenUserEvent.OnCustomThemeToggled -> viewModelScope.launch {
                if (event.isChecked) {
                    // Picking the custom theme switch uses the last preset, matching the
                    // behaviour of the original app.
                    val theme = _state.value.availableCustomThemes.last()

                    userDataRepository.setTheme(theme = Theme.Custom(theme.primaryColor))

                    _state.update { currentState ->
                        currentState.copy(theme = theme)
                    }
                } else {
                    userDataRepository.setTheme(theme = Theme.Default)

                    _state.update { currentState ->
                        currentState.copy(theme = ThemeUi.Default)
                    }
                }
            }

            is ThemeScreenUserEvent.OnCustomThemeSelected -> viewModelScope.launch {
                userDataRepository.setTheme(
                    theme = Theme.Custom(primaryColor = event.theme.primaryColor)
                )

                _state.update { currentState ->
                    currentState.copy(theme = event.theme)
                }
            }

            is ThemeScreenUserEvent.OnColorPickerOpened -> {
                _state.update { currentState ->
                    currentState.copy(isColorPickerOpen = true)
                }
            }

            is ThemeScreenUserEvent.OnColorPickerDismissed -> {
                _state.update { currentState ->
                    currentState.copy(isColorPickerOpen = false)
                }
            }

            is ThemeScreenUserEvent.OnColorPicked -> viewModelScope.launch {
                userDataRepository.setTheme(theme = Theme.Custom(primaryColor = event.color))

                _state.update { currentState ->
                    currentState.copy(
                        theme = ThemeUi.Custom(
                            primaryColor = event.color,
                            customColor = true
                        ),
                        isColorPickerOpen = false
                    )
                }
            }

            else -> { /* no-op */
            }
        }
    }
}
