package com.soundly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val titleNormalized: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val dateAdded: Long,
    val duration: Long,
    val path: String
)