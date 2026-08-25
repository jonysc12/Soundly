package com.soundly.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.ColorFilter
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
import com.soundly.R
import com.soundly.player.PlayerUiState
import com.soundly.data.model.Song
import android.content.ContentUris
import android.net.Uri
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.action.actionParametersOf
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.EntryPointAccessors
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.*
import android.os.PowerManager
import androidx.glance.appwidget.LinearProgressIndicator
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.text.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@UnstableApi
class MusicWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        private val SMALL_SQUARE = DpSize(120.dp, 120.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(250.dp, 120.dp)
        private val BIG_SQUARE = DpSize(200.dp, 200.dp)
        private val TALL_LIST = DpSize(200.dp, 360.dp)

        // Preference Keys for State Management
        val KEY_TITLE = stringPreferencesKey("title")
        val KEY_ARTIST = stringPreferencesKey("artist")
        val KEY_IS_PLAYING = booleanPreferencesKey("is_playing")
        val KEY_POSITION = longPreferencesKey("position_ms")
        val KEY_DURATION = longPreferencesKey("duration_ms")
        val KEY_IS_FAVORITE = booleanPreferencesKey("is_favorite")
        val KEY_BG_COLOR = intPreferencesKey("bg_color")
        val KEY_REVISION = intPreferencesKey("revision")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECTANGLE, BIG_SQUARE, TALL_LIST)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        
        provideContent {
            val playbackManager = entryPoint.playbackManager()
            val prefs = currentState<Preferences>()
            
            // Acceso directo al value para asegurar que al recomponer por Preferences
            // obtengamos los Bitmaps más recientes del PlaybackManager.
            // En Glance esto es seguro ya que las actualizaciones son manuales.
            @Suppress("StateFlowValueCalledInComposition")
            val currentUiState = playbackManager.uiState.value
            
            // Combinamos el estado persistente (metadatos/progreso) con el estado volátil (bitmaps)
            val uiState = currentUiState.copy(
                title = prefs[KEY_TITLE] ?: currentUiState.title,
                artist = prefs[KEY_ARTIST] ?: currentUiState.artist,
                isPlaying = prefs[KEY_IS_PLAYING] ?: currentUiState.isPlaying,
                positionMs = prefs[KEY_POSITION] ?: currentUiState.positionMs,
                durationMs = prefs[KEY_DURATION] ?: currentUiState.durationMs,
                isCurrentSongFavorite = prefs[KEY_IS_FAVORITE] ?: currentUiState.isCurrentSongFavorite,
                currentBackgroundColor = prefs[KEY_BG_COLOR] ?: currentUiState.currentBackgroundColor
            )
            
            WidgetContent(context, uiState)
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun WidgetContent(context: Context, uiState: PlayerUiState) {
        val size = LocalSize.current
        val backgroundColor = Color(uiState.currentBackgroundColor.toLong() and 0xFFFFFFFFL)
        val isLightBackground = ColorUtils.calculateLuminance(backgroundColor.toArgb()) > 0.5
        val textColorValue = if (isLightBackground) Color.Black else Color.White
        val textColor = ColorProvider(textColorValue)
        val trackColor = ColorProvider(textColorValue.copy(alpha = 0.2f))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selección inteligente del bitmap según el aspect ratio para evitar distorsión en las curvas
            val ratio = size.width / size.height
            val gradientBitmap = if (ratio > 1.5f) uiState.backgroundGradientWide else uiState.backgroundGradientSquare
            
            if (gradientBitmap != null) {
                Image(
                    provider = ImageProvider(gradientBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(backgroundColor))) {}
            }

            // Contenido con padding
            Box(modifier = GlanceModifier.fillMaxSize().padding(14.dp)) {
                if (size.width >= HORIZONTAL_RECTANGLE.width && size.height < BIG_SQUARE.height) {
                    ModernLayout(context, uiState, textColor, trackColor, textColorValue)
                } else if (size.height >= BIG_SQUARE.height) {
                    BigSquareWithQueueLayout(context, uiState, textColor, trackColor, textColorValue)
                } else {
                    SmallLayout(context, uiState, textColor, ColorProvider(backgroundColor))
                }
            }
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun SmallLayout(context: Context, uiState: PlayerUiState, textColor: ColorProvider, backgroundProvider: ColorProvider) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageProvider = uiState.artworkBitmap?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.mono_soundly_logo)
            Image(
                provider = imageProvider,
                contentDescription = context.getString(R.string.cd_artwork),
                modifier = GlanceModifier.size(60.dp).cornerRadius(12.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(GlanceModifier.height(8.dp))
            CircleIconButton(
                imageProvider = ImageProvider(if (uiState.isPlaying) R.drawable.ic_pause_rounded else R.drawable.ic_play_rounded),
                contentDescription = context.getString(R.string.widget_play_pause_cd),
                onClick = actionRunCallback<PlayPauseAction>(),
                backgroundColor = textColor,
                contentColor = backgroundProvider
            )
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun ModernLayout(
        context: Context,
        uiState: PlayerUiState, 
        textColor: ColorProvider, 
        trackColor: ColorProvider,
        textColorValue: Color
    ) {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ModernLayoutContent(context, uiState, textColor, trackColor, textColorValue)
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun ModernLayoutContent(
        context: Context,
        uiState: PlayerUiState,
        textColor: ColorProvider,
        trackColor: ColorProvider,
        textColorValue: Color
    ) {
        val size = LocalSize.current
        // El texto se muestra si el widget es ancho (4 columnas, ~240dp) 
        // o si es alto mostrando la cola (>= 200dp de altura)
        val showButtonText = size.width >= 240.dp || size.height >= 200.dp
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageProvider = uiState.artworkBitmap?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.mono_soundly_logo)
            Image(
                provider = imageProvider,
                contentDescription = context.getString(R.string.cd_artwork),
                modifier = GlanceModifier.size(85.dp).cornerRadius(16.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(GlanceModifier.width(16.dp))
            Column(modifier = GlanceModifier.defaultWeight().padding(vertical = 4.dp)) {
                Text(
                    text = uiState.title.ifBlank { context.getString(R.string.app_name) },
                    style = TextStyle(color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = uiState.artist.ifBlank { context.getString(R.string.unknown_artist) },
                    style = TextStyle(color = ColorProvider(textColorValue.copy(alpha = 0.7f)), fontSize = 14.sp),
                    maxLines = 1

                )
                Spacer(GlanceModifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Previous Button
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_previous_rounded),
                        contentDescription = context.getString(R.string.cd_previous),
                        modifier = GlanceModifier.size(28.dp).clickable(actionRunCallback<PreviousAction>()),
                        colorFilter = ColorFilter.tint(textColor)
                    )
                    Spacer(GlanceModifier.width(16.dp))
                    
                    // Play Button Adaptable
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(textColorValue.copy(alpha = 0.2f)))
                            .cornerRadius(24.dp)
                            .then(if (showButtonText) GlanceModifier.padding(horizontal = 14.dp, vertical = 6.dp) else GlanceModifier.size(42.dp))
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(if (uiState.isPlaying) R.drawable.ic_pause_rounded else R.drawable.ic_play_rounded),
                                contentDescription = context.getString(R.string.widget_play_pause_cd),
                                modifier = GlanceModifier.size(if (showButtonText) 16.dp else 22.dp),
                                colorFilter = ColorFilter.tint(textColor)
                            )
                            if (showButtonText) {
                                Spacer(GlanceModifier.width(6.dp))
                                Text(
                                    text = if (uiState.isPlaying) context.getString(R.string.widget_pause) else context.getString(R.string.widget_play),
                                    style = TextStyle(color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                    Spacer(GlanceModifier.width(16.dp))
                    // Next Button
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_next_rounded),
                        contentDescription = context.getString(R.string.cd_next),
                        modifier = GlanceModifier.size(28.dp).clickable(actionRunCallback<NextAction>()),
                        colorFilter = ColorFilter.tint(textColor)
                    )
                    Spacer(GlanceModifier.width(16.dp))
                    // Favorite Button
                    Image(
                        provider = ImageProvider(if (uiState.isCurrentSongFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                        contentDescription = context.getString(R.string.cd_favorite),
                        modifier = GlanceModifier.size(24.dp).clickable(actionRunCallback<ToggleFavoriteAction>()),
                        colorFilter = ColorFilter.tint(textColor)
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                val progress = if (uiState.durationMs > 0) uiState.positionMs.toFloat() / uiState.durationMs.toFloat() else 0f
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = textColor,
                    backgroundColor = trackColor
                )
            }
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun BigSquareWithQueueLayout(
        context: Context,
        uiState: PlayerUiState,
        textColor: ColorProvider,
        trackColor: ColorProvider,
        textColorValue: Color
    ) {
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            item {
                Box(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                    ModernLayoutContent(context, uiState, textColor, trackColor, textColorValue)
                }
            }

            if (uiState.queue.isNotEmpty()) {
                item {
                    Column(modifier = GlanceModifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = "",
                            style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                        // Espacio exacto de 4dp entre el texto y la lista horizontal
                        Spacer(GlanceModifier.height(16.dp))
                        
                        val nextSongs = uiState.queue.drop(uiState.currentSongIndex + 1).take(8)
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            nextSongs.forEach { song ->
                                QueueItemHorizontal(song, uiState.queueArtworks[song.id], textColor, textColorValue)
                            }
                        }
                        Spacer(GlanceModifier.height(0.dp))
                    }
                }
            }
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun QueueItemHorizontal(
        song: Song,
        artwork: Bitmap?,
        textColor: ColorProvider,
        textColorValue: Color
    ) {
        Column(
            modifier = GlanceModifier
                .width(81.dp)
                .height(104.dp) // Contenedor cuadrado fijo
                .padding(4.dp)
                .clickable(actionRunCallback<PlaySongAction>(
                    actionParametersOf(PlaySongAction.KEY_SONG_ID to song.id)
                )),
            horizontalAlignment = Alignment.Start
        ) {
            val imageProvider = artwork?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.mono_soundly_logo)

            Image(
                provider = imageProvider,
                contentDescription = null,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight() // Toma todo el alto sobrante de forma dinámica
                    .cornerRadius(10.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = song.title,
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )

        }
    }
}
