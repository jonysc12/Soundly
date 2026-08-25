@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.soundly.ui.componentes

import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.soundly.R
import com.soundly.player.LyricLane
import com.soundly.player.LyricLine
import com.soundly.player.LyricVariant
import com.soundly.player.LyricsUiState
import com.soundly.player.StructuredLyricLine
import com.soundly.player.TimedWord
import com.soundly.ui.componentes.Lyrics_fullscreen.LyricLineItem
import com.soundly.ui.componentes.Lyrics_fullscreen.formatTime
import com.soundly.ui.componentes.Lyrics_fullscreen.resolveActiveLine
import com.soundly.ui.componentes.Lyrics_fullscreen.toStructuredFallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// ==========================
// Constants
// ==========================

private val SmoothEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private const val CONTROLS_HIDE_DELAY_MS = 3500L

private const val LYRICS_WAVE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 color1; // Normal
    layout(color) uniform half4 color2; // Vibrante
    layout(color) uniform half4 color3; // Claro
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
        float t = iTime * 0.12;
        
        // Capa 1: Mezcla entre Normal y Vibrante
        float n = noise(uv * 1.0 + t * 0.15);
        float n2 = noise(uv * 1.8 - t * 0.25);
        float combined = (n + n2) * 0.5 + 0.5;
        float mixVal = smoothstep(0.3, 0.7, combined);
        half4 baseColor = mix(color1, color2, mixVal);
        
        // Capa 2: Color Claro (Luz) - Zonas más grandes y con mejor mezcla
        float n3 = noise(uv * 2.0 + t * 0.3) * 0.5 + 0.5;
        float highlight = smoothstep(0.4, 0.8, n3);
        
        // Mezclamos el color base con el claro usando el highlight
        half4 finalColor = mix(baseColor, color3, highlight * 0.7);
        
        // Añadimos un resplandor adicional basado en el ruido para dar profundidad
        finalColor += color3 * highlight * 0.2;
        
        return mix(bgColor, finalColor, 0.95);
    }
"""

// ==========================
// Root composable
// ==========================

@Composable
fun FullscreenLyricsView(
    lyrics: LyricsUiState,
    title: String,
    artist: String,
    artworkUri: Uri?,
    positionMs: Long,
    durationMs: Long = 0L,
    containerColor: Color,
    onColor: Color,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    isPlaying: Boolean = false,
    useAgsl: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val currentPositionMs by rememberUpdatedState(positionMs)
    val lines = remember(lyrics) {
        lyrics.structuredLines.ifEmpty { lyrics.toStructuredFallback() }
    }
    val activeIndex by remember(lines) {
        derivedStateOf { resolveActiveLine(lines, currentPositionMs) }
    }

    val initialIndex = remember { activeIndex.coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()

    BackHandler {
        onClose()
    }

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "lyrics_container"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else Modifier

    val room = 0.8f
    val lineSpacing = 32.dp

    // --- UI state & Controls ---
    var showControls by remember { mutableStateOf(true) }
    var sliderPosition by remember {
        mutableFloatStateOf(
            if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        )
    }
    var isUserSeeking by remember { mutableStateOf(false) }
    var hideControlsJob by remember { mutableStateOf<Job?>(null) }

    val scheduleHideControls = remember<() -> Unit> {
        {
            hideControlsJob?.cancel()
            hideControlsJob = scope.launch {
                delay(CONTROLS_HIDE_DELAY_MS)
                showControls = false
            }
        }
    }

    LaunchedEffect(Unit) {
        scheduleHideControls()
    }

    LaunchedEffect(lines) {
        // Al cambiar de canción o de estado de letras, volvemos arriba de forma instantánea
        listState.scrollToItem(0)
    }

    // --- Scroll monitoring inteligente ---
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var isUserScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            isUserScrolling = true
        } else {
            // Cuando el usuario suelta, esperamos un tiempo prudencial (ej. tras el fling)
            // para retomar el control automático.
            delay(3500)
            isUserScrolling = false
        }
    }

    // --- Auto-scroll inteligente y fluido ---
    LaunchedEffect(activeIndex, isUserScrolling) {
        if (activeIndex >= 0 && !isUserScrolling) {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            
            if (viewportHeight > 0) {
                val targetItem = layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
                val itemSize = targetItem?.size ?: 100 // Estimación si no es visible
                
                // Calculamos el offset para centrar la línea
                // El scrollOffset en animateScrollToItem es relativo al inicio del viewport
                // Un valor negativo empuja el ítem hacia abajo (hacia el centro)
                val centerOffset = -(viewportHeight / 2 - itemSize / 2)
                
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = centerOffset
                )
            } else {
                // Fallback si el layout aún no está listo
                listState.animateScrollToItem(activeIndex)
            }
        }
    }

    LaunchedEffect(positionMs, durationMs) {
        if (!isUserSeeking && durationMs > 0) {
            sliderPosition = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        }
    }

    val animatedWaveHeight by animateFloatAsState(
        targetValue = if (isPlaying) 7f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "waveHeight"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fluid")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val variantNormal = containerColor
    val vibrantColor = remember(containerColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(containerColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 2.2f).coerceIn(0f, 1f) // Saturación extrema
        hsv[2] = (hsv[2] * 1.4f).coerceIn(0f, 1f) // Más brillante
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val lightColor = remember(containerColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(containerColor.toArgb(), hsv)
        hsv[2] = (hsv[2] * 2.5f).coerceIn(0f, 1f) // Brillo máximo
        hsv[1] = (hsv[1] * 0.35f).coerceIn(0f, 1f) // Aumentamos saturación para que el color sea distinguible
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Box(
        modifier = modifier
            .then(sharedModifier)
            .fillMaxSize()
            .background(containerColor)
    ) {
        if (useAgsl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = remember { RuntimeShader(LYRICS_WAVE_SHADER) }
            Canvas(modifier = Modifier.fillMaxSize()) {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setColorUniform("color1", variantNormal.toArgb())
                shader.setColorUniform("color2", vibrantColor.toArgb())
                shader.setColorUniform("color3", lightColor.toArgb())
                shader.setColorUniform("bgColor", containerColor.toArgb())
                drawRect(brush = ShaderBrush(shader))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(8.dp)
                        .background(onColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = onColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = true
                        scheduleHideControls()
                    }
            ) {
                val isTransitioning = animatedVisibilityScope?.transition?.let {
                    it.currentState != it.targetState
                } ?: false

                val contentAlpha by animateFloatAsState(
                    targetValue = if (isTransitioning) 0f else 1f,
                    animationSpec = tween(durationMillis = 400),
                    label = "lyricsFade"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = contentAlpha }
                ) {
                    if (lines.isNotEmpty() && (contentAlpha > 0f || !isTransitioning)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.10f to Color.Black,
                                            0.90f to Color.Black,
                                            1f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                },
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                bottom = 180.dp,
                                top = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(lineSpacing)
                        ) {
                            itemsIndexed(lines, key = { idx, line -> idx to line.startMs }) { index, line ->
                                val distanceFromActive =
                                    if (activeIndex >= 0) abs(index - activeIndex) else Int.MAX_VALUE
                                LyricLineItem(
                                    line = line,
                                    positionMs = positionMs,
                                    distanceFromActive = distanceFromActive,
                                    onColor = onColor,
                                    room = room,
                                    isUserScrolling = isUserScrolling,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onLineClick = {
                                        hideControlsJob?.cancel()
                                        if (showControls) {
                                            onSeek(line.startMs)
                                        } else {
                                            showControls = true
                                        }
                                        scheduleHideControls()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(420, easing = SmoothEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(380, easing = SmoothEasing)
            ),
            label = "playerControls"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to containerColor.copy(alpha = 0.3f), // Arriba: 30% de opacidad (transparente)
                            0.2f to containerColor.copy(alpha = 0.8f), // Al 20% de la altura alcanza el 80% sólido
                            1.0f to containerColor.copy(alpha = 0.8f)  // Hasta el fondo se mantiene en 80% sólido
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                        SoundlyWavySlider(
                            value = sliderPosition,
                            onValueChange = { value: Float ->
                                sliderPosition = value
                                isUserSeeking = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true,
                            valueRange = 0f..1f,
                            onValueChangeFinished = {
                                val seekTo = (sliderPosition * durationMs).toLong()
                                onSeek(seekTo)
                                isUserSeeking = false
                            },
                            activeColor = onColor,
                            inactiveColor = onColor.copy(alpha = 0.25f),
                            thumbColor = onColor,
                            showThumb = true,
                            isWave = true,
                            waveHeight = animatedWaveHeight.dp,
                            waveLength = 32.dp,
                            waveThickness = 6.dp,
                            trackThickness = 6.dp
                        )

                        Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(positionMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = onColor.copy(alpha = 0.82f)
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = onColor.copy(alpha = 0.82f)
                    )
                }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerSideButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.SkipPrevious,
                                onClick = {
                                    hideControlsJob?.cancel()
                                    onPrevious()
                                    scheduleHideControls()
                                },
                                iconColor = onColor,
                                buttonSize = 64.dp,
                                iconSize = 28.dp,
                                horizontalPadding = 12.dp
                            )

                            PlayerMainButton(
                                isPlaying = isPlaying,
                                onClick = {
                                    hideControlsJob?.cancel()
                                    onPlayPause()
                                    scheduleHideControls()
                                },
                                containerColor = onColor,
                                iconColor = onColor.copy(alpha = 0.1f),
                                buttonSize = 72.dp,
                                iconSize = 36.dp,
                                cornerRadius = 22.dp
                            )

                            PlayerSideButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.SkipNext,
                                onClick = {
                                    hideControlsJob?.cancel()
                                    onNext()
                                    scheduleHideControls()
                                },
                                iconColor = onColor,
                                buttonSize = 64.dp,
                                iconSize = 28.dp,
                                horizontalPadding = 12.dp
                            )
                        }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun FullscreenLyricsViewPreview() {
    val dummyWords = listOf(
        TimedWord("Hello", 0, 500),
        TimedWord("world", 600, 1000),
        TimedWord("this", 1100, 1500),
        TimedWord("is", 1600, 2000),
        TimedWord("a", 2100, 2300),
        TimedWord("test", 2400, 3000)
    )

    val dummyLines = listOf(
        StructuredLyricLine(
            text = "Hello world this is a test",
            startMs = 0,
            endMs = 3000,
            words = dummyWords,
            lane = LyricLane.CENTER
        ),
        StructuredLyricLine(
            text = "This is the second line",
            startMs = 3500,
            endMs = 6000,
            words = listOf(
                TimedWord("This", 3500, 4000),
                TimedWord("is", 4100, 4500),
                TimedWord("the", 4600, 5000),
                TimedWord("second", 5100, 5500),
                TimedWord("line", 5600, 6000)
            ),
            lane = LyricLane.LEFT,
            secondaryLines = listOf(LyricVariant("Traducción de la segunda línea"))
        ),
        StructuredLyricLine(
            text = "And a third one for good measure",
            startMs = 6500,
            endMs = 9000,
            words = emptyList(),
            lane = LyricLane.RIGHT
        )
    )

    val dummyState = LyricsUiState(
        structuredLines = dummyLines,
        syncedLines = dummyLines.map { LyricLine(timestampMs = it.startMs, text = it.text) }
    )

    MaterialTheme {
        FullscreenLyricsView(
            lyrics = dummyState,
            title = "Hello World",
            artist = "Soundly Artist",
            artworkUri = null,
            positionMs = 1200,
            durationMs = 9000,
            containerColor = Color(0xFF1A1A2E),
            onColor = Color.White,
            onClose = {},
            isPlaying = true
        )
    }
}
