package com.soundly.feature.biblioteca

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.FolderSummary
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.model.buildLibraryCatalog
import com.soundly.data.repository.MusicRepository
import com.soundly.inicio.data.ProfilePreferences
import com.soundly.ui.componentes.LibraryFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BibliotecaViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val albumArtLruCache = android.util.LruCache<Long, Uri>(100)
    private val artistArtLruCache = android.util.LruCache<Long, Uri>(100)
    private val _userImageUri = MutableStateFlow<Uri?>(null)
    private val _userName = MutableStateFlow("")
    val userImageUri: StateFlow<Uri?> = _userImageUri.asStateFlow()
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val catalog = repository.librarySongsFlow
        .mapLatest { songs ->
            withContext(Dispatchers.Default) {
                buildLibraryCatalog(songs)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildLibraryCatalog(emptyList())
        )

    val selectedFilter: StateFlow<LibraryFilter?> =
        BibliotecaUiPreferences.selectedFilterFlow(context).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val favoriteFolderPaths: StateFlow<Set<String>> =
        com.soundly.feature.library.LibraryUiPreferences.favoriteFoldersFlow(context).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val pinnedPlaylistIds: StateFlow<Set<String>> =
        BibliotecaUiPreferences.pinnedIdsFlow(context, "playlist").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val pinnedAlbumIds: StateFlow<Set<String>> =
        BibliotecaUiPreferences.pinnedIdsFlow(context, "album").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val pinnedArtistIds: StateFlow<Set<String>> =
        BibliotecaUiPreferences.pinnedIdsFlow(context, "artist").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val pinnedFolderPaths: StateFlow<Set<String>> =
        BibliotecaUiPreferences.pinnedIdsFlow(context, "folder").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val playlists: StateFlow<List<Playlist>> =
        combine(repository.playlistsFlow, pinnedPlaylistIds) { list, pinned ->
            list.sortedWith(
                compareByDescending<Playlist> { it.id in pinned }
                    .thenByDescending { it.updatedAt }
                    .thenBy { it.name.lowercase() }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val favoriteAlbums: StateFlow<List<Album>> =
        combine(catalog, repository.favoriteAlbumIdsFlow, pinnedAlbumIds) { libraryCatalog, favoriteIds, pinned ->
            libraryCatalog.albums
                .filter { it.id in favoriteIds }
                .sortedWith(
                    compareByDescending<Album> { it.id.toString() in pinned }
                        .thenBy { it.name.lowercase() }
                )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val favoriteArtists: StateFlow<List<Artist>> =
        combine(catalog, repository.favoriteArtistIdsFlow, pinnedArtistIds) { libraryCatalog, favoriteIds, pinned ->
            libraryCatalog.artists
                .filter { it.id in favoriteIds }
                .sortedWith(
                    compareByDescending<Artist> { it.id.toString() in pinned }
                        .thenBy { it.name.lowercase() }
                )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val folders: StateFlow<List<FolderSummary>> =
        combine(catalog, pinnedFolderPaths) { cat, pinned ->
            cat.folders.sortedWith(
                compareByDescending<FolderSummary> { it.path in pinned }
                    .thenBy { it.name.lowercase() }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureLibrarySongsLoaded()
        }
        viewModelScope.launch {
            _userImageUri.value = ProfilePreferences.getImageUri(context)
            _userName.value = ProfilePreferences.getUsername(context)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshLibrarySongs()
        }
    }

    fun setSelectedFilter(filter: LibraryFilter?) {
        viewModelScope.launch {
            BibliotecaUiPreferences.setSelectedFilter(context, filter)
        }
    }

    suspend fun createPlaylist(
        name: String,
        artworkSourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            repository.createPlaylist(
                name = name,
                artworkSourceUri = artworkSourceUri
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            BibliotecaUiPreferences.removePin(context, "playlist", playlistId)
        }
    }

    suspend fun updatePlaylist(
        id: String,
        name: String,
        artworkSourceUri: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            repository.updatePlaylist(id, name, artworkSourceUri)
        }
    }

    fun togglePin(type: String, id: String) {
        viewModelScope.launch {
            BibliotecaUiPreferences.togglePin(context, type, id)
        }
    }

    fun toggleAlbumFavorite(albumId: Long) {
        viewModelScope.launch {
            repository.toggleAlbumFavorite(albumId)
            // If it was pinned, we ensure it's unpinned when removed from favs
            BibliotecaUiPreferences.removePin(context, "album", albumId.toString())
        }
    }

    fun toggleArtistFavorite(artistId: Long) {
        viewModelScope.launch {
            repository.toggleArtistFavorite(artistId)
            // If it was pinned, we ensure it's unpinned when removed from favs
            BibliotecaUiPreferences.removePin(context, "artist", artistId.toString())
        }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return repository.getSongsForPlaylist(playlistId)
    }

    fun getSongsForFolder(folderPath: String): Flow<List<Song>> {
        return catalog.mapLatest { cat ->
            cat.songs.filter { song ->
                val normalized = song.path.replace('\\', '/')
                normalized.substringBeforeLast('/', missingDelimiterValue = normalized) == folderPath
            }
        }
    }

    fun toggleFavoriteFolder(folderPath: String) {
        viewModelScope.launch {
            com.soundly.feature.library.LibraryUiPreferences.toggleFavoriteFolder(context, folderPath)
            // If it was pinned, we ensure it's unpinned when removed from favs
            BibliotecaUiPreferences.removePin(context, "folder", folderPath)
        }
    }

    fun getAlbumArtUri(albumId: Long): Uri {
        return albumArtLruCache.get(albumId) ?: run {
            val uri = repository.getAlbumArtUri(albumId)
            albumArtLruCache.put(albumId, uri)
            uri
        }
    }

    fun getArtistArtUri(artistId: Long): Uri? {
        return artistArtLruCache.get(artistId) ?: run {
            // Intenta obtener el arte del primer álbum del artista
            val uri = catalog.value.artistPrimaryAlbumId[artistId]?.let(::getAlbumArtUri)
            if (uri != null) {
                artistArtLruCache.put(artistId, uri)
            }
            uri
        }
    }
}
