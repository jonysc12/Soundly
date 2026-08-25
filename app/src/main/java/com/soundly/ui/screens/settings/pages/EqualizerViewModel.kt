package com.soundly.ui.screens.settings.pages

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.AudioSettings
import com.soundly.data.repository.AudioSettingsRepository
import com.soundly.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BandInfo(
    val index: Int,
    val frequency: Int,
    val level: Int
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val repository: AudioSettingsRepository,
    private val playbackManager: PlaybackManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<AudioSettings> = repository.audioSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioSettings()
        )

    fun toggleEqualizer(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateEqualizerEnabled(enabled)
        }
    }

    fun updateBandLevel(index: Int, level: Int) {
        viewModelScope.launch {
            repository.updateBandLevel(index, level)
        }
    }

    fun updateBassBoost(strength: Int) {
        viewModelScope.launch {
            repository.updateBassBoostStrength(strength)
        }
    }

    fun updateVirtualizer(strength: Int) {
        viewModelScope.launch {
            repository.updateVirtualizerStrength(strength)
        }
    }
    
    fun resetEqualizer() {
        viewModelScope.launch {
            repository.resetEqualizer()
        }
    }
    
    fun openSystemEqualizer() {
        try {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Handle error or fallback
        }
    }
    
    // Band frequencies are typically: 60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz for 5 bands
    // Or more depending on the device.
    fun getBandFrequencies(): List<Int> {
        return playbackManager.getEqualizerBandFrequencies()
    }
    
    fun getBandLevelRange(): IntArray {
        return playbackManager.getEqualizerBandLevelRange()
    }
}
