package com.soundly.player

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PlayerUiState(
    val currentSongId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: Uri? = null,
    val isCurrentSongFavorite: Boolean = false,
    val lyrics: LyricsUiState = LyricsUiState(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF,
    val sleepRemainingMs: Long? = null,
)

@Immutable
data class LyricsUiState(
    val syncedLines: List<LyricLine> = emptyList(),
    val plainText: String? = null,
    val rawContent: String? = null,
    val track: KaraokeTrack? = null,
    val structuredLines: List<StructuredLyricLine> = emptyList(),
    val timingMode: LyricsTimingMode = LyricsTimingMode.NONE,
    val format: LyricsFormat = LyricsFormat.UNKNOWN,
    val retrievalMethod: LyricsRetrievalMethod = LyricsRetrievalMethod.UNKNOWN,
    val provider: String? = null,
    val sourceLabel: String? = null,
) {
    val hasSynced: Boolean
        get() = track?.lines?.isNotEmpty() == true || syncedLines.isNotEmpty() || structuredLines.isNotEmpty()

    val isEmpty: Boolean
        get() = !hasSynced && plainText.isNullOrBlank()

    val methodLabel: String?
        get() = when (retrievalMethod) {
            LyricsRetrievalMethod.SEPARATE_FILE -> "Archivo separado"
            LyricsRetrievalMethod.EMBEDDED_AUDIO -> "Audio incrustado"
            LyricsRetrievalMethod.API -> provider?.let { "API $it" } ?: "API"
            LyricsRetrievalMethod.CACHE -> "Cache local"
            LyricsRetrievalMethod.UNKNOWN -> null
        }

    val formatLabel: String?
        get() = when (format) {
            LyricsFormat.PLAIN -> "Plain"
            LyricsFormat.LRC -> "LRC"
            LyricsFormat.LRC_MULTI_PERSON -> "LRC Multi-Person"
            LyricsFormat.ELRC -> "ELRC"
            LyricsFormat.ELRC_MULTI_PERSON -> "ELRC Multi-Person"
            LyricsFormat.TTML -> "TTML"
            LyricsFormat.OTHER -> "Other"
            LyricsFormat.UNKNOWN -> null
        }

    val displaySource: String?
        get() = sourceLabel ?: listOfNotNull(methodLabel, formatLabel)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
}

enum class LyricsTimingMode {
    NONE,
    LINE,
    WORD,
    GENERATED_WORD
}

enum class LyricsFormat {
    UNKNOWN,
    PLAIN,
    LRC,
    LRC_MULTI_PERSON,
    ELRC,
    ELRC_MULTI_PERSON,
    TTML,
    OTHER
}

enum class LyricsRetrievalMethod {
    UNKNOWN,
    SEPARATE_FILE,
    EMBEDDED_AUDIO,
    API,
    CACHE
}

@Immutable
data class LyricLine(
    val timestampMs: Long?,
    val text: String,
    val translation: String? = null,
    val secondaryTexts: List<String> = emptyList(),
    val speaker: String? = null,
    val lane: LyricLane = LyricLane.CENTER,
)

@Immutable
data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

@Immutable
data class StructuredLyricLine(
    val text: String,
    val translation: String? = null,
    val secondaryLines: List<LyricVariant> = emptyList(),
    val startMs: Long,
    val endMs: Long,
    val words: List<TimedWord> = emptyList(),
    val speaker: String? = null,
    val lane: LyricLane = LyricLane.CENTER,
)

@Immutable
data class LyricVariant(
    val text: String,
    val words: List<TimedWord> = emptyList(),
)

@Immutable
data class KaraokeSyllable(
    val content: String,
    val startMs: Long,
    val endMs: Long,
)

@Immutable
data class KaraokeLine(
    val text: String,
    val translation: String? = null,
    val secondaryTexts: List<String> = emptyList(),
    val startMs: Long,
    val endMs: Long,
    val syllables: List<KaraokeSyllable> = emptyList(),
    val speaker: String? = null,
    val lane: LyricLane = LyricLane.CENTER,
)

enum class LyricLane {
    LEFT,
    RIGHT,
    CENTER,
    DUET,
    BACKGROUND
}

@Immutable
data class KaraokeTrack(
    val lines: List<KaraokeLine> = emptyList()
) {
    fun activeLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val timestamps = lines.map { it.startMs }
        val idx = timestamps.binarySearch(positionMs)
        return if (idx >= 0) idx else (idx.inv() - 1).coerceIn(0, lines.lastIndex)
    }

    fun lineProgress(index: Int, positionMs: Long): Float {
        val line = lines.getOrNull(index) ?: return 0f
        val duration = (line.endMs - line.startMs).coerceAtLeast(1L)
        return ((positionMs - line.startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    fun syllableProgress(lineIndex: Int, positionMs: Long): Pair<Int, Float>? {
        val line = lines.getOrNull(lineIndex) ?: return null
        val syllIdx = line.syllables.indexOfLast { positionMs >= it.startMs }
        if (syllIdx < 0) return null
        val syll = line.syllables[syllIdx]
        val dur = (syll.endMs - syll.startMs).coerceAtLeast(1L)
        val prog = ((positionMs - syll.startMs).toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        return syllIdx to prog
    }
}
