package com.soundly.ui.screens.settings.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.UserSettingsRepository
import com.soundly.data.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceViewModel @Inject constructor(
    private val repository: UserSettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val dynamicColorsEnabled: StateFlow<Boolean> = repository.dynamicColorsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val vividColors: StateFlow<Boolean> = repository.vividColorsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val showHomePage: StateFlow<Boolean> = repository.showHomePageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setShowHomePage(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShowHomePage(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDynamicColorsEnabled(enabled)
        }
    }

    fun setVividColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVividColors(enabled)
        }
    }
}
