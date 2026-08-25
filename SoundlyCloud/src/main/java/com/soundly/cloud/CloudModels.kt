package com.soundly.cloud

import com.soundly.cloud.R

enum class ResultType { SONG, VIDEO, ARTIST, PLAYLIST, ALBUM }

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val thumbnailUrl: String,
    val streamUrl: String?,
    val videoUrl: String,
    val durationSeconds: Long = 0,
    val isM4A: Boolean = true,
    val resultType: ResultType = ResultType.SONG
)

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val url: String = "",
    val songCount: Int = 0,
    val albumCount: Int = 0
)
data class Playlist(val id: String, val title: String, val uploader: String, val thumbnailUrl: String, val songCount: Int = 0)
data class Album(val id: String, val title: String, val artist: String, val thumbnailUrl: String, val songCount: Int = 0)

enum class SearchCategory(val labelResId: Int, val filter: String) {
    SONGS(R.string.category_songs, "music_songs"),
    ALBUMS(R.string.category_albums, "music_albums"),
    ARTISTS(R.string.category_artists, "music_artists"),
    PLAYLISTS(R.string.category_playlists, "playlists")
}

data class DetailUiState(
    val id: String = "",
    val title: String = "",
    val uploader: String = "",
    val thumbnailUrl: String = "",
    val items: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val type: ResultType = ResultType.PLAYLIST
)

data class ArtistDetailUiState(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val subscriberCount: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val videos: List<Song> = emptyList(),
    val singles: List<Album> = emptyList(),
    val isLoading: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val results: List<Any> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isSearchCommitted: Boolean = false,
    val isLoading: Boolean = false,
    val selectedCategory: SearchCategory = SearchCategory.SONGS,
    val error: String? = null,
    val detailState: DetailUiState? = null,
    val artistDetailState: ArtistDetailUiState? = null
)

data class SongMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: Int = 0,
    val artworkUrl: String = ""
)
