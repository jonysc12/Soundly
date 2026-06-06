package com.soundly.feature.biblioteca

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soundly.ui.componentes.LibraryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bibliotecaUiDataStore by preferencesDataStore(name = "biblioteca_ui_prefs")

object BibliotecaUiPreferences {
    private val selectedFilterKey = stringPreferencesKey("selected_filter")

    fun selectedFilterFlow(context: Context): Flow<LibraryFilter?> {
        return context.bibliotecaUiDataStore.data.map { preferences ->
            preferences[selectedFilterKey]?.let(::filterFromStorage)
        }
    }

    suspend fun setSelectedFilter(
        context: Context,
        filter: LibraryFilter?
    ) {
        context.bibliotecaUiDataStore.edit { preferences ->
            if (filter == null) {
                preferences.remove(selectedFilterKey)
            } else {
                preferences[selectedFilterKey] = filterToStorage(filter)
            }
        }
    }

    private fun filterToStorage(filter: LibraryFilter): String = when (filter) {
        LibraryFilter.PLAYLISTS -> "playlists"
        LibraryFilter.ALBUMS -> "albums"
        LibraryFilter.ARTISTS -> "artists"
        LibraryFilter.FOLDERS -> "folders"
    }

    private fun filterFromStorage(value: String): LibraryFilter? = when (value) {
        "playlists" -> LibraryFilter.PLAYLISTS
        "albums" -> LibraryFilter.ALBUMS
        "artists" -> LibraryFilter.ARTISTS
        "folders" -> LibraryFilter.FOLDERS
        else -> null
    }

    private val pinnedPlaylistsKey = stringSetPreferencesKey("pinned_playlists")
    private val pinnedAlbumsKey = stringSetPreferencesKey("pinned_albums")
    private val pinnedArtistsKey = stringSetPreferencesKey("pinned_artists")
    private val pinnedFoldersKey = stringSetPreferencesKey("pinned_folders")

    fun pinnedIdsFlow(context: Context, type: String): Flow<Set<String>> {
        val key = when (type) {
            "playlist" -> pinnedPlaylistsKey
            "album" -> pinnedAlbumsKey
            "artist" -> pinnedArtistsKey
            "folder" -> pinnedFoldersKey
            else -> return kotlinx.coroutines.flow.flowOf(emptySet())
        }
        return context.bibliotecaUiDataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }

    suspend fun togglePin(context: Context, type: String, id: String) {
        val key = when (type) {
            "playlist" -> pinnedPlaylistsKey
            "album" -> pinnedAlbumsKey
            "artist" -> pinnedArtistsKey
            "folder" -> pinnedFoldersKey
            else -> return
        }
        context.bibliotecaUiDataStore.edit { preferences ->
            val current = preferences[key] ?: emptySet()
            if (id in current) {
                preferences[key] = current - id
            } else {
                preferences[key] = current + id
            }
        }
    }

    suspend fun removePin(context: Context, type: String, id: String) {
        val key = when (type) {
            "playlist" -> pinnedPlaylistsKey
            "album" -> pinnedAlbumsKey
            "artist" -> pinnedArtistsKey
            "folder" -> pinnedFoldersKey
            else -> return
        }
        context.bibliotecaUiDataStore.edit { preferences ->
            val current = preferences[key] ?: emptySet()
            if (id in current) {
                preferences[key] = current - id
            }
        }
    }
}
