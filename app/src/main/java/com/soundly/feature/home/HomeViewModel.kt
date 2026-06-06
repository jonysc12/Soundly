package com.soundly.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    val userPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Cache para evitar actualizaciones constantes de secciones "estáticas"
    private var cachedAlbums: List<Album> = emptyList()
    private var cachedTopArtists: List<Artist> = emptyList()
    private var artistPrimaryAlbumMap: Map<Long, Long> = emptyMap()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            combine(
                repository.librarySongsFlow,
                repository.userPlaylistsFlow,
                repository.observeRecentSongs(20),
                repository.observeTopSongs(50)
            ) { allSongs: List<Song>, playlists: List<Playlist>, recentSongs: List<Song>, topSongs: List<Song> ->
                // Mover procesamiento pesado a Default dispatcher
                withContext(Dispatchers.Default) {
                    // Solo actualizamos álbumes y artistas si la biblioteca de canciones cambió significativamente (ej. tamaño)
                    if (cachedAlbums.isEmpty() || allSongs.size != artistPrimaryAlbumMap.size) {
                        val allAlbums = deriveAlbums(allSongs)
                        cachedAlbums = allAlbums.shuffled().take(8)

                        // Mapeo de artista a su primer álbum para la carátula
                        artistPrimaryAlbumMap = allSongs.groupBy { it.artistId }
                            .mapValues { it.value.first().albumId }
                    }

                    // Los artistas más escuchados SÍ pueden actualizarse, pero solo si cambian los topSongs
                    val topArtists = deriveTopArtists(topSongs, allSongs)
                    if (cachedTopArtists.isEmpty() || topArtists != cachedTopArtists) {
                        cachedTopArtists = topArtists
                    }

                    // 2. Recently Played (5 items) - Siempre fresco
                    val recentlyPlayed = recentSongs.take(5)

                    // 4. Recently Added (10 items) - Fresco si cambia la biblioteca
                    val recentlyAdded = allSongs.sortedByDescending { it.dateAdded }.take(10)

                    // 5. Recommended for You (12 items) - Basado en topSongs
                    val recommended = deriveRecommendations(topSongs, allSongs, recentSongs)

                    HomeUiState(
                        discoverAlbums = cachedAlbums,
                        recentlyPlayed = recentlyPlayed,
                        mostListenedArtists = cachedTopArtists,
                        recentlyAdded = recentlyAdded,
                        recommendedSongs = recommended,
                        userPlaylists = playlists,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun deriveAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.albumId }.map { (id, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = id,
                name = first.album,
                artist = first.artist,
                songCount = albumSongs.size
            )
        }
    }

    private fun deriveTopArtists(topSongs: List<Song>, allSongs: List<Song>): List<Artist> {
        val artistCounts = topSongs.groupBy { it.artist }.mapValues { it.value.size }
        val sortedArtists = artistCounts.entries.sortedByDescending { it.value }.map { it.key }
        
        return sortedArtists.take(10).mapNotNull { artistName ->
            val artistSongs = allSongs.filter { it.artist == artistName }
            if (artistSongs.isEmpty()) null
            else {
                val first = artistSongs.first()
                Artist(
                    id = first.artistId,
                    name = artistName,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.distinctBy { it.albumId }.size
                )
            }
        }
    }

    private fun deriveRecommendations(topSongs: List<Song>, allSongs: List<Song>, recentSongs: List<Song>): List<Song> {
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
        return artistPrimaryAlbumMap[artistId]?.let { getAlbumArtUri(it) }
    }
}
