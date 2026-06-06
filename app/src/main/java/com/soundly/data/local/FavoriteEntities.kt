package com.soundly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey val songId: Long,
    val createdAt: Long
)

@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(
    @PrimaryKey val albumId: Long,
    val createdAt: Long
)

@Entity(tableName = "favorite_artists")
data class FavoriteArtistEntity(
    @PrimaryKey val artistId: Long,
    val createdAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val artworkUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: Long,
    val addedAt: Long
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val songId: Long,
    val lastPlayedAt: Long,
    val playCount: Int = 1
)