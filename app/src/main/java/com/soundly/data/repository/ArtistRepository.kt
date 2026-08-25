package com.soundly.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class ScrapedArtist(
    val name: String,
    val imageUrl: String?,
    val bio: String
)

@Singleton
class ArtistRepository @Inject constructor() {

    suspend fun getArtistInfo(artistName: String): ScrapedArtist? {
        return withContext(Dispatchers.IO) {
            try {
                val formattedName = artistName.replace(" ", "+")
                val url = "https://www.last.fm/music/$formattedName"

                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get()

                // Nombre del artista
                val name = document.selectFirst("h1")?.text()?.trim() ?: artistName

                // Intentar obtener imagen de alta calidad desde meta tags (OpenGraph)
                var imageUrl = document.selectFirst("meta[property=og:image]")?.attr("content")

                if (imageUrl.isNullOrEmpty()) {
                    // Fallback a selectores comunes
                    imageUrl = document.selectFirst("img.avatar, .artist-header-image img, .cover-art img")?.attr("src")
                }

                if (imageUrl.isNullOrEmpty()) {
                    imageUrl = document.select("img").mapNotNull { it.attr("src") }
                        .firstOrNull { it.contains("last.fm") && (it.contains("avatar") || it.contains("large")) }
                }

                // Mejora de calidad: Reemplazar tamaños pequeños por versiones grandes/originales
                // Last.fm usa patrones como /avatar170s/, /300x300/, /174s/, etc.
                imageUrl = imageUrl?.let { urlStr ->
                    urlStr.replace(Regex("/u/\\d+s/"), "/u/770x0/")
                        .replace(Regex("/u/\\d+x\\d+/"), "/u/770x0/")
                        .replace("/avatar170s/", "/770x0/")
                        .replace("/174s/", "/770x0/")
                }

                // Biografía corta
                var bio = document.selectFirst(".wiki-content, .artist-bio, p")?.text()?.trim() ?: ""

                // Si la bio es muy corta, intentar ir a la página +wiki
                if (bio.length < 100) {
                    val wikiUrl = "$url/+wiki"
                    try {
                        val wikiDoc = Jsoup.connect(wikiUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(8000)
                            .get()
                        bio = wikiDoc.selectFirst(".wiki-content, .content")?.text()?.trim() ?: bio
                    } catch (e: Exception) {
                        // Ignorar si falla la página wiki
                    }
                }

                ScrapedArtist(
                    name = name,
                    imageUrl = imageUrl,
                    bio = bio
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
