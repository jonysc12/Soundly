package com.soundly.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepository @Inject constructor() {

    private var isInitialized = false

    private fun ensureInitialized() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    try {
                        android.util.Log.d("CloudRepository", "Inicializando NewPipe...")
                        // Forzamos la inicialización en un hilo seguro
                        NewPipe.init(NewPipeDownloader(), Localization.DEFAULT)
                        isInitialized = true
                        android.util.Log.d("CloudRepository", "NewPipe inicializado correctamente")
                    } catch (e: Exception) {
                        if (e.message?.contains("already initialized") == true) {
                            isInitialized = true
                        } else {
                            android.util.Log.e("CloudRepository", "Error al inicializar NewPipe: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    suspend fun search(query: String, category: SearchCategory): List<Any> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        ensureInitialized()
        
        try {
            android.util.Log.d("CloudRepository", "Buscando en la nube: $query en categoría ${category.name}")
            val results = if (isYouTubeUrl(query)) {
                val song = resolveSingleUrl(query)
                if (song != null) listOf(song) else emptyList()
            } else {
                performSearch(query, category)
            }
            android.util.Log.d("CloudRepository", "Búsqueda finalizada. Resultados encontrados: ${results.size}")
            results
        } catch (e: Exception) {
            android.util.Log.e("CloudRepository", "Error en search: ${e.message}", e)
            emptyList()
        }
    }

    private fun isYouTubeUrl(text: String): Boolean = text.contains("youtube.com") || text.contains("youtu.be")

    fun resolveSingleUrl(url: String): Song? {
        ensureInitialized()
        return try {
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            val info = StreamInfo.getInfo(extractor)
            val bestAudio = info.audioStreams.filter { it.format?.name?.contains("M4A", true) == true }.maxByOrNull { it.averageBitrate } ?: info.audioStreams.maxByOrNull { it.averageBitrate }
            
            val bestThumb = info.thumbnails.maxByOrNull { it.height * it.width }
            val thumbUrl = bestThumb?.url ?: ""
            
            val cleanArtist = (info.uploaderName ?: "Desconocido").cleanYouTubeArtist()
            val cleanTitle = (info.name ?: "Sin título").trim()

            val isTopic = info.uploaderName?.endsWith(" - Topic", ignoreCase = true) == true
            val isSquare = bestThumb?.let { it.width.toFloat() / it.height.toFloat() < 1.2f } ?: false

            Song(
                id = info.id, title = cleanTitle, artist = cleanArtist, album = cleanTitle,
                duration = info.duration.toMinSec(), thumbnailUrl = thumbUrl,
                streamUrl = bestAudio?.content, videoUrl = url,
                durationSeconds = info.duration,
                isM4A = bestAudio?.format?.name?.contains("M4A", true) == true,
                resultType = if (isTopic || isSquare) ResultType.SONG else ResultType.VIDEO
            )
        } catch (e: Exception) { 
            android.util.Log.e("CloudRepository", "Error resolveSingleUrl: ${e.message}", e)
            null 
        }
    }

    private fun performSearch(query: String, category: SearchCategory): List<Any> {
        ensureInitialized()
        return try {
            val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(category.filter), "music")
            extractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(extractor)

            android.util.Log.d("CloudRepository", "Items relacionados encontrados: ${searchInfo.relatedItems.size}")

            searchInfo.relatedItems.mapNotNull { item ->
                when (item) {
                    is StreamInfoItem -> {
                        val isOfficialSong = item.uploaderName?.endsWith(" - Topic", ignoreCase = true) == true || 
                                           item.thumbnails.any { it.width == it.height }
                        
                        val bestThumb = item.thumbnails.find { it.width == it.height } 
                                        ?: item.thumbnails.maxByOrNull { it.height * it.width }
                        
                        val thumbUrl = bestThumb?.url?.forceSquareHighRes() ?: ""

                        Song(
                            id = item.url.substringAfter("v=").substringBefore("&"),
                            title = (item.name ?: "Sin título").trim(),
                            artist = (item.uploaderName ?: "Desconocido").cleanYouTubeArtist(),
                            album = "",
                            duration = item.duration.toMinSec(),
                            thumbnailUrl = thumbUrl,
                            streamUrl = null,
                            videoUrl = item.url,
                            durationSeconds = item.duration,
                            resultType = if (category == SearchCategory.SONGS || isOfficialSong) ResultType.SONG else ResultType.VIDEO
                        )
                    }
                    is ChannelInfoItem -> {
                        val count = item.streamCount.toInt()
                        android.util.Log.d("CloudRepository", "Artista encontrado: ${item.name}, streamCount: $count")
                        Artist(
                            id = item.url.substringAfterLast("/"),
                            name = item.name ?: "Desconocido",
                            thumbnailUrl = item.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
                            url = item.url,
                            songCount = count.coerceAtLeast(0),
                            albumCount = 0
                        )
                    }
                    is PlaylistInfoItem -> {
                        val count = item.streamCount.toInt()
                        android.util.Log.d("CloudRepository", "Playlist/Album encontrado: ${item.name}, streamCount: $count")
                        if (category == SearchCategory.ALBUMS) {
                            Album(
                                id = item.url.substringAfter("list="),
                                title = item.name ?: "Sin título",
                                artist = item.uploaderName ?: "Desconocido",
                                thumbnailUrl = item.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
                                songCount = count.coerceAtLeast(0)
                            )
                        } else {
                            Playlist(
                                id = item.url.substringAfter("list="),
                                title = item.name ?: "Sin título",
                                uploader = item.uploaderName ?: "Desconocido",
                                thumbnailUrl = item.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
                                songCount = count.coerceAtLeast(0)
                            )
                        }
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudRepository", "Error performSearch: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPlaylistDetail(id: String, title: String, uploader: String): List<Song> = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            val url = "https://www.youtube.com/playlist?list=$id"
            val extractor = ServiceList.YouTube.getPlaylistExtractor(url)
            extractor.fetchPage()
            
            val allSongs = mutableListOf<Song>()
            var currentPage = extractor.initialPage
            
            fun collect(items: List<Any>) {
                items.forEach { item ->
                    if (item is StreamInfoItem) {
                        allSongs.add(Song(
                            id = item.url.substringAfter("v=").substringBefore("&"),
                            title = (item.name ?: "Sin título").trim(),
                            artist = (item.uploaderName ?: uploader).cleanYouTubeArtist(),
                            album = title,
                            duration = item.duration.toMinSec(),
                            thumbnailUrl = item.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
                            streamUrl = null,
                            videoUrl = item.url,
                            durationSeconds = item.duration,
                            resultType = ResultType.SONG
                        ))
                    }
                }
            }

            collect(currentPage.items)
            
            var safetyCounter = 0
            while (currentPage.hasNextPage() && allSongs.size < 1000 && safetyCounter < 20) {
                try {
                    currentPage = extractor.getPage(currentPage.nextPage)
                    collect(currentPage.items)
                    safetyCounter++
                } catch (_: Exception) { break }
            }
            
            allSongs
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getArtistDetail(artist: Artist): ArtistDetailUiState? = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            if (artist.url.isEmpty()) return@withContext null
            
            val channelExtractor = ServiceList.YouTube.getChannelExtractor(artist.url)
            channelExtractor.fetchPage()
            val channelInfo = ChannelInfo.getInfo(channelExtractor)
            
            val bannerUrl = channelInfo.banners.maxByOrNull { it.height * it.width }?.url ?: ""
            val avatarUrl = channelInfo.avatars.maxByOrNull { it.height * it.width }?.url ?: artist.thumbnailUrl
            val description = channelInfo.description ?: ""
            val subscriberCount = channelInfo.subscriberCount.toString()
            
            android.util.Log.d("CloudRepository", "Detalle de artista cargado: ${channelInfo.name}. Tabs encontradas: ${channelInfo.tabs.size}")
            
            val songs = mutableListOf<Song>()
            val albums = mutableListOf<Album>()
            val playlists = mutableListOf<Playlist>()
            val videos = mutableListOf<Song>()
            val singles = mutableListOf<Album>()

            channelInfo.tabs.forEachIndexed { index, tab ->
                try {
                    val tabUrl = tab.url ?: ""
                    android.util.Log.d("CloudRepository", "Procesando tab [$index]: $tabUrl")

                    // Ser más permisivo con las tabs para no perder contenido
                    val shouldProcess = index == 0 || 
                                       tabUrl.contains("video", true) || 
                                       tabUrl.contains("album", true) || 
                                       tabUrl.contains("playlist", true) || 
                                       tabUrl.contains("release", true) || 
                                       tabUrl.contains("song", true) || 
                                       tabUrl.contains("music", true) ||
                                       tabUrl.contains("channel", true)

                    if (shouldProcess) {
                        val tabExtractor = ServiceList.YouTube.getChannelTabExtractor(tab)
                        tabExtractor.fetchPage()
                        
                        var currentPage = tabExtractor.initialPage
                        android.util.Log.d("CloudRepository", "Tab [$tabUrl] - Items iniciales: ${currentPage.items.size}")
                        
                        currentPage.items.forEach { processItem(it, songs, albums, playlists, videos, singles) }
                        
                        var pCount = 0
                        while (currentPage.hasNextPage() && pCount < 3) {
                            try {
                                currentPage = tabExtractor.getPage(currentPage.nextPage)
                                android.util.Log.d("CloudRepository", "Tab [$tabUrl] - Página ${pCount + 1} cargada: ${currentPage.items.size} items")
                                currentPage.items.forEach { processItem(it, songs, albums, playlists, videos, singles) }
                                pCount++
                            } catch (_: Exception) { break }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CloudRepository", "Error procesando tab: ${e.message}")
                }
            }

            android.util.Log.d("CloudRepository", "Finalizado proceso de artista. Canciones: ${songs.size}, Álbumes: ${albums.size}, Singles: ${singles.size}, Videos: ${videos.size}, Playlists: ${playlists.size}")

            if (songs.isEmpty() && albums.isEmpty() && playlists.isEmpty()) {
                android.util.Log.w("CloudRepository", "ADVERTENCIA: No se encontró contenido en ninguna tab filtrada. Intentando búsqueda forzada de lanzamientos...")
                try {
                    // Fallback: Si el canal está vacío (común en Topics), buscar sus álbumes y canciones top
                    val releasesQuery = "${artist.name.replace(" - Topic", "").trim()} songs"
                    val searchExt = ServiceList.YouTube.getSearchExtractor(releasesQuery, listOf("music_songs"), "music")
                    searchExt.fetchPage()
                    val info = SearchInfo.getInfo(searchExt)
                    info.relatedItems.forEach { processItem(it, songs, albums, playlists, videos, singles) }
                    
                    // También buscar álbumes
                    val albumsQuery = "${artist.name.replace(" - Topic", "").trim()} albums"
                    val albumExt = ServiceList.YouTube.getSearchExtractor(albumsQuery, listOf("music_albums"), "music")
                    albumExt.fetchPage()
                    val albumInfo = SearchInfo.getInfo(albumExt)
                    albumInfo.relatedItems.forEach { processItem(it, songs, albums, playlists, videos, singles) }
                } catch (e: Exception) {
                    android.util.Log.e("CloudRepository", "Error en fallback de búsqueda: ${e.message}")
                }
            }

            ArtistDetailUiState(
                id = artist.id,
                name = channelInfo.name ?: artist.name,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                description = description,
                subscriberCount = subscriberCount,
                songs = songs.distinctBy { s -> s.id },
                albums = albums.distinctBy { a -> a.id },
                playlists = playlists.distinctBy { p -> p.id },
                videos = videos.distinctBy { v -> v.id },
                singles = singles.distinctBy { s -> s.id },
                isLoading = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processItem(item: Any, songs: MutableList<Song>, albums: MutableList<Album>, playlists: MutableList<Playlist>, videos: MutableList<Song>, singles: MutableList<Album>) {
        when (item) {
            is StreamInfoItem -> {
                val bestThumb = item.thumbnails.find { it.width == it.height } 
                                ?: item.thumbnails.maxByOrNull { it.height * it.width }
                
                // Si la duración es 0, suele ser un item no cargado aún, lo tratamos como canción por defecto
                val isSong = item.duration == 0L || item.duration < 600 || item.uploaderName?.endsWith(" - Topic", true) == true
                
                val song = Song(
                    id = item.url.substringAfter("v=").substringBefore("&"),
                    title = (item.name ?: "Sin título").trim(),
                    artist = (item.uploaderName ?: "").cleanYouTubeArtist(),
                    album = "",
                    duration = item.duration.toMinSec(),
                    thumbnailUrl = bestThumb?.url?.forceSquareHighRes() ?: "",
                    streamUrl = null,
                    videoUrl = item.url,
                    durationSeconds = item.duration,
                    resultType = if (isSong) ResultType.SONG else ResultType.VIDEO
                )
                if (isSong) {
                    if (songs.none { it.id == song.id }) {
                        android.util.Log.v("CloudRepository", "Añadida canción: ${song.title}")
                        songs.add(song)
                    }
                } else {
                    if (videos.none { it.id == song.id }) {
                        android.util.Log.v("CloudRepository", "Añadido video: ${song.title}")
                        videos.add(song)
                    }
                }
            }
            is PlaylistInfoItem -> {
                val title = item.name ?: ""
                val id = item.url.substringAfter("list=")
                val bestThumb = item.thumbnails.find { it.width == it.height } 
                                ?: item.thumbnails.maxByOrNull { it.height * it.width }
                
                val thumbUrl = bestThumb?.url?.forceSquareHighRes() ?: ""
                val count = item.streamCount.toInt().coerceAtLeast(0)
                
                val artistName = (item.uploaderName ?: "").cleanYouTubeArtist()
                val album = Album(
                    id = id,
                    title = title,
                    artist = artistName,
                    thumbnailUrl = thumbUrl,
                    songCount = count
                )
                
                // Lógica de detección mejorada
                val isTopicChannel = item.uploaderName?.contains("Topic", true) == true
                
                val isAlbum = title.contains("Album", true) || title.contains("LP", true) || 
                             (isTopicChannel && !title.contains("Playlist", true) && !title.contains("EP", true) && !title.contains("Single", true))
                
                val isSingle = title.contains("Single", true) || title.contains("EP", true) || title.contains("Sencillo", true)
                
                if (isAlbum || (isTopicChannel && !isSingle)) {
                    if (albums.none { it.id == album.id }) {
                        android.util.Log.v("CloudRepository", "Añadido álbum: $title")
                        albums.add(album)
                    }
                } else if (isSingle) {
                    if (singles.none { it.id == album.id }) {
                        android.util.Log.v("CloudRepository", "Añadido single: $title")
                        singles.add(album)
                    }
                } else {
                    if (playlists.none { it.id == id }) {
                        android.util.Log.v("CloudRepository", "Añadida playlist: $title")
                        playlists.add(Playlist(id, title, item.uploaderName ?: "", thumbUrl, songCount = count))
                    }
                }
            }
            else -> {
                android.util.Log.d("CloudRepository", "Item ignorado (Tipo: ${item.javaClass.simpleName})")
            }
        }
    }

    suspend fun downloadSong(context: Context, song: Song, onProgress: (Int) -> Unit = {}) {
        com.soundly.cloud.downloadSong(context, song, onProgress)
    }

    suspend fun getPlaylistInfo(url: String): DetailUiState = withContext(Dispatchers.IO) {
        ensureInitialized()
        val extractor = ServiceList.YouTube.getPlaylistExtractor(url).apply { fetchPage() }
        val info = PlaylistInfo.getInfo(extractor)
        DetailUiState(
            id = info.id,
            title = info.name ?: "Sin título",
            uploader = info.uploaderName ?: "Desconocido",
            thumbnailUrl = info.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
            isLoading = false,
            type = ResultType.PLAYLIST
        )
    }

    suspend fun getTrendingSongs(): List<Song> = withContext(Dispatchers.IO) {
        ensureInitialized()
        val allSongs = mutableListOf<Song>()
        
        try {
            // Intento 1: Kiosk de "Music" de YouTube (más cercano a YT Music)
            try {
                android.util.Log.d("CloudRepository", "Cargando Kiosk de Música...")
                val musicExtractor = ServiceList.YouTube.kioskList.getExtractorById("Music", null)
                musicExtractor.fetchPage()
                
                // Extraer solo canciones de la página de música
                musicExtractor.initialPage.items.filterIsInstance<StreamInfoItem>().forEach { item ->
                    allSongs.add(mapStreamToSong(item))
                }
            } catch (e: Exception) {
                android.util.Log.w("CloudRepository", "Kiosk de Música no disponible: ${e.message}")
            }

            // Intento 2: Búsqueda de canciones tendencia si lo anterior falló o dio poco resultado
            if (allSongs.size < 10) {
                android.util.Log.d("CloudRepository", "Buscando canciones tendencia para Selecciones Rápidas...")
                // Filtramos por canciones cortas y evitamos términos de "Full Album"
                val searchExtractor = ServiceList.YouTube.getSearchExtractor("trending songs", listOf("music_songs"), "music")
                searchExtractor.fetchPage()
                
                searchExtractor.initialPage.items.filterIsInstance<StreamInfoItem>().forEach { item ->
                    // Evitamos videos demasiado largos que suelen ser mixes o álbumes completos
                    if (item.duration < 600) { 
                        allSongs.add(mapStreamToSong(item))
                    }
                }
            }

            allSongs.distinctBy { it.id }.take(20)
        } catch (e: Exception) {
            android.util.Log.e("CloudRepository", "Error en Selecciones Rápidas: ${e.message}", e)
            emptyList()
        }
    }

    private fun mapStreamToSong(item: StreamInfoItem): Song {
        val bestThumb = item.thumbnails.find { it.width == it.height } 
                        ?: item.thumbnails.maxByOrNull { it.height * it.width }
        
        return Song(
            id = item.url.substringAfter("v=").substringBefore("&"),
            title = (item.name ?: "Sin título").trim(),
            artist = (item.uploaderName ?: "Desconocido").cleanYouTubeArtist(),
            album = "",
            duration = item.duration.toMinSec(),
            thumbnailUrl = bestThumb?.url?.forceSquareHighRes() ?: "",
            streamUrl = null,
            videoUrl = item.url,
            durationSeconds = item.duration,
            resultType = ResultType.SONG
        )
    }
}
