package com.soundly.data

import android.content.Context
import com.soundly.data.datasource.MediaStoreDataSource
import com.soundly.data.local.LibraryMetadataDao
import com.soundly.data.local.SongCacheDao
import com.soundly.data.local.SoundlyDatabase
import com.soundly.data.repository.MusicRepository
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    private const val DATABASE_NAME = "soundly_music_cache.db"

    @Provides
    @Singleton
    fun provideMediaStoreDataSource(@ApplicationContext context: Context): MediaStoreDataSource {
        return MediaStoreDataSource(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SoundlyDatabase {
        return Room.databaseBuilder(
            context,
            SoundlyDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideSongCacheDao(database: SoundlyDatabase): SongCacheDao {
        return database.songCacheDao()
    }

    @Provides
    @Singleton
    fun provideLibraryMetadataDao(database: SoundlyDatabase): LibraryMetadataDao {
        return database.libraryMetadataDao()
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        mediaStoreDataSource: MediaStoreDataSource,
        songCacheDao: SongCacheDao,
        libraryMetadataDao: LibraryMetadataDao,
        database: SoundlyDatabase,
        @ApplicationContext context: Context
    ): MusicRepository {
        return MusicRepository(
            mediaStore = mediaStoreDataSource,
            songCacheDao = songCacheDao,
            libraryMetadataDao = libraryMetadataDao,
            database = database,
            context = context
        )
    }
}
