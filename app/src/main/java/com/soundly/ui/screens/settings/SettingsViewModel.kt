package com.soundly.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.ThemeMode
import com.soundly.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.annotation.StringRes
import com.soundly.R

sealed class SettingsSection(@StringRes val titleRes: Int) {
    object Main : SettingsSection(R.string.settings_title)
    object Profile : SettingsSection(R.string.profile_screen_title)
    object Scan : SettingsSection(R.string.settings_scan_title)
    object Audio : SettingsSection(R.string.settings_audio_title)
    object Equalizer : SettingsSection(R.string.equalizer_title)
    object General : SettingsSection(R.string.settings_general_title)
    object Interface : SettingsSection(R.string.settings_interface_title)
    object Home : SettingsSection(R.string.interface_customize_home_title)
    object Player : SettingsSection(R.string.player_settings_title)
    object MiniPlayer : SettingsSection(R.string.interface_mini_player_title)
    object PlayerAnimations : SettingsSection(R.string.animations_title)
    object PlayerStyle : SettingsSection(R.string.player_style_title)
    object Lyrics : SettingsSection(R.string.lyrics_title)
    object Connection : SettingsSection(R.string.connection_title)
    object SafePlayback : SettingsSection(R.string.audio_safe_playback_title)
    object Language : SettingsSection(R.string.language_selection_title)
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val repository: MusicRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    val cloudEnabled = userSettingsRepository.cloudEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode = userSettingsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColorsEnabled = userSettingsRepository.dynamicColorsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    private val _currentSection = MutableStateFlow<SettingsSection>(SettingsSection.Main)
    val currentSection: StateFlow<SettingsSection> = _currentSection.asStateFlow()

    fun navigateTo(section: SettingsSection) {
        _currentSection.value = section
    }

    fun updateCloudEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.updateCloudEnabled(enabled)
        }
    }

    fun handleBack(): Boolean {
        return when (_currentSection.value) {
            SettingsSection.Main -> false
            SettingsSection.Player -> {
                _currentSection.value = SettingsSection.Interface
                true
            }
            SettingsSection.PlayerAnimations -> {
                _currentSection.value = SettingsSection.Player
                true
            }
            SettingsSection.PlayerStyle -> {
                _currentSection.value = SettingsSection.Player
                true
            }
            SettingsSection.MiniPlayer -> {
                _currentSection.value = SettingsSection.Interface
                true
            }
            SettingsSection.Lyrics -> {
                _currentSection.value = SettingsSection.Interface
                true
            }
            SettingsSection.Home -> {
                _currentSection.value = SettingsSection.Interface
                true
            }
            SettingsSection.Equalizer -> {
                _currentSection.value = SettingsSection.Audio
                true
            }
            SettingsSection.Connection -> {
                _currentSection.value = SettingsSection.Audio
                true
            }
            SettingsSection.SafePlayback -> {
                _currentSection.value = SettingsSection.Audio
                true
            }
            SettingsSection.Language -> {
                _currentSection.value = SettingsSection.General
                true
            }
            else -> {
                _currentSection.value = SettingsSection.Main
                true
            }
        }
    }
}
