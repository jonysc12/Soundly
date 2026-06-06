package com.soundly.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.activity.compose.BackHandler
import kotlin.math.roundToInt

private const val OVERLAY_DRAG_SENSITIVITY = 0.30f

// ─────────────────────────────────────────────────────────────────────────────
//  FullPlayerOverlay — MÁXIMO RENDIMIENTO
//
//  Estrategia:
//  1. `progress` llega como State<Float> para que el modifier lambda lo lea
//     en la DRAW/LAYOUT phase, sin recomponer el árbol en cada frame.
//  2. Toda la geometría (tamaño, offset, corner, color, alpha) se calcula
//     dentro de lambdas de Modifier → cero recomposiciones durante el drag.
//  3. `.layout { }` mueve y dimensiona el contenedor sin recomponer.
//  4. `drawWithCache` pinta el fondo redondeado sin allocar shapes en cada frame.
//  5. `graphicsLayer` para alpha de las capas de contenido (GPU, no CPU).
//  6. `pointerInput` con keys estables para evitar re-registros.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FullPlayerOverlay(
    // ✅ Recibe State<Float> en lugar de Float plano → el modifier lee el valor
    //    en la fase de layout/draw sin forzar recomposición del Composable.
    progressState: State<Float>,
    miniPlayerState: MiniPlayerState,
    fullPlayerState: com.soundly.player.PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
    accentColor: Color = Color.Unspecified,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onFling: (velocityPx: Float) -> Unit,
    miniLeftPx: Float,
    miniTopPx: Float,
    miniWidthPx: Float,
    miniHeightPx: Float,
    screenHeightPx: Float,
    screenWidthPx: Float,
    onMiniTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Leemos UNA sola vez para el guard; el resto lo leen las lambdas en draw/layout
    val progress by progressState

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val rawDominant = if (accentColor != Color.Unspecified) accentColor else rememberDominantColor(fullPlayerState.artworkUri)
    val adaptedDominant = adaptDominantInstant(
        rawColor = rawDominant,
        isDarkTheme = isDarkTheme,
        fallback = MaterialTheme.colorScheme.surface
    )
    val baseMini = MaterialTheme.colorScheme.surface
    val miniColor = blendOnSurface(adaptedDominant, baseMini, 0.25f)

    val pillColor = blendOnSurface(adaptedDominant, MaterialTheme.colorScheme.surfaceVariant, 0.40f)
    val miniOnBase = if (pillColor.luminance() < 0.35f) Color.White else Color.Black
    val miniAccentVibrant = blendOnSurface(adaptedDominant, miniOnBase, 0.70f)
    val miniSub = miniAccentVibrant.copy(alpha = 0.82f)
    val miniBtnBg = pillColor

    val hasValidMeasures = screenHeightPx > 0f && miniHeightPx > 0f && screenWidthPx > 0f && miniWidthPx > 0f
    if (!hasValidMeasures) return
    val dragSensitivityPx = remember(screenHeightPx) {
        (screenHeightPx * OVERLAY_DRAG_SENSITIVITY).coerceAtLeast(1f)
    }

    val maxCornerPx    = miniHeightPx / 2f

    BackHandler(enabled = progressState.value > 0.05f) {
        onCollapse()
    }

    // Alphas para mostrar/ocultar capas.
    // Para evitar el efecto de "reinicio", mantenemos el FullPlayer compuesto
    // desde que empieza la transición (0.01) hasta el final.
    val showMini by remember {
        derivedStateOf { progressState.value < 0.36f }
    }
    val shouldComposeFull by remember {
        derivedStateOf { progressState.value > 0.01f }
    }

    // ── Canvas externo: ocupa toda la pantalla, no se recompone ──────────────
    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .offset {
                    val p = progressState.value
                    val pShape = easeInOutQuintic(p)
                    IntOffset(
                        x = lerpF(miniLeftPx, 0f, pShape).roundToInt(),
                        y = lerpF(miniTopPx, 0f, pShape).roundToInt()
                    )
                }
                // ── 1. LAYOUT PHASE: posición y tamaño sin recomponer ─────────
                .layout { measurable, constraints ->
                    val p       = progressState.value
                    val pShape  = easeInOutQuintic(p)

                    val w = lerpF(miniWidthPx,    screenWidthPx,  pShape).roundToInt()
                    val h = lerpF(miniHeightPx,   screenHeightPx, pShape).roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth  = w, maxWidth  = w,
                            minHeight = h, maxHeight = h,
                        )
                    )
                    layout(w, h) {
                        placeable.place(0, 0)
                    }
                }
                // ── 2. DRAW PHASE: fondo redondeado sin allocar Shape ─────────
                .drawWithCache {
                    onDrawBehind {
                        val p          = progressState.value
                        // Color en dos estados: mini hasta llegar al full, sin mezcla intermedia
                        val bgColor    = if (p < 0.98f) miniColor else adaptedDominant
                        val cornerPx   = maxCornerPx * (1f - p.coerceIn(0f, 1f))
                        drawRoundRect(
                            color = bgColor,
                            size = size,
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    }
                }
                // ── 3. TOUCH: keys estables para no re-registrar el detector ──
                .pointerInput(onDrag, onFling, screenHeightPx) {
                    val velocityTracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragEnd = {
                            val v = velocityTracker.calculateVelocity().y
                            velocityTracker.resetTracking()
                            onFling(v)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            velocityTracker.addPointerInputChange(change)
                            change.consume()
                            onDrag(-dragAmount / dragSensitivityPx)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {

            // ── Mini Player content ───────────────────────────────────────────
            // Solo recompone al cruzar el umbral 0.35 (derivedStateOf arriba)
            if (showMini) {
                val miniAlphaModifier = remember {
                    Modifier.graphicsLayer {
                        alpha = (1f - progressState.value / 0.35f).coerceIn(0f, 1f)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(miniAlphaModifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMiniTap,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    MiniPlayerBody(
                        state            = miniPlayerState,
                        onPlayPauseClick = onPlayPause,
                        onNextClick     = onNext,
                        modifier         = Modifier.fillMaxSize(),
                        textColor        = miniAccentVibrant,
                        subTextColor     = miniSub,
                        buttonBg         = miniBtnBg,
                        buttonIconColor  = miniAccentVibrant,
                        progress         = miniPlayerState.progress,
                    )
                }
            }

            // ── Full Player content ───────────────────────────────────────────
            // Mantenerlo compuesto durante toda la transición para evitar reinicios de estado.
            // El alpha se encarga de la visibilidad suave.
            if (shouldComposeFull) {
                val fullAlphaModifier = remember {
                    Modifier.graphicsLayer {
                        // Empieza a aparecer suavemente desde 0.3 y es totalmente opaco en 0.9
                        alpha = ((progressState.value - 0.25f) / 0.65f).coerceIn(0f, 1f)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(fullAlphaModifier),
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
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Si tu ViewModel/padre te da Float y no State<Float>, usa este wrapper
//  para NO cambiar la firma en el resto del proyecto:
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FullPlayerOverlay(
    progress: Float,               // ← firma original que ya tenías
    miniPlayerState: MiniPlayerState,
    fullPlayerState: com.soundly.player.PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
    accentColor: Color = Color.Unspecified,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onFling: (velocityPx: Float) -> Unit,
    miniLeftPx: Float,
    miniTopPx: Float,
    miniWidthPx: Float,
    miniHeightPx: Float,
    screenHeightPx: Float,
    screenWidthPx: Float,
    onMiniTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Convertimos el Float a un State estable para que el overload de arriba
    // no recomponga en cada frame
    val progressState = rememberUpdatedState(progress)
    FullPlayerOverlay(
        progressState    = progressState,
        miniPlayerState  = miniPlayerState,
        fullPlayerState  = fullPlayerState,
        onPlayPause      = onPlayPause,
        onNext           = onNext,
        onPrevious       = onPrevious,
        onSeek           = onSeek,
        onToggleShuffle  = onToggleShuffle,
        onToggleFavorite = onToggleFavorite,
        onCycleRepeat    = onCycleRepeat,
        onSleepTimerSelected = onSleepTimerSelected,
        onSleepTimerCancel   = onSleepTimerCancel,
        accentColor     = accentColor,
        onCollapse       = onCollapse,
        onDrag           = onDrag,
        onFling          = onFling,
        miniLeftPx       = miniLeftPx,
        miniTopPx        = miniTopPx,
        miniWidthPx      = miniWidthPx,
        miniHeightPx     = miniHeightPx,
        screenHeightPx   = screenHeightPx,
        screenWidthPx    = screenWidthPx,
        onMiniTap        = onMiniTap,
        modifier         = modifier,
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun easeInOutQuintic(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) 16f * x * x * x * x * x
    else 1f - (-2f * x + 2f).let { v -> v * v * v * v * v } / 2f
}

private fun easeOutCubic(t: Float): Float {
    val x   = t.coerceIn(0f, 1f)
    val inv = 1f - x
    return 1f - inv * inv * inv
}

// ── Placeholder Full Player ───────────────────────────────────────────────────


