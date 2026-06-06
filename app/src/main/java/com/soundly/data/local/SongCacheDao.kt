package com.soundly.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.SkipQueryVerification
import kotlinx.coroutines.flow.Flow

@Dao
@SkipQueryVerification
interface SongCacheDao {

    @Query("SELECT * FROM songs ORDER BY titleNormalized ASC, id ASC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY titleNormalized ASC, id ASC")
    suspend fun getSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()
}
