package com.soundly.data.model

/**
 * 🎵 Representa una canción individual
 */
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
)

/**
 * 💿 Representa un álbum musical
 */
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int
)

/**
 * 🎤 Representa un artista
 */
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int // 👈 nuevo
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
