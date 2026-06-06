package com.soundly.player

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.soundly.MainActivity
import com.soundly.R
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import com.soundly.debug.perfMark
import com.soundly.debug.perfTrace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playbackDataStore by preferencesDataStore(name = "playback_state")

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lyricsRepository by lazy { LyricsRepository(context) }
    private var progressJob: Job? = null
    private var lyricsJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttrs, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    var mediaSession: MediaSession? = null
        private set

    private var notificationManager: PlayerNotificationManager? = null
    private var playlist: List<Song> = emptyList()
    private var hasInitializedQueue = false
    private var restoredQueue = false
    private var lastPersistedPositionMs: Long = 0L
    private var lastPersistWallTimeMs: Long = 0L
    private var sleepTimerJob: Job? = null
    private var sleepTargetRealtimeMs: Long? = null
    private var lastLyricsRequestKey: String? = null
    private val artworkUriCache = android.util.LruCache<Long, Uri>(100)
    private var favoriteSongIds: Set<Long> = emptySet()
    private var isSessionWarm = false
    private var isWarmupScheduled = false
    private var hasRequestedServiceStart = false

    init {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_METADATA_CHANGED)) {
                    updateMetadata()
                }
                if (events.containsAny(Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_TIMELINE_CHANGED)) {
                    pushPlaybackState()
                    handleProgressTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && player.repeatMode == Player.REPEAT_MODE_ONE) {
                    player.seekTo(player.currentMediaItemIndex, 0L)
                    player.playWhenReady = true
                }
            }
        })

        scope.launch {
            repository.librarySongsFlow.collect { songs ->
                if (songs.isNotEmpty()) {
                    playlist = songs
                    prewarm()
                    if (!restoredQueue) {
                        restoreSavedQueue(songs)
                    }
                }
            }
        }
        scope.launch {
            repository.favoriteSongIdsFlow.collect { ids ->
                favoriteSongIds = ids
                syncFavoriteState()
            }
        }
    }

    fun prewarm() {
        if (isWarmupScheduled) return
        isWarmupScheduled = true
        scope.launch {
            perfTrace("PlaybackManager.prewarm") {
                perfTrace("PlaybackManager.ensureSession.prewarm") {
                    ensureSession()
                }
            }
        }
    }

    fun ensureSession(serviceContext: Context = context) {
        if (mediaSession != null && notificationManager != null) {
            isSessionWarm = true
            return
        }

        val sessionActivity = PendingIntent.getActivity(
            serviceContext,
            0,
            Intent(serviceContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (mediaSession == null) {
            mediaSession = MediaSession.Builder(serviceContext, player)
                .setSessionActivity(sessionActivity)
                .build()
        }

        attachNotification(serviceContext)
        isSessionWarm = true
    }

    fun startForegroundPlaceholder(service: PlaybackService) {
        // Minimal notification to satisfy startForegroundService timeout; updated later by PlayerNotificationManager.
        createChannel(service)
        val notification = NotificationCompat.Builder(service, CHANNEL_ID)
            .setContentTitle("Reproducción")
            .setContentText("Inicializando reproductor")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        try {
            service.startForeground(NOTIFICATION_ID, notification)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w("PlaybackManager", "Foreground service start blocked by system", e)
            service.stopSelf()
        } catch (e: IllegalStateException) {
            Log.w("PlaybackManager", "Foreground service start failed", e)
            service.stopSelf()
        }
    }

    fun playPause() {
        if (player.playWhenReady) {
            player.pause()
            persistCurrentPosition()
        } else {
            ensureServiceRunning()
            if (!isSessionWarm) {
                ensureSession()
            }
            player.playWhenReady = true
            player.play()
        }
    }

    fun play(song: Song, queue: List<Song>) {
        play(song = song, queue = queue, forceShuffle = null)
    }

    fun playCollection(queue: List<Song>, startShuffled: Boolean) {
        if (queue.isEmpty()) return
        perfMark("playCollection size=${queue.size} startShuffled=$startShuffled")
        val startSong = if (startShuffled) {
            queue.random()
        } else {
            queue.first()
        }
        play(song = startSong, queue = queue, forceShuffle = startShuffled)
    }

    fun playNext(song: Song) {
        enqueueSong(song = song, insertAfterCurrent = true)
    }

    fun addToQueue(song: Song) {
        enqueueSong(song = song, insertAfterCurrent = false)
    }

    private fun play(song: Song, queue: List<Song>, forceShuffle: Boolean?) {
        scope.launch {
            perfTrace("PlaybackManager.play") {
                normalizeRepeatForManualAction()
                if (!isSessionWarm) {
                    perfTrace("PlaybackManager.ensureSession.coldPlay") {
                        ensureSession()
                    }
                }
                val keepShuffle = forceShuffle ?: player.shuffleModeEnabled
                val keepRepeat = player.repeatMode
                val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                val canReuseQueue = hasInitializedQueue && playlist.matchesQueue(queue)

                val items = if (canReuseQueue) {
                    emptyList()
                } else {
                    withContext(Dispatchers.Default) {
                        perfTrace("PlaybackManager.mapQueueToMediaItems") {
                            queue.map { it.toMediaItem() }
                        }
                    }
                }

                perfMark(
                    "play queueSize=${queue.size} startIndex=$startIndex " +
                            "forceShuffle=$forceShuffle effectiveShuffle=$keepShuffle"
                )

                // Back to main thread for player interaction (already in Main scope, but ensure sequential)
                if (canReuseQueue) {
                    perfTrace("PlaybackManager.seekWithinQueue") {
                        player.seekTo(startIndex, 0L)
                    }
                } else {
                    perfTrace("PlaybackManager.setMediaItems") {
                        player.setMediaItems(items, startIndex, 0L)
                    }
                }
                player.shuffleModeEnabled = keepShuffle
                player.repeatMode = keepRepeat
                hasInitializedQueue = true
                playlist = queue
                player.playWhenReady = true
                if (!canReuseQueue) {
                    perfTrace("PlaybackManager.prepare") {
                        player.prepare()
                    }
                }
                perfTrace("PlaybackManager.ensureServiceRunning") {
                    ensureServiceRunning()
                }
                persistState(queue, song.id, 0L, keepShuffle, keepRepeat)
            }
        }
    }

    fun next() {
        normalizeRepeatForManualAction()
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.playWhenReady = true
        }
    }

    fun previous() {
        normalizeRepeatForManualAction()
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.playWhenReady = true
        } else {
            player.seekTo(0)
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        persistCurrentPosition()
        pushPlaybackState()
    }

    fun toggleCurrentSongFavorite() {
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            repository.toggleSongFavorite(songId)
        }
    }

    fun cycleRepeatMode() {
        val next = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
        persistCurrentPosition()
        pushPlaybackState()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        persistCurrentPosition()
    }

    private fun enqueueSong(
        song: Song,
        insertAfterCurrent: Boolean
    ) {
        if (!hasInitializedQueue || playlist.isEmpty()) {
            play(song = song, queue = listOf(song), forceShuffle = false)
            return
        }

        val currentIndex = player.currentMediaItemIndex.takeIf { it >= 0 } ?: (playlist.lastIndex).coerceAtLeast(0)
        val insertionIndex = if (insertAfterCurrent) {
            (currentIndex + 1).coerceAtMost(playlist.size)
        } else {
            playlist.size
        }

        val updatedQueue = playlist.toMutableList().apply {
            add(insertionIndex, song)
        }
        player.addMediaItem(insertionIndex, song.toMediaItem())
        playlist = updatedQueue

        val currentId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: song.id
        persistState(
            queue = updatedQueue,
            currentId = currentId,
            position = player.currentPosition.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeat = player.repeatMode
        )
        pushPlaybackState()
    }

    private fun updateMetadata() {
        val mediaMetadata = player.currentMediaItem?.mediaMetadata ?: player.mediaMetadata
        val currentSongId = player.currentMediaItem?.mediaId?.toLongOrNull()

        currentSongId?.let { id ->
            scope.launch {
                repository.recordSongPlay(id)
            }
        }

        _uiState.value = _uiState.value.copy(
            currentSongId = currentSongId,
            title = mediaMetadata.title?.toString().orEmpty(),
            artist = mediaMetadata.artist?.toString().orEmpty(),
            artworkUri = mediaMetadata.artworkUri,
            isCurrentSongFavorite = currentSongId != null && currentSongId in favoriteSongIds,
            durationMs = if (player.duration != C.TIME_UNSET) player.duration else 0L,
            canSkipNext = player.hasNextMediaItem(),
            canSkipPrevious = player.hasPreviousMediaItem()
        )
        fetchLyricsForCurrent(mediaMetadata)
        pushPlaybackState()
    }

    private fun fetchLyricsForCurrent(mediaMetadata: MediaMetadata) {
        val title = mediaMetadata.title?.toString().orEmpty()
        val artist = mediaMetadata.artist?.toString().orEmpty()
        if (title.isBlank() && artist.isBlank()) {
            lastLyricsRequestKey = null
            _uiState.value = _uiState.value.copy(lyrics = LyricsUiState())
            return
        }

        val currentSong = currentSong()
        val mediaUri = player.currentMediaItem?.localConfiguration?.uri
        val audioFile = currentSong?.path?.takeIf { it.isNotBlank() }?.let(::File)
            ?: mediaUri
                ?.takeIf { it.scheme.equals("file", ignoreCase = true) }
                ?.path
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
        val audioUri = audioFile?.let(Uri::fromFile) ?: mediaUri
        val requestKey = listOfNotNull(
            title,
            artist,
            audioFile?.absolutePath,
            audioUri?.toString()
        ).joinToString("|")
        if (requestKey == lastLyricsRequestKey && !_uiState.value.lyrics.isEmpty) return

        lastLyricsRequestKey = requestKey
        lyricsJob?.cancel()
        lyricsJob = scope.launch {
            val lyrics = runCatching {
                lyricsRepository.loadLyrics(
                    audioFile = audioFile,
                    audioUri = audioUri,
                    title = title,
                    artist = artist,
                    album = mediaMetadata.albumTitle?.toString()?.takeIf { it.isNotBlank() },
                    duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: currentSong?.duration
                )
            }.getOrDefault(LyricsUiState())

            _uiState.value = _uiState.value.copy(lyrics = lyrics)
        }
    }

    private fun pushPlaybackState() {
        val duration = if (player.duration != C.TIME_UNSET) player.duration else 0L
        val currentSongId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val sleepRemaining = sleepTargetRealtimeMs?.let { target ->
            val rem = target - System.currentTimeMillis()
            rem.coerceAtLeast(0L)
        }
        _uiState.value = _uiState.value.copy(
            currentSongId = currentSongId,
            isPlaying = player.isPlaying,
            isCurrentSongFavorite = currentSongId != null && currentSongId in favoriteSongIds,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
            durationMs = duration,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            isShuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            sleepRemainingMs = sleepRemaining
        )
    }

    private fun syncFavoriteState() {
        val currentSongId = player.currentMediaItem?.mediaId?.toLongOrNull()
        _uiState.value = _uiState.value.copy(
            currentSongId = currentSongId,
            isCurrentSongFavorite = currentSongId != null && currentSongId in favoriteSongIds
        )
    }

    private fun attachNotification(ctx: Context) {
        if (notificationManager != null || mediaSession == null) return

        createChannel(ctx)

        // Asegura permiso antes de notificar (Android 9+ requiere permiso declarativo ya en el manifest)
        val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.mediaMetadata.title ?: ""
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                return mediaSession?.sessionActivity
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return player.mediaMetadata.artist
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ) = null
        }

        notificationManager = PlayerNotificationManager.Builder(
            ctx,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(descriptionAdapter)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: android.app.Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing && ctx is PlaybackService) {
                        ctx.startForeground(notificationId, notification)
                    } else if (ongoing) {
                        NotificationManagerCompat.from(ctx).notify(notificationId, notification)
                    }
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopService()
                    if (ctx is PlaybackService) {
                        ctx.stopForeground(MediaSessionService.STOP_FOREGROUND_REMOVE)
                    }
                }
            })
            .setChannelImportance(NotificationManager.IMPORTANCE_LOW)
            .build()
            .apply {
                @Suppress("DEPRECATION")
                mediaSession?.sessionCompatToken?.let { setMediaSessionToken(it) }
                setUseNextAction(true)
                setUsePreviousAction(true)
                setSmallIcon(R.mipmap.ic_launcher)
                setPlayer(player)
            }
    }

    private fun ensureServiceRunning() {
        if (hasRequestedServiceStart) return
        hasRequestedServiceStart = true
        ContextCompat.startForegroundService(
            context,
            Intent(context, PlaybackService::class.java)
        )
    }

    private fun stopService() {
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    fun onServiceDestroyed() {
        isSessionWarm = false
        hasRequestedServiceStart = false
        notificationManager?.setPlayer(null)
        notificationManager = null
    }

    fun handleTaskRemoved(service: PlaybackService) {
        persistCurrentPosition()
        player.pause()
        isSessionWarm = false
        hasRequestedServiceStart = false
        notificationManager?.setPlayer(null)
        notificationManager = null
        service.stopForeground(MediaSessionService.STOP_FOREGROUND_REMOVE)
        service.stopSelf()
    }

    fun scheduleSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        if (durationMs <= 0) return
        sleepTargetRealtimeMs = System.currentTimeMillis() + durationMs
        sleepTimerJob = scope.launch {
            delay(durationMs)
            player.pause()
            persistCurrentPosition()
            stopService()
            sleepTargetRealtimeMs = null
            pushPlaybackState()
        }
        pushPlaybackState()
        // ticker para UI
        scope.launch {
            while (sleepTargetRealtimeMs != null) {
                pushPlaybackState()
                delay(1000)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTargetRealtimeMs = null
        pushPlaybackState()
    }

    fun release() {
        lyricsJob?.cancel()
        isSessionWarm = false
        hasRequestedServiceStart = false
        notificationManager?.setPlayer(null)
        mediaSession?.release()
        player.release()
    }

    private fun persistState(queue: List<Song>, currentId: Long, position: Long, shuffle: Boolean, repeat: Int) {
        if (queue.isEmpty()) return
        val ids = queue.joinToString(",") { it.id.toString() }
        scope.launch(Dispatchers.IO) {
            context.playbackDataStore.edit { prefs ->
                prefs[KEY_QUEUE_IDS] = ids
                prefs[KEY_CURRENT_ID] = currentId
                prefs[KEY_POSITION] = position
                prefs[KEY_SHUFFLE] = shuffle
                prefs[KEY_REPEAT] = repeat.toLong()
            }
        }
        lastPersistedPositionMs = position
        lastPersistWallTimeMs = System.currentTimeMillis()
    }

    private fun persistCurrentPosition() {
        val currentId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        if (playlist.isEmpty()) return
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode
        persistState(playlist, currentId, player.currentPosition.coerceAtLeast(0L), shuffle, repeat)
    }

    private fun maybePersistProgress() {
        val now = System.currentTimeMillis()
        val pos = player.currentPosition.coerceAtLeast(0L)
        val timeSince = now - lastPersistWallTimeMs
        val deltaPos = pos - lastPersistedPositionMs
        if (timeSince >= 5000 || deltaPos >= 5000) {
            persistCurrentPosition()
        }
    }

    private suspend fun restoreSavedQueue(currentSongs: List<Song>) {
        val prefs = try { context.playbackDataStore.data.first() } catch (_: Exception) { return }
        val idsStr = prefs[KEY_QUEUE_IDS] ?: return
        val ids = idsStr.split(",").mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return
        val queue = ids.mapNotNull { id -> currentSongs.find { it.id == id } }
        if (queue.isEmpty()) return

        val currentId = prefs[KEY_CURRENT_ID] ?: queue.first().id
        val position = prefs[KEY_POSITION] ?: 0L
        val shuffle = prefs[KEY_SHUFFLE] ?: false
        val repeat = (prefs[KEY_REPEAT] ?: Player.REPEAT_MODE_OFF.toLong()).toInt()
        val startIndex = queue.indexOfFirst { it.id == currentId }.coerceAtLeast(0)

        playlist = queue
        player.setMediaItems(queue.map { it.toMediaItem() }, startIndex, position)
        player.shuffleModeEnabled = shuffle
        player.repeatMode = repeat
        player.prepare()
        hasInitializedQueue = true
        restoredQueue = true
        updateMetadata()
        pushPlaybackState()
        lastPersistedPositionMs = position
        lastPersistWallTimeMs = System.currentTimeMillis()
    }

    private fun Song.toMediaItem(): MediaItem {
        val artworkUri = artworkUriCache.get(albumId) ?: run {
            val uri = repository.getAlbumArtUri(albumId)
            artworkUriCache.put(albumId, uri)
            uri
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .build()
        return MediaItem.Builder()
            .setUri(Uri.fromFile(File(path)))
            .setMediaId(id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    private fun List<Song>.matchesQueue(other: List<Song>): Boolean {
        if (size != other.size) return false
        return indices.all { index -> this[index].id == other[index].id }
    }

    private fun normalizeRepeatForManualAction() {
        if (player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.repeatMode = Player.REPEAT_MODE_ALL
            pushPlaybackState()
            persistCurrentPosition()
        }
    }

    private fun currentSong(): Song? {
        val currentId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return null
        return playlist.firstOrNull { it.id == currentId }
    }

    private fun handleProgressTicker() {
        if (player.isPlaying) {
            if (progressJob?.isActive != true) {
                progressJob = scope.launch {
                    while (player.isPlaying) {
                        pushPlaybackState()
                        maybePersistProgress()
                        delay(50)
                    }
                }
            }
        } else {
            progressJob?.cancel()
            progressJob = null
        }
    }

    companion object {
        private const val CHANNEL_ID = "soundly_playback"
        private const val NOTIFICATION_ID = 33
        private val KEY_QUEUE_IDS = stringPreferencesKey("queue_ids")
        private val KEY_CURRENT_ID = longPreferencesKey("current_id")
        private val KEY_POSITION = longPreferencesKey("position_ms")
        private val KEY_SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        private val KEY_REPEAT = longPreferencesKey("repeat_mode")
        private fun createChannel(ctx: Context) {
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Reproducción",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Controles de reproducción"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
