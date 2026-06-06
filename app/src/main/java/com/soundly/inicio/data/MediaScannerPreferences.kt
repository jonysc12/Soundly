package com.soundly.inicio.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "media_scanner_prefs")

data class MediaScannerSettings(
    val ignoreTempFolders: Boolean = true,
    val ignoreShortAudios: Boolean = false,
    val blockedFolders: Set<String> = emptySet(),
    val blockedSongIds: Set<Long> = emptySet()
)

object MediaScannerPreferences {
    private val IGNORE_TEMP_FOLDERS = booleanPreferencesKey("ignore_temp_folders")
    private val IGNORE_SHORT_AUDIOS = booleanPreferencesKey("ignore_short_audios")
    private val BLOCKED_FOLDERS = stringSetPreferencesKey("blocked_folders")
    private val BLOCKED_SONG_IDS = stringSetPreferencesKey("blocked_song_ids")
    private val SCAN_CONFIRMED = booleanPreferencesKey("scan_confirmed")

    suspend fun saveSettings(context: Context, settings: MediaScannerSettings) {
        context.dataStore.edit { prefs ->
            prefs[IGNORE_TEMP_FOLDERS] = settings.ignoreTempFolders
            prefs[IGNORE_SHORT_AUDIOS] = settings.ignoreShortAudios
            prefs[BLOCKED_FOLDERS] = settings.blockedFolders
            prefs[BLOCKED_SONG_IDS] = settings.blockedSongIds.map { it.toString() }.toSet()
        }
    }

    suspend fun getSettings(context: Context): MediaScannerSettings {
        val prefs = context.dataStore.data.first()
        return MediaScannerSettings(
            ignoreTempFolders = prefs[IGNORE_TEMP_FOLDERS] ?: true,
            ignoreShortAudios = prefs[IGNORE_SHORT_AUDIOS] ?: false,
            blockedFolders = prefs[BLOCKED_FOLDERS] ?: emptySet(),
            blockedSongIds = (prefs[BLOCKED_SONG_IDS] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
        )
    }

    suspend fun setScanConfirmed(context: Context, confirmed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SCAN_CONFIRMED] = confirmed
        }
    }

    suspend fun isScanConfirmed(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[SCAN_CONFIRMED] ?: false
    }
}
