package com.soundly.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.*
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import com.google.common.collect.ImmutableList
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouterParams
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.soundly.MainActivity
import com.soundly.R
import android.os.PowerManager
import com.soundly.ui.widgets.MusicWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import com.soundly.data.model.Song
import com.soundly.data.repository.AudioSettings
import com.soundly.data.repository.AudioSettingsRepository
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.NormalizationLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@UnstableApi
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val artistRepository: com.soundly.data.repository.ArtistRepository,
    private val audioSettingsRepository: AudioSettingsRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lyricsRepository by lazy { LyricsRepository(context) }
    private var progressJob: Job? = null
    private var crossfadeJob: Job? = null
    private var exposureJob: Job? = null
    private var lyricsJob: Job? = null
    private var artistJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var sleepTargetRealtimeMs: Long? = null
    
    private var lastHighVolNotificationMs: Long = 0L

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioServer = LocalAudioServer(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Permite que el PlaybackService (o cualquier host) pida un refresco de la
    // notificación cuando cambia algo que NO es un evento nativo del Player,
    // por ejemplo marcar/desmarcar favorito. Se setea desde PlaybackService.onCreate()
    // y se limpia en onServiceDestroyed() para no retener una referencia al Service.
    var notificationUpdateListener: (() -> Unit)? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> activePlayer.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> activePlayer.volume = 0.2f
            AudioManager.AUDIOFOCUS_GAIN -> {
                activePlayer.volume = 1.0f
                activePlayer.play()
            }
        }
    }

    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val monoProcessor1 = MonoAudioProcessor()
    private val monoProcessor2 = MonoAudioProcessor()

    private val player1: ExoPlayer by lazy { createExoPlayer(monoProcessor1) }
    private val player2: ExoPlayer by lazy { createExoPlayer(monoProcessor2) }

    private var activePlayer: Player
    private var shadowPlayer: ExoPlayer
    private var isCrossfading = false

    private val castPlayer: CastPlayer by lazy {
        CastPlayer(CastContext.getSharedInstance(context), DefaultMediaItemConverter())
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            setCurrentPlayer(castPlayer)
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            setCurrentPlayer(castPlayer)
        }
        override fun onSessionEnded(session: CastSession, error: Int) {
            setCurrentPlayer(player1)
        }
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    private class AudioEffectsContainer {
        var loudnessEnhancer: LoudnessEnhancer? = null
        var equalizer: Equalizer? = null
        var bassBoost: BassBoost? = null
        var virtualizer: Virtualizer? = null
        var lastSid: Int = C.AUDIO_SESSION_ID_UNSET

        fun release() {
            loudnessEnhancer?.release(); loudnessEnhancer = null
            equalizer?.release(); equalizer = null
            bassBoost?.release(); bassBoost = null
            virtualizer?.release(); virtualizer = null
            lastSid = C.AUDIO_SESSION_ID_UNSET
        }
    }

    private val player1Effects = AudioEffectsContainer()
    private val player2Effects = AudioEffectsContainer()
    private val globallyFailedEffectTypes = mutableSetOf<String>()
    
    private var currentAudioSettings = AudioSettings()

    private fun createExoPlayer(processor: AudioProcessor): ExoPlayer {
        val renderersFactory: RenderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(ctx: Context, float: Boolean, params: Boolean): AudioSink {
                return DefaultAudioSink.Builder(ctx).setAudioProcessors(arrayOf(processor)).build()
            }
        }
        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttrs, false)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player != activePlayer) return
            
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) {
                updateMetadata()
                updateCustomLayout()
            }
            if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                if (activePlayer.playWhenReady && currentAudioSettings.isResting) {
                    val now = System.currentTimeMillis()
                    if (now < currentAudioSettings.restEndTime) {
                        activePlayer.pause()
                        Log.d("PlaybackManager", "Safe Playback: Rest Mode active. Playback blocked.")
                    } else {
                        // Rest period ended automatically
                        scope.launch { audioSettingsRepository.updateIsResting(false, 0L) }
                    }
                }
            }
            if (events.containsAny(Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_TIMELINE_CHANGED)) {
                pushPlaybackState()
                handleProgressTicker()
                // Pide al servicio que refresque la notificación MediaStyle
                // (play/pause, título, etc). Antes esto intentaba castear el
                // ApplicationContext a PlaybackService, lo cual nunca funcionaba.
                notificationUpdateListener?.invoke()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED && activePlayer.repeatMode == Player.REPEAT_MODE_ONE) {
                activePlayer.seekTo(activePlayer.currentMediaItemIndex, 0L)
                activePlayer.playWhenReady = true
            }
        }

        override fun onAudioSessionIdChanged(id: Int) {
            // No usamos el parámetro 'id' directamente, sino que aplicamos al player que lanzó el evento
            // El parámetro 'player' no está disponible en este scope de sobrecarga simple, 
            // así que usamos la lógica correcta para identificar cuál cambió
            applyAudioEffects(player1)
            applyAudioEffects(player2)
        }
    }

    var mediaSession: MediaSession? = null
        private set

    private var playlist: List<Song> = emptyList()
    private var hasInitializedQueue = false
    private var restoredQueue = false
    private var lastPersistWallTimeMs: Long = 0L
    private var lastWidgetUpdateMs: Long = 0L
    private var lastWidgetTitle: String? = null
    private var lastWidgetIsPlaying: Boolean = false
    private var lastWidgetArtwork: Uri? = null
    private var lastWidgetArtworkBitmap: Bitmap? = null
    private var widgetRevision: Int = 0
    private var lastLyricsRequestKey: String? = null
    private var lastArtistRequestKey: String? = null
    private var favoriteSongIds: Set<Long> = emptySet()
    private var isSessionWarm = false
    private var hasRequestedServiceStart = false

    init {
        activePlayer = player1
        shadowPlayer = player2
        player1.addListener(playerListener)
        player2.addListener(playerListener)
        castPlayer.addListener(playerListener)

        // Iniciar servidor local y registrar listener de Cast
        try {
            audioServer.start()
            CastContext.getSharedInstance(context).sessionManager.addSessionManagerListener(
                sessionManagerListener, CastSession::class.java
            )
        } catch (e: Exception) {
            Log.w("PlaybackManager", "Cast SDK or Server not available", e)
        }

        scope.launch { audioSettingsRepository.audioSettingsFlow.collect { applyAudioSettings(it) } }
        scope.launch {
            repository.librarySongsFlow.collect { songs ->
                if (songs.isNotEmpty()) {
                    playlist = songs
                    prewarm()
                    if (!restoredQueue) restoreSavedQueue(songs)
                }
            }
        }
        scope.launch { repository.favoriteSongIdsFlow.collect { favoriteSongIds = it; syncFavoriteState() } }
    }

    fun prewarm() {
        scope.launch { ensureSession() }
    }

    private fun requestAudioFocus(): Boolean {
        if (!currentAudioSettings.audioFocusEnabled) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AndroidAudioAttributes.Builder()
                    .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
                    .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    fun ensureSession(serviceContext: Context = context) {
        Log.d("PlaybackManager", "ensureSession called, current session: $mediaSession")
        if (mediaSession != null) { isSessionWarm = true; return }
        val intent = PendingIntent.getActivity(serviceContext, 0, Intent(serviceContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val callback = object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand("ACTION_LIKE", Bundle.EMPTY))
                    .add(SessionCommand("ACTION_SHUFFLE", Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(getCustomLayoutList())
                    .build()
            }

            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                when (command.customAction) {
                    "ACTION_LIKE" -> toggleCurrentSongFavorite()
                    "ACTION_SHUFFLE" -> toggleShuffle()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(serviceContext, activePlayer)
            .setSessionActivity(intent)
            .setCallback(callback)
            .setCustomLayout(getCustomLayoutList())
            .build()

        isSessionWarm = true
    }

    private fun getCustomLayoutList(): ImmutableList<CommandButton> {
        val id = activePlayer.currentMediaItem?.mediaId?.toLongOrNull()
        val isFav = id != null && id in favoriteSongIds

        val likeBtn = CommandButton.Builder()
            .setDisplayName("Like")
            .setSessionCommand(SessionCommand("ACTION_LIKE", Bundle.EMPTY))
            .setCustomIconResId(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            .build()

        val shuffleBtn = CommandButton.Builder()
            .setDisplayName("Shuffle")
            .setSessionCommand(SessionCommand("ACTION_SHUFFLE", Bundle.EMPTY))
            .setCustomIconResId(R.drawable.ic_shuffle)
            .setEnabled(true)
            .build()

        return ImmutableList.of(likeBtn, shuffleBtn)
    }

    private fun updateCustomLayout() {
        mediaSession?.setCustomLayout(getCustomLayoutList())
        // El customLayout de la sesión no es lo que dibuja nuestra notificación manual
        // en PlaybackService.onUpdateNotification(), así que pedimos un refresco explícito
        // para que los íconos de Like/Shuffle se mantengan sincronizados.
        notificationUpdateListener?.invoke()
    }

    fun playPause() {
        if (activePlayer.playWhenReady) {
            activePlayer.pause()
            stopCrossfade(jumpToFinish = false) // Mantener volúmenes actuales al pausar si se desea, o normalizar
            abandonAudioFocus()
            persistCurrentPosition()
        } else {
            if (requestAudioFocus()) {
                ensureServiceRunning()
                if (!isSessionWarm) ensureSession()
                activePlayer.playWhenReady = true
                activePlayer.play()
            }
        }
    }

    fun playCollection(queue: List<Song>, startShuffled: Boolean) {
        if (queue.isEmpty()) return
        val startSong = if (startShuffled) queue.random() else queue.first()
        play(song = startSong, queue = queue, forceShuffle = startShuffled)
    }

    fun playNext(song: Song) {
        enqueueSongs(listOf(song), insertAfterCurrent = true)
    }

    fun playNext(songs: List<Song>) {
        enqueueSongs(songs, insertAfterCurrent = true)
    }

    fun addToQueue(song: Song) {
        enqueueSongs(listOf(song), insertAfterCurrent = false)
    }

    fun addToQueue(songs: List<Song>) {
        enqueueSongs(songs, insertAfterCurrent = false)
    }

    private fun enqueueSongs(songs: List<Song>, insertAfterCurrent: Boolean) {
        if (songs.isEmpty()) return
        
        if (!hasInitializedQueue || playlist.isEmpty()) {
            play(song = songs.first(), queue = songs, forceShuffle = false)
            return
        }

        val currentIndex = activePlayer.currentMediaItemIndex.takeIf { it >= 0 } ?: (playlist.lastIndex).coerceAtLeast(0)
        val insertionIndex = if (insertAfterCurrent) (currentIndex + 1).coerceAtMost(playlist.size) else playlist.size

        val updatedQueue = playlist.toMutableList().apply { addAll(insertionIndex, songs) }
        val items = songs.map { it.toMediaItem() }
        
        activePlayer.addMediaItems(insertionIndex, items)
        shadowPlayer.addMediaItems(insertionIndex, items)
        playlist = updatedQueue

        val currentId = activePlayer.currentMediaItem?.mediaId?.toLongOrNull() ?: songs.first().id
        persistState(updatedQueue, currentId, activePlayer.currentPosition.coerceAtLeast(0L), activePlayer.shuffleModeEnabled, activePlayer.repeatMode)
        pushPlaybackState()
    }

    fun play(song: Song, queue: List<Song>, forceShuffle: Boolean? = null) {
        scope.launch {
            stopCrossfade(jumpToFinish = true)
            normalizeRepeatForManualAction()
            if (!isSessionWarm) ensureSession()
            if (requestAudioFocus()) {
                val shuffle = forceShuffle ?: activePlayer.shuffleModeEnabled
                val repeat = activePlayer.repeatMode
                val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                val items = withContext(Dispatchers.Default) { queue.map { it.toMediaItem() } }

                activePlayer.setMediaItems(items, index, 0L)
                shadowPlayer.setMediaItems(items, index, 0L)
                activePlayer.shuffleModeEnabled = shuffle; shadowPlayer.shuffleModeEnabled = shuffle
                activePlayer.repeatMode = repeat; shadowPlayer.repeatMode = repeat

                playlist = queue; hasInitializedQueue = true
                activePlayer.prepare(); activePlayer.playWhenReady = true
                ensureServiceRunning()
                persistState(queue, song.id, 0L, shuffle, repeat)
            }
        }
    }

    fun next() { stopCrossfade(jumpToFinish = true); if (activePlayer.hasNextMediaItem()) { activePlayer.seekToNextMediaItem(); activePlayer.playWhenReady = true } }
    fun previous() { stopCrossfade(jumpToFinish = true); if (activePlayer.hasPreviousMediaItem()) activePlayer.seekToPreviousMediaItem() else activePlayer.seekTo(0) }

    private fun stopCrossfade(jumpToFinish: Boolean) {
        crossfadeJob?.cancel()
        if (isCrossfading) {
            shadowPlayer.stop()
            if (jumpToFinish) activePlayer.volume = 1f
            isCrossfading = false
        }
    }

    fun seekTo(pos: Long) { stopCrossfade(jumpToFinish = true); activePlayer.seekTo(pos); persistCurrentPosition() }

    fun toggleShuffle() { activePlayer.shuffleModeEnabled = !activePlayer.shuffleModeEnabled; shadowPlayer.shuffleModeEnabled = activePlayer.shuffleModeEnabled; persistCurrentPosition(); updateCustomLayout(); pushPlaybackState() }

    fun toggleCurrentSongFavorite() {
        val songId = activePlayer.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                repository.toggleSongFavorite(songId)
            }
        }
    }

    fun cycleRepeatMode() {
        val next = when (activePlayer.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
        activePlayer.repeatMode = next; shadowPlayer.repeatMode = next; persistCurrentPosition(); updateCustomLayout(); pushPlaybackState()
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from !in playlist.indices || to !in playlist.indices) return
        val updatedQueue = playlist.toMutableList().apply {
            val item = removeAt(from)
            add(to, item)
        }
        playlist = updatedQueue
        activePlayer.moveMediaItem(from, to)
        shadowPlayer.moveMediaItem(from, to)
        persistCurrentPosition()
        pushPlaybackState()
    }

    private fun updateMetadata() {
        val meta = activePlayer.currentMediaItem?.mediaMetadata ?: activePlayer.mediaMetadata
        val id = activePlayer.currentMediaItem?.mediaId?.toLongOrNull()
        val index = activePlayer.currentMediaItemIndex
        id?.let { scope.launch { repository.recordSongPlay(it) } }
        
        loadQueueArtworks(index)

        // Limpiar estado visual previo para forzar actualización del widget y evitar "fantaseo" de carátulas
        _uiState.value = _uiState.value.copy(
            artworkBitmap = null,
            backgroundGradientSquare = null,
            backgroundGradientWide = null,
            currentBackgroundColor = 0xFF121212.toInt()
        )

        // Procesar todo lo visual una sola vez por canción
        val artUri = meta.artworkUri
        scope.launch {
            var finalBitmap: Bitmap? = null
            var bgColor = 0xFF121212.toInt()
            var gradientSquare: Bitmap? = null
            var gradientWide: Bitmap? = null

            if (artUri != null) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(artUri)
                        .size(500, 500)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    if (result is SuccessResult) {
                        finalBitmap = result.drawable.toBitmap()
                        val palette = Palette.from(finalBitmap!!).generate()
                        bgColor = palette.getDarkVibrantColor(palette.getDominantColor(0xFF121212.toInt()))
                        
                        // Colores: Inicio más claro, Fin más oscuro
                        val startColor = androidx.core.graphics.ColorUtils.blendARGB(bgColor, android.graphics.Color.WHITE, 0.25f)
                        val endColor = androidx.core.graphics.ColorUtils.blendARGB(bgColor, android.graphics.Color.BLACK, 0.15f)
                        
                        // Generamos dos versiones para evitar distorsión en las curvas (1:1 y 2.08:1)
                        gradientSquare = createVisualBackground(startColor, endColor, 500, 500)
                        gradientWide = createVisualBackground(startColor, endColor, 500, 240)
                    }
                } catch (e: Exception) { Log.e("PlaybackManager", "Visual processing error", e) }
            }
            
            _uiState.value = _uiState.value.copy(
                currentBackgroundColor = bgColor,
                artworkBitmap = finalBitmap,
                backgroundGradientSquare = gradientSquare,
                backgroundGradientWide = gradientWide
            )
            pushPlaybackState()
            // Avisar al servicio que ya tenemos la carátula lista para la notificación
            notificationUpdateListener?.invoke()
        }

        _uiState.value = _uiState.value.copy(
            currentSongId = id, title = meta.title?.toString().orEmpty(), artist = meta.artist?.toString().orEmpty(),
            artworkUri = artUri, isCurrentSongFavorite = id != null && id in favoriteSongIds,
            durationMs = if (activePlayer.duration != C.TIME_UNSET) activePlayer.duration else 0L,
            canSkipNext = activePlayer.hasNextMediaItem(), canSkipPrevious = activePlayer.hasPreviousMediaItem()
        )
        fetchLyricsForCurrent(meta); fetchArtistInfoForCurrent(meta); pushPlaybackState()
    }

    private fun loadQueueArtworks(currentIndex: Int) {
        val nextItems = playlist.drop(currentIndex + 1).take(10)
        Log.d("PlaybackManager", "Loading queue artworks for ${nextItems.size} items")
        scope.launch(Dispatchers.IO) {
            val newArtworks = _uiState.value.queueArtworks.toMutableMap()
            var changed = false
            nextItems.forEach { song ->
                if (!newArtworks.containsKey(song.id)) {
                    val artUri = repository.getAlbumArtUri(song.albumId)
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(artUri)
                            .size(150, 150)
                            .allowHardware(true)
                            .build()
                        val result = context.imageLoader.execute(request)
                        if (result is SuccessResult) {
                            newArtworks[song.id] = result.drawable.toBitmap()
                            changed = true
                            Log.d("PlaybackManager", "Loaded artwork for song: ${song.title}")
                        } else {
                            Log.w("PlaybackManager", "Failed to load artwork for song: ${song.title}")
                        }
                    } catch (e: Exception) { 
                        Log.e("PlaybackManager", "Error loading queue artwork for ${song.title}", e)
                    }
                }
            }
            
            if (changed) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(queueArtworks = newArtworks)
                    pushPlaybackState()
                }
            }
        }
    }

    private fun createVisualBackground(startColor: Int, endColor: Int, w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true; isDither = true }
        
        // 1. Fondo Diagonal
        val gradient = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        
        // 2. Borde de Cristal Real "Al Ras"
        val strokeWidth = 3f 
        val radius = 38f //se adapta a la vista
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        val borderGradient = LinearGradient(0f, 0f, 0f, h.toFloat(), 
            android.graphics.Color.argb(180, 255, 255, 255),
            android.graphics.Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        borderPaint.shader = borderGradient
        
        // Dibujamos el borde con el redondeado exacto para que NO se vea recortado por el padre,
        // sino que fluya exactamente por la misma línea.
        val inset = strokeWidth / 2f
        canvas.drawRoundRect(
            inset, inset, w.toFloat() - inset, h.toFloat() - inset, 
            radius, radius, 
            borderPaint
        )
        
        return bitmap
    }

    private fun pushPlaybackState() {
        val id = activePlayer.currentMediaItem?.mediaId?.toLongOrNull()
        val sleepRemaining = sleepTargetRealtimeMs?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) }
        val isCasting = activePlayer === castPlayer
        val castName = if (isCasting) {
            try {
                CastContext.getSharedInstance(context).sessionManager.currentCastSession?.castDevice?.friendlyName
            } catch (e: Exception) { null }
        } else null

        _uiState.value = _uiState.value.copy(
            currentSongId = id, currentSongIndex = activePlayer.currentMediaItemIndex, isPlaying = activePlayer.isPlaying,
            isCurrentSongFavorite = id != null && id in favoriteSongIds, positionMs = activePlayer.currentPosition.coerceAtLeast(0L),
            durationMs = if (activePlayer.duration != C.TIME_UNSET) activePlayer.duration else 0L,
            isBuffering = activePlayer.playbackState == Player.STATE_BUFFERING, isShuffleEnabled = activePlayer.shuffleModeEnabled,
            repeatMode = activePlayer.repeatMode, sleepRemainingMs = sleepRemaining, queue = playlist,
            isCasting = isCasting, castDeviceName = castName
        )
        
        // Actualizar el widget de Glance de forma muy eficiente
        val now = System.currentTimeMillis()
        val titleChanged = lastWidgetTitle != _uiState.value.title
        val playStateChanged = lastWidgetIsPlaying != _uiState.value.isPlaying
        val artworkChanged = lastWidgetArtwork != _uiState.value.artworkUri
        val artworkBitmapChanged = lastWidgetArtworkBitmap != _uiState.value.artworkBitmap
        
        // Actualizar cada 800ms para asegurar fluidez extrema
        val shouldUpdateProgress = _uiState.value.isPlaying && now - lastWidgetUpdateMs >= 800
        
        if (titleChanged || playStateChanged || artworkChanged || artworkBitmapChanged || shouldUpdateProgress) {
            lastWidgetUpdateMs = now
            lastWidgetTitle = _uiState.value.title
            lastWidgetIsPlaying = _uiState.value.isPlaying
            lastWidgetArtwork = _uiState.value.artworkUri
            lastWidgetArtworkBitmap = _uiState.value.artworkBitmap
            if (artworkChanged || artworkBitmapChanged) widgetRevision++
            
            if (!powerManager.isInteractive) return

            scope.launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val ids = manager.getGlanceIds(MusicWidget::class.java)
                    if (ids.isEmpty()) return@launch

                    for (glanceId in ids) {
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[MusicWidget.KEY_TITLE] = _uiState.value.title
                            prefs[MusicWidget.KEY_ARTIST] = _uiState.value.artist
                            prefs[MusicWidget.KEY_IS_PLAYING] = _uiState.value.isPlaying
                            prefs[MusicWidget.KEY_POSITION] = _uiState.value.positionMs
                            prefs[MusicWidget.KEY_DURATION] = _uiState.value.durationMs
                            prefs[MusicWidget.KEY_IS_FAVORITE] = _uiState.value.isCurrentSongFavorite
                            prefs[MusicWidget.KEY_BG_COLOR] = _uiState.value.currentBackgroundColor
                            prefs[MusicWidget.KEY_REVISION] = widgetRevision
                        }
                        // Es esencial llamar a update para que la barra de progreso y botones cambien en tiempo real
                        MusicWidget().update(context, glanceId)
                    }
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Widget update error", e)
                }
            }
        }
    }

    private fun handleProgressTicker() {
        if (activePlayer.isPlaying && progressJob?.isActive != true) {
            progressJob = scope.launch {
                while (activePlayer.isPlaying) {
                    pushPlaybackState(); maybePersistProgress(); checkCrossfadeTrigger(); delay(500)
                }
            }
        }
        handleExposureTracking()
    }

    private fun handleExposureTracking() {
        if (activePlayer.isPlaying && exposureJob?.isActive != true) {
            exposureJob = scope.launch {
                while (activePlayer.isPlaying) {
                    delay(60000) // Track every minute
                    if (activePlayer.isPlaying) {
                        trackAudioExposure()
                    }
                }
            }
        }
    }

    private fun trackAudioExposure() {
        if (!currentAudioSettings.safePlaybackEnabled) return

        val isHeadphones = isHeadphonesConnected()
        if (currentAudioSettings.ignoreSpeakerExposure && !isHeadphones) return

        // dB Monitoring
        if (currentAudioSettings.dbMonitoringEnabled) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val percent = (currentVol.toFloat() / maxVol) * 100
            val db = 40 + (percent * 0.5f)
            
            if (db > 85) {
                val now = System.currentTimeMillis()
                if (now - lastHighVolNotificationMs > 30 * 60 * 1000L) { // Limit to once every 30 mins
                    showSafePlaybackNotification(
                        context.getString(R.string.notification_safe_playback_high_vol_title),
                        context.getString(R.string.notification_safe_playback_high_vol_msg),
                        "HIGH_VOL"
                    )
                    lastHighVolNotificationMs = now
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            val currentStats = audioSettingsRepository.audioSettingsFlow.first()
            
            // Reset daily stats if it's a new day
            val lastReset = currentStats.lastExposureResetTimestamp
            val now = System.currentTimeMillis()
            val isNewDay = !isSameDay(lastReset, now)
            
            val newMinutes = if (isNewDay) 1 else currentStats.dailyExposureMinutes + 1
            audioSettingsRepository.updateDailyExposureMinutes(newMinutes)
            
            // Update weekly history
            val calendar = java.util.Calendar.getInstance()
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            audioSettingsRepository.updateWeeklyExposure(dayOfWeek, newMinutes)

            // Intelligent Volume Reduction
            val limitMinutes = when {
                currentStats.userAge < 12 -> 60
                currentStats.userAge < 18 -> 90
                else -> 120
            }

            if (currentAudioSettings.intelligentVolumeReduction && isHeadphones) {
                if (newMinutes > limitMinutes * 0.9) {
                    withContext(Dispatchers.Main) {
                        if (activePlayer.volume > 0.6f) {
                            activePlayer.volume = 0.6f
                            Log.d("PlaybackManager", "Safe Playback: Intelligent Volume Reduction applied")
                            showSafePlaybackNotification(
                                context.getString(R.string.notification_safe_playback_auto_reduce_title),
                                context.getString(R.string.notification_safe_playback_auto_reduce_msg),
                                "AUTO_REDUCE"
                            )
                        }
                    }
                }
            }

            // Forced Rest Mode
            if (currentAudioSettings.forcedRestEnabled && newMinutes >= limitMinutes) {
                if (!currentStats.isResting) {
                    val restDuration = 15 * 60 * 1000L // 15 minutes
                    withContext(Dispatchers.Main) {
                        activePlayer.pause()
                        audioSettingsRepository.updateIsResting(true, now + restDuration)
                        Log.d("PlaybackManager", "Safe Playback: Forced Rest Mode triggered")
                        showSafePlaybackNotification(
                            context.getString(R.string.notification_safe_playback_limit_title),
                            context.getString(R.string.notification_safe_playback_limit_msg),
                            "LIMIT_REACHED"
                        )
                    }
                }
            }
        }
    }

    private fun showSafePlaybackNotification(title: String, message: String, tag: String) {
        if (!currentAudioSettings.safePlaybackNotificationsEnabled) return
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel if needed
        if (notificationManager.getNotificationChannel("safe_playback_alerts") == null) {
            val channel = NotificationChannel(
                "safe_playback_alerts",
                context.getString(R.string.notification_channel_safe_playback),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, "safe_playback_alerts")
            .setSmallIcon(R.drawable.mono_soundly_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(tag.hashCode(), notification)
    }

    private fun isHeadphonesConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { 
            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun checkCrossfadeTrigger() {
        if (activePlayer !is ExoPlayer || !currentAudioSettings.crossfadeEnabled || isCrossfading) return
        val remaining = activePlayer.duration - activePlayer.currentPosition
        val trigger = currentAudioSettings.crossfadeDuration * 1000L
        if (remaining in 1..trigger && activePlayer.hasNextMediaItem()) startCrossfade()
    }

    private fun startCrossfade() {
        val currentPlayer = activePlayer
        if (currentPlayer !is ExoPlayer || !currentAudioSettings.crossfadeEnabled || isCrossfading) return

        isCrossfading = true
        val nextIndex = currentPlayer.nextMediaItemIndex
        val durationMs = currentAudioSettings.crossfadeDuration * 1000L

        crossfadeJob = scope.launch(Dispatchers.Main) {
            val fadingPlayer = currentPlayer
            val nextEngine = shadowPlayer

            nextEngine.seekTo(nextIndex, 0L)
            nextEngine.volume = 0f
            nextEngine.prepare()
            nextEngine.playWhenReady = true

            while (nextEngine.playbackState == Player.STATE_BUFFERING) { delay(50) }

            // --- EL CAMBIO DE MANDO PRO ---
            activePlayer = nextEngine
            shadowPlayer = fadingPlayer

            mediaSession?.setPlayer(activePlayer)
            updateMetadata() // UI cambia a Canción B AHORA
            applyAudioEffects(activePlayer)

            val steps = 40
            val stepTime = durationMs / steps
            for (i in 1..steps) {
                if (!isCrossfading) break
                val progress = i.toFloat() / steps
                fadingPlayer.volume = 1f - progress
                activePlayer.volume = progress
                delay(stepTime)
            }

            fadingPlayer.stop()
            fadingPlayer.volume = 1f
            isCrossfading = false
        }
    }

    private fun applyAudioSettings(s: AudioSettings) {
        monoProcessor1.setEnabled(s.monoEnabled); monoProcessor2.setEnabled(s.monoEnabled)
        currentAudioSettings = s
        applyAudioEffects(player1)
        applyAudioEffects(player2)
    }

    private fun applyAudioEffects(player: Player) {
        if (player !is ExoPlayer) return
        val sid = player.audioSessionId
        if (sid <= 0 || sid == C.AUDIO_SESSION_ID_UNSET) return
        val effects = if (player === player1) player1Effects else player2Effects

        if (effects.lastSid != sid) {
            effects.release()
            effects.lastSid = sid
        }

        try {
            // Normalización (LoudnessEnhancer)
            if (currentAudioSettings.normalizationEnabled && !globallyFailedEffectTypes.contains("loudness")) {
                try {
                    if (effects.loudnessEnhancer == null) {
                        effects.loudnessEnhancer = LoudnessEnhancer(sid)
                    }
                    val g = when (currentAudioSettings.normalizationLevel) {
                        NormalizationLevel.LOW -> 200
                        NormalizationLevel.NORMAL -> 400
                        NormalizationLevel.HIGH -> 800
                    }
                    effects.loudnessEnhancer?.setTargetGain(g)
                    effects.loudnessEnhancer?.enabled = true
                } catch (e: Exception) {
                    Log.w("PlaybackManager", "LoudnessEnhancer no disponible en este dispositivo")
                    globallyFailedEffectTypes.add("loudness")
                }
            } else {
                effects.loudnessEnhancer?.enabled = false
            }

            // Ecualizador
            if (!globallyFailedEffectTypes.contains("equalizer")) {
                try {
                    if (effects.equalizer == null) {
                        effects.equalizer = Equalizer(0, sid)
                    }
                    effects.equalizer?.let { eq ->
                        eq.enabled = currentAudioSettings.equalizerEnabled
                        if (eq.enabled) {
                            val bands = eq.numberOfBands
                            currentAudioSettings.equalizerBandLevels.forEach { (b, l) ->
                                if (b < bands) eq.setBandLevel(b.toShort(), l.toShort())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PlaybackManager", "Equalizer no disponible en este dispositivo")
                    globallyFailedEffectTypes.add("equalizer")
                }
            }

            // Bass Boost
            if (!globallyFailedEffectTypes.contains("bass")) {
                try {
                    if (effects.bassBoost == null) {
                        effects.bassBoost = BassBoost(0, sid)
                    }
                    effects.bassBoost?.let { bb ->
                        bb.enabled = currentAudioSettings.bassBoostStrength > 0
                        if (bb.enabled) bb.setStrength(currentAudioSettings.bassBoostStrength.toShort())
                    }
                } catch (e: Exception) {
                    Log.w("PlaybackManager", "BassBoost no disponible en este dispositivo")
                    globallyFailedEffectTypes.add("bass")
                }
            }

            // Virtualizer
            if (!globallyFailedEffectTypes.contains("virtualizer")) {
                try {
                    if (effects.virtualizer == null) {
                        effects.virtualizer = Virtualizer(0, sid)
                    }
                    effects.virtualizer?.let { vi ->
                        vi.enabled = currentAudioSettings.virtualizerStrength > 0
                        if (vi.enabled) vi.setStrength(currentAudioSettings.virtualizerStrength.toShort())
                    }
                } catch (e: Exception) {
                    Log.w("PlaybackManager", "Surround (Virtualizer) no disponible en este dispositivo")
                    globallyFailedEffectTypes.add("virtualizer")
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error inesperado al aplicar efectos de audio", e)
        }
    }

    fun getEqualizerBandFrequencies() = (0 until (player1Effects.equalizer?.numberOfBands ?: 5).toInt()).map { player1Effects.equalizer?.getCenterFreq(it.toShort()) ?: (60 * (it + 1) * (it + 1)) }
    fun getEqualizerBandLevelRange() = player1Effects.equalizer?.bandLevelRange?.map { it.toInt() }?.toIntArray() ?: intArrayOf(-1500, 1500)

    private fun Song.toMediaItem(): MediaItem {
        val uri = if (activePlayer === castPlayer) {
            Uri.parse(audioServer.getStreamUrl(path) ?: Uri.fromFile(File(path)).toString())
        } else {
            Uri.fromFile(File(path))
        }
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(repository.getAlbumArtUri(albumId))
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(meta)
            .build()
    }

    private fun persistState(q: List<Song>, id: Long, pos: Long, s: Boolean, r: Int) {
        scope.launch(Dispatchers.IO) { context.playbackDataStore.edit { it[KEY_QUEUE_IDS] = q.joinToString(",") { it.id.toString() }; it[KEY_CURRENT_ID] = id; it[KEY_POSITION] = pos; it[KEY_SHUFFLE] = s; it[KEY_REPEAT] = r.toLong() } }
    }

    private fun persistCurrentPosition() {
        val id = activePlayer.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        persistState(playlist, id, activePlayer.currentPosition, activePlayer.shuffleModeEnabled, activePlayer.repeatMode)
    }

    private fun maybePersistProgress() {
        val now = System.currentTimeMillis()
        if (now - lastPersistWallTimeMs >= 5000) { persistCurrentPosition(); lastPersistWallTimeMs = now }
    }

    private suspend fun restoreSavedQueue(songs: List<Song>) {
        val p = try { context.playbackDataStore.data.first() } catch (_: Exception) { return }
        val ids = p[KEY_QUEUE_IDS]?.split(",") ?: return
        val q = ids.mapNotNull { id -> songs.find { it.id == id.toLongOrNull() } }
        if (q.isEmpty()) return
        val id = p[KEY_CURRENT_ID] ?: q.first().id
        val idx = q.indexOfFirst { it.id == id }.coerceAtLeast(0)
        playlist = q; val items = q.map { it.toMediaItem() }
        activePlayer.setMediaItems(items, idx, p[KEY_POSITION] ?: 0L); shadowPlayer.setMediaItems(items, idx, 0L)
        activePlayer.shuffleModeEnabled = p[KEY_SHUFFLE] ?: false; shadowPlayer.shuffleModeEnabled = activePlayer.shuffleModeEnabled
        activePlayer.repeatMode = when (p[KEY_REPEAT]) { 1L -> Player.REPEAT_MODE_ONE; 2L -> Player.REPEAT_MODE_ALL; else -> Player.REPEAT_MODE_OFF }
        shadowPlayer.repeatMode = activePlayer.repeatMode
        activePlayer.prepare(); restoredQueue = true; updateMetadata()
    }

    private fun fetchArtistInfoForCurrent(m: MediaMetadata) {
        val name = m.artist?.toString().orEmpty()
        if (name.isBlank() || name == lastArtistRequestKey) return
        lastArtistRequestKey = name; artistJob?.cancel()
        artistJob = scope.launch {
            _uiState.value = _uiState.value.copy(
                artistInfo = _uiState.value.artistInfo.copy(isLoading = true),
                artistsInfo = emptyList()
            )
            
            // Usar el divisor inteligente centralizado
            val artistNames = com.soundly.data.model.splitArtistNames(name)
            
            val results = artistNames.map { artistName ->
                async { 
                    try {
                        artistRepository.getArtistInfo(artistName)
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll()
            
            val artistStates = artistNames.mapIndexed { index, artistName ->
                val info = results[index]
                if (info != null) {
                    ArtistUiState(
                        name = info.name,
                        description = info.bio.ifBlank { "Sin biografía disponible" },
                        imageUrl = info.imageUrl ?: "",
                        isLoading = false
                    )
                } else {
                    // Fallback crucial: Si falla el scraping, mostrar el artista de todos modos
                    // con su nombre original para que aparezca en el carrusel.
                    ArtistUiState(
                        name = artistName,
                        description = "Sin biografía disponible",
                        imageUrl = "",
                        isLoading = false
                    )
                }
            }
            
            _uiState.value = _uiState.value.copy(
                artistInfo = artistStates.firstOrNull() ?: ArtistUiState(error = "Sin información"),
                artistsInfo = artistStates
            )
        }
    }

    private fun fetchLyricsForCurrent(m: MediaMetadata) {
        val t = m.title?.toString().orEmpty(); val a = m.artist?.toString().orEmpty()
        if (t.isBlank() && a.isBlank()) return
        val song = currentSong(); val file = song?.path?.let { File(it) }
        val key = "${t}|${a}|${file?.absolutePath}"
        if (key == lastLyricsRequestKey) return
        lastLyricsRequestKey = key; lyricsJob?.cancel()
        lyricsJob = scope.launch {
            _uiState.value = _uiState.value.copy(lyrics = _uiState.value.lyrics.copy(isLoading = true))
            val l = lyricsRepository.loadLyrics(file, null, t, a, m.albumTitle?.toString(), activePlayer.duration)
            _uiState.value = _uiState.value.copy(lyrics = l.copy(isLoading = false))
        }
    }

    private fun syncFavoriteState() {
        val id = activePlayer.currentMediaItem?.mediaId?.toLongOrNull()
        _uiState.value = _uiState.value.copy(isCurrentSongFavorite = id != null && id in favoriteSongIds)
        updateCustomLayout()
    }

    private fun currentSong() = playlist.find { it.id == activePlayer.currentMediaItem?.mediaId?.toLongOrNull() }

    fun stopPlayback() {
        activePlayer.pause()
        activePlayer.stop()
        activePlayer.clearMediaItems()
        abandonAudioFocus()
        playlist = emptyList()
        context.stopService(Intent(context, PlaybackService::class.java))
        _uiState.value = PlayerUiState() // Reset UI state completely
    }

    fun onServiceDestroyed() {
        try {
            CastContext.getSharedInstance(context).sessionManager.removeSessionManagerListener(
                sessionManagerListener, CastSession::class.java
            )
        } catch (e: Exception) {}
        isSessionWarm = false
        hasRequestedServiceStart = false
        // Evita retener una referencia al Service después de que fue destruido.
        notificationUpdateListener = null
        mediaSession?.release()
        mediaSession = null
    }

    fun handleTaskRemoved(service: PlaybackService) {
        persistCurrentPosition()
        activePlayer.pause()
        onServiceDestroyed()
        service.stopSelf()
    }

    fun scheduleSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        if (durationMs <= 0) return
        sleepTargetRealtimeMs = System.currentTimeMillis() + durationMs
        sleepTimerJob = scope.launch {
            val ticker = launch {
                while (isActive) {
                    delay(1000)
                    pushPlaybackState()
                }
            }
            delay(durationMs)
            ticker.cancel()
            activePlayer.pause()
            persistCurrentPosition()
            context.stopService(Intent(context, PlaybackService::class.java))
            sleepTargetRealtimeMs = null
            pushPlaybackState()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTargetRealtimeMs = null
        pushPlaybackState()
    }

    fun release() {
        scope.cancel()
        audioServer.stop()
        mediaSession?.release()
        player1Effects.release()
        player2Effects.release()
        player1.release()
        player2.release()
    }
    private fun setCurrentPlayer(newPlayer: Player) {
        if (activePlayer === newPlayer) return

        val playWhenReady = activePlayer.playWhenReady
        val currentMediaItemIndex = activePlayer.currentMediaItemIndex
        val currentPosition = activePlayer.currentPosition

        activePlayer.stop()
        
        activePlayer = newPlayer
        
        if (activePlayer === castPlayer) {
            val items = playlist.map { it.toMediaItem() }
            activePlayer.setMediaItems(items, currentMediaItemIndex, currentPosition)
        }
        
        activePlayer.playWhenReady = playWhenReady
        activePlayer.prepare()

        mediaSession?.setPlayer(activePlayer)
        
        updateMetadata()
        pushPlaybackState()
    }

    private fun ensureServiceRunning() {
        if (!hasRequestedServiceStart) {
            hasRequestedServiceStart = true
            try {
                val intent = Intent(context, PlaybackService::class.java)

                // IMPORTANTE: En Android 14+ (API 34), llamar a startForegroundService desde el background
                // dispara ForegroundServiceStartNotAllowedException si no se cumplen requisitos estrictos.
                // MediaSessionService está diseñado para manejarse como un servicio normal que promociona a 
                // foreground cuando empieza la reproducción (onUpdateNotification). 
                // Usamos startService para evitar el crash inmediato.
                context.startService(intent)

                Log.d("PlaybackManager", "Started PlaybackService with startService")
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Failed to start service", e)
                hasRequestedServiceStart = false

                // Reintentar después de un breve delay
                scope.launch {
                    delay(1000)
                    hasRequestedServiceStart = false
                }
            }
        }
    }
    private fun normalizeRepeatForManualAction() { if (activePlayer.repeatMode == Player.REPEAT_MODE_ONE) { activePlayer.repeatMode = Player.REPEAT_MODE_ALL; pushPlaybackState() } }
    private fun createChannel(ctx: Context) { val m = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; if (m.getNotificationChannel(CHANNEL_ID) == null) m.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Reproducción", NotificationManager.IMPORTANCE_LOW)) }

    companion object {
        private const val CHANNEL_ID = "soundly_playback"; private const val NOTIFICATION_ID = 33
        private val KEY_QUEUE_IDS = stringPreferencesKey("queue_ids"); private val KEY_CURRENT_ID = longPreferencesKey("current_id")
        private val KEY_POSITION = longPreferencesKey("position_ms"); private val KEY_SHUFFLE = booleanPreferencesKey("shuffle_enabled"); private val KEY_REPEAT = longPreferencesKey("repeat_mode")
    }
}