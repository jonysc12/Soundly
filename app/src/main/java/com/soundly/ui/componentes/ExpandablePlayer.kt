package com.soundly.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.activity.compose.BackHandler
import com.soundly.data.repository.PlayerExpansionMode
import androidx.compose.animation.core.FastOutSlowInEasing
import com.soundly.ui.componentes.blendOnSurface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

import kotlin.math.roundToInt

private const val OVERLAY_DRAG_SENSITIVITY = 0.30f

private class ReusablePlayerShape : Shape {
    var width = 0f
    var height = 0f
    var cornerRadius = 0f
    
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Rounded(
            RoundRect(
                rect = Rect(offset = Offset.Zero, size = Size(width, height)),
                cornerRadius = CornerRadius(cornerRadius)
            )
        )
    }
}

private data class PlayerColorParams(
    val adaptedDominant: Color,
    val miniColor: Color,
    val fullColor: Color,
    val miniAccentVibrant: Color,
    val miniSub: Color,
    val miniBtnBg: Color
)

@Composable
fun FullPlayerOverlay(
    progressState: State<Float>,
    miniPlayerMetadata: MiniPlayerMetadata,
    miniPlayerProgress: () -> Float,
    fullPlayerState: com.soundly.player.PlayerUiState,
    animationsViewModel: com.soundly.ui.screens.settings.pages.AnimationsViewModel, // ViewModel inyectado
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
    onPlaySong: (com.soundly.data.model.Song) -> Unit,
    onPlayNext: (com.soundly.data.model.Song) -> Unit = {},
    onAddToQueue: (com.soundly.data.model.Song) -> Unit = {},
    onAddToPlaylist: (String, Long) -> Unit = { _, _ -> },
    onHideSong: (Long) -> Unit = {},
    onOpenAlbum: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    userPlaylists: List<com.soundly.data.model.Playlist> = emptyList(),
    playlistMembershipBySong: Map<Long, Set<String>> = emptyMap(),
    accentColor: Color = Color.Unspecified,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onFling: (velocityPx: Float) -> Unit,
    onDismiss: () -> Unit = {},
    miniLeftPx: Float,
    miniTopPx: Float,
    miniWidthPx: Float,
    miniHeightPx: Float,
    screenHeightPx: Float,
    screenWidthPx: Float,
    isWideLayout: Boolean = false,
    onMiniTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // REFACTOR: Recolectar todos los estados aquí
    val expansionMode by animationsViewModel.expansionMode.collectAsStateWithLifecycle()
    val miniPlayerStyle by animationsViewModel.miniPlayerStyle.collectAsStateWithLifecycle()
    val progressBarType by animationsViewModel.progressBarType.collectAsStateWithLifecycle()
    val showThumb by animationsViewModel.showThumb.collectAsStateWithLifecycle()
    val progressBarThickness by animationsViewModel.progressBarThickness.collectAsStateWithLifecycle()
    val artworkShape by animationsViewModel.artworkShape.collectAsStateWithLifecycle()
    val miniArtworkShape by animationsViewModel.miniArtworkShape.collectAsStateWithLifecycle()
    val miniProgressBarType by animationsViewModel.miniProgressBarType.collectAsStateWithLifecycle()
    val miniProgressBarThickness by animationsViewModel.miniProgressBarThickness.collectAsStateWithLifecycle()
    val showMiniPrevious by animationsViewModel.showMiniPrevious.collectAsStateWithLifecycle()
    val swipeToDismiss by animationsViewModel.swipeToDismiss.collectAsStateWithLifecycle()
    val vividColors by animationsViewModel.vividColors.collectAsStateWithLifecycle()
    val lyricsExpansionSpeed by animationsViewModel.lyricsExpansionSpeed.collectAsStateWithLifecycle()
    val useLyricsAgslAnimation by animationsViewModel.useLyricsAgslAnimation.collectAsStateWithLifecycle()
    val textAlignCentered by animationsViewModel.textAlignCentered.collectAsStateWithLifecycle()
    val marqueeTextEnabled by animationsViewModel.marqueeTextEnabled.collectAsStateWithLifecycle()
    val carouselEnabled by animationsViewModel.carouselEnabled.collectAsStateWithLifecycle()
    val playerType by animationsViewModel.playerType.collectAsStateWithLifecycle()
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnFling by rememberUpdatedState(onFling)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentSwipeToDismiss by rememberUpdatedState<Boolean>(swipeToDismiss)
    val scope = rememberCoroutineScope()
    val horizontalOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val rawDominant = if (accentColor != Color.Unspecified) accentColor else rememberDominantColor(fullPlayerState.artworkUri)
    
    val colorParams = remember(rawDominant, isDarkTheme, surfaceColor, surfaceVariant, miniPlayerStyle, vividColors) {
        val adaptedDominant = adaptDominantInstant(
            rawColor = rawDominant, 
            isDarkTheme = isDarkTheme, 
            fallback = surfaceColor,
            isVivid = vividColors
        )
        
        if (!isDarkTheme && vividColors) {
            val baseColor = when(miniPlayerStyle) {
                com.soundly.data.repository.MiniPlayerStyle.BLUR -> surfaceColor
                com.soundly.data.repository.MiniPlayerStyle.TINTED -> adaptedDominant.copy(alpha = 0.15f)
                else -> blendOnSurface(adaptedDominant, surfaceColor, 0.12f)
            }
            val fullColor = blendOnSurface(adaptedDominant, surfaceColor, 0.12f)
            val miniAccentVibrant = adaptedDominant
            val miniSub = miniAccentVibrant.copy(alpha = 0.80f)
            val pillColor = adaptedDominant.copy(alpha = 0.18f)

            PlayerColorParams(
                adaptedDominant = adaptedDominant,
                miniColor = baseColor,
                fullColor = fullColor,
                miniAccentVibrant = miniAccentVibrant,
                miniSub = miniSub,
                miniBtnBg = pillColor
            )
        } else {
            val baseColor = when(miniPlayerStyle) {
                com.soundly.data.repository.MiniPlayerStyle.SOLID -> surfaceColor
                com.soundly.data.repository.MiniPlayerStyle.TINTED -> blendOnSurface(adaptedDominant, surfaceColor, if (vividColors) 0.65f else 0.25f)
                com.soundly.data.repository.MiniPlayerStyle.BLUR -> surfaceColor
            }
            
            val fullColor = blendOnSurface(adaptedDominant, surfaceColor, if (vividColors) 0.65f else 0.32f)
            
            // Determinar contraste basado en el fondo real
            val miniOnBase = if (baseColor.luminance() < 0.52f) Color.White else Color.Black
            
            val pillColor = blendOnSurface(adaptedDominant, surfaceVariant, if (vividColors) 0.65f else 0.40f)
            val vividFactor = if (vividColors) 0.25f else 0.70f
            val miniAccentVibrant = blendOnSurface(adaptedDominant, miniOnBase, vividFactor)
            val miniSub = miniAccentVibrant.copy(alpha = if (vividColors) 0.85f else 0.82f)
            
            PlayerColorParams(
                adaptedDominant = adaptedDominant,
                miniColor = baseColor,
                fullColor = fullColor,
                miniAccentVibrant = miniAccentVibrant,
                miniSub = miniSub,
                miniBtnBg = pillColor
            )
        }
    }

    val hasValidMeasures = screenHeightPx > 0f && miniHeightPx > 0f && screenWidthPx > 0f && miniWidthPx > 0f
    if (!hasValidMeasures) return

    val dragSensitivityPx = remember(screenHeightPx) { (screenHeightPx * OVERLAY_DRAG_SENSITIVITY).coerceAtLeast(1f) }
    val maxCornerPx = miniHeightPx / 2f

    val easedProgress by remember { derivedStateOf { ProgressiveEasing.transform(progressState.value) } }
    val effectiveExpansionProgress by remember(expansionMode) {
        derivedStateOf {
            if (expansionMode == PlayerExpansionMode.ELEVATION) {
                val startExpansionThreshold = 0.10f 
                if (easedProgress < startExpansionThreshold) 0f 
                else (easedProgress - startExpansionThreshold) / (1f - startExpansionThreshold)
            } else {
                easedProgress
            }
        }
    }
    
    val backHandlerEnabled by remember { derivedStateOf { progressState.value > 0.05f } }
    val showMini by remember { derivedStateOf { effectiveExpansionProgress < 0.30f } }
    val shouldComposeFull by remember { derivedStateOf { effectiveExpansionProgress > 0.30f } }
    val isAnimatingState = remember { derivedStateOf { progressState.value > 0f && progressState.value < 1f } }
    val isLayoutCollapsed by remember { derivedStateOf { progressState.value <= 0f } }

    BackHandler(enabled = backHandlerEnabled) { onCollapse() }

    val density = LocalDensity.current
    val targetWidthPx = remember(screenWidthPx, isWideLayout, density) {
        if (isWideLayout) {
            (screenWidthPx * 0.42f).coerceIn(with(density) { 380.dp.toPx() }, with(density) { 520.dp.toPx() })
        } else {
            screenWidthPx
        }
    }
    val reusableShape = remember { ReusablePlayerShape() }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .layout { measurable, _ ->
                    // OPTIMIZACIÓN SUPREMA: Estabilización de Layout
                    // El nodo de layout solo tiene dos estados: Mini o Full.
                    // Usamos un boolean derivado para que el layout solo se dispare al INICIO o al FINAL.
                    val isCollapsed = isLayoutCollapsed
                    
                    val fullW = targetWidthPx.roundToInt()
                    val fullH = screenHeightPx.roundToInt()

                    val w = if (isCollapsed) miniWidthPx.roundToInt() else fullW
                    val h = if (isCollapsed) miniHeightPx.roundToInt() else fullH

                    // Siempre medimos el contenido al tamaño completo para que el árbol interno sea estable
                    val placeable = measurable.measure(Constraints.fixed(fullW, fullH))
                    layout(w, h) {
                        placeable.place(0, 0)
                    }
                }
                .graphicsLayer {
                    val p = progressState.value
                    val t = ProgressiveEasing.transform(p)
                    
                    // 1. Animación de Posición (GPU)
                    val targetXPx = 0f
                    val y: Float
                    val x: Float
                    
                    if (expansionMode == PlayerExpansionMode.ELEVATION) {
                        val elevationThreshold = 0.20f
                        val jumpAmount = screenHeightPx * 0.12f
                        if (t < elevationThreshold) {
                            val jumpT = t / elevationThreshold
                            y = miniTopPx - (jumpAmount * jumpT)
                            x = miniLeftPx + horizontalOffset.value
                        } else {
                            val ep = (t - elevationThreshold) / (1f - elevationThreshold)
                            val startTop = miniTopPx - jumpAmount
                            y = lerpF(startTop, 0f, ep)
                            x = lerpF(miniLeftPx, targetXPx, ep) + horizontalOffset.value * (1f - ep)
                        }
                    } else {
                        y = lerpF(miniTopPx, 0f, t)
                        x = lerpF(miniLeftPx, targetXPx, t) + horizontalOffset.value * (1f - t)
                    }
                    
                    translationX = x
                    translationY = y

                    // 2. Animación de Recorte (GPU Masking)
                    val et = effectiveExpansionProgress
                    
                    val currentW = lerpF(miniWidthPx, targetWidthPx, et)
                    val currentH = lerpF(miniHeightPx, screenHeightPx, et)
                    
                    val cornerPx = if (effectiveExpansionProgress < 0.95f) {
                        maxCornerPx
                    } else {
                        val cornerT = (effectiveExpansionProgress - 0.95f) / 0.05f
                        maxCornerPx * (1f - ProgressiveEasing.transform(cornerT))
                    }

                    // OPTIMIZACIÓN: Solo aplicar clip si es estrictamente necesario (ahorra GPU en estado expandido)
                    val isFullyExpanded = effectiveExpansionProgress >= 1f
                    if (!isFullyExpanded) {
                        reusableShape.width = currentW
                        reusableShape.height = currentH
                        reusableShape.cornerRadius = cornerPx
                        this.shape = reusableShape
                        clip = true
                    } else {
                        clip = false
                    }

                    // 3. Efecto de Descarte Lateral
                    if (p < 0.05f && swipeToDismiss) {
                        val hOffsetAbs = kotlin.math.abs(horizontalOffset.value)
                        val dismissThreshold = miniWidthPx * 0.45f
                        val dismissProgress = (hOffsetAbs / dismissThreshold).coerceIn(0f, 1f)
                        alpha = (1f - dismissProgress).coerceIn(0f, 1f)
                        val s = (1f - dismissProgress * 0.25f).coerceIn(0.7f, 1f)
                        scaleX = s
                        scaleY = s
                    }
                }
                .drawWithCache {
                    onDrawBehind {
                        val p = progressState.value
                        val t = ProgressiveEasing.transform(p)
                        val effectiveExpansionProgress = if (expansionMode == PlayerExpansionMode.ELEVATION) {
                            val startExpansionThreshold = 0.10f
                            if (t < startExpansionThreshold) 0f else (t - startExpansionThreshold) / (1f - startExpansionThreshold)
                        } else t

                        val et = effectiveExpansionProgress
                        
                        val bgColor = if (miniPlayerStyle == com.soundly.data.repository.MiniPlayerStyle.BLUR) {
                            // En modo Blur, usamos un fundido desde el color de superficie al color del reproductor completo
                            lerp(surfaceColor, colorParams.fullColor, et)
                        } else {
                            lerp(colorParams.miniColor, colorParams.fullColor, et)
                        }
                        
                        val currentW = lerpF(miniWidthPx, targetWidthPx, et)
                        val currentH = lerpF(miniHeightPx, screenHeightPx, et)
                        
                        val cornerPx = if (effectiveExpansionProgress < 0.95f) maxCornerPx
                        else {
                            val cornerT = (effectiveExpansionProgress - 0.95f) / 0.05f
                            maxCornerPx * (1f - ProgressiveEasing.transform(cornerT))
                        }
                        
                        drawRoundRect(
                            color = bgColor, 
                            size = Size(currentW, currentH), 
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    }
                }
                .pointerInput(screenHeightPx) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragEnd = {
                            val v = velocityTracker.calculateVelocity()
                            velocityTracker.resetTracking()
                            hasTriggeredHaptic = false
                            
                            if (progressState.value < 0.05f && currentSwipeToDismiss) {
                                val threshold = miniWidthPx * 0.45f // Usar el ancho real del mini
                                if (kotlin.math.abs(horizontalOffset.value) > threshold || kotlin.math.abs(v.x) > 1200f) {
                                    val targetX = if (horizontalOffset.value > 0) screenWidthPx * 1.5f else -screenWidthPx * 1.5f
                                    scope.launch {
                                        horizontalOffset.animateTo(
                                            targetX, 
                                            androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                                        )
                                        currentOnDismiss()
                                        horizontalOffset.snapTo(0f)
                                    }
                                } else {
                                    scope.launch {
                                        horizontalOffset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                            currentOnFling(v.y)
                        },
                        onDrag = { change, dragAmount ->
                            velocityTracker.addPointerInputChange(change)
                            val isMiniPlayer = progressState.value < 0.05f
                            
                            if (isMiniPlayer && currentSwipeToDismiss) {
                                scope.launch {
                                    val newOffset = horizontalOffset.value + dragAmount.x
                                    horizontalOffset.snapTo(newOffset)
                                    
                                    val threshold = miniWidthPx * 0.35f
                                    if (kotlin.math.abs(newOffset) > threshold && !hasTriggeredHaptic) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        hasTriggeredHaptic = true
                                    } else if (kotlin.math.abs(newOffset) < threshold && hasTriggeredHaptic) {
                                        hasTriggeredHaptic = false
                                    }
                                }
                            }

                            val isDismissing = isMiniPlayer && currentSwipeToDismiss && kotlin.math.abs(horizontalOffset.value) > 10f
                            if (!isDismissing) {
                                currentOnDrag(-dragAmount.y / dragSensitivityPx)
                            }

                            change.consume()
                        }
                    )
                },
            contentAlignment = Alignment.TopStart, // ALINEACIÓN CRÍTICA: Los elementos deben empezar en 0,0 para el recorte GPU
        ) {
            if (showMini) {
                // Contenedor del Mini Player con tamaño ESTABLE para evitar tirones
                Box(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { miniWidthPx.toDp() })
                        .height(with(LocalDensity.current) { miniHeightPx.toDp() })
                        .graphicsLayer { 
                            val alphaOut = ((0.30f - effectiveExpansionProgress) / 0.30f).coerceIn(0f, 1f)
                            alpha = ProgressiveEasing.transform(alphaOut)
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onMiniTap),
                    contentAlignment = Alignment.Center,
                ) {
                    MiniPlayerBody(
                        metadata = miniPlayerMetadata,
                        onPlayPauseClick = onPlayPause,
                        onNextClick = onNext,
                        onPreviousClick = onPrevious,
                        modifier = Modifier.fillMaxSize(),
                        textColor = colorParams.miniAccentVibrant,
                        subTextColor = colorParams.miniSub,
                        buttonBg = colorParams.miniBtnBg,
                        buttonIconColor = colorParams.miniAccentVibrant,
                        progress = miniPlayerProgress,
                        artworkShape = miniArtworkShape,
                        miniProgressBarType = miniProgressBarType,
                        miniProgressBarThickness = miniProgressBarThickness,
                        showPrevious = showMiniPrevious,
                        marqueeTextEnabled = marqueeTextEnabled,
                    )
                }
            }

            if (shouldComposeFull) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            // El contenido aparece progresivamente desde el 30% hasta el 70%
                            val alphaIn = ((effectiveExpansionProgress - 0.30f) / 0.40f).coerceIn(0f, 1f)
                            alpha = ProgressiveEasing.transform(alphaIn)
                        },
                ) {
                    FullPlayerContent(
                        state = fullPlayerState,
                        onCollapse = onCollapse,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onToggleShuffle = onToggleShuffle,
                        onToggleFavorite = onToggleFavorite,
                        onCycleRepeat = onCycleRepeat,
                        onSleepTimerSelected = onSleepTimerSelected,
                        onSleepTimerCancel = onSleepTimerCancel,
                        onMoveQueueItem = onMoveQueueItem,
                        onPlaySong = onPlaySong,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onHideSong = onHideSong,
                        onOpenAlbum = onOpenAlbum,
                        onArtistClick = onArtistClick,
                        userPlaylists = userPlaylists,
                        playlistMembershipBySong = playlistMembershipBySong,
                        isWideLayout = isWideLayout,
                        progressBarType = progressBarType,
                        showThumb = showThumb,
                        progressBarThickness = progressBarThickness,
                        artworkShape = artworkShape,
                        vividColors = vividColors,
                        lyricsExpansionSpeed = lyricsExpansionSpeed,
                        useLyricsAgslAnimation = useLyricsAgslAnimation,
                        textAlignCentered = textAlignCentered,
                        marqueeTextEnabled = marqueeTextEnabled,
                        carouselEnabled = carouselEnabled,
                        playerType = playerType
                    )
                }
            }

            if (isAnimatingState.value) {
                Box(modifier = Modifier.fillMaxSize().clickable(enabled = false) {})
            }
        }
    }
}

@Composable
fun FullPlayerOverlay(
    progress: Float,
    miniPlayerMetadata: MiniPlayerMetadata,
    miniPlayerProgress: () -> Float,
    fullPlayerState: com.soundly.player.PlayerUiState,
    animationsViewModel: com.soundly.ui.screens.settings.pages.AnimationsViewModel,
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
    onPlaySong: (com.soundly.data.model.Song) -> Unit,
    onPlayNext: (com.soundly.data.model.Song) -> Unit = {},
    onAddToQueue: (com.soundly.data.model.Song) -> Unit = {},
    onAddToPlaylist: (String, Long) -> Unit = { _, _ -> },
    onHideSong: (Long) -> Unit = {},
    onOpenAlbum: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    userPlaylists: List<com.soundly.data.model.Playlist> = emptyList(),
    playlistMembershipBySong: Map<Long, Set<String>> = emptyMap(),
    accentColor: Color = Color.Unspecified,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onFling: (velocityPx: Float) -> Unit,
    onDismiss: () -> Unit = {},
    miniLeftPx: Float,
    miniTopPx: Float,
    miniWidthPx: Float,
    miniHeightPx: Float,
    screenHeightPx: Float,
    screenWidthPx: Float,
    isWideLayout: Boolean = false,
    onMiniTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val progressState = rememberUpdatedState(progress)
    FullPlayerOverlay(
        progressState = progressState,
        miniPlayerMetadata = miniPlayerMetadata,
        miniPlayerProgress = miniPlayerProgress,
        fullPlayerState = fullPlayerState,
        animationsViewModel = animationsViewModel,
        onPlayPause = onPlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        onSeek = onSeek,
        onToggleShuffle = onToggleShuffle,
        onToggleFavorite = onToggleFavorite,
        onCycleRepeat = onCycleRepeat,
        onSleepTimerSelected = onSleepTimerSelected,
        onSleepTimerCancel = onSleepTimerCancel,
        onMoveQueueItem = onMoveQueueItem,
        onPlaySong = onPlaySong,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onHideSong = onHideSong,
        onOpenAlbum = onOpenAlbum,
        onArtistClick = onArtistClick,
        userPlaylists = userPlaylists,
        playlistMembershipBySong = playlistMembershipBySong,
        accentColor = accentColor,
        onCollapse = onCollapse,
        onDrag = onDrag,
        onFling = onFling,
        onDismiss = onDismiss,
        miniLeftPx = miniLeftPx,
        miniTopPx = miniTopPx,
        miniWidthPx = miniWidthPx,
        miniHeightPx = miniHeightPx,
        screenHeightPx = screenHeightPx,
        screenWidthPx = screenWidthPx,
        isWideLayout = isWideLayout,
        onMiniTap = onMiniTap,
        modifier = modifier,
    )
}

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t
