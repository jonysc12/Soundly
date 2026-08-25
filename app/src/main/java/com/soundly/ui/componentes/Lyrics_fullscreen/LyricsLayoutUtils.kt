package com.soundly.ui.componentes.Lyrics_fullscreen

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.soundly.player.TimedWord
import kotlin.math.max

// ==========================
// Active line layout model
// ==========================

internal data class ActiveTokenPlacement(
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

internal data class ActiveLineLayout(
    val tokens: List<ActiveTokenPlacement>,
    val heightPx: Float,
)

// ==========================
// Text building helpers
// ==========================

internal fun buildLineText(words: List<TimedWord>): String {
    val builder = StringBuilder()
    words.forEachIndexed { index, word ->
        if (needsLeadingGap(words, index)) builder.append(' ')
        builder.append(word.text)
    }
    return builder.toString()
}

internal fun needsLeadingGap(words: List<TimedWord>, index: Int): Boolean {
    if (index <= 0) return false
    val current = words[index].text
    if (current.all { !it.isLetterOrDigit() }) return false
    val previous = words[index - 1].text
    return previous.any { it.isLetterOrDigit() || it == '"' || it == '\'' }
}

// ==========================
// Measurement of a single lyric line into rows + per-token geometry
// ==========================

internal fun measureActiveLineLayout(
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
