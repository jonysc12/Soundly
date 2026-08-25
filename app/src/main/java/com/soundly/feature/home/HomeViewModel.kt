package com.soundly.feature.home

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.HomeSectionType
import com.soundly.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val discoverAlbums: List<Album> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val mostListenedArtists: List<Artist> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val monthlyRecapSongs: List<Song> = emptyList(),
    val isRecapPeriod: Boolean = false,
    val userPlaylists: List<Playlist> = emptyList(),
    val cloudRecommendations: List<com.soundly.cloud.Song> = emptyList(),
    val downloadProgress: Map<String, Int> = emptyMap(),
    val sectionsOrder: List<HomeSectionType> = emptyList(),
    val showSubtitles: Boolean = true,
    val isCloudEnabled: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val cloudRepository: com.soundly.cloud.CloudRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _artistPrimaryAlbumMap = MutableStateFlow<Map<Long, Long>>(emptyMap())
    private val _dominantColorCache = mutableMapOf<Long, Color>()
    private var lastCatalogVersion = 0
    private var cachedDiscoverAlbums = emptyList<Album>()

    init {
        loadHomeData()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadHomeData() {
        viewModelScope.launch {
            // Breve espera para que los repositorios estabilicen sus estados iniciales
            delay(100)

            val catalogFlow = repository.libraryCatalogFlow
            val playlistsFlow = repository.playlistsFlow
            val recentSongsFlow = repository.observeRecentSongs(10)
            val topSongsFlow = repository.observeTopSongs(50)
            val recentlyAddedFlow = repository.observeRecentlyAdded(15)
            
            // Lógica para Recap Mensual
            val calendar = java.util.Calendar.getInstance()
            val isRecapPeriod = calendar.get(java.util.Calendar.DAY_OF_MONTH) >= 25
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            
            val monthlyRecapFlow = if (isRecapPeriod) {
                repository.observeTopSongsInRange(monthStart, 50)
            } else {
                flowOf(emptyList())
            }

            val homeSectionsOrderFlow = userSettingsRepository.homeSectionsOrderFlow
            val showHomeSectionSubtitlesFlow = userSettingsRepository.showHomeSectionSubtitlesFlow
            val cloudEnabledFlow = userSettingsRepository.cloudEnabledFlow

            val cloudRecommendationsFlow = cloudEnabledFlow.flatMapLatest { enabled ->
                if (enabled) {
                    flow {
                        // Emitimos una lista vacía primero para no bloquear el combine
                        emit(emptyList<com.soundly.cloud.Song>())
                        val trending = cloudRepository.getTrendingSongs()
                        emit(trending)
                    }
                } else {
                    flowOf(emptyList())
                }
            }.onStart { emit(emptyList()) }

            combine(
                catalogFlow,
                playlistsFlow,
                recentSongsFlow,
                topSongsFlow,
                recentlyAddedFlow,
                monthlyRecapFlow,
                homeSectionsOrderFlow,
                showHomeSectionSubtitlesFlow,
                cloudEnabledFlow,
                cloudRecommendationsFlow
            ) { args: Array<Any> ->
                args
            }
            .debounce(200L) 
            .map { args ->
                val catalog = args[0] as com.soundly.data.model.LibraryCatalog
                val playlists = args[1] as List<Playlist>
                val recentSongs = args[2] as List<Song>
                val topSongs = args[3] as List<Song>
                val recentlyAddedItems = args[4] as List<Song>
                val monthlyRecap = args[5] as List<Song>
                val sectionsOrder = args[6] as List<HomeSectionType>
                val showSubtitles = args[7] as Boolean
                val isCloudEnabled = args[8] as Boolean
                val cloudRecommendations = args[9] as List<com.soundly.cloud.Song>

                withContext(Dispatchers.Default) {
                    val allAlbums = catalog.albums
                    
                    val catalogVersion = catalog.songs.size + allAlbums.size
                    if (catalogVersion != lastCatalogVersion || cachedDiscoverAlbums.isEmpty()) {
                        cachedDiscoverAlbums = allAlbums.shuffled().take(8)
                        lastCatalogVersion = catalogVersion
                    }
                    
                    _artistPrimaryAlbumMap.value = catalog.artistPrimaryAlbumId

                    val topArtists = deriveTopArtists(topSongs, catalog)
                    val recommendedItems = deriveRecommendations(topSongs, catalog.songs, recentSongs)

                    HomeUiState(
                        discoverAlbums = cachedDiscoverAlbums,
                        recentlyPlayed = recentSongs,
                        mostListenedArtists = topArtists,
                        recentlyAdded = recentlyAddedItems,
                        recommendedSongs = recommendedItems,
                        monthlyRecapSongs = monthlyRecap,
                        isRecapPeriod = isRecapPeriod,
                        userPlaylists = playlists,
                        cloudRecommendations = cloudRecommendations,
                        sectionsOrder = sectionsOrder,
                        showSubtitles = showSubtitles,
                        isCloudEnabled = isCloudEnabled,
                        isLoading = false
                    )
                }
            }
            .distinctUntilChanged()
            .catch { _ ->
                _uiState.update { it.copy(isLoading = false) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun deriveTopArtists(topSongs: List<Song>, catalog: com.soundly.data.model.LibraryCatalog): List<Artist> {
        if (topSongs.isEmpty()) return emptyList()
        
        // Split collab artists to count them individually, consistent with LibraryCatalog
        val allArtistOccurrences = topSongs.flatMap { it.artistNames }
        val artistCounts = allArtistOccurrences.groupingBy { it }.eachCount()
        val sortedArtistNames = artistCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
        
        val artistsById = catalog.artists.associateBy { it.id }
        
        return sortedArtistNames.take(10).mapNotNull { name ->
            val artistId = com.soundly.data.model.generateArtistId(name)
            val artist = artistsById[artistId]
            artist?.copy(
                artworkUri = catalog.artistPrimaryAlbumId[artistId]?.let { getAlbumArtUri(it) }
            )
        }
    }

    private fun deriveRecommendations(topSongs: List<Song>, allSongs: List<Song>, recentSongs: List<Song>): List<Song> {
        if (allSongs.isEmpty()) return emptyList()
        val topArtistNames = topSongs.take(10).map { it.artist }.distinct().take(3)
        val recentIds = recentSongs.map { it.id }.toSet()
        
        val recs = allSongs.filter { it.artist in topArtistNames && it.id !in recentIds }
            .shuffled()
            .take(12)
        
        return if (recs.size < 12) {
            val fill = allSongs.filter { it.id !in recentIds && it.id !in recs.map { r -> r.id } }
                .shuffled()
                .take(12 - recs.size)
            recs + fill
        } else recs
    }

    fun getAlbumArtUri(albumId: Long): Uri = repository.getAlbumArtUri(albumId)
    
    fun getArtistArtUri(artistId: Long): Uri? {
        return _artistPrimaryAlbumMap.value[artistId]?.let { getAlbumArtUri(it) }
    }

    fun getAlbumSongs(albumId: Long): List<Song> {
        return repository.librarySongsFlow.value.filter { it.albumId == albumId }
    }

    fun playCloudSong(cloudSong: com.soundly.cloud.Song, playbackViewModel: com.soundly.player.PlaybackViewModel) {
        viewModelScope.launch {
            val resolvedSong = withContext(Dispatchers.IO) {
                if (cloudSong.streamUrl != null) cloudSong
                else cloudRepository.resolveSingleUrl(cloudSong.videoUrl)
            }
            
            resolvedSong?.let { s ->
                val localSong = Song(
                    id = s.id.hashCode().toLong(),
                    title = s.title,
                    artist = s.artist,
                    artistId = 0L,
                    album = s.album.ifEmpty { "Cloud" },
                    albumId = 0L,
                    dateAdded = System.currentTimeMillis(),
                    duration = s.durationSeconds * 1000L,
                    path = s.streamUrl ?: ""
                )
                playbackViewModel.play(localSong, listOf(localSong))
            }
        }
    }

    fun downloadCloudSong(context: android.content.Context, song: com.soundly.cloud.Song) {
        if (_uiState.value.downloadProgress.containsKey(song.id)) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(downloadProgress = it.downloadProgress + (song.id to -1)) }
            cloudRepository.downloadSong(context, song) { p ->
                _uiState.update { it.copy(downloadProgress = it.downloadProgress + (song.id to p)) }
            }
            // Remove from progress map after short delay when finished
            delay(2000)
            _uiState.update { it.copy(downloadProgress = it.downloadProgress - song.id) }
        }
    }

    suspend fun getDominantColor(albumId: Long, bitmap: android.graphics.Bitmap): Color {
        return _dominantColorCache[albumId] ?: run {
            val color = extractDominantColorSyncInternal(bitmap)
            _dominantColorCache[albumId] = color
            color
        }
    }
}

private suspend fun extractDominantColorSyncInternal(bitmap: android.graphics.Bitmap): Color {
    return withContext(Dispatchers.Default) {
        val softwareBitmap = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        // OPTIMIZED: Escalar a miniatura para extracción de color ultrarrápida
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(softwareBitmap, 48, 48, false)
        val palette = androidx.palette.graphics.Palette.from(scaledBitmap)
            .maximumColorCount(12)
            .generate()
        val colorInt = palette.getDominantColor(0)
        if (colorInt == 0) Color.Black else Color(colorInt)
    }
}
