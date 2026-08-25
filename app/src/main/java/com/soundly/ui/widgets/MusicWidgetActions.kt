package com.soundly.ui.widgets

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.soundly.player.PlaybackManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@UnstableApi
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun playbackManager(): PlaybackManager
}

@UnstableApi
class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("MusicWidgetActions", "PlayPauseAction clicked")
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        withContext(Dispatchers.Main) {
            entryPoint.playbackManager().playPause()
        }
    }
}

@UnstableApi
class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("MusicWidgetActions", "NextAction clicked")
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        withContext(Dispatchers.Main) {
            entryPoint.playbackManager().next()
        }
    }
}

@UnstableApi
class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("MusicWidgetActions", "PreviousAction clicked")
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        withContext(Dispatchers.Main) {
            entryPoint.playbackManager().previous()
        }
    }
}

@UnstableApi
class ToggleFavoriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("MusicWidgetActions", "ToggleFavoriteAction clicked")
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        withContext(Dispatchers.Main) {
            entryPoint.playbackManager().toggleCurrentSongFavorite()
        }
    }
}

@UnstableApi
class PlaySongAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val songId = parameters[KEY_SONG_ID] ?: return
        Log.d("MusicWidgetActions", "PlaySongAction clicked for song: $songId")
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        withContext(Dispatchers.Main) {
            val playbackManager = entryPoint.playbackManager()
            val queue = playbackManager.uiState.value.queue
            val song = queue.find { it.id == songId }
            if (song != null) {
                playbackManager.play(song, queue)
            }
        }
    }

    companion object {
        val KEY_SONG_ID = ActionParameters.Key<Long>("song_id")
    }
}
