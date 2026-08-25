package com.soundly.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.soundly.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    companion object {
        private const val NOTIFICATION_ID = 33
        private const val CHANNEL_ID = "soundly_playback"
        private const val TAG = "PlaybackService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")

        try {
            // 1. Crear el canal de notificación
            createNotificationChannel()
            Log.d(TAG, "Notification channel created")

            // 2. Ya NO llamamos a startForeground() aquí. 
            // Deja que MediaSessionService lo maneje automáticamente a través de onUpdateNotification.
            // Esto evita el ForegroundServiceStartNotAllowedException en Android 14+.
            Log.d(TAG, "Skipping manual startForeground in onCreate")

            // 3. Crear la MediaSession
            playbackManager.ensureSession(this)
            val session = playbackManager.mediaSession
            Log.d(TAG, "MediaSession created: $session")

            // 4. IMPORTANTE: registrar la sesión en el MediaSessionService.
            // Sin esto, Media3 nunca conecta sus listeners internos al player y
            // onUpdateNotification() jamás se llama automáticamente: la notificación
            // se queda pegada en el placeholder "Loading..." para siempre.
            if (session != null) {
                addSession(session)
                Log.d(TAG, "Session added to MediaSessionService")
            } else {
                Log.e(TAG, "MediaSession is null after ensureSession(), cannot add session")
            }

            // 5. Darle a PlaybackManager una forma de pedirle al servicio que refresque
            // la notificación cuando cambian cosas que NO son eventos nativos del Player
            // (por ejemplo, marcar/desmarcar favorito).
            playbackManager.notificationUpdateListener = { updateMediaStyleNotification() }

            Log.d(TAG, "onCreate completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            throw e
        }
    }

    fun updateMediaStyleNotification() {
        Log.d(TAG, "updateMediaStyleNotification called")
        if (playbackManager.mediaSession != null) {
            onUpdateNotification(playbackManager.mediaSession!!, false)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Soundly Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media playback controls"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        Log.d(TAG, "onUpdateNotification called, startInForegroundRequired=$startInForegroundRequired")

        try {
            // Construir la notificación con MediaStyle moderno de Media3
            // Usamos uiState del PlaybackManager porque es mucho más estable durante transiciones (crossfade)
            val uiState = playbackManager.uiState.value
            val isFavorite = uiState.isCurrentSongFavorite
            val isShuffleOn = uiState.isShuffleEnabled
            val isPlaying = uiState.isPlaying

            // Orden de acciones: 0=Like, 1=Previous, 2=Play/Pause, 3=Next, 4=Shuffle
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.mono_soundly_logo) // Logo directo para evitar el padding excesivo de los adaptive icons
                .setContentTitle(uiState.title.ifBlank { "Soundly" })
                .setContentText(uiState.artist.ifBlank { "Unknown Artist" })
                .setLargeIcon(uiState.artworkBitmap ?: BitmapFactory.decodeResource(resources, R.drawable.carga)) // Fallback solicitado
                .setStyle(MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(1, 2, 3) // Previous, Play/Pause, Next
                )
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(isPlaying) // Solo ongoing si está reproduciendo para permitir quitarla si se pausa
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(NotificationCompat.Action(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                    if (isFavorite) "Unlike" else "Like",
                    createPendingIntent("LIKE")
                ))
                .addAction(NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    createPendingIntent("PREVIOUS")
                ))
                .addAction(if (isPlaying) {
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        createPendingIntent("PAUSE")
                    )
                } else {
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_play,
                        "Play",
                        createPendingIntent("PLAY")
                    )
                })
                .addAction(NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    createPendingIntent("NEXT")
                ))
                .addAction(NotificationCompat.Action(
                    R.drawable.ic_shuffle,
                    if (isShuffleOn) "Shuffle: On" else "Shuffle: Off",
                    createPendingIntent("SHUFFLE")
                ))
                .build()

            if (startInForegroundRequired) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                Log.d(TAG, "Called startForeground with MediaStyle notification")
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "Updated notification with MediaStyle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onUpdateNotification", e)
            throw e
        }
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand called with action: $action")

        if (intent != null) {
            if (playbackManager.mediaSession == null) {
                Log.d(TAG, "MediaSession is null, ensuring session before processing action $action")
                playbackManager.ensureSession(this)
            }
            
            val session = playbackManager.mediaSession
            if (session != null) {
                when (action) {
                    "PLAY" -> {
                        Log.d(TAG, "Executing PLAY action")
                        session.player.play()
                    }
                    "PAUSE" -> {
                        Log.d(TAG, "Executing PAUSE action")
                        session.player.pause()
                    }
                    "NEXT" -> {
                        Log.d(TAG, "Executing NEXT action")
                        session.player.seekToNextMediaItem()
                    }
                    "PREVIOUS" -> {
                        Log.d(TAG, "Executing PREVIOUS action")
                        session.player.seekToPreviousMediaItem()
                    }
                    "LIKE" -> {
                        Log.d(TAG, "Executing LIKE action")
                        playbackManager.toggleCurrentSongFavorite()
                    }
                    "SHUFFLE" -> {
                        Log.d(TAG, "Executing SHUFFLE action")
                        playbackManager.toggleShuffle()
                        updateMediaStyleNotification()
                    }
                    else -> Log.d(TAG, "Unknown action: $action")
                }
            } else {
                Log.e(TAG, "MediaSession is STILL null after ensureSession, cannot process action $action")
            }
        }

        return super.onStartCommand(intent, flags, startId)
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

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}