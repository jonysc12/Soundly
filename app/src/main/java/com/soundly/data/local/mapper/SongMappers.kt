package com.soundly.data.local.mapper

import com.soundly.data.local.SongEntity
import com.soundly.data.model.Song

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        titleNormalized = title.lowercase(),
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        dateAdded = dateAdded,
        duration = duration,
        path = path
    )
}

fun SongEntity.toDomain(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        dateAdded = dateAdded,
        duration = duration,
        path = path
    )
}
