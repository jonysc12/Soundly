package com.soundly.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.audioDataStore by preferencesDataStore(name = "audio_settings")

enum class NormalizationLevel {
    LOW, NORMAL, HIGH
}

data class PairedDevice(
    val name: String,
    val address: String
)

data class AudioSettings(
    val gaplessEnabled: Boolean = true,
    val monoEnabled: Boolean = false,
    val normalizationEnabled: Boolean = true,
    val normalizationLevel: NormalizationLevel = NormalizationLevel.NORMAL,
    val audioFocusEnabled: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val equalizerBandLevels: Map<Int, Int> = emptyMap(), // Band index to level
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDuration: Int = 5, // In seconds
    val bluetoothAutoplayEnabled: Boolean = false,
    val bluetoothAutoplayDevices: Set<String> = emptySet(),
    
    // Safe Playback
    val safePlaybackEnabled: Boolean = false,
    val ignoreSpeakerExposure: Boolean = true,
    val userAge: Int = 20,
    val dailyExposureMinutes: Int = 0,
    val lastExposureResetTimestamp: Long = 0L,
    val intelligentVolumeReduction: Boolean = true,
    
    // Advanced Safe Playback
    val weeklyExposureMinutes: Map<Int, Int> = emptyMap(), // Day of week to minutes
    val forcedRestEnabled: Boolean = false,
    val isResting: Boolean = false,
    val restEndTime: Long = 0L,
    val dbMonitoringEnabled: Boolean = true,
    val safePlaybackNotificationsEnabled: Boolean = true
)

@Singleton
class AudioSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
        val MONO_ENABLED = booleanPreferencesKey("mono_enabled")
        val NORMALIZATION_ENABLED = booleanPreferencesKey("normalization_enabled")
        val NORMALIZATION_LEVEL = stringPreferencesKey("normalization_level")
        val AUDIO_FOCUS_ENABLED = booleanPreferencesKey("audio_focus_enabled")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_BAND_LEVELS = stringPreferencesKey("equalizer_band_levels")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val BLUETOOTH_AUTOPLAY_ENABLED = booleanPreferencesKey("bluetooth_autoplay_enabled")
        val BLUETOOTH_AUTOPLAY_DEVICES = stringSetPreferencesKey("bluetooth_autoplay_devices")
        
        // Safe Playback
        val SAFE_PLAYBACK_ENABLED = booleanPreferencesKey("safe_playback_enabled")
        val IGNORE_SPEAKER_EXPOSURE = booleanPreferencesKey("ignore_speaker_exposure")
        val USER_AGE = intPreferencesKey("user_age")
        val DAILY_EXPOSURE_MINUTES = intPreferencesKey("daily_exposure_minutes")
        val LAST_EXPOSURE_RESET_TIMESTAMP = longPreferencesKey("last_exposure_reset_timestamp")
        val INTELLIGENT_VOLUME_REDUCTION = booleanPreferencesKey("intelligent_volume_reduction")
        
        // Advanced Safe Playback
        val WEEKLY_EXPOSURE_MINUTES = stringPreferencesKey("weekly_exposure_minutes")
        val FORCED_REST_ENABLED = booleanPreferencesKey("forced_rest_enabled")
        val IS_RESTING = booleanPreferencesKey("is_resting")
        val REST_END_TIME = longPreferencesKey("rest_end_time")
        val DB_MONITORING_ENABLED = booleanPreferencesKey("db_monitoring_enabled")
        val SAFE_PLAYBACK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("safe_playback_notifications_enabled")
    }

    val audioSettingsFlow: Flow<AudioSettings> = context.audioDataStore.data.map { preferences ->
        val bandLevelsString = preferences[PreferencesKeys.EQUALIZER_BAND_LEVELS] ?: ""
        val bandLevelsMap = if (bandLevelsString.isBlank()) emptyMap() else {
            bandLevelsString.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) {
                    val band = parts[0].toIntOrNull()
                    val level = parts[1].toIntOrNull()
                    if (band != null && level != null) band to level else null
                } else null
            }.toMap()
        }

        val weeklyStatsString = preferences[PreferencesKeys.WEEKLY_EXPOSURE_MINUTES] ?: ""
        val weeklyStatsMap = if (weeklyStatsString.isBlank()) emptyMap() else {
            weeklyStatsString.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) {
                    val day = parts[0].toIntOrNull()
                    val mins = parts[1].toIntOrNull()
                    if (day != null && mins != null) day to mins else null
                } else null
            }.toMap()
        }

        AudioSettings(
            gaplessEnabled = preferences[PreferencesKeys.GAPLESS_ENABLED] ?: true,
            monoEnabled = preferences[PreferencesKeys.MONO_ENABLED] ?: false,
            normalizationEnabled = preferences[PreferencesKeys.NORMALIZATION_ENABLED] ?: true,
            normalizationLevel = NormalizationLevel.valueOf(
                preferences[PreferencesKeys.NORMALIZATION_LEVEL] ?: NormalizationLevel.NORMAL.name
            ),
            audioFocusEnabled = preferences[PreferencesKeys.AUDIO_FOCUS_ENABLED] ?: true,
            equalizerEnabled = preferences[PreferencesKeys.EQUALIZER_ENABLED] ?: false,
            equalizerBandLevels = bandLevelsMap,
            bassBoostStrength = preferences[PreferencesKeys.BASS_BOOST_STRENGTH] ?: 0,
            virtualizerStrength = preferences[PreferencesKeys.VIRTUALIZER_STRENGTH] ?: 0,
            crossfadeEnabled = preferences[PreferencesKeys.CROSSFADE_ENABLED] ?: false,
            crossfadeDuration = preferences[PreferencesKeys.CROSSFADE_DURATION] ?: 5,
            bluetoothAutoplayEnabled = preferences[PreferencesKeys.BLUETOOTH_AUTOPLAY_ENABLED] ?: false,
            bluetoothAutoplayDevices = preferences[PreferencesKeys.BLUETOOTH_AUTOPLAY_DEVICES] ?: emptySet(),
            
            // Safe Playback
            safePlaybackEnabled = preferences[PreferencesKeys.SAFE_PLAYBACK_ENABLED] ?: false,
            ignoreSpeakerExposure = preferences[PreferencesKeys.IGNORE_SPEAKER_EXPOSURE] ?: true,
            userAge = preferences[PreferencesKeys.USER_AGE] ?: 20,
            dailyExposureMinutes = preferences[PreferencesKeys.DAILY_EXPOSURE_MINUTES] ?: 0,
            lastExposureResetTimestamp = preferences[PreferencesKeys.LAST_EXPOSURE_RESET_TIMESTAMP] ?: 0L,
            intelligentVolumeReduction = preferences[PreferencesKeys.INTELLIGENT_VOLUME_REDUCTION] ?: true,
            
            // Advanced Safe Playback
            weeklyExposureMinutes = weeklyStatsMap,
            forcedRestEnabled = preferences[PreferencesKeys.FORCED_REST_ENABLED] ?: false,
            isResting = preferences[PreferencesKeys.IS_RESTING] ?: false,
            restEndTime = preferences[PreferencesKeys.REST_END_TIME] ?: 0L,
            dbMonitoringEnabled = preferences[PreferencesKeys.DB_MONITORING_ENABLED] ?: true,
            safePlaybackNotificationsEnabled = preferences[PreferencesKeys.SAFE_PLAYBACK_NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun updateGaplessEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.GAPLESS_ENABLED] = enabled
        }
    }

    suspend fun updateMonoEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.MONO_ENABLED] = enabled
        }
    }

    suspend fun updateNormalizationEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.NORMALIZATION_ENABLED] = enabled
        }
    }

    suspend fun updateNormalizationLevel(level: NormalizationLevel) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.NORMALIZATION_LEVEL] = level.name
        }
    }

    suspend fun updateAudioFocusEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FOCUS_ENABLED] = enabled
        }
    }

    suspend fun updateEqualizerEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.EQUALIZER_ENABLED] = enabled
        }
    }

    suspend fun updateBandLevel(bandIndex: Int, level: Int) {
        context.audioDataStore.edit { preferences ->
            val bandLevelsString = preferences[PreferencesKeys.EQUALIZER_BAND_LEVELS] ?: ""
            val currentMap = if (bandLevelsString.isBlank()) mutableMapOf() else {
                bandLevelsString.split(",").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val band = parts[0].toIntOrNull()
                        val lvl = parts[1].toIntOrNull()
                        if (band != null && lvl != null) band to lvl else null
                    } else null
                }.toMap().toMutableMap()
            }
            currentMap[bandIndex] = level
            val serialized = currentMap.map { "${it.key}:${it.value}" }.joinToString(",")
            preferences[PreferencesKeys.EQUALIZER_BAND_LEVELS] = serialized
        }
    }

    suspend fun updateBassBoostStrength(strength: Int) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.BASS_BOOST_STRENGTH] = strength
        }
    }

    suspend fun updateVirtualizerStrength(strength: Int) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.VIRTUALIZER_STRENGTH] = strength
        }
    }

    suspend fun updateCrossfadeEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.CROSSFADE_ENABLED] = enabled
        }
    }

    suspend fun updateCrossfadeDuration(seconds: Int) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.CROSSFADE_DURATION] = seconds
        }
    }

    suspend fun updateBluetoothAutoplayEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.BLUETOOTH_AUTOPLAY_ENABLED] = enabled
        }
    }

    suspend fun updateBluetoothAutoplayDevices(devices: Set<String>) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.BLUETOOTH_AUTOPLAY_DEVICES] = devices
        }
    }

    suspend fun updateSafePlaybackEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.SAFE_PLAYBACK_ENABLED] = enabled
        }
    }

    suspend fun updateIgnoreSpeakerExposure(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.IGNORE_SPEAKER_EXPOSURE] = enabled
        }
    }

    suspend fun updateUserAge(age: Int) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_AGE] = age
        }
    }

    suspend fun updateDailyExposureMinutes(minutes: Int) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_EXPOSURE_MINUTES] = minutes
            preferences[PreferencesKeys.LAST_EXPOSURE_RESET_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun updateIntelligentVolumeReduction(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.INTELLIGENT_VOLUME_REDUCTION] = enabled
        }
    }

    suspend fun updateWeeklyExposure(dayOfWeek: Int, minutes: Int) {
        context.audioDataStore.edit { preferences ->
            val currentStatsString = preferences[PreferencesKeys.WEEKLY_EXPOSURE_MINUTES] ?: ""
            val currentMap = if (currentStatsString.isBlank()) mutableMapOf() else {
                currentStatsString.split(",").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val day = parts[0].toIntOrNull()
                        val mins = parts[1].toIntOrNull()
                        if (day != null && mins != null) day to mins else null
                    } else null
                }.toMap().toMutableMap()
            }
            currentMap[dayOfWeek] = minutes
            val serialized = currentMap.map { "${it.key}:${it.value}" }.joinToString(",")
            preferences[PreferencesKeys.WEEKLY_EXPOSURE_MINUTES] = serialized
        }
    }

    suspend fun updateForcedRestEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.FORCED_REST_ENABLED] = enabled
        }
    }

    suspend fun updateIsResting(resting: Boolean, endTime: Long = 0L) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_RESTING] = resting
            preferences[PreferencesKeys.REST_END_TIME] = endTime
        }
    }

    suspend fun updateDbMonitoringEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.DB_MONITORING_ENABLED] = enabled
        }
    }

    suspend fun updateSafePlaybackNotificationsEnabled(enabled: Boolean) {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.SAFE_PLAYBACK_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun resetEqualizer() {
        context.audioDataStore.edit { preferences ->
            preferences[PreferencesKeys.EQUALIZER_BAND_LEVELS] = ""
            preferences[PreferencesKeys.BASS_BOOST_STRENGTH] = 0
            preferences[PreferencesKeys.VIRTUALIZER_STRENGTH] = 0
        }
    }
}
