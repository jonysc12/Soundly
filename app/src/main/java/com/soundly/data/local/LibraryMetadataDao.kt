package com.soundly.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryMetadataDao {

    @Query("SELECT songId FROM favorite_songs")
    fun observeFavoriteSongIds(): Flow<List<Long>>

    @Query("SELECT COUNT(*) > 0 FROM favorite_songs WHERE songId = :songId")
    suspend fun isSongFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavoriteSong(entity: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE songId = :songId")
    suspend fun deleteFavoriteSong(songId: Long)

    @Query("SELECT albumId FROM favorite_albums")
    fun observeFavoriteAlbumIds(): Flow<List<Long>>

    @Query("SELECT COUNT(*) > 0 FROM favorite_albums WHERE albumId = :albumId")
    suspend fun isAlbumFavorite(albumId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavoriteAlbum(entity: FavoriteAlbumEntity)

    @Query("DELETE FROM favorite_albums WHERE albumId = :albumId")
    suspend fun deleteFavoriteAlbum(albumId: Long)

    @Query("SELECT artistId FROM favorite_artists")
    fun observeFavoriteArtistIds(): Flow<List<Long>>

    @Query("SELECT COUNT(*) > 0 FROM favorite_artists WHERE artistId = :artistId")
    suspend fun isArtistFavorite(artistId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavoriteArtist(entity: FavoriteArtistEntity)

    @Query("DELETE FROM favorite_artists WHERE artistId = :artistId")
    suspend fun deleteFavoriteArtist(artistId: Long)

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC, createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY addedAt DESC")
    fun observePlaylistSongs(): Flow<List<PlaylistSongEntity>>

    @Query("SELECT COUNT(*) > 0 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun playlistContainsSong(playlistId: String, songId: Long): Boolean

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(entity: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistSongs(entities: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deletePlaylistSong(playlistId: String, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun deletePlaylistEntriesBySongId(songId: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: String, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET name = :name, artworkUri = :artworkUri, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylist(playlistId: String, name: String, artworkUri: String?, updatedAt: Long)

    // --- PLAY HISTORY ---
    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC")
    fun observePlayHistory(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayHistory(entity: PlayHistoryEntity)

    @Query("SELECT * FROM play_history WHERE songId = :songId LIMIT 1")
    suspend fun getPlayHistory(songId: Long): PlayHistoryEntity?

    @Query("SELECT songId FROM play_history ORDER BY playCount DESC LIMIT :limit")
    fun observeTopSongs(limit: Int): Flow<List<Long>>

    @Query("SELECT songId FROM play_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentSongs(limit: Int): Flow<List<Long>>
}
