package com.soundly.player

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.soundly.data.model.Song

@Immutable
data class PlayerUiState(
    val currentSongId: Long? = null,
    val currentSongIndex: Int = -1,
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
    val artistInfo: ArtistUiState = ArtistUiState(),
    val artistsInfo: List<ArtistUiState> = emptyList(),
    val queue: List<Song> = emptyList(),
    val isCasting: Boolean = false,
    val castDeviceName: String? = null,
    val currentBackgroundColor: Int = 0xFF121212.toInt(),
    val artworkBitmap: Bitmap? = null,
    val backgroundGradientSquare: Bitmap? = null,
    val backgroundGradientWide: Bitmap? = null,
    val queueArtworks: Map<Long, Bitmap> = emptyMap()
)

@Immutable
data class LyricsUiState(
    val syncedLines: List<LyricLine> = emptyList(),
    val plainText: String? = null,
    val track: KaraokeTrack? = null,
    val structuredLines: List<StructuredLyricLine> = emptyList(),
    val timingMode: LyricsTimingMode = LyricsTimingMode.NONE,
    val format: LyricsFormat = LyricsFormat.UNKNOWN,
    val rawContent: String? = null,
    val isLoading: Boolean = false,
    val retrievalMethod: LyricsRetrievalMethod = LyricsRetrievalMethod.API,
    val provider: String? = null
) {
    val isEmpty: Boolean get() = syncedLines.isEmpty() && plainText.isNullOrBlank() && track == null
}

@Immutable
data class LyricLine(
    val timestampMs: Long? = null,
    val text: String,
    val translation: String? = null,
    val secondaryTexts: List<String> = emptyList(),
    val speaker: String? = null,
    val lane: LyricLane = LyricLane.CENTER
)

@Immutable
data class StructuredLyricLine(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val words: List<TimedWord> = emptyList(),
    val translation: String? = null,
    val secondaryLines: List<LyricVariant> = emptyList(),
    val speaker: String? = null,
    val lane: LyricLane = LyricLane.CENTER
)

@Immutable
data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

@Immutable
data class LyricVariant(
    val text: String,
    val words: List<TimedWord> = emptyList()
)

enum class LyricsTimingMode {
    NONE, LINE, WORD, GENERATED_WORD
}

enum class LyricsFormat {
    LRC, ELRC, LRC_MULTI_PERSON, ELRC_MULTI_PERSON, TTML, PLAIN, OTHER, UNKNOWN
}

enum class LyricLane {
    LEFT, CENTER, RIGHT, DUET, BACKGROUND
}

@Immutable
data class ArtistUiState(
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@Immutable
data class KaraokeTrack(
    val lines: List<KaraokeLine> = emptyList()
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
    val lane: LyricLane = LyricLane.CENTER
)

@Immutable
data class KaraokeSyllable(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

enum class LyricsRetrievalMethod {
    EMBEDDED_AUDIO, SEPARATE_FILE, API, CACHE
}
