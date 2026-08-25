package com.soundly.data.datasource.paging

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.soundly.data.model.Artist
import com.soundly.data.model.generateArtistId

class ArtistPagingSource(
    private val contentResolver: ContentResolver,
    private val searchQuery: String? = null
) : PagingSource<Int, Artist>() {

    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        return try {
            val artists = mutableListOf<Artist>()
            val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
            )

            val selection = if (searchQuery.isNullOrBlank()) null else "${MediaStore.Audio.Artists.ARTIST} LIKE ?"
            val selectionArgs = if (searchQuery.isNullOrBlank()) null else arrayOf("%$searchQuery%")

            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(ContentResolver.QUERY_ARG_OFFSET, page * pageSize)
                putString(ContentResolver.QUERY_ARG_SORT_COLUMNS, MediaStore.Audio.Artists.ARTIST)
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_ASCENDING)
                if (selection != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                }
            }

            contentResolver.query(uri, projection, queryArgs, null)?.use { cursor ->
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(artistColumn) ?: "Unknown"
                    artists.add(
                        Artist(
                            id = generateArtistId(name),
                            name = name,
                            songCount = cursor.getInt(trackCountColumn),
                            albumCount = cursor.getInt(albumCountColumn)
                        )
                    )
                }
            }

            LoadResult.Page(
                data = artists,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (artists.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
