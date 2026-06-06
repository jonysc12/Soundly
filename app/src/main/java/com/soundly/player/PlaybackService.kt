package com.soundly.player

import android.content.Intent
import android.os.IBinder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreate() {
        super.onCreate()
        playbackManager.ensureSession(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        playbackManager.ensureSession(this)
        playbackManager.startForegroundPlaceholder(this)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        playbackManager.handleTaskRemoved(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playbackManager.mediaSession
    }

    override fun onDestroy() {
        playbackManager.onServiceDestroyed()
        super.onDestroy()
    }

    // Explicitly disallow binding; controllers should use the MediaSession token.
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
