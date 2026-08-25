package com.soundly.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

fun String.cleanTitleForSearch(): String {
    return this.replace(Regex("\\[.*?\\]"), "")
        .replace(Regex("\\(.*?\\)"), "")
        .replace(Regex("Official (Video|Audio|Lyric|Music Video)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("Video Oficial", RegexOption.IGNORE_CASE), "")
        .replace(Regex("Full HD", RegexOption.IGNORE_CASE), "")
        .replace(Regex("4K", RegexOption.IGNORE_CASE), "")
        .replace(Regex("HQ", RegexOption.IGNORE_CASE), "")
        .replace(Regex("Lyrics?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s\\s+"), " ")
        .trim()
}

suspend fun getMetadataFromItunes(title: String, artist: String): SongMetadata? = withContext(Dispatchers.IO) {
    try {
        val cleanT = title.cleanTitleForSearch()
        val cleanA = artist.cleanTitleForSearch()
        
        // Intentar búsqueda combinada primero
        val query = URLEncoder.encode("$cleanA $cleanT", "UTF-8")
        val url = "https://itunes.apple.com/search?term=$query&media=music&entity=song&limit=5"

        var response = sharedHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return@withContext null

        var json = JSONObject(response.body!!.string())
        var results = json.getJSONArray("results")

        // Si no hay resultados, intentar solo con el título (a veces el artista de YT es genérico)
        if (results.length() == 0) {
            val queryTitleOnly = URLEncoder.encode(cleanT, "UTF-8")
            val url2 = "https://itunes.apple.com/search?term=$queryTitleOnly&media=music&entity=song&limit=5"
            response = sharedHttpClient.newCall(Request.Builder().url(url2).build()).execute()
            if (response.isSuccessful) {
                json = JSONObject(response.body!!.string())
                results = json.getJSONArray("results")
            }
        }

        if (results.length() == 0) return@withContext null

        val titleLower = title.lowercase()
        val artistLower = artist.lowercase()

        var bestMatch: JSONObject? = null
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            val tName = item.optString("trackName", "").lowercase()
            val aName = item.optString("artistName", "").lowercase()
            if (tName.contains(titleLower) || titleLower.contains(tName) ||
                aName.contains(artistLower) || artistLower.contains(aName)) {
                bestMatch = item
                break
            }
        }

        val track = bestMatch ?: results.getJSONObject(0)
        val artwork = track.optString("artworkUrl100", "")
            .replace("100x100bb", "600x600bb").replace("100x100", "600x600")

        val releaseDate = track.optString("releaseDate", "")

        return@withContext SongMetadata(
            title = track.optString("trackName", title),
            artist = track.optString("artistName", artist),
            album = track.optString("collectionName", "").removeSuffix(" - Single").removeSuffix(" - EP"),
            year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "",
            genre = track.optString("primaryGenreName", ""),
            trackNumber = track.optInt("trackNumber", 0),
            artworkUrl = artwork
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun getMetadataFromMusicBrainz(title: String, artist: String): SongMetadata? = withContext(Dispatchers.IO) {
    try {
        val query = URLEncoder.encode("recording:\"$title\" AND artist:\"$artist\"", "UTF-8")
        val url = "https://musicbrainz.org/ws/2/recording/?query=$query&limit=3&fmt=json"

        val response = sharedHttpClient.newCall(
            Request.Builder().url(url).addHeader("User-Agent", "SoundlyCloud/1.0")
                .build()
        ).execute()

        if (!response.isSuccessful) return@withContext null

        val json = JSONObject(response.body!!.string())
        val recordings = json.optJSONArray("recordings") ?: return@withContext null
        if (recordings.length() == 0) return@withContext null

        val rec = recordings.getJSONObject(0)
        val release = rec.optJSONArray("releases")?.optJSONObject(0)
        val albumTitle = release?.optString("title", "") ?: ""
        val releaseDate = release?.optString("date", "") ?: ""
        val trackNum = release?.optJSONObject("media")?.optJSONArray("tracks")?.optJSONObject(0)?.optInt("number", 0) ?: 0

        return@withContext SongMetadata(
            title = rec.optString("title", title),
            artist = artist,
            album = albumTitle,
            year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "",
            trackNumber = trackNum,
            artworkUrl = ""
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun getMetadataFromYouTubeMusic(videoId: String): SongMetadata? = withContext(Dispatchers.IO) {
    try {
        val url = "https://music.youtube.com/watch?v=$videoId"
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "es-ES,es;q=0.9")
            .get()

        val html = doc.html()
        var artwork = ""
        var album = ""
        var year = ""

        // 1. EXTRAER TÍTULO Y ARTISTA BASE
        val title = doc.select("meta[property=og:title]").attr("content")
        val artistRaw = doc.select("meta[property=og:video:tag]").firstOrNull()?.attr("content") 
            ?: doc.select("link[itemprop=name]").attr("content")
        val cleanArtist = artistRaw.replace(" - Topic", "").trim()

        // 2. EXTRACCIÓN DE METADATOS DESDE EL PLAYER RESPONSE
        val playerResponseStr = html.substringAfter("var ytInitialPlayerResponse = ", "").substringBefore(";</script>", "")
        if (playerResponseStr.isNotEmpty()) {
            val json = JSONObject(playerResponseStr)
            val videoDetails = json.optJSONObject("videoDetails")
            val description = videoDetails?.optString("shortDescription", "") ?: ""
            
            // Año de publicación (Microformatos)
            year = json.optJSONObject("microformat")
                ?.optJSONObject("microformatDataRenderer")
                ?.optString("publishDate", "")?.take(4) ?: ""

            // Intento A: Descripción oficial de YouTube Music (Topic channels)
            if (description.contains("Provided to YouTube by")) {
                val lines = description.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val separatorIndex = lines.indexOfFirst { it.contains("·") || it.contains("•") }
                if (separatorIndex != -1 && separatorIndex + 1 < lines.size) {
                    val potentialAlbum = lines[separatorIndex + 1]
                    if (!potentialAlbum.equals(cleanArtist, true) && 
                        !potentialAlbum.contains("Released on") && 
                        !potentialAlbum.contains("℗")) {
                        album = potentialAlbum
                    }
                }
                
                if (year.isBlank()) {
                    lines.find { it.contains("Released on:") }?.let {
                        year = it.substringAfter("Released on:").trim().take(4)
                    }
                }
            }

            // Intento B: Buscar "Album: " en la descripción
            if (album.isBlank()) {
                val albumRegex = Regex("Album: (.*)")
                albumRegex.find(description)?.let { 
                    album = it.groupValues[1].trim().split("·", "•")[0].trim() 
                }
            }

            // Intento C: Microformatos
            if (album.isBlank()) {
                album = json.optJSONObject("microformat")
                    ?.optJSONObject("microformatDataRenderer")
                    ?.optString("album", "") ?: ""
            }
            
            // Carátula (Google User Content es mejor que ytimg)
            val thumbnailList = videoDetails?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            if (thumbnailList != null) {
                for (i in 0 until thumbnailList.length()) {
                    val thumbUrl = thumbnailList.getJSONObject(i).optString("url", "")
                    if (thumbUrl.contains("googleusercontent.com")) {
                        artwork = thumbUrl.substringBefore("=")
                        break
                    }
                }
            }
        }

        // 3. EXTRACCIÓN PROFUNDA DESDE ytInitialData (Donde está el álbum enlazado)
        if (album.isBlank()) {
            val initialDataStr = html.substringAfter("var ytInitialData = ", "").substringBefore(";</script>", "")
            if (initialDataStr.isNotEmpty()) {
                // Intento A: BrowseEndpoint de tipo álbum (MPREb)
                // Buscamos el patrón: "text":"Nombre del Album","navigationEndpoint":{..."browseId":"MPREb_..."
                val albumMatch = Regex("\"text\":\"([^\"]+)\",\"navigationEndpoint\":\\{[^}]*\"browseEndpoint\":\\{\"browseId\":\"(MPREb_[^\"]+)\"").find(initialDataStr)
                if (albumMatch != null) {
                    album = albumMatch.groupValues[1]
                }
                
                // Intento B: Búsqueda de la sección de álbum en el panel lateral/información
                if (album.isBlank()) {
                    val albumSectionRegex = Regex("\"title\":\\{\"runs\":\\[\\{\"text\":\"Álbum\"\\}\\]\\},\"value\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"")
                    albumSectionRegex.find(initialDataStr)?.let {
                        album = it.groupValues[1]
                    }
                }

                // Intento C: Búsqueda genérica de etiquetas de álbum en el JSON
                if (album.isBlank()) {
                    val genericAlbumMatch = Regex("\"label\":\"Album\",\"value\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"").find(initialDataStr)
                    if (genericAlbumMatch != null) {
                        album = genericAlbumMatch.groupValues[1]
                    }
                }
            }
        }

        // 4. FALLBACK FINAL: HTML crudo (Etiqueta Album o meta descripción)
        if (album.isBlank()) {
            album = doc.select("meta[name=description]").attr("content")
                .substringAfter("Album: ", "").substringBefore("\n").trim()
                .split("·", "•")[0].trim()
        }
        
        if (album.isBlank()) {
            // A veces está en una etiqueta <link itemprop="name" content="..."> dentro de un contexto de album
            album = doc.select("div.ytmusic-player-bar span.subtitle a[href*=browse/MPREb]").text()
        }

        // 5. PROCESAMIENTO DE IMAGEN
        if (artwork.isBlank()) {
            val ogImage = doc.select("meta[property=og:image]").attr("content")
            if (ogImage.contains("googleusercontent.com")) artwork = ogImage.substringBefore("=")
        }

        if (title.isBlank()) return@withContext null

        val finalArtwork = if (artwork.isNotEmpty()) "$artwork=w1200-h1200-c-rj-l90" 
                           else "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

        return@withContext SongMetadata(
            title = title,
            artist = cleanArtist,
            album = album.ifBlank { "" },
            year = year,
            artworkUrl = finalArtwork
        )
    } catch (e: Exception) {
        null
    }
}

suspend fun getRealMetadata(
    youtubeTitle: String,
    youtubeArtist: String,
    youtubeThumbnailUrl: String,
    videoId: String? = null
): SongMetadata = withContext(Dispatchers.IO) {
    // 1. Limpieza básica
    val cleanT = youtubeTitle.cleanTitleForSearch()
    val cleanA = youtubeArtist.cleanYouTubeArtist()

    // 2. FORZAR ALTA CALIDAD Y CUADRADO EN LA MINIATURA
    val highResSquareThumb = if (youtubeThumbnailUrl.contains("googleusercontent.com")) {
        val baseUrl = youtubeThumbnailUrl.substringBefore("=")
        "$baseUrl=w1200-h1200-c-rj-l90"
    } else if (youtubeThumbnailUrl.contains("ytimg.com") && videoId != null) {
        "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
    } else {
        youtubeThumbnailUrl
    }

    // 3. OBTENER METADATOS DE MÚLTIPLES FUENTES
    var meta: SongMetadata? = null

    // A. YouTube Music (Prioridad 1)
    if (videoId != null) {
        meta = getMetadataFromYouTubeMusic(videoId)
    }

    // B. iTunes (Si no hay álbum o falló YT Music)
    if (meta == null || meta.album.isBlank()) {
        val itunes = getMetadataFromItunes(cleanT, cleanA)
        if (itunes != null) {
            meta = if (meta == null) itunes else meta.copy(
                album = itunes.album,
                year = if (meta.year.isBlank()) itunes.year else meta.year,
                genre = if (meta.genre.isBlank()) itunes.genre else meta.genre,
                trackNumber = if (meta.trackNumber == 0) itunes.trackNumber else meta.trackNumber,
                artworkUrl = if (meta.artworkUrl.isBlank() || !meta.artworkUrl.contains("googleusercontent.com")) itunes.artworkUrl else meta.artworkUrl
            )
        }
    }

    // C. MusicBrainz (Si sigue sin haber álbum)
    if (meta == null || meta.album.isBlank()) {
        val mb = getMetadataFromMusicBrainz(cleanT, cleanA)
        if (mb != null) {
            meta = if (meta == null) mb else meta.copy(
                album = mb.album,
                year = if (meta.year.isBlank()) mb.year else meta.year,
                trackNumber = if (meta.trackNumber == 0) mb.trackNumber else meta.trackNumber
            )
        }
    }

    if (meta != null && meta.title.isNotBlank()) {
        // Aseguramos que la carátula sea de alta resolución si vino de YT o iTunes
        val finalArtwork = if (meta.artworkUrl.contains("googleusercontent.com")) {
            val baseUrl = meta.artworkUrl.substringBefore("=")
            "$baseUrl=w1200-h1200-c-rj-l90"
        } else if (meta.artworkUrl.isBlank()) {
            highResSquareThumb
        } else {
            meta.artworkUrl
        }
        
        return@withContext meta.copy(artworkUrl = finalArtwork)
    }

    // 4. FALLBACK FINAL: Solo con datos de YouTube
    var finalT = cleanT
    var finalA = cleanA
    if (cleanT.contains(" - ")) {
        val parts = cleanT.split(" - ")
        if (parts.size >= 2) {
            val possibleA = parts[0].trim()
            val possibleT = parts[1].trim()
            if (cleanA.lowercase() == "desconocido" || possibleA.lowercase().contains(cleanA.lowercase())) {
                finalA = possibleA
                finalT = possibleT
            }
        }
    }

    return@withContext SongMetadata(
        title = finalT,
        artist = finalA,
        album = "", // Dejamos vacío para que se use el del objeto Song o el fallback
        artworkUrl = highResSquareThumb
    )
}
