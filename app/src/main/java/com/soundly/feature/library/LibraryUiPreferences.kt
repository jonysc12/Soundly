package com.soundly.feature.library

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soundly.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryUiDataStore by preferencesDataStore(name = "library_ui_prefs")

enum class LibrarySortOption(val storageValue: String, @StringRes val labelResId: Int) {
    TitleAsc("title_asc", R.string.sort_az),
    TitleDesc("title_desc", R.string.sort_za),
    DateAddedDesc("date_added_desc", R.string.sort_recent),
    DateAddedAsc("date_added_asc", R.string.sort_oldest);

    companion object {
        fun fromStorage(value: String?): LibrarySortOption {
            return entries.firstOrNull { it.storageValue == value } ?: TitleAsc
        }
    }
}

enum class ArtistsLayoutMode(val storageValue: String, @StringRes val labelResId: Int) {
    Grid("grid", R.string.layout_grid),
    List("list", R.string.layout_list);

    companion object {
        fun fromStorage(value: String?): ArtistsLayoutMode {
            return entries.firstOrNull { it.storageValue == value } ?: Grid
        }
    }
}

object LibraryUiPreferences {
    private val songsSortKey = stringPreferencesKey("songs_sort")
    private val albumsSortKey = stringPreferencesKey("albums_sort")
    private val artistsLayoutKey = stringPreferencesKey("artists_layout")
    private val favoriteFoldersKey = stringSetPreferencesKey("favorite_folders")

    fun songsSortFlow(context: Context): Flow<LibrarySortOption> {
        return context.libraryUiDataStore.data.map { prefs ->
            LibrarySortOption.fromStorage(prefs[songsSortKey])
        }
    }

    fun albumsSortFlow(context: Context): Flow<LibrarySortOption> {
        return context.libraryUiDataStore.data.map { prefs ->
            LibrarySortOption.fromStorage(prefs[albumsSortKey])
        }
    }

    fun artistsLayoutFlow(context: Context): Flow<ArtistsLayoutMode> {
        return context.libraryUiDataStore.data.map { prefs ->
            ArtistsLayoutMode.fromStorage(prefs[artistsLayoutKey])
        }
    }

    suspend fun setSongsSort(context: Context, value: LibrarySortOption) {
        context.libraryUiDataStore.edit { prefs ->
            prefs[songsSortKey] = value.storageValue
        }
    }

    suspend fun setAlbumsSort(context: Context, value: LibrarySortOption) {
        context.libraryUiDataStore.edit { prefs ->
            prefs[albumsSortKey] = value.storageValue
        }
    }

    suspend fun setArtistsLayout(context: Context, value: ArtistsLayoutMode) {
        context.libraryUiDataStore.edit { prefs ->
            prefs[artistsLayoutKey] = value.storageValue
        }
    }

    fun favoriteFoldersFlow(context: Context): Flow<Set<String>> {
        return context.libraryUiDataStore.data.map { prefs ->
            prefs[favoriteFoldersKey] ?: emptySet()
        }
    }

    suspend fun toggleFavoriteFolder(context: Context, path: String) {
        context.libraryUiDataStore.edit { prefs ->
            val current = prefs[favoriteFoldersKey] ?: emptySet()
            if (path in current) {
                prefs[favoriteFoldersKey] = current - path
            } else {
                prefs[favoriteFoldersKey] = current + path
            }
        }
    }
}
