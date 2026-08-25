package com.soundly.ui.screens.settings.pages

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.AudioSettings
import com.soundly.data.repository.AudioSettingsRepository
import com.soundly.data.repository.NormalizationLevel
import com.soundly.data.repository.PairedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<AudioSettings> = repository.audioSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioSettings()
        )

    fun toggleGapless(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateGaplessEnabled(enabled)
        }
    }

    fun toggleMono(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMonoEnabled(enabled)
        }
    }

    fun toggleNormalization(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNormalizationEnabled(enabled)
        }
    }

    fun setNormalizationLevel(level: NormalizationLevel) {
        viewModelScope.launch {
            repository.updateNormalizationLevel(level)
        }
    }

    fun toggleAudioFocus(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAudioFocusEnabled(enabled)
        }
    }

    fun toggleCrossfade(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        viewModelScope.launch {
            repository.updateCrossfadeDuration(seconds)
        }
    }

    fun toggleBluetoothAutoplay(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBluetoothAutoplayEnabled(enabled)
        }
    }

    fun getPairedDevices(): List<PairedDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        return if (adapter != null) {
            try {
                adapter.bondedDevices.map { PairedDevice(it.name ?: "Desconocido", it.address) }
            } catch (e: SecurityException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun toggleDeviceAutoplay(address: String) {
        viewModelScope.launch {
            val currentDevices = settings.value.bluetoothAutoplayDevices.toMutableSet()
            if (currentDevices.contains(address)) {
                currentDevices.remove(address)
            } else {
                currentDevices.add(address)
            }
            repository.updateBluetoothAutoplayDevices(currentDevices)
        }
    }

    // Safe Playback
    fun toggleSafePlayback(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSafePlaybackEnabled(enabled)
        }
    }

    fun toggleIgnoreSpeaker(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateIgnoreSpeakerExposure(enabled)
        }
    }

    fun setUserAge(age: Int) {
        viewModelScope.launch {
            repository.updateUserAge(age)
        }
    }

    fun toggleIntelligentVolume(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateIntelligentVolumeReduction(enabled)
        }
    }

    fun toggleForcedRest(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateForcedRestEnabled(enabled)
        }
    }

    fun toggleDbMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDbMonitoringEnabled(enabled)
        }
    }

    fun toggleSafeNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSafePlaybackNotificationsEnabled(enabled)
        }
    }

    fun cancelRest() {
        viewModelScope.launch {
            repository.updateIsResting(false, 0L)
        }
    }
}
