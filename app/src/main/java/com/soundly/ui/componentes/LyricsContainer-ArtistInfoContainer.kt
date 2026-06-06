package com.soundly.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import com.soundly.player.LyricVariant
import com.soundly.player.LyricLane
import com.soundly.player.LyricsUiState
import com.soundly.player.StructuredLyricLine
import com.soundly.player.TimedWord
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ArtistInfoContainer(
    modifier: Modifier = Modifier,
    artistName: String,
    artistDescription: String,
    imageUrl: String,
    onColor: Color
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(onColor.copy(alpha = 0.05f))
    ) {
        Text(
            text = "Artist",
            style = MaterialTheme.typography.titleLarge,
            color = onColor,
            modifier = Modifier.padding(18.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = artistName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = artistDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = onColor.copy(alpha = 0.75f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
fun LyricsContainer(
    lyrics: LyricsUiState,
    positionMs: Long,
    onColor: Color,
    modifier: Modifier = Modifier,
) {
    val lines = remember(lyrics) { lyrics.structuredLines.ifEmpty { lyrics.toStructuredFallback() } }
    val lyricsContentKey = remember(lyrics.rawContent, lyrics.plainText) {
        lyrics.rawContent ?: lyrics.plainText.orEmpty()
    }
    val listState = rememberLazyListState()
    val activeIndex = remember(lines, positionMs) { resolveActiveLine(lines, positionMs) }

    val room = 0.55f
    val viewportHeight = lerp(300.dp, 430.dp, room)
    val horizontalPadding = lerp(18.dp, 28.dp, room)
    val verticalPadding = lerp(18.dp, 26.dp, room)
    val lineSpacing = lerp(16.dp, 24.dp, room)
    val centerPadding = lerp(92.dp, 144.dp, room)
    val edgePadding = lerp(20.dp, 28.dp, room)
    var lastCenteredIndex by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(lyricsContentKey) {
        lastCenteredIndex = -1
        listState.scrollToItem(0)
    }

    LaunchedEffect(activeIndex, lines.size) {
        if (activeIndex < 0 || lines.isEmpty() || activeIndex == lastCenteredIndex) return@LaunchedEffect
        val targetIndex = (activeIndex - 1).coerceAtLeast(0)
        listState.scrollToItem(targetIndex)
        lastCenteredIndex = activeIndex
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        onColor.copy(alpha = 0.08f),
                        onColor.copy(alpha = 0.04f),
                        onColor.copy(alpha = 0.06f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleLarge,
                    color = onColor,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onColor.copy(alpha = 0.10f),
                        contentColor = onColor
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(28.dp) // 👈 más bajito
                ) {
                    Text(
                        text = "Most",

                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (lines.isEmpty() && lyrics.plainText.isNullOrBlank()) {
                Text(
                    text = "No hay letras disponibles para esta cancion.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = onColor.copy(alpha = 0.68f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )
                return
            }

            if (lines.isEmpty()) {
                Text(
                    text = lyrics.plainText.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onColor.copy(alpha = 0.84f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )
                return
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight, max = viewportHeight),
                contentPadding = PaddingValues(
                    top = if (activeIndex <= 0) edgePadding else centerPadding,
                    bottom = if (activeIndex >= lines.lastIndex) edgePadding else centerPadding
                ),
                verticalArrangement = Arrangement.spacedBy(lineSpacing),
                userScrollEnabled = false
            ) {
                itemsIndexed(lines) { index, line ->
                    val distanceFromActive = if (activeIndex >= 0) abs(index - activeIndex) else Int.MAX_VALUE
                    LyricLineItem(
                        line = line,
                        positionMs = positionMs,
                        isActive = index == activeIndex,
                        distanceFromActive = distanceFromActive,
                        onColor = onColor,
                        room = room
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: StructuredLyricLine,
    positionMs: Long,
    isActive: Boolean,
    distanceFromActive: Int,
    onColor: Color,
    room: Float
) {
    val emphasis = when {
        isActive -> 1f
        distanceFromActive == 1 -> 0.7f
        distanceFromActive == 2 -> 0.48f
        else -> 0.28f
    }
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.004f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "lyricsScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = (0.22f + (0.78f * emphasis)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "lyricsAlpha"
    )
    val animatedTranslationAlpha by animateFloatAsState(
        targetValue = (0.16f + (0.58f * emphasis)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "lyricsTranslationAlpha"
    )
    val animatedY by animateFloatAsState(
        targetValue = if (isActive) -0.7f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "lyricsOffset"
    )
    val tokenGap = lerp(4.dp, 8.dp, room)
    val lineGap = lerp(4.dp, 10.dp, room)
    val titleStyle = if (room > 0.72f) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge
    val textStyle = titleStyle.copy(fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium)
    val itemAlignment = line.lane.toAlignment()
    val horizontalAlignment = line.lane.toHorizontalAlignment()
    val activeVerticalPadding = if (isActive) lerp(14.dp, 24.dp, room) else 0.dp
    val contentWidthFraction = when (line.lane) {
        LyricLane.LEFT, LyricLane.RIGHT, LyricLane.DUET -> 0.76f
        LyricLane.BACKGROUND -> 0.74f
        LyricLane.CENTER -> 0.9f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = activeVerticalPadding),
        contentAlignment = itemAlignment
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (isActive) lineGap * 1.7f else lineGap),
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
                .graphicsLayer {
                    alpha = animatedAlpha
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationY = animatedY
                }
        ) {
            line.secondaryLines.getOrNull(0)?.let { secondaryLine ->
                SecondaryLyricLine(
                    variant = secondaryLine,
                    positionMs = positionMs,
                    onColor = onColor.copy(alpha = animatedTranslationAlpha * 0.88f),
                    room = room,
                    lane = line.lane,
                    placement = SecondaryPlacement.TOP,
                    visible = isActive
                )
            }

            if (line.words.isNotEmpty()) {
                if (isActive) {
                    ActiveLyricsLine(
                        line = line,
                        positionMs = positionMs,
                        baseColor = onColor.copy(alpha = 0.40f),
                        fillColor = onColor,
                        style = textStyle,
                        tokenGap = tokenGap
                    )
                } else {
                    Text(
                        text = buildLineText(line.words),
                        style = textStyle,
                        color = onColor.copy(alpha = 0.28f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = line.text,
                    style = textStyle,
                    color = onColor.copy(alpha = if (isActive) 0.94f else 0.42f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            line.secondaryLines.getOrNull(1)?.let { secondaryLine ->
                SecondaryLyricLine(
                    variant = secondaryLine,
                    positionMs = positionMs,
                    onColor = onColor.copy(alpha = animatedTranslationAlpha * 0.84f),
                    room = room,
                    lane = line.lane,
                    placement = SecondaryPlacement.BOTTOM,
                    visible = isActive
                )
            }

            line.lane.activeRoleLabel()?.takeIf { isActive }?.let { roleLabel ->
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = onColor.copy(alpha = 0.56f)
                )
            }
        }
    }
}

@Composable
private fun SecondaryLyricLine(
    variant: LyricVariant,
    positionMs: Long,
    onColor: Color,
    room: Float,
    lane: LyricLane,
    placement: SecondaryPlacement,
    visible: Boolean,
) {
    val secondaryStyle = if (room > 0.72f) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.titleMedium
    }.copy(fontWeight = FontWeight.Medium)
    val tokenGap = lerp(3.dp, 6.dp, room)
    if (!visible) return
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
            tokenGap = tokenGap
        )
    } else {
        Text(
            text = variant.text,
            style = secondaryStyle,
            color = onColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActiveLyricsLine(
    line: StructuredLyricLine,
    positionMs: Long,
    baseColor: Color,
    fillColor: Color,
    style: TextStyle,
    tokenGap: androidx.compose.ui.unit.Dp,
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

    val animatedLineFront by animateFloatAsState(
        targetValue = lineProgress(line.startMs, line.endMs, positionMs),
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "lineFront"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .height(with(density) { layout.heightPx.coerceAtLeast(1f).toDp() })
    ) {
        layout.tokens.forEach { token ->
            val rowFront = smoothSweepProgress(
                front = animatedLineFront,
                start = token.rowStartRatio,
                end = token.rowEndRatio,
                softness = 0.10f
            )
            val renderProgress = smoothSweepProgress(
                front = rowFront,
                start = token.startVisualRatio,
                end = token.endVisualRatio,
                softness = 0.16f
            ).coerceIn(0f, 1f)
            val body = smoothSweepProgress(
                front = rowFront,
                start = token.startVisualRatio - 0.035f,
                end = token.endVisualRatio + 0.10f,
                softness = 0.18f
            ).coerceIn(0f, 1f)
            val scale = 1f + (0.02f * body)
            val alpha = 0.34f + (0.66f * body)
            val dimColor = baseColor.copy(alpha = 0.82f)
            val headSoftness = 0.18f
            val litStart = (renderProgress - headSoftness).coerceIn(0f, 1f)
            val litMid = renderProgress.coerceIn(litStart, 1f)
            val litEnd = (renderProgress + 0.10f).coerceIn(litMid, 1f)
            val wordBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to fillColor.copy(alpha = 0.94f),
                    litStart to fillColor.copy(alpha = 0.94f),
                    litMid to fillColor.copy(alpha = 0.88f),
                    litEnd to dimColor,
                    1f to dimColor
                )
            )

            translate(left = token.x, top = token.y) {
                scale(
                    scaleX = scale,
                    scaleY = scale,
                    pivot = Offset(0f, token.layout.size.height * 0.55f)
                ) {
                    drawText(
                        textLayoutResult = token.layout,
                        brush = wordBrush,
                        topLeft = Offset.Zero,
                        alpha = alpha
                    )
                }
            }
        }
    }
}

private fun buildLineText(words: List<TimedWord>): String {
    val builder = StringBuilder()
    words.forEachIndexed { index, word ->
        if (needsLeadingGap(words, index)) builder.append(' ')
        builder.append(word.text)
    }
    return builder.toString()
}

private fun needsLeadingGap(words: List<TimedWord>, index: Int): Boolean {
    if (index <= 0) return false
    val current = words[index].text
    if (current.all { !it.isLetterOrDigit() }) return false
    val previous = words[index - 1].text
    return previous.any { it.isLetterOrDigit() || it == '"' || it == '\'' }
}

private data class ActiveTokenPlacement(
    val word: TimedWord,
    val layout: TextLayoutResult,
    val x: Float,
    val y: Float,
    val rowIndex: Int,
    val centerRatio: Float,
    val rowStartRatio: Float,
    val rowEndRatio: Float,
    val startVisualRatio: Float,
    val endVisualRatio: Float,
)

private data class ActiveLineLayout(
    val tokens: List<ActiveTokenPlacement>,
    val heightPx: Float,
)

private fun measureActiveLineLayout(
    words: List<TimedWord>,
    style: TextStyle,
    maxWidthPx: Float,
    gapPx: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): ActiveLineLayout {
    if (words.isEmpty()) return ActiveLineLayout(emptyList(), 0f)

    val placements = mutableListOf<ActiveTokenPlacement>()
    var x = 0f
    var y = 0f
    var rowHeight = 0f
    var rowIndex = 0

    words.forEachIndexed { index, word ->
        val layout = textMeasurer.measure(
            text = word.text,
            style = style,
            softWrap = false,
            maxLines = 1
        )
        val gap = if (needsLeadingGap(words, index)) gapPx else 0f
        val tokenWidth = layout.size.width.toFloat()
        val tokenHeight = layout.size.height.toFloat()

        if (x > 0f && x + gap + tokenWidth > maxWidthPx) {
            x = 0f
            y += rowHeight + (tokenHeight * 0.16f)
            rowHeight = 0f
            rowIndex += 1
        }

        x += gap
        rowHeight = max(rowHeight.toInt(), tokenHeight.toInt()).toFloat()
        placements += ActiveTokenPlacement(
            word = word,
            layout = layout,
            x = x,
            y = y,
            rowIndex = rowIndex,
            centerRatio = 0f,
            rowStartRatio = 0f,
            rowEndRatio = 0f,
            startVisualRatio = 0f,
            endVisualRatio = 0f
        )
        x += tokenWidth
    }

    val lineStart = words.first().startMs
    val lineEnd = words.last().endMs
    val lineDuration = (lineEnd - lineStart).coerceAtLeast(1L).toFloat()
    val rows = placements.groupBy { it.rowIndex }

    return ActiveLineLayout(
        tokens = placements.map { placement ->
            val rowPlacements = rows.getValue(placement.rowIndex)
            val rowWidth = rowPlacements.maxOfOrNull { it.x + it.layout.size.width.toFloat() }?.coerceAtLeast(1f) ?: 1f
            val rowStartMs = rowPlacements.first().word.startMs
            val rowEndMs = rowPlacements.last().word.endMs
            val wordCenter = (placement.word.startMs + placement.word.endMs) / 2f
            placement.copy(
                centerRatio = ((wordCenter - lineStart) / lineDuration).coerceIn(0f, 1f),
                rowStartRatio = ((rowStartMs - lineStart) / lineDuration).coerceIn(0f, 1f),
                rowEndRatio = ((rowEndMs - lineStart) / lineDuration).coerceIn(0f, 1f),
                startVisualRatio = (placement.x / rowWidth).coerceIn(0f, 1f),
                endVisualRatio = ((placement.x + placement.layout.size.width.toFloat()) / rowWidth).coerceIn(0f, 1f)
            )
        },
        heightPx = y + rowHeight
    )
}

private fun LyricsUiState.toStructuredFallback(): List<StructuredLyricLine> {
    if (syncedLines.isEmpty()) return emptyList()
    return syncedLines.mapIndexed { index, line ->
        val startMs = line.timestampMs ?: 0L
        val endMs = syncedLines
            .getOrNull(index + 1)
            ?.timestampMs
            ?.takeIf { it > startMs }
            ?: (startMs + 4_000L)
        StructuredLyricLine(
            text = line.text,
            translation = line.translation,
            secondaryLines = line.secondaryTexts.map { secondaryText ->
                LyricVariant(text = secondaryText)
            },
            startMs = startMs,
            endMs = endMs
        )
    }
}

private fun resolveActiveLine(lines: List<StructuredLyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    return lines.indexOfLast { positionMs >= it.startMs }
        .takeIf { it >= 0 }
        ?.coerceIn(0, lines.lastIndex)
        ?: 0
}

private fun wordProgress(startMs: Long, endMs: Long, positionMs: Long): Float {
    val duration = (endMs - startMs).coerceAtLeast(1L)
    return ((positionMs - startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun lineProgress(startMs: Long, endMs: Long, positionMs: Long): Float {
    val duration = (endMs - startMs).coerceAtLeast(1L)
    return ((positionMs - startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun smoothSweepProgress(front: Float, start: Float, end: Float, softness: Float): Float {
    val span = (end - start).coerceAtLeast(0.0001f)
    val expandedStart = start - softness
    val expandedEnd = end + softness
    val normalized = ((front - expandedStart) / (expandedEnd - expandedStart).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
    val eased = normalized * normalized * (3f - 2f * normalized)
    return ((eased * (expandedEnd - expandedStart)) / span).coerceIn(0f, 1f)
}

private fun LyricLane.toAlignment(): Alignment = when (this) {
    LyricLane.LEFT, LyricLane.DUET, LyricLane.BACKGROUND -> Alignment.CenterStart
    LyricLane.RIGHT -> Alignment.CenterEnd
    LyricLane.CENTER -> Alignment.Center
}

private fun LyricLane.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    LyricLane.LEFT, LyricLane.DUET, LyricLane.BACKGROUND -> Alignment.Start
    LyricLane.RIGHT -> Alignment.End
    LyricLane.CENTER -> Alignment.CenterHorizontally
}

private fun LyricLane.activeRoleLabel(): String? = when (this) {
    LyricLane.DUET -> "Dueto"
    LyricLane.BACKGROUND -> "Coro"
    else -> null
}

private enum class SecondaryPlacement {
    TOP,
    BOTTOM
}
