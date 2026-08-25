package com.soundly.ui.componentes.Lyrics_fullscreen

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.soundly.player.LyricLane
import com.soundly.player.LyricVariant
import com.soundly.player.LyricsUiState
import com.soundly.player.StructuredLyricLine

// ==========================
// Data model helpers
// ==========================

internal enum class SecondaryPlacement {
    TOP,
    BOTTOM
}

internal fun LyricsUiState.toStructuredFallback(): List<StructuredLyricLine> {
    if (syncedLines.isEmpty()) {
        return plainText?.lineSequence()?.map { text ->
            StructuredLyricLine(
                text = text,
                startMs = 0,
                endMs = 0,
                lane = LyricLane.LEFT
            )
        }?.toList() ?: emptyList()
    }
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

internal fun resolveActiveLine(
    lines: List<StructuredLyricLine>,
    positionMs: Long
): Int {
    if (lines.isEmpty()) return -1
    var lo = 0
    var hi = lines.lastIndex
    var result = -1
    while (lo <= hi) {
        val mid = (lo + hi).ushr(1)
        if (lines[mid].startMs <= positionMs) {
            result = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return result
}

// ==========================
// LyricLane alignment helpers
// ==========================

internal fun LyricLane.toAlignment(): Alignment = when (this) {
    LyricLane.LEFT, LyricLane.DUET, LyricLane.BACKGROUND -> Alignment.CenterStart
    LyricLane.RIGHT -> Alignment.CenterEnd
    LyricLane.CENTER -> Alignment.Center
}

internal fun LyricLane.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    LyricLane.LEFT, LyricLane.BACKGROUND, LyricLane.CENTER -> Alignment.Start
    LyricLane.RIGHT -> Alignment.End
    LyricLane.DUET -> Alignment.CenterHorizontally
}

internal fun LyricLane.toTextAlign(): TextAlign = when (this) {
    LyricLane.LEFT, LyricLane.BACKGROUND, LyricLane.CENTER -> TextAlign.Start
    LyricLane.RIGHT -> TextAlign.End
    LyricLane.DUET -> TextAlign.Center
}

internal fun LyricLane.activeRoleLabel(): String? = when (this) {
    LyricLane.DUET -> "Dueto"
    LyricLane.BACKGROUND -> "Coro"
    else -> null
}

// ==========================
// Content width per lane / layout geometry
// ==========================

internal fun contentWidthFractionForLane(lane: LyricLane): Float = when (lane) {
    LyricLane.LEFT, LyricLane.RIGHT, LyricLane.DUET -> 0.76f
    LyricLane.BACKGROUND -> 0.74f
    LyricLane.CENTER -> 0.9f
}

internal fun titleStyleForRoom(room: Float, titleStyleHm: TextStyle, titleStyleHs: TextStyle): TextStyle =
    if (room > 0.72f) titleStyleHm else titleStyleHs

internal fun tokenGapForRoom(room: Float): Dp = lerp(3.dp, 6.dp, room)

internal fun lineGapForRoom(room: Float): Dp = lerp(3.dp, 8.dp, room)

internal fun activeVerticalPaddingForRoom(room: Float, isActive: Boolean): Dp =
    lerp(6.dp, 10.dp, room)

internal fun fontWeightForActive(activeWeight: Float): FontWeight = FontWeight.SemiBold

// ==========================
// Time formatting
// ==========================

internal fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 60 / 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
