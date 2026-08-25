package com.soundly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        FavoriteSongEntity::class,
        FavoriteAlbumEntity::class,
        FavoriteArtistEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayHistoryEntity::class,
        PlayEventEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class SoundlyDatabase : RoomDatabase() {
    abstract fun songCacheDao(): SongCacheDao
    abstract fun libraryMetadataDao(): LibraryMetadataDao
}
