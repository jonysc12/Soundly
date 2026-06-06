package com.soundly.feature.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.model.buildLibraryCatalog
import com.soundly.data.repository.MusicRepository
import com.soundly.debug.perfMark
import com.soundly.debug.perfTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _songsByAlbumId = MutableStateFlow<Map<Long, List<Song>>>(emptyMap())
    private val _artistPrimaryAlbumId = MutableStateFlow<Map<Long, Long>>(emptyMap())

    // Usar LruCache para evitar limpiezas totales innecesarias
    private val albumArtLruCache = android.util.LruCache<Long, Uri>(200)
    private val artistArtLruCache = android.util.LruCache<Long, Uri>(100)

    val songsSortOption: StateFlow<LibrarySortOption> =
        LibraryUiPreferences.songsSortFlow(context).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibrarySortOption.TitleAsc
        )

    val albumsSortOption: StateFlow<LibrarySortOption> =
        LibraryUiPreferences.albumsSortFlow(context).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibrarySortOption.TitleAsc
        )

    val artistsLayoutMode: StateFlow<ArtistsLayoutMode> =
        LibraryUiPreferences.artistsLayoutFlow(context).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtistsLayoutMode.Grid
        )

    val librarySongs: StateFlow<List<Song>> =
        combine(_songs, songsSortOption) { songs, sortOption ->
            songs to sortOption
        }.mapLatest { (songs, sortOption) ->
            withContext(Dispatchers.Default) {
                perfTrace("LibraryViewModel.sortSongs.${sortOption.storageValue}") {
                    sortSongsForDisplay(songs, sortOption)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val libraryAlbums: StateFlow<List<Album>> =
        combine(_albums, _songsByAlbumId, albumsSortOption) { albums, songsByAlbum, sortOption ->
            Triple(albums, songsByAlbum, sortOption)
        }.mapLatest { (albums, songsByAlbum, sortOption) ->
            withContext(Dispatchers.Default) {
                perfTrace("LibraryViewModel.sortAlbums.${sortOption.storageValue}") {
                    sortAlbumsForDisplay(albums, songsByAlbum, sortOption)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val libraryArtists: StateFlow<List<Artist>> = artists
    val favoriteSongIds: StateFlow<Set<Long>> = repository.favoriteSongIdsFlow
    val favoriteAlbumIds: StateFlow<Set<Long>> = repository.favoriteAlbumIdsFlow
    val favoriteArtistIds: StateFlow<Set<Long>> = repository.favoriteArtistIdsFlow
    val userPlaylists: StateFlow<List<Playlist>> = repository.userPlaylistsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val playlistMembershipBySong: StateFlow<Map<Long, Set<String>>> =
        repository.playlistMembershipBySongFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val albumsPlaybackQueue: StateFlow<List<Song>> =
        combine(libraryAlbums, _songsByAlbumId) { albums, songsByAlbum ->
            albums to songsByAlbum
        }.mapLatest { (albums, songsByAlbum) ->
            withContext(Dispatchers.Default) {
                perfTrace("LibraryViewModel.buildAlbumsPlaybackQueue") {
                    albums
                        .asSequence()
                        .flatMap { album -> songsByAlbum[album.id].orEmpty().asSequence() }
                        .distinctBy { it.id }
                        .toList()
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        observeLibraryData()
        loadLibraryData()
    }

    private fun observeLibraryData() {
        viewModelScope.launch {
            repository.librarySongsFlow.collectLatest { result ->
                if (result.isEmpty() && songs.value.isEmpty()) return@collectLatest
                perfMark("librarySongsFlow emission size=${result.size}")
                val snapshot = withContext(Dispatchers.Default) {
                    perfTrace("LibraryViewModel.buildLibrarySnapshot") {
                        buildLibraryCatalog(result)
                    }
                }

                _songs.value = snapshot.songs
                _songsByAlbumId.value = snapshot.songsByAlbumId
                _artistPrimaryAlbumId.value = snapshot.artistPrimaryAlbumId
                _albums.value = snapshot.albums
                _artists.value = snapshot.artists
            }
        }
    }

    fun loadLibraryData() {
        viewModelScope.launch {
            repository.ensureLibrarySongsLoaded()
        }
    }

    fun refreshLibraryData() {
        viewModelScope.launch {
            repository.refreshLibrarySongs()
        }
    }

    fun loadSongs() {
        loadLibraryData()
    }

    fun loadAlbums() {
        loadLibraryData()
    }

    fun loadArtists() {
        loadLibraryData()
    }

    fun getSongsForAlbum(albumId: Long): Flow<List<Song>> {
        return _songsByAlbumId.map { currentByAlbum ->
            currentByAlbum[albumId].orEmpty()
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
            val uri = _artistPrimaryAlbumId.value[artistId]?.let(::getAlbumArtUri)
            if (uri != null) {
                artistArtLruCache.put(artistId, uri)
            }
            uri
        }
    }

    fun setSongsSort(option: LibrarySortOption) {
        viewModelScope.launch {
            LibraryUiPreferences.setSongsSort(context, option)
        }
    }

    fun setAlbumsSort(option: LibrarySortOption) {
        viewModelScope.launch {
            LibraryUiPreferences.setAlbumsSort(context, option)
        }
    }

    fun toggleArtistsLayout() {
        viewModelScope.launch {
            val next = when (artistsLayoutMode.value) {
                ArtistsLayoutMode.Grid -> ArtistsLayoutMode.List
                ArtistsLayoutMode.List -> ArtistsLayoutMode.Grid
            }
            LibraryUiPreferences.setArtistsLayout(context, next)
        }
    }

    fun toggleSongFavorite(songId: Long) {
        viewModelScope.launch {
            repository.toggleSongFavorite(songId)
        }
    }

    fun addSongToPlaylist(
        playlistId: String,
        songId: Long
    ) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(
        playlistId: String,
        songId: Long
    ) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun hideSong(songId: Long) {
        viewModelScope.launch {
            repository.hideSong(songId)
        }
    }

    fun toggleAlbumFavorite(albumId: Long) {
        viewModelScope.launch {
            repository.toggleAlbumFavorite(albumId)
        }
    }

    fun toggleArtistFavorite(artistId: Long) {
        viewModelScope.launch {
            repository.toggleArtistFavorite(artistId)
        }
    }

    private fun sortSongsForDisplay(
        songs: List<Song>,
        sortOption: LibrarySortOption
    ): List<Song> {
        return when (sortOption) {
            LibrarySortOption.TitleAsc -> songs.sortedWith(
                compareBy<Song> { it.title.lowercase() }.thenBy { it.id }
            )

            LibrarySortOption.TitleDesc -> songs.sortedWith(
                compareByDescending<Song> { it.title.lowercase() }.thenByDescending { it.id }
            )

            LibrarySortOption.DateAddedAsc -> songs.sortedWith(
                compareBy<Song> { it.dateAdded }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.id }
            )

            LibrarySortOption.DateAddedDesc -> songs.sortedWith(
                compareByDescending<Song> { it.dateAdded }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.id }
            )
        }
    }

    private fun sortAlbumsForDisplay(
        albums: List<Album>,
        songsByAlbum: Map<Long, List<Song>>,
        sortOption: LibrarySortOption
    ): List<Album> {
        return when (sortOption) {
            LibrarySortOption.TitleAsc -> albums.sortedWith(
                compareBy<Album> { it.name.lowercase() }.thenBy { it.id }
            )

            LibrarySortOption.TitleDesc -> albums.sortedWith(
                compareByDescending<Album> { it.name.lowercase() }.thenByDescending { it.id }
            )

            LibrarySortOption.DateAddedAsc -> albums.sortedWith(
                compareBy<Album> { album ->
                    songsByAlbum[album.id].orEmpty().maxOfOrNull { it.dateAdded } ?: 0L
                }.thenBy { it.name.lowercase() }.thenBy { it.id }
            )

            LibrarySortOption.DateAddedDesc -> albums.sortedWith(
                compareByDescending<Album> { album ->
                    songsByAlbum[album.id].orEmpty().maxOfOrNull { it.dateAdded } ?: 0L
                }.thenBy { it.name.lowercase() }.thenBy { it.id }
            )
        }
    }

}
