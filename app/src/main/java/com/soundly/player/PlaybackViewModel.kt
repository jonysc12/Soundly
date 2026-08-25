package com.soundly.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import com.soundly.data.model.Song

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playbackManager.uiState

    init {
        playbackManager.prewarm()
    }

    fun onPlayPause() = playbackManager.playPause()
    fun onNext() = playbackManager.next()
    fun onPrevious() = playbackManager.previous()
    fun onSeek(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun play(song: Song, queue: List<Song>) = playbackManager.play(song, queue)
    fun playCollection(queue: List<Song>, startShuffled: Boolean) =
        playbackManager.playCollection(queue, startShuffled)
    fun playNext(song: Song) = playbackManager.playNext(song)
    fun playNext(songs: List<Song>) = playbackManager.playNext(songs)
    fun addToQueue(song: Song) = playbackManager.addToQueue(song)
    fun addToQueue(songs: List<Song>) = playbackManager.addToQueue(songs)
    fun moveQueueItem(from: Int, to: Int) = playbackManager.moveQueueItem(from, to)
    fun onToggleShuffle() = playbackManager.toggleShuffle()
    fun onToggleFavorite() = playbackManager.toggleCurrentSongFavorite()
    fun onCycleRepeat() = playbackManager.cycleRepeatMode()
    fun scheduleSleepTimer(minutes: Int) = playbackManager.scheduleSleepTimer(minutes * 60_000L)
    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()
    fun stop() = playbackManager.stopPlayback()
}
