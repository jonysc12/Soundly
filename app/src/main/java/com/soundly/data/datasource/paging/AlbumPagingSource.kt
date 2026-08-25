package com.soundly.data.datasource.paging

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.soundly.data.model.Album

class AlbumPagingSource(
    private val contentResolver: ContentResolver,
    private val searchQuery: String? = null
) : PagingSource<Int, Album>() {

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        return try {
            val albums = mutableListOf<Album>()
            val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS
            )
            
            val selection = if (searchQuery.isNullOrBlank()) null else "${MediaStore.Audio.Albums.ALBUM} LIKE ? OR ${MediaStore.Audio.Albums.ARTIST} LIKE ?"
            val selectionArgs = if (searchQuery.isNullOrBlank()) null else arrayOf("%$searchQuery%", "%$searchQuery%")

            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(ContentResolver.QUERY_ARG_OFFSET, page * pageSize)
                putString(ContentResolver.QUERY_ARG_SORT_COLUMNS, MediaStore.Audio.Albums.ALBUM)
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_ASCENDING)
                if (selection != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                }
            }

            contentResolver.query(uri, projection, queryArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                while (cursor.moveToNext()) {
                    albums.add(
                        Album(
                            id = cursor.getLong(idColumn),
                            name = cursor.getString(albumColumn) ?: "Unknown",
                            artist = cursor.getString(artistColumn) ?: "Unknown",
                            songCount = cursor.getInt(songCountColumn)
                        )
                    )
                }
            }

            LoadResult.Page(
                data = albums,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (albums.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
