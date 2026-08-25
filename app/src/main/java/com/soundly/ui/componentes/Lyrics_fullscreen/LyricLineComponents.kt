package com.soundly.ui.componentes.Lyrics_fullscreen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.soundly.player.LyricLane
import com.soundly.player.LyricVariant
import com.soundly.player.StructuredLyricLine
import kotlin.math.abs

// ==========================
// Constants
// ==========================

internal val SmoothEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private const val LINE_EDGE_WINDOW_MS = 350L
internal const val ACTIVE_LINE_CONTRAST_MS = 750

// ==========================
// LyricLineItem
// ==========================

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun LyricLineItem(
    line: StructuredLyricLine,
    positionMs: Long,
    distanceFromActive: Int,
    onColor: Color,
    room: Float,
    isUserScrolling: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onLineClick: () -> Unit = {}
) {
    val currentPosition by rememberUpdatedState(positionMs)

    val isActive by remember(line) {
        derivedStateOf { currentPosition >= line.startMs && currentPosition < line.endMs }
    }

    val sharedModifier = if (isActive && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "active_lyric_line"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else Modifier

    val activeWeight by remember(line) {
        derivedStateOf {
            val lineDur = (line.endMs - line.startMs).coerceAtLeast(1L)
            val window = LINE_EDGE_WINDOW_MS.coerceAtMost(lineDur / 2L).coerceAtLeast(120L)
            when {
                currentPosition < line.startMs -> {
                    val before = line.startMs - currentPosition
                    if (before < window) 1f - (before.toFloat() / window.toFloat()) else 0f
                }
                currentPosition >= line.endMs -> {
                    val after = currentPosition - line.endMs
                    if (after < window) 1f - (after.toFloat() / window.toFloat()) else 0f
                }
                else -> {
                    val within = (currentPosition - line.startMs).toFloat() / lineDur.toFloat()
                    0.88f + 0.12f * (within * within * (3f - 2f * within))
                }
            }.coerceIn(0f, 1f)
        }
    }

    val baseEmphasis by remember(distanceFromActive) {
        derivedStateOf {
            when {
                distanceFromActive <= 0 -> 1f
                distanceFromActive == 1 -> 0.7f
                distanceFromActive == 2 -> 0.48f
                else -> 0.28f
            }
        }
    }
    val emphasis by remember(activeWeight, baseEmphasis) {
        derivedStateOf {
            (baseEmphasis * (1f - activeWeight) + 1f * activeWeight).coerceIn(0.28f, 1f)
        }
    }

    val targetScale = 1f + 0.05f * activeWeight
    val targetBaseAlpha = 1f - 0.50f * activeWeight

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 400, easing = SmoothEasing),
        label = "lyricsScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = (0.28f + 0.72f * emphasis).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400, easing = SmoothEasing),
        label = "lyricsAlpha"
    )
    val animatedTranslationAlpha by animateFloatAsState(
        targetValue = (0.16f + 0.58f * emphasis).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = SmoothEasing),
        label = "lyricsTranslationAlpha"
    )
    val animatedBaseAlpha by animateFloatAsState(
        targetValue = targetBaseAlpha,
        animationSpec = tween(durationMillis = ACTIVE_LINE_CONTRAST_MS, easing = SmoothEasing),
        label = "lyricsBaseContrast"
    )

    val tokenGap = tokenGapForRoom(room)
    val lineGap = lineGapForRoom(room)

    val titleStyleHm = MaterialTheme.typography.headlineMedium
    val titleStyleHs = MaterialTheme.typography.headlineSmall
    val titleStyle = if (room > 0.72f) titleStyleHm else titleStyleHs
    val weight = fontWeightForActive(activeWeight)
    val textStyle = titleStyle.copy(fontWeight = weight)

    val itemAlignment = line.lane.toAlignment()
    val horizontalAlignment = line.lane.toHorizontalAlignment()
    val activeVerticalPadding = activeVerticalPaddingForRoom(room, isActive)
    val contentWidthFraction = contentWidthFractionForLane(line.lane)

    Box(
        modifier = Modifier
            .then(sharedModifier)
            .fillMaxWidth()
            .padding(vertical = activeVerticalPadding)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLineClick
            ),
        contentAlignment = itemAlignment
    ) {
        val spacedBy = lineGap * 1.4f
        Column(
            verticalArrangement = Arrangement.spacedBy(spacedBy),
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
                .graphicsLayer {
                    alpha = animatedAlpha
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
        ) {
            line.secondaryLines.getOrNull(0)?.let { secondaryLine ->
                SecondaryLyricLine(
                    variant = secondaryLine,
                    positionMs = currentPosition,
                    onColor = onColor,
                    room = room,
                    lane = line.lane,
                    placement = SecondaryPlacement.TOP,
                    visibleWeight = activeWeight,
                    baseTranslationAlpha = animatedTranslationAlpha * 0.88f
                )
            }

            if (line.words.isNotEmpty()) {
                ActiveLyricsLine(
                    line = line,
                    positionMs = currentPosition,
                    baseColor = onColor.copy(alpha = animatedBaseAlpha),
                    fillColor = onColor,
                    style = textStyle,
                    tokenGap = tokenGap,
                    isActive = activeWeight > 0.05f
                )
            } else {
                Text(
                    text = line.text,
                    style = textStyle,
                    color = onColor,
                    textAlign = line.lane.toTextAlign(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            line.secondaryLines.getOrNull(1)?.let { secondaryLine ->
                SecondaryLyricLine(
                    variant = secondaryLine,
                    positionMs = currentPosition,
                    onColor = onColor,
                    room = room,
                    lane = line.lane,
                    placement = SecondaryPlacement.BOTTOM,
                    visibleWeight = activeWeight,
                    baseTranslationAlpha = animatedTranslationAlpha * 0.84f
                )
            }
        }
    }
}

// ==========================
// SecondaryLyricLine
// ==========================

@Composable
internal fun SecondaryLyricLine(
    variant: LyricVariant,
    positionMs: Long,
    onColor: Color,
    room: Float,
    lane: LyricLane,
    placement: SecondaryPlacement,
    visibleWeight: Float,
    baseTranslationAlpha: Float,
) {
    val secondaryStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
    val tokenGap = tokenGapForRoom(room)

    val effectiveAlpha by animateFloatAsState(
        targetValue = (baseTranslationAlpha * visibleWeight).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420, easing = SmoothEasing),
        label = "secondaryAlpha"
    )
    val driftY by animateFloatAsState(
        targetValue = if (visibleWeight > 0.5f) 0f else when (placement) {
            SecondaryPlacement.TOP -> -6f
            SecondaryPlacement.BOTTOM -> 6f
        },
        animationSpec = tween(durationMillis = 520, easing = SmoothEasing),
        label = "secondaryDrift"
    )
    val lineVisible = visibleWeight > 0.02f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = effectiveAlpha
                translationY = driftY
            }
    ) {
        if (variant.words.isNotEmpty()) {
            val variantLine = remember(variant) {
                StructuredLyricLine(
                    text = variant.text,
                    startMs = variant.words.first().startMs,
                    endMs = variant.words.last().endMs,
                    words = variant.words
                )
            }
            ActiveLyricsLine(
                line = variantLine,
                positionMs = positionMs,
                baseColor = onColor.copy(alpha = 0.34f),
                fillColor = onColor,
                style = secondaryStyle,
                tokenGap = tokenGap,
                isActive = lineVisible
            )
        } else {
            Text(
                text = variant.text,
                style = secondaryStyle,
                color = onColor,
                textAlign = lane.toTextAlign(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==========================
// ActiveLyricsLine (karaoke word-by-word canvas)
// ==========================

@Composable
internal fun ActiveLyricsLine(
    line: StructuredLyricLine,
    positionMs: Long,
    baseColor: Color,
    fillColor: Color,
    style: TextStyle,
    tokenGap: Dp,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gapPx = with(density) { tokenGap.toPx() }
    var widthPx by remember(line, style) { mutableIntStateOf(0) }

    val layout = remember(line, style, widthPx, gapPx) {
        if (widthPx <= 0) {
            ActiveLineLayout(emptyList(), 0f)
        } else {
            measureActiveLineLayout(
                words = line.words,
                style = style,
                maxWidthPx = widthPx.toFloat(),
                gapPx = gapPx,
                textMeasurer = textMeasurer
            )
        }
    }

    val tokensByRow = remember(layout) { layout.tokens.groupBy { it.rowIndex }.toSortedMap() }

    val rowWidths = remember(layout) {
        tokensByRow.values.map { tokens ->
            tokens.maxOfOrNull { it.x + it.layout.size.width.toFloat() } ?: 0f
        }
    }
    val totalVisualWidth = remember(rowWidths) { rowWidths.sum().coerceAtLeast(1f) }
    val rowCumulativeWidths = remember(rowWidths) {
        val cumul = mutableListOf<Float>()
        var sum = 0f
        rowWidths.forEach { w ->
            cumul.add(sum)
            sum += w
        }
        cumul
    }

    val driftSnapThresholdMs = 220f

    var anchorPositionMs by remember(line) { mutableFloatStateOf(positionMs.toFloat()) }
    var anchorFrameTimeNanos by remember(line) { mutableLongStateOf(0L) }
    var hasAnchor by remember(line) { mutableStateOf(false) }
    var currentSmoothPos by remember(line) { mutableFloatStateOf(positionMs.toFloat()) }

    LaunchedEffect(line, positionMs) {
        val predicted = if (hasAnchor) currentSmoothPos else positionMs.toFloat()
        val drift = abs(predicted - positionMs)
        val nowNanos = withFrameNanos { it }
        anchorPositionMs = if (drift > driftSnapThresholdMs) positionMs.toFloat() else predicted
        anchorFrameTimeNanos = nowNanos
        hasAnchor = true
    }

    LaunchedEffect(line) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (hasAnchor) {
                    val elapsedMs = (frameTimeNanos - anchorFrameTimeNanos) / 1_000_000f
                    currentSmoothPos = anchorPositionMs + elapsedMs
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { intSize -> widthPx = intSize.width }
            .height(with(density) { layout.heightPx.coerceAtLeast(1f).toDp() })
    ) {
        val currentPos = currentSmoothPos
        val lineDuration = (line.endMs - line.startMs).coerceAtLeast(1L).toFloat()
        val lineProgress = ((currentPos - line.startMs) / lineDuration).coerceIn(0f, 1f)
        val targetVisualPos = totalVisualWidth * lineProgress

        tokensByRow.forEach { (rowIndex, rowTokens) ->
            val rowWidth = rowWidths.getOrNull(rowIndex) ?: 0f
            val rowStartVisual = rowCumulativeWidths.getOrNull(rowIndex) ?: 0f
            val rowEndVisual = rowStartVisual + rowWidth
            val rowActiveX = when {
                targetVisualPos <= rowStartVisual -> 0f
                targetVisualPos >= rowEndVisual -> rowWidth
                else -> targetVisualPos - rowStartVisual
            }
            val horizontalOffset = when (line.lane) {
                LyricLane.RIGHT -> size.width - rowWidth
                LyricLane.DUET -> (size.width - rowWidth) / 2f
                else -> 0f
            }
            val headWidth = 100f
            rowTokens.forEach { token ->
                val tokenWidth = token.layout.size.width.toFloat()
                val tokenCenterX = token.x + tokenWidth / 2f
                val dist = abs(rowActiveX - tokenCenterX)
                val influence = if (isActive) (1f - dist / 400f).coerceIn(0f, 1f) else 0f
                val influenceSq = influence * influence
                val scale = 1f + 0.05f * influenceSq
                val alpha = 0.35f + 0.65f * influenceSq
                val brush: Brush = when {
                    rowActiveX <= 0f -> SolidColor(baseColor)
                    rowActiveX >= rowWidth -> SolidColor(fillColor)
                    else -> Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to fillColor,
                            ((rowActiveX - headWidth) / rowWidth).coerceIn(0f, 1f) to fillColor,
                            ((rowActiveX + headWidth) / rowWidth).coerceIn(0f, 1f) to baseColor,
                            1f to baseColor
                        ),
                        startX = -token.x,
                        endX = rowWidth - token.x
                    )
                }
                translate(left = token.x + horizontalOffset, top = token.y) {
                    scale(
                        scaleX = scale,
                        scaleY = scale,
                        pivot = Offset(0f, token.layout.size.height * 0.5f)
                    ) {
                        drawText(
                            textLayoutResult = token.layout,
                            brush = brush,
                            alpha = alpha
                        )
                    }
                }
            }
        }
    }
}
