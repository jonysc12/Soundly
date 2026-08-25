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
import com.soundly.data.service.PlaylistExportService
import com.soundly.data.service.PlaylistImportResult
import com.soundly.data.service.PlaylistImportService
import com.soundly.feature.library.LibrarySortOption
import com.soundly.feature.library.ArtistsLayoutMode
import com.soundly.feature.library.LibraryUiPreferences
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LibrarySnapshot(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val songsByAlbumId: Map<Long, List<Song>> = emptyMap(),
    val songsByArtistId: Map<Long, List<Song>> = emptyMap(),
    val artistPrimaryAlbumId: Map<Long, Long> = emptyMap()
)

sealed class PlaylistExportResult {
    object Idle : PlaylistExportResult()
    object Loading : PlaylistExportResult()
    object Success : PlaylistExportResult()
    data class Error(val message: String) : PlaylistExportResult()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val artistRepository: com.soundly.data.repository.ArtistRepository,
    private val importService: PlaylistImportService,
    private val exportService: PlaylistExportService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _importState = MutableStateFlow<PlaylistImportResult>(PlaylistImportResult.Idle)
    val importState: StateFlow<PlaylistImportResult> = _importState.asStateFlow()

    private val _exportState = MutableStateFlow<PlaylistExportResult>(PlaylistExportResult.Idle)
    val exportState: StateFlow<PlaylistExportResult> = _exportState.asStateFlow()

    private val _librarySnapshot = repository.libraryCatalogFlow
        .map { catalog ->
            LibrarySnapshot(
                songs = catalog.songs,
                albums = catalog.albums,
                artists = catalog.artists,
                songsByAlbumId = catalog.songsByAlbumId,
                songsByArtistId = catalog.songsByArtistId,
                artistPrimaryAlbumId = catalog.artistPrimaryAlbumId
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LibrarySnapshot()
        )

    val songs: StateFlow<List<Song>> = _librarySnapshot.map { it.songs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = _librarySnapshot.map { it.albums }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = _librarySnapshot.map { it.artists }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artistPrimaryAlbumId: StateFlow<Map<Long, Long>> = _librarySnapshot.map { it.artistPrimaryAlbumId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    val librarySongs: Flow<PagingData<Song>> =
        songsSortOption.flatMapLatest { sortOption ->
            repository.getSongsPaged(sortOption)
        }.cachedIn(viewModelScope)

    val libraryAlbums: Flow<PagingData<Album>> =
        repository.getAlbumsPaged().cachedIn(viewModelScope)

    val libraryArtists: Flow<PagingData<Artist>> =
        _librarySnapshot
            .map { it.artists }
            .distinctUntilChanged()
            .map { artists -> PagingData.from(artists) }
            .cachedIn(viewModelScope)

    val favoriteSongIds: StateFlow<Set<Long>> = repository.favoriteSongIdsFlow
    val favoriteAlbumIds: StateFlow<Set<Long>> = repository.favoriteAlbumIdsFlow
    val favoriteArtistIds: StateFlow<Set<Long>> = repository.favoriteArtistIdsFlow
    val userPlaylists: StateFlow<List<Playlist>> = repository.playlistsFlow.stateIn(
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

    private val _currentArtistInfo = MutableStateFlow<com.soundly.player.ArtistUiState>(com.soundly.player.ArtistUiState())
    val currentArtistInfo: StateFlow<com.soundly.player.ArtistUiState> = _currentArtistInfo.asStateFlow()

    fun fetchArtistInfo(artistName: String) {
        if (_currentArtistInfo.value.name == artistName) return
        
        viewModelScope.launch {
            _currentArtistInfo.value = com.soundly.player.ArtistUiState(isLoading = true)
            val info = artistRepository.getArtistInfo(artistName)
            if (info != null) {
                _currentArtistInfo.value = com.soundly.player.ArtistUiState(
                    name = info.name,
                    description = info.bio.ifBlank { "Sin biografía disponible" },
                    imageUrl = info.imageUrl ?: ""
                )
            } else {
                _currentArtistInfo.value = _currentArtistInfo.value.copy(isLoading = false)
            }
        }
    }

    val albumsPlaybackQueue: StateFlow<List<Song>> =
        combine(_librarySnapshot, albumsSortOption) { snapshot, sortOption ->
            snapshot to sortOption
        }.mapLatest { (snapshot, sortOption) ->
            withContext(Dispatchers.Default) {
                perfTrace("LibraryViewModel.buildAlbumsPlaybackQueue") {
                    val sortedAlbums = sortAlbumsForDisplay(snapshot.albums, snapshot.songsByAlbumId, sortOption)
                    sortedAlbums
                        .asSequence()
                        .flatMap { album -> snapshot.songsByAlbumId[album.id].orEmpty().asSequence() }
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
        loadLibraryData()
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
        return _librarySnapshot.map { it.songsByAlbumId[albumId].orEmpty() }
    }

    fun getSongsForArtist(artistId: Long): Flow<List<Song>> {
        return _librarySnapshot.map { it.songsByArtistId[artistId].orEmpty() }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return if (playlistId == MusicRepository.TOP_MONTH_RECAP_ID) {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            repository.observeTopSongsInRange(calendar.timeInMillis, 50)
        } else {
            repository.getSongsForPlaylist(playlistId)
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
            val artist = _librarySnapshot.value.artists.find { it.id == artistId }
            val uri = artist?.artworkUri
            if (uri != null) {
                artistArtLruCache.put(artistId, uri)
            }
            uri
        }
    }

    fun setSongsSort(option: LibrarySortOption) {
        viewModelScope.launch(Dispatchers.IO) {
            LibraryUiPreferences.setSongsSort(context, option)
        }
    }

    fun setAlbumsSort(option: LibrarySortOption) {
        viewModelScope.launch(Dispatchers.IO) {
            LibraryUiPreferences.setAlbumsSort(context, option)
        }
    }

    fun toggleArtistsLayout() {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun addSongsToPlaylist(
        playlistId: String,
        songIds: List<Long>
    ) {
        viewModelScope.launch {
            songIds.forEach { songId ->
                repository.addSongToPlaylist(playlistId, songId)
            }
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

    fun togglePlaylistShowOnHome(playlistId: String) {
        viewModelScope.launch {
            repository.togglePlaylistShowOnHome(playlistId)
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

    suspend fun createPlaylist(
        name: String,
        artworkSourceUri: Uri?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            repository.createPlaylist(
                name = name,
                artworkSourceUri = artworkSourceUri
            )
        }
    }

    suspend fun createPlaylistWithSongs(
        name: String,
        artworkSourceUri: Uri?,
        songIds: List<Long>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            repository.createPlaylistWithSongs(name, artworkSourceUri, songIds)
        }
    }

    fun importPlaylist(uri: Uri) {
        viewModelScope.launch {
            _importState.value = PlaylistImportResult.Loading
            val catalog = repository.libraryCatalogFlow.value
            val result = importService.importPlaylist(uri, catalog.songs)
            result.onSuccess { imported ->
                _importState.value = PlaylistImportResult.Success(imported)
            }.onFailure { error ->
                _importState.value = PlaylistImportResult.Error(error.message ?: "Error al importar")
            }
        }
    }

    fun clearImportState() {
        _importState.value = PlaylistImportResult.Idle
    }

    fun exportPlaylist(uri: Uri, songs: List<Song>, format: String) {
        viewModelScope.launch {
            _exportState.value = PlaylistExportResult.Loading
            exportService.exportPlaylist(uri, songs, format)
                .onSuccess {
                    _exportState.value = PlaylistExportResult.Success
                }
                .onFailure { error ->
                    _exportState.value = PlaylistExportResult.Error(error.message ?: "Error al exportar")
                }
        }
    }

    fun clearExportState() {
        _exportState.value = PlaylistExportResult.Idle
    }

    internal fun sortSongsForDisplay(
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
