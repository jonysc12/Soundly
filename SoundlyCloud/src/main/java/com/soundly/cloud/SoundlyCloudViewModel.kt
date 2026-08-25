package com.soundly.cloud

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.soundly.cloud.R
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ==================== VIEWMODEL ====================
@HiltViewModel
class SoundlyCloudViewModel @Inject constructor(
    private val cloudRepository: CloudRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery, isSearchCommitted = false)
        suggestionJob?.cancel()
        if (newQuery.length > 1) {
            suggestionJob = viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                fetchSuggestions(newQuery)
            }
        } else {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
        }
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getSuggestionExtractor()
                val suggestions = extractor.suggestionList(query)
                _uiState.value = _uiState.value.copy(suggestions = suggestions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onBackToSearch() {
        if (_uiState.value.artistDetailState != null) {
            _uiState.value = _uiState.value.copy(artistDetailState = null)
        } else if (_uiState.value.detailState != null) {
            _uiState.value = _uiState.value.copy(detailState = null)
        } else {
            _uiState.value = _uiState.value.copy(
                isSearchCommitted = false,
                results = emptyList(),
                suggestions = emptyList()
            )
        }
    }

    fun loadDetail(id: String, title: String, uploader: String, thumbnailUrl: String, type: ResultType) {
        _uiState.value = _uiState.value.copy(
            detailState = DetailUiState(id, title, uploader, thumbnailUrl, isLoading = true, type = type)
        )
        
        viewModelScope.launch {
            try {
                val songs = cloudRepository.getPlaylistDetail(id, title, uploader)
                _uiState.value = _uiState.value.copy(
                    detailState = _uiState.value.detailState?.copy(items = songs, isLoading = false)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    detailState = _uiState.value.detailState?.copy(isLoading = false)
                )
            }
        }
    }

    fun onCategorySelected(category: SearchCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        if (_uiState.value.isSearchCommitted && _uiState.value.query.isNotBlank()) {
            search(_uiState.value.query)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val input = query.trim()
            _uiState.value = _uiState.value.copy(
                query = input,
                isLoading = true, 
                error = null, 
                isSearchCommitted = true,
                suggestions = emptyList()
            )

            if (isYouTubeUrl(input) && input.contains("list=")) {
                loadPlaylistFromUrl(input)
                return@launch
            }

            try {
                val results = cloudRepository.search(input, _uiState.value.selectedCategory)
                _uiState.value = _uiState.value.copy(results = results, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun loadPlaylistFromUrl(url: String) {
        try {
            val detail = cloudRepository.getPlaylistInfo(url)
            _uiState.update { it.copy(detailState = detail.copy(isLoading = true), isLoading = false) }
            loadDetail(detail.id, detail.title, detail.uploader, detail.thumbnailUrl, ResultType.PLAYLIST)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isLoading = false, error = "Error al cargar playlist: ${e.message}") }
        }
    }

    private fun isYouTubeUrl(text: String): Boolean = text.contains("youtube.com") || text.contains("youtu.be")

    fun loadArtistDetail(artist: Artist) {
        _uiState.update { it.copy(artistDetailState = ArtistDetailUiState(isLoading = true, name = artist.name, avatarUrl = artist.thumbnailUrl)) }
        viewModelScope.launch(Dispatchers.IO) {
            val detail = cloudRepository.getArtistDetail(artist)
            if (detail != null) {
                _uiState.update { it.copy(artistDetailState = detail) }
            } else {
                _uiState.update { it.copy(artistDetailState = null) }
            }
        }
    }

    fun downloadSong(context: Context, song: Song) {
        viewModelScope.launch {
            cloudRepository.downloadSong(context, song)
        }
    }

    fun downloadPlaylist(context: Context, songs: List<Song>) {
        viewModelScope.launch {
            songs.forEach { song ->
                cloudRepository.downloadSong(context, song)
                kotlinx.coroutines.delay(500)
            }
        }
    }
}
