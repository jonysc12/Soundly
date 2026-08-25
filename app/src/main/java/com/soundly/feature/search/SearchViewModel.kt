package com.soundly.feature.search

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.UserSettingsRepository
import com.soundly.cloud.CloudRepository
import com.soundly.cloud.SearchCategory
import com.soundly.cloud.ResultType
import com.soundly.cloud.DetailUiState
import com.soundly.cloud.ArtistDetailUiState
import com.soundly.cloud.Song as CloudSong
import com.soundly.cloud.Artist as CloudArtist
import com.soundly.cloud.Album as CloudAlbum
import com.soundly.cloud.Playlist as CloudPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SearchMode { OFFLINE, ONLINE }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val cloudRepository: CloudRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.OFFLINE)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    val isCloudEnabled = userSettingsRepository.cloudEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    private val _searchResult = MutableStateFlow<SearchResult>(SearchResult.Idle)
    val searchResult: StateFlow<SearchResult> = _searchResult.asStateFlow()

    // Estados para la Nube
    private val _cloudResults = MutableStateFlow<List<Any>>(emptyList())
    val cloudResults = _cloudResults.asStateFlow()

    private val _isCloudLoading = MutableStateFlow(false)
    val isCloudLoading = _isCloudLoading.asStateFlow()

    // Estados para Detalles de la Nube (BottomSheets)
    private val _cloudDetailState = MutableStateFlow<DetailUiState?>(null)
    val cloudDetailState = _cloudDetailState.asStateFlow()

    private val _cloudArtistDetailState = MutableStateFlow<ArtistDetailUiState?>(null)
    val cloudArtistDetailState = _cloudArtistDetailState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    // Estados para expandir secciones
    private val _isSongsExpanded = MutableStateFlow(false)
    val isSongsExpanded = _isSongsExpanded.asStateFlow()

    init {
        @OptIn(FlowPreview::class)
        combine(_query, _searchMode, isCloudEnabled) { q, mode, cloudEnabled -> 
            Triple(q, if (cloudEnabled) mode else SearchMode.OFFLINE, cloudEnabled) 
        }
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { (q, mode, cloudEnabled) ->
                val trimmedQuery = q.trim()
                if (trimmedQuery.isBlank()) {
                    _searchResult.value = SearchResult.Idle
                    _cloudResults.value = emptyList()
                    _isSongsExpanded.value = false
                } else {
                    if (mode == SearchMode.OFFLINE) {
                        _searchResult.value = SearchResult.Success(
                            songs = musicRepository.searchSongsPaged(trimmedQuery).cachedIn(viewModelScope),
                            albums = musicRepository.searchAlbumsPaged(trimmedQuery).cachedIn(viewModelScope),
                            artists = musicRepository.searchArtistsPaged(trimmedQuery).cachedIn(viewModelScope),
                            playlists = emptyList(),
                            priority = SearchPriority.SONG
                        )
                    } else if (cloudEnabled) {
                        searchInCloudInternal(trimmedQuery)
                    }
                }
            }
            .launchIn(viewModelScope)
        
        // Reset search mode if cloud is disabled
        isCloudEnabled
            .onEach { enabled ->
                if (!enabled && _searchMode.value == SearchMode.ONLINE) {
                    _searchMode.value = SearchMode.OFFLINE
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery.trimStart() // Permitimos espacios al final mientras escribe, pero no al inicio
    }

    fun onModeChange(newMode: SearchMode) {
        if (isCloudEnabled.value || newMode == SearchMode.OFFLINE) {
            _searchMode.value = newMode
        }
    }

    private fun searchInCloudInternal(q: String) {
        viewModelScope.launch {
            _isCloudLoading.value = true
            try {
                val songsDeferred = async { cloudRepository.search(q, SearchCategory.SONGS) }
                val albumsDeferred = async { cloudRepository.search(q, SearchCategory.ALBUMS) }
                val artistsDeferred = async { cloudRepository.search(q, SearchCategory.ARTISTS) }
                val playlistsDeferred = async { cloudRepository.search(q, SearchCategory.PLAYLISTS) }

                val allResults = mutableListOf<Any>()
                allResults.addAll(songsDeferred.await())
                allResults.addAll(albumsDeferred.await())
                allResults.addAll(artistsDeferred.await())
                allResults.addAll(playlistsDeferred.await())

                _cloudResults.value = allResults
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error buscando en la nube: ${e.message}", e)
            } finally {
                _isCloudLoading.value = false
            }
        }
    }

    fun loadCloudDetail(id: String, title: String, uploader: String, thumbnailUrl: String, type: ResultType) {
        _cloudDetailState.value = DetailUiState(id, title, uploader, thumbnailUrl, isLoading = true, type = type)
        viewModelScope.launch {
            try {
                val songs = cloudRepository.getPlaylistDetail(id, title, uploader)
                _cloudDetailState.value = _cloudDetailState.value?.copy(items = songs, isLoading = false)
            } catch (e: Exception) {
                _cloudDetailState.value = _cloudDetailState.value?.copy(isLoading = false)
            }
        }
    }

    fun loadCloudArtistDetail(artist: CloudArtist) {
        _cloudArtistDetailState.value = ArtistDetailUiState(isLoading = true, name = artist.name, avatarUrl = artist.thumbnailUrl)
        viewModelScope.launch {
            val detail = cloudRepository.getArtistDetail(artist)
            _cloudArtistDetailState.value = detail
        }
    }

    fun downloadSong(context: android.content.Context, song: CloudSong) {
        if (_downloadProgress.value.containsKey(song.id)) return
        
        viewModelScope.launch {
            _downloadProgress.update { it + (song.id to -1) }
            cloudRepository.downloadSong(context, song) { p ->
                _downloadProgress.update { it + (song.id to p) }
            }
            _downloadProgress.update { it - song.id }
        }
    }

    fun dismissCloudDetail() {
        _cloudDetailState.value = null
    }

    fun dismissCloudArtistDetail() {
        _cloudArtistDetailState.value = null
    }

    fun toggleSongsExpansion() {
        _isSongsExpanded.value = !_isSongsExpanded.value
    }
}

enum class SearchPriority {
    ARTIST, ALBUM, SONG, PLAYLIST
}

sealed class SearchResult {
    data object Idle : SearchResult()
    data object Loading : SearchResult()
    data class Success(
        val songs: Flow<PagingData<Song>>,
        val albums: Flow<PagingData<Album>>,
        val artists: Flow<PagingData<Artist>>,
        val playlists: List<Playlist>,
        val priority: SearchPriority
    ) : SearchResult()
}
