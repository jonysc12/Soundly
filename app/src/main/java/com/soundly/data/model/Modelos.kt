package com.soundly.data.model

import androidx.compose.runtime.Immutable

/**
 * 🎵 Representa una canción individual
 */
@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val dateAdded: Long,
    val duration: Long,
    val path: String
) {
    /**
     * Devuelve una lista de nombres de artistas individuales si la canción es una colaboración.
     */
    val artistNames: List<String> get() = splitArtistNames(artist)

    /**
     * Devuelve el URI de la carátula del álbum al que pertenece la canción.
     */
    val artworkUri: android.net.Uri get() = android.content.ContentUris.withAppendedId(
        android.net.Uri.parse("content://media/external/audio/albumart"),
        albumId
    )
}

/**
 * Divide un string de artistas en una lista de nombres individuales.
 * Detecta separadores comunes como ",", "&", "feat.", "ft.", etc.
 */
fun splitArtistNames(artist: String): List<String> {
    if (artist.isBlank() || artist == "<unknown>" || artist == "Unknown") {
        return listOf("Unknown Artist")
    }

    // 1. Limpiar paréntesis comunes que envuelven colaboraciones como "(feat. Artista)"
    val cleanedArtist = artist
        .replace("(", " ")
        .replace(")", " ")
        .replace("[", " ")
        .replace("]", " ")

    // 2. Definir separadores comunes (Regex)
    // - Comas, barras, punto y coma, ampersand, x (cuando está rodeada de espacios)
    // - Palabras clave: feat, ft, featuring, with (insensible a mayúsculas, opcionalmente con punto)
    val separatorsRegex = Regex(
        "(?:\\s*[,/;&]\\s*)|" +                        // , / ; &
        "(?:\\s+x\\s+)|" +                             // x (como en A x B)
        "(?:\\s+(?:feat|ft|featuring|with)\\.?\\s+)",  // feat. ft. featuring with
        RegexOption.IGNORE_CASE
    )
    
    // 3. Dividir y limpiar
    return cleanedArtist.split(separatorsRegex)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

/**
 * Genera un ID estable para un artista basado en su nombre.
 */
fun generateArtistId(name: String): Long {
    return name.lowercase(java.util.Locale.ROOT).trim().hashCode().toLong()
}

/**
 * 💿 Representa un álbum musical
 */
@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int
)

/**
 * 🎤 Representa un artista
 */
@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkUri: android.net.Uri? = null
)

data class MusicScanFilters(
    val ignoreTempFolders: Boolean,
    val ignoreShortAudios: Boolean
)

data class MusicScanReport(
    val scannedSongs: Int,
    val importedSongs: Int,
    val blockedSongs: Int,
    val blockedByTempFolders: Int,
    val blockedByShortDuration: Int,
    val activeFilters: MusicScanFilters
)
