package com.soundly.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.SkipQueryVerification
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
@SkipQueryVerification
interface SongCacheDao {

    @Query("SELECT * FROM songs ORDER BY titleNormalized ASC, id ASC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY titleNormalized ASC, id ASC")
    fun observeSongsPaged(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY titleNormalized DESC, id DESC")
    fun observeSongsPagedTitleDesc(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded ASC, titleNormalized ASC")
    fun observeSongsPagedDateAddedAsc(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC, titleNormalized ASC")
    fun observeSongsPagedDateAddedDesc(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs WHERE titleNormalized LIKE :query OR artist LIKE :query OR album LIKE :query ORDER BY titleNormalized ASC")
    fun searchSongsPaged(query: String): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY titleNormalized ASC, id ASC")
    suspend fun getSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int): Flow<List<SongEntity>>
}
