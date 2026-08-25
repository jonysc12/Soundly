package com.soundly.cloud

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveDownload(
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val progress: Int = -1 // -1 means preparing
)

object DownloadTracker {
    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ActiveDownload>> = _activeDownloads.asStateFlow()

    fun updateProgress(song: Song, progress: Int) {
        val current = _activeDownloads.value.toMutableMap()
        current[song.id] = ActiveDownload(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            thumbnailUrl = song.thumbnailUrl,
            progress = progress
        )
        _activeDownloads.value = current
    }

    fun removeActiveDownload(songId: String) {
        val current = _activeDownloads.value.toMutableMap()
        current.remove(songId)
        _activeDownloads.value = current
    }
}
