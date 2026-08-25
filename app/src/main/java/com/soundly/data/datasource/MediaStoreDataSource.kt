package com.soundly.data.datasource

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Song
import com.soundly.data.utils.fixEncoding

class MediaStoreDataSource(
    private val context: Context
) {

    private val contentResolver = context.contentResolver

    private data class SongColumns(
        val idColumn: Int,
        val titleColumn: Int,
        val artistColumn: Int,
        val artistIdColumn: Int,
        val albumColumn: Int,
        val albumIdColumn: Int,
        val dateAddedColumn: Int,
        val durationColumn: Int,
        val dataColumn: Int
    )

    fun getSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        streamSongs { songs.add(it) }
        return songs
    }

    fun streamSongs(onSong: (Song) -> Unit): Int {
        var scannedCount = 0
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val columns = SongColumns(
                idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID),
                titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE),
                artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST),
                artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID),
                albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM),
                albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID),
                dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED),
                durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION),
                dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            )

            while (cursor.moveToNext()) {
                onSong(mapSong(cursor, columns))
                scannedCount++
            }
        }

        return scannedCount
    }

    fun getAlbums(): List<Album> {
        val albums = mutableListOf<Album>()
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(albumColumn).fixEncoding().ifBlank { "Unknown" }
                val artist = cursor.getString(artistColumn).fixEncoding().ifBlank { "Unknown" }
                val songCount = cursor.getInt(songCountColumn)

                albums.add(
                    Album(
                        id = id,
                        name = name,
                        artist = artist,
                        songCount = songCount
                    )
                )
            }
        }

        return albums
    }

    fun getArtists(): List<Artist> {
        val artists = mutableListOf<Artist>()
        val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
        )
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(artistColumn).fixEncoding().ifBlank { "Unknown" }
                val songCount = cursor.getInt(trackCountColumn)
                val albumCount = cursor.getInt(albumCountColumn)

                artists.add(
                    Artist(
                        id = id,
                        name = name,
                        songCount = songCount,
                        albumCount = albumCount
                    )
                )
            }
        }

        return artists
    }

    fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }

    fun getAlbumIdForArtist(artistId: Long): Long? {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artistId.toString())
        val sortOrder = "${MediaStore.Audio.Media.ALBUM_ID} ASC"

        contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
            }
        }
        return null
    }

    private fun mapSong(cursor: Cursor, columns: SongColumns): Song {
        return Song(
            id = cursor.getLong(columns.idColumn),
            title = cursor.getString(columns.titleColumn).fixEncoding().ifBlank { "Unknown" },
            artist = cursor.getString(columns.artistColumn).fixEncoding().ifBlank { "Unknown" },
            artistId = cursor.getLong(columns.artistIdColumn),
            album = cursor.getString(columns.albumColumn).fixEncoding().ifBlank { "Unknown" },
            albumId = cursor.getLong(columns.albumIdColumn),
            dateAdded = cursor.getLong(columns.dateAddedColumn),
            duration = cursor.getLong(columns.durationColumn),
            path = cursor.getString(columns.dataColumn)
        )
    }
}
