@file:OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.soundly.ui.componentes

import android.graphics.RuntimeShader
import android.os.Build
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.player.PlayerUiState
import com.soundly.data.model.Song
import com.soundly.data.model.Playlist
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenu
import com.soundly.ui.componentes.edit.SongEditSheet
import com.soundly.data.repository.ProgressBarType
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.AnimationSpeed
import com.soundly.ui.theme.rememberArtworkShape
import kotlin.math.max

private const val PLAYER_WAVE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 color1;
    layout(color) uniform half4 color2;
    layout(color) uniform half4 color3;
    layout(color) uniform half4 bgColor;

    float2 hash(float2 p) {
        p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
        return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(dot(hash(i + float2(0.0, 0.0)), f - float2(0.0, 0.0)),
                       dot(hash(i + float2(1.0, 0.0)), f - float2(1.0, 0.0)), u.x),
                   mix(dot(hash(i + float2(0.0, 1.0)), f - float2(0.0, 1.0)),
                       dot(hash(i + float2(1.0, 1.0)), f - float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float t = iTime * 0.4;
        
        // 1. Warping estilo iOS: Deforma el espacio para que los colores "se conviertan unos en otros"
        float2 q = float2(
            noise(uv * 1.5 + t * 0.2),
            noise(uv * 1.5 - t * 0.3)
        );
        
        float2 warpedUv = uv + q * 0.15; // Distorsión líquida del espacio

        // 2. Mesh Gradient con puntos móviles sobre el espacio distorsionado
        float2 p1 = float2(0.5 + 0.35 * cos(t), 0.5 + 0.25 * sin(t * 1.1));
        float2 p2 = float2(0.2 + 0.3 * sin(t * 0.7), 0.7 + 0.2 * cos(t * 1.3));
        float2 p3 = float2(0.8 + 0.2 * cos(t * 0.9), 0.3 + 0.3 * sin(t * 1.5));
        float2 p4 = float2(0.5 + 0.4 * sin(t * 0.6), 0.4 + 0.35 * cos(t * 0.8));

        // Influencia de color con caída suave (Blur extremo)
        float d1 = 1.0 - smoothstep(0.0, 1.3, distance(warpedUv, p1));
        float d2 = 1.0 - smoothstep(0.0, 1.3, distance(warpedUv, p2));
        float d3 = 1.0 - smoothstep(0.0, 1.3, distance(warpedUv, p3));
        float d4 = 1.0 - smoothstep(0.0, 1.3, distance(warpedUv, p4));

        // Mezcla de colores (la variante blanca color2 tiene peso reducido 0.4)
        half4 color = color1 * d1;
        color += color2 * (d2 * 0.4); 
        color += color3 * d3;
        color += color1 * d4;

        color /= (d1 + d2 * 0.4 + d3 + d4 + 0.001);

        // 3. Olas: Ahora son visibles y tienen frecuencia para que parezcan olas reales
        float waveTop = sin(uv.x * 7.0 + t * 1.5) * 0.04 + 
                        sin(uv.x * 12.0 - t * 0.8) * 0.02 +
                        cos(uv.x * 15.0 + t) * 0.01;
        
        // Máscara superior: Suave transición para que los colores se vean en la cresta
        float maskTop = smoothstep(0.02, 0.25, uv.y + waveTop);

        // Fundido inferior al color de fondo
        float maskBot = smoothstep(0.9, 1.0, uv.y + sin(uv.x * 5.0 + t) * 0.02);
        
        half4 finalColor = mix(color, bgColor, maskBot);
        return finalColor * maskTop;
    }
"""

@Composable
fun ModernPlayerLayout(
    state: PlayerUiState,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (String, Long) -> Unit,
    onHideSong: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    userPlaylists: List<Playlist>,
    playlistMembershipBySong: Map<Long, Set<String>>,
    isWideLayout: Boolean,
    progressBarType: ProgressBarType,
    showThumb: Boolean,
    progressBarThickness: Float,
    artworkShape: ArtworkShape,
    windowInsets: WindowInsets?,
    vividColors: Boolean,
    lyricsExpansionSpeed: AnimationSpeed,
    useLyricsAgslAnimation: Boolean,
    textAlignCentered: Boolean,
    marqueeTextEnabled: Boolean,
    carouselEnabled: Boolean,
    colorScheme: PlayerColorScheme,
    animatedContainerColor: Color,
    animatedOnColor: Color,
    animatedSubColor: Color,
    animatedTertiaryColor: Color,
    animatedButtonSurface: Color,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentArtworkShape = rememberArtworkShape(artworkShape)
    
    // Sheets state
    var showSleepSheet by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val currentSong = remember(state.queue, state.currentSongId) {
        state.queue.find { it.id == state.currentSongId }
    }

    val activeRoute = rememberCurrentRoute()
    val deviceName = remember(activeRoute) { getRouteName(context, activeRoute) }
    val deviceIcon = remember(activeRoute) { getRouteIcon(activeRoute) }

    if (showSleepSheet) {
        SleepTimerSheet(
            remainingMs = state.sleepRemainingMs,
            onSelect = onSleepTimerSelected,
            onCancelTimer = onSleepTimerCancel,
            onDismiss = { showSleepSheet = false }
        )
    }

    if (showMenuSheet && currentSong != null) {
        SongOverflowMenu(
            song = currentSong,
            source = SongMenuSource.Library,
            userPlaylists = userPlaylists,
            playlistIdsContainingSong = playlistMembershipBySong[currentSong.id] ?: emptySet(),
            isFavorite = state.isCurrentSongFavorite,
            showMenu = true,
            onDismissRequest = { showMenuSheet = false },
            onPlayNext = { onPlayNext(currentSong) },
            onAddToQueue = { onAddToQueue(currentSong) },
            onOpenAlbum = { onOpenAlbum(it); onCollapse() },
            onOpenArtist = { onArtistClick(it); onCollapse() },
            onAddToPlaylist = { onAddToPlaylist(it, currentSong.id) },
            onToggleFavorite = onToggleFavorite,
            onDeleteSong = { onHideSong(currentSong.id) },
            onOpenSleepTimer = { showSleepSheet = true },
            onViewQueue = { showQueueSheet = true },
            onEditClick = { showEditSheet = true },
            showDeleteOption = false
        )
    }

    if (showEditSheet && currentSong != null) {
        SongEditSheet(
            song = currentSong,
            onDismissRequest = { showEditSheet = false }
        )
    }

    DeviceControlSheet(
        isOpen = showDeviceSheet,
        onDismiss = { showDeviceSheet = false },
        containerColor = animatedContainerColor,
        onColor = animatedOnColor,
        isCasting = state.isCasting
    )

    PlayerQueueSheet(
        isOpen = showQueueSheet,
        onDismiss = { showQueueSheet = false },
        queue = state.queue,
        currentSongId = state.currentSongId,
        currentSongIndex = state.currentSongIndex,
        isShuffleEnabled = state.isShuffleEnabled,
        repeatMode = state.repeatMode,
        isFavorite = state.isCurrentSongFavorite,
        onToggleShuffle = onToggleShuffle,
        onToggleFavorite = onToggleFavorite,
        onCycleRepeat = onCycleRepeat,
        onMoveItem = onMoveQueueItem,
        onPlaySong = onPlaySong,
        onPlayPause = onPlayPause,
        onSkipNext = onNext,
        isPlaying = state.isPlaying,
        title = state.title,
        artist = state.artist,
        artworkUri = state.artworkUri,
        durationMs = state.durationMs,
        positionMs = state.positionMs,
        containerColor = animatedContainerColor,
        onColor = animatedOnColor
    )

    // Animación de tiempo para el Shader
    val infiniteTransition = rememberInfiniteTransition(label = "player_waves")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(15000, easing = LinearEasing)
        ),
        label = "time"
    )

    // 2. EFECTO AGSL (Cubre desde la parte inferior con colores derivados de la carátula)
    val shaderColors = remember(animatedContainerColor) {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(animatedContainerColor.toArgb(), hsl)
        
        val base = animatedContainerColor
        
        // Variante suave (en lugar de blanca pura): Más luminosa pero con tinte del color original
        val lightHsl = hsl.copyOf()
        lightHsl[2] = (lightHsl[2] + 0.25f).coerceAtMost(0.9f)
        lightHsl[1] = (lightHsl[1] * 0.6f) 
        val light = Color(androidx.core.graphics.ColorUtils.HSLToColor(lightHsl))
        
        // Variante oscura/vibrante para dar profundidad al degradado
        val satHsl = hsl.copyOf()
        satHsl[1] = (satHsl[1] + 0.3f).coerceAtMost(1.0f)
        satHsl[2] = (satHsl[2] - 0.15f).coerceAtLeast(0.1f)
        val sat = Color(androidx.core.graphics.ColorUtils.HSLToColor(satHsl))
        
        listOf(base, light, sat)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedContainerColor)
    ) {
        // 1. CARATULA (Con máscara de fundido)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startY = size.height * 0.5f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.artworkUri)
                    .crossfade(800)
                    .allowHardware(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradiente superior (Barra de sistema)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
            )
        }

        // 2. EFECTO AGSL (Superpuesto para ocultar el corte)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = remember { RuntimeShader(PLAYER_WAVE_SHADER) }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f) // Aumentado para dar más margen vertical y evitar el corte
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        if (size.width > 0 && size.height > 0) {
                            shader.setFloatUniform("iResolution", size.width.toFloat(), size.height.toFloat())
                        }
                    }
            ) {
                shader.setFloatUniform("iTime", time)
                shader.setColorUniform("color1", shaderColors[0].toArgb())
                shader.setColorUniform("color2", shaderColors[1].toArgb())
                shader.setColorUniform("color3", shaderColors[2].toArgb())
                shader.setColorUniform("bgColor", animatedContainerColor.toArgb())
                drawRect(brush = ShaderBrush(shader))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, animatedContainerColor, animatedContainerColor)))
            )
        }

        // 3. CONTENIDO (Metadata y Controles)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {

            }

            Spacer(modifier = Modifier.weight(1f))

            // Información de la canción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = animatedOnColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = if (marqueeTextEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = if (marqueeTextEnabled) Modifier.basicMarquee() else Modifier
                    )
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = animatedSubColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = if (marqueeTextEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = (if (marqueeTextEnabled) Modifier.basicMarquee() else Modifier)
                            .clickable { onArtistClick(0L) }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = animatedOnColor.copy(alpha = 0.08f),
                        modifier = Modifier.size(50.dp)
                    ) {
                        IconButton(onClick = { showMenuSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = animatedOnColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Surface(
                        shape = CircleShape,
                        color = animatedOnColor.copy(alpha = 0.08f),
                        modifier = Modifier.size(50.dp)
                    ) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (state.isCurrentSongFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = animatedOnColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Barra de progreso y Controles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sección con padding estándar para progreso y botones
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val durationMs = max(state.durationMs, 1L)
                    val interpolatedProgress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs.toFloat() else 0f
                    
                    IsolatedProgressSection(
                        progressProvider = { interpolatedProgress },
                        progressBarType = progressBarType,
                        onValueChange = { onSeek((it * durationMs).toLong()) },
                        onValueChangeFinished = { },
                        sliderOffsetPx = 0f,
                        onColor = animatedOnColor,
                        subColor = animatedSubColor,
                        showThumb = showThumb,
                        waveHeight = if (state.isPlaying) 7f else 0f,
                        progressBarThickness = progressBarThickness,
                        durationMs = durationMs,
                        timeStyle = MaterialTheme.typography.labelSmall,
                        sliderInteraction = remember { MutableInteractionSource() }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    null,
                                    tint = animatedOnColor,
                                    modifier = Modifier.size(55.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = onPlayPause,
                                modifier = Modifier.size(100.dp)
                            ) {
                                Icon(
                                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    null,
                                    tint = animatedOnColor,
                                    modifier = Modifier.size(65.dp)
                                )
                            }

                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(55.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    null,
                                    tint = animatedOnColor,
                                    modifier = Modifier.size(68.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controles Extra con padding reducido para llegar a los extremos
                PlayerExtraControls(
                    onOpenDevice = { showDeviceSheet = true },
                    onShare = {
                        val currentSong = state.queue.find { it.id == state.currentSongId }
                        if (currentSong != null) {
                            val file = File(currentSong.path)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_song_text))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_song_chooser_title)))
                            }
                        }
                    },
                    onOpenQueue = { showQueueSheet = true },
                    onOpenLyrics = { showLyrics = true },
                    onColor = animatedOnColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    deviceName = deviceName,
                    deviceIcon = deviceIcon
                )
            }
        }

        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FullscreenLyricsView(
                lyrics = state.lyrics,
                title = state.title,
                artist = state.artist,
                artworkUri = state.artworkUri,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                containerColor = animatedContainerColor,
                onColor = animatedOnColor,
                onClose = { showLyrics = false },
                onSeek = onSeek,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                isPlaying = state.isPlaying,
                useAgsl = useLyricsAgslAnimation,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModernPlayerLayoutPreview() {
    val dummyState = PlayerUiState(
        title = "Song Title",
        artist = "Artist Name",
        isPlaying = true
    )
    val dummyColorScheme = PlayerColorScheme(
        containerColor = Color(0xFF121212),
        onColor = Color.White,
        subColor = Color.LightGray,
        tertiaryColor = Color.Gray,
        buttonSurface = Color.DarkGray
    )
    
    SharedTransitionLayout {
        AnimatedContent(targetState = false, label = "preview") { isPreview ->
            ModernPlayerLayout(
                state = dummyState,
                onCollapse = {},
                onPlayPause = {},
                onNext = {},
                onPrevious = {},
                onSeek = {},
                onToggleShuffle = {},
                onToggleFavorite = {},
                onCycleRepeat = {},
                onSleepTimerSelected = {},
                onSleepTimerCancel = {},
                onMoveQueueItem = { _, _ -> },
                onPlaySong = {},
                onPlayNext = {},
                onAddToQueue = {},
                onAddToPlaylist = { _, _ -> },
                onHideSong = {},
                onOpenAlbum = {},
                onArtistClick = {},
                userPlaylists = emptyList(),
                playlistMembershipBySong = emptyMap(),
                isWideLayout = false,
                progressBarType = ProgressBarType.WAVE,
                showThumb = true,
                progressBarThickness = 6f,
                artworkShape = ArtworkShape.DEFAULT,
                windowInsets = null,
                vividColors = isPreview,
                lyricsExpansionSpeed = AnimationSpeed.NORMAL,
                useLyricsAgslAnimation = false,
                textAlignCentered = false,
                marqueeTextEnabled = false,
                carouselEnabled = false,
                colorScheme = dummyColorScheme,
                animatedContainerColor = dummyColorScheme.containerColor,
                animatedOnColor = dummyColorScheme.onColor,
                animatedSubColor = dummyColorScheme.subColor,
                animatedTertiaryColor = dummyColorScheme.tertiaryColor,
                animatedButtonSurface = dummyColorScheme.buttonSurface,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this
            )
        }
    }
}
