package com.soundly.player

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.max

object LyricsParser {

    private val lineTimestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
    private val wordTimestampRegex = Regex("""<(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?>""")
    private val metadataRegex = Regex("""^\[(\w+):.*]$""", RegexOption.IGNORE_CASE)
    private val languageRegex = Regex("""^\[(lang|language):\s*([a-zA-Z-]+)\s*]$""", RegexOption.IGNORE_CASE)
    private val bracketSpeakerRegex = Regex("""^\[(?!\d{1,2}:\d{2})([^]]+)]\s*(.+)$""")
    private val colonSpeakerRegex = Regex("""^([^\s:<>\[\]]{1,24}):\s*(.+)$""")
    private val variantSpeakerRegex = Regex("""^(v\d+|ver\d+|main|lead)$""", RegexOption.IGNORE_CASE)
    private val backgroundSpeakerRegex = Regex("""^(bg|chorus|harm|harmony)$""", RegexOption.IGNORE_CASE)
    private val ttmlTagRegex = Regex("""<tt(\s|>)|<body(\s|>)|<p(\s|>)""", RegexOption.IGNORE_CASE)

    fun parse(
        content: String?,
        formatHint: LyricsFormat = LyricsFormat.UNKNOWN,
        mainArtists: List<String> = emptyList()
    ): LyricsUiState {
        if (content.isNullOrBlank()) return LyricsUiState()
        val normalized = content.replace("\uFEFF", "").trim()
        if (normalized.isBlank()) return LyricsUiState()

        val detectedFormat = detectFormat(normalized, formatHint)
        val parsed = when (detectedFormat) {
            LyricsFormat.TTML -> parseTtml(normalized, mainArtists)
            LyricsFormat.PLAIN -> parsePlain(normalized)
            LyricsFormat.OTHER -> parseOther(normalized)
            else -> parseTimedLyrics(normalized, detectedFormat, mainArtists)
        }

        return parsed.copy(
            rawContent = normalized,
            format = parsed.format.takeUnless { it == LyricsFormat.UNKNOWN } ?: detectedFormat
        )
    }

    fun parseLrc(lrcContent: String?): LyricsUiState = parse(lrcContent, LyricsFormat.LRC)

    fun parseLrcWithKaraoke(lrcContent: String?): LyricsUiState = parseLrc(lrcContent)

    fun convertToKaraokeTrack(syncedLines: List<LyricLine>): KaraokeTrack {
        if (syncedLines.isEmpty()) return KaraokeTrack()

        val structured = syncedLines.mapIndexed { index, line ->
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
                    LyricVariant(
                        text = secondaryText,
                        words = distributeWordsEvenly(secondaryText, startMs, endMs)
                    )
                },
                startMs = startMs,
                endMs = endMs,
                words = distributeWordsEvenly(line.text, startMs, endMs),
                speaker = line.speaker,
                lane = line.lane
            )
        }

        return structured.toKaraokeTrack()
    }

    private fun detectFormat(content: String, formatHint: LyricsFormat): LyricsFormat {
        if (formatHint != LyricsFormat.UNKNOWN) return formatHint
        if (ttmlTagRegex.containsMatchIn(content)) return LyricsFormat.TTML
        if (lineTimestampRegex.containsMatchIn(content)) {
            val hasExplicitWords = wordTimestampRegex.containsMatchIn(content)
            val hasSpeakers = content.lineSequence()
                .map { lineTimestampRegex.replace(it, "").trim() }
                .any { extractSpeaker(it).speaker != null }
            return when {
                hasExplicitWords && hasSpeakers -> LyricsFormat.ELRC_MULTI_PERSON
                hasExplicitWords -> LyricsFormat.ELRC
                hasSpeakers -> LyricsFormat.LRC_MULTI_PERSON
                else -> LyricsFormat.LRC
            }
        }
        return when {
            content.startsWith("<", ignoreCase = false) -> LyricsFormat.OTHER
            content.contains("{\\") || content.contains("|") -> LyricsFormat.OTHER
            else -> LyricsFormat.PLAIN
        }
    }

    private fun parsePlain(content: String): LyricsUiState =
        LyricsUiState(
            plainText = content,
            timingMode = LyricsTimingMode.NONE,
            format = LyricsFormat.PLAIN
        )

    private fun parseOther(content: String): LyricsUiState =
        LyricsUiState(
            plainText = content,
            timingMode = LyricsTimingMode.NONE,
            format = LyricsFormat.OTHER
        )

    private fun parseTimedLyrics(
        content: String,
        formatHint: LyricsFormat,
        mainArtists: List<String> = emptyList()
    ): LyricsUiState {
        val rawEntries = mutableListOf<RawLyricEntry>()
        val plainTextLines = mutableListOf<String>()
        var currentLanguage: String? = null
        var offsetMs = 0L
        var lastTimestampMs: Long? = null

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val languageMatch = languageRegex.matchEntire(line)
                if (languageMatch != null) {
                    currentLanguage = languageMatch.groupValues[2].lowercase()
                    return@forEach
                }

                if (metadataRegex.matches(line)) {
                    if (line.startsWith("[offset:", ignoreCase = true)) {
                        offsetMs = line.substringAfter(':').substringBefore(']').trim().toLongOrNull() ?: offsetMs
                    }
                    return@forEach
                }

                val timestampMatches = lineTimestampRegex.findAll(line).toList()
                if (timestampMatches.isEmpty()) {
                    val speakerInfo = extractSpeaker(line)
                    if (lastTimestampMs != null && speakerInfo.speaker != null && speakerInfo.text.isNotBlank()) {
                        rawEntries += RawLyricEntry(
                            timestampMs = lastTimestampMs ?: 0L,
                            text = speakerInfo.text,
                            language = currentLanguage,
                            speaker = speakerInfo.speaker,
                            hasExplicitWordTiming = wordTimestampRegex.containsMatchIn(speakerInfo.text),
                            sortOrder = rawEntries.size
                        )
                        plainTextLines += speakerInfo.text
                        return@forEach
                    }
                    plainTextLines += line
                    return@forEach
                }

                val timedText = line.substring(timestampMatches.last().range.last + 1).trim()
                if (timedText.isBlank()) return@forEach

                val speakerInfo = extractSpeaker(timedText)
                val lyricText = speakerInfo.text
                val hasExplicitWordTiming = wordTimestampRegex.containsMatchIn(lyricText)

                timestampMatches.forEach { match ->
                    val timestampMs = (
                        parseTimestamp(
                            minutes = match.groupValues[1],
                            seconds = match.groupValues[2],
                            fraction = match.groupValues.getOrNull(3).orEmpty()
                        ) + offsetMs
                        ).coerceAtLeast(0L)
                    rawEntries += RawLyricEntry(
                        timestampMs = timestampMs,
                        text = lyricText,
                        language = currentLanguage,
                        speaker = speakerInfo.speaker,
                        hasExplicitWordTiming = hasExplicitWordTiming,
                        sortOrder = rawEntries.size
                    )
                    lastTimestampMs = timestampMs
                }

                plainTextLines += lyricText
            }

        if (rawEntries.isEmpty()) {
            return if (plainTextLines.isNotEmpty()) {
                parsePlain(plainTextLines.joinToString("\n"))
            } else {
                LyricsUiState()
            }
        }

        val mergedEntries = mergeTranslations(rawEntries.sortedBy { it.timestampMs }, mainArtists)
        val structuredLines = buildStructuredLines(mergedEntries, mainArtists)
        val syncedLines = structuredLines.map { line ->
            LyricLine(
                timestampMs = line.startMs,
                text = line.text,
                translation = line.translation,
                secondaryTexts = line.secondaryLines.map { it.text },
                speaker = line.speaker,
                lane = line.lane
            )
        }
        val karaokeTrack = structuredLines.toKaraokeTrack()
        val resolvedFormat = resolveTimedFormat(formatHint, mergedEntries)

        return LyricsUiState(
            syncedLines = syncedLines,
            plainText = plainTextLines.joinToString("\n").takeIf { it.isNotBlank() },
            track = karaokeTrack.takeIf { it.lines.isNotEmpty() },
            structuredLines = structuredLines,
            timingMode = resolveTimingMode(structuredLines, syncedLines),
            format = resolvedFormat
        )
    }

    private fun parseTtml(content: String, mainArtists: List<String> = emptyList()): LyricsUiState {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(content))

        val lines = mutableListOf<StructuredLyricLine>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("p", ignoreCase = true)) {
                readTtmlParagraph(parser, mainArtists)?.let(lines::add)
            }
            eventType = parser.next()
        }

        if (lines.isEmpty()) {
            return parseOther(content).copy(format = LyricsFormat.TTML)
        }

        val syncedLines = lines.map { line ->
            LyricLine(
                timestampMs = line.startMs,
                text = line.text,
                translation = line.translation,
                secondaryTexts = line.secondaryLines.map { it.text },
                speaker = line.speaker,
                lane = line.lane
            )
        }

        return LyricsUiState(
            syncedLines = syncedLines,
            plainText = lines.joinToString("\n") { it.text },
            track = lines.toKaraokeTrack().takeIf { it.lines.isNotEmpty() },
            structuredLines = lines,
            timingMode = resolveTimingMode(lines, syncedLines),
            format = LyricsFormat.TTML
        )
    }

    private fun readTtmlParagraph(parser: XmlPullParser, mainArtists: List<String>): StructuredLyricLine? {
        val paragraphStart = parseTtmlTime(parser.getAttributeValue(null, "begin"))
        val explicitEnd = parseTtmlTime(parser.getAttributeValue(null, "end"))
        val durationMs = parseTtmlTime(parser.getAttributeValue(null, "dur"))
        val speaker = parser.attributeNames().firstNotNullOfOrNull { name ->
            if (name.equals("speaker", ignoreCase = true) ||
                name.equals("agent", ignoreCase = true) ||
                name.equals("who", ignoreCase = true)
            ) {
                parser.getAttributeValue(null, name)
            } else {
                null
            }
        }?.takeIf { it.isNotBlank() }

        val paragraphDepth = parser.depth
        val textBuilder = StringBuilder()
        val timedWords = mutableListOf<TimedWord>()

        while (true) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> if (parser.depth == paragraphDepth && parser.name.equals("p", ignoreCase = true)) break
                XmlPullParser.TEXT -> appendInlineText(textBuilder, parser.text)
                XmlPullParser.START_TAG -> when {
                    parser.name.equals("br", ignoreCase = true) -> appendInlineText(textBuilder, "\n")
                    parser.name.equals("span", ignoreCase = true) -> {
                        val spanStart = parseTtmlTime(parser.getAttributeValue(null, "begin"))
                        val spanEnd = parseTtmlTime(parser.getAttributeValue(null, "end"))
                        val spanDuration = parseTtmlTime(parser.getAttributeValue(null, "dur"))
                        val spanText = readInnerText(parser, "span")
                        appendInlineText(textBuilder, spanText)
                        val normalizedText = spanText.trim()
                        if (normalizedText.isNotEmpty() && spanStart != null) {
                            val resolvedEnd = spanEnd ?: spanDuration?.let { spanStart + it }
                            timedWords += TimedWord(
                                text = normalizedText,
                                startMs = spanStart,
                                endMs = maxOf(resolvedEnd ?: (spanStart + estimateWordDuration(normalizedText)), spanStart + 1L)
                            )
                        }
                    }
                }
            }
        }

        val text = textBuilder.toString().normalizeWhitespace()
        if (text.isBlank() || paragraphStart == null) return null

        val endMs = explicitEnd
            ?: durationMs?.let { paragraphStart + it }
            ?: timedWords.lastOrNull()?.endMs
            ?: (paragraphStart + estimateLineDuration(text))

        val normalizedWords = when {
            timedWords.isNotEmpty() -> normalizeExplicitWords(timedWords, endMs)
            else -> distributeWordsEvenly(text, paragraphStart, endMs)
        }

        return StructuredLyricLine(
            text = text,
            startMs = paragraphStart,
            endMs = maxOf(endMs, paragraphStart + 1L),
            words = normalizedWords,
            speaker = speaker,
            lane = resolveLane(speaker, mainArtists)
        )
    }

    private fun readInnerText(parser: XmlPullParser, tagName: String): String {
        val tagDepth = parser.depth
        val builder = StringBuilder()
        while (true) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> if (parser.depth == tagDepth && parser.name.equals(tagName, ignoreCase = true)) break
                XmlPullParser.TEXT -> appendInlineText(builder, parser.text)
                XmlPullParser.START_TAG -> if (parser.name.equals("br", ignoreCase = true)) appendInlineText(builder, "\n")
            }
        }
        return builder.toString().normalizeWhitespace()
    }

    private fun buildStructuredLines(entries: List<RawLyricEntry>, mainArtists: List<String>): List<StructuredLyricLine> {
        if (entries.isEmpty()) return emptyList()

        return entries.mapIndexed { index, entry ->
            val startMs = entry.timestampMs
            
            // Buscar el próximo inicio de línea que sea realmente posterior (no en el mismo bloque de tiempo)
            val nextSignificantStartMs = entries.asSequence()
                .drop(index + 1)
                .map { it.timestampMs }
                .firstOrNull { it > startMs + 100L } // Al menos 100ms de diferencia
            
            val endMs = nextSignificantStartMs ?: (startMs + estimateLineDuration(entry.text))

            val explicitWords = parseExplicitTimedWords(entry.text)
            val words = if (explicitWords.isNotEmpty()) {
                normalizeExplicitWords(explicitWords, fallbackEndMs = endMs)
            } else {
                distributeWordsEvenly(entry.text, startMs, endMs)
            }

            StructuredLyricLine(
                text = entry.text,
                translation = entry.translation,
                secondaryLines = entry.secondaryTexts.map { secondaryText ->
                    val secondaryWords = parseExplicitTimedWords(secondaryText).takeIf { it.isNotEmpty() }
                        ?: distributeWordsEvenly(secondaryText, startMs, maxOf(endMs, startMs + 1L))
                    LyricVariant(
                        text = secondaryText,
                        words = normalizeExplicitWords(secondaryWords, maxOf(endMs, startMs + 1L))
                    )
                }.take(2),
                startMs = startMs,
                endMs = maxOf(endMs, startMs + 1L),
                words = words,
                speaker = entry.speaker,
                lane = resolveLane(entry.speaker, mainArtists)
            )
        }
    }

    private fun parseExplicitTimedWords(text: String): List<TimedWord> {
        val matches = wordTimestampRegex.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        val words = mutableListOf<TimedWord>()
        for (index in matches.indices) {
            val current = matches[index]
            val next = matches.getOrNull(index + 1)
            val startMs = parseTimestamp(
                minutes = current.groupValues[1],
                seconds = current.groupValues[2],
                fraction = current.groupValues.getOrNull(3).orEmpty()
            )
            val segmentEnd = next?.range?.first ?: text.length
            val rawWord = text.substring(current.range.last + 1, segmentEnd).trim()
            if (rawWord.isBlank()) continue
            val endMs = next?.let {
                parseTimestamp(
                    minutes = it.groupValues[1],
                    seconds = it.groupValues[2],
                    fraction = it.groupValues.getOrNull(3).orEmpty()
                )
            } ?: (startMs + estimateWordDuration(rawWord))

            words += TimedWord(
                text = rawWord,
                startMs = startMs,
                endMs = maxOf(endMs, startMs + 1L)
            )
        }
        return words
    }

    private fun normalizeExplicitWords(words: List<TimedWord>, fallbackEndMs: Long): List<TimedWord> {
        if (words.isEmpty()) return emptyList()

        return words.mapIndexed { index, word ->
            val nextStart = words.getOrNull(index + 1)?.startMs
            val normalizedEnd = nextStart?.takeIf { it > word.startMs }
                ?: word.endMs.takeIf { it > word.startMs }
                ?: fallbackEndMs

            word.copy(endMs = maxOf(normalizedEnd, word.startMs + 1L))
        }
    }

    private fun distributeWordsEvenly(text: String, startMs: Long, endMs: Long): List<TimedWord> {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val totalDuration = (endMs - startMs).coerceAtLeast(words.size.toLong())
        val totalWeight = words.sumOf { it.length.coerceAtLeast(1) }
        var cursor = startMs

        return words.mapIndexed { index, word ->
            val remainingWords = words.size - index
            val remainingDuration = (endMs - cursor).coerceAtLeast(remainingWords.toLong())
            val baseDuration = if (index == words.lastIndex) {
                remainingDuration
            } else {
                ((word.length.coerceAtLeast(1).toFloat() / totalWeight.toFloat()) * totalDuration)
                    .toLong()
                    .coerceAtLeast(120L)
                    .coerceAtMost(remainingDuration - (remainingWords - 1))
            }
            val wordEnd = if (index == words.lastIndex) endMs else (cursor + baseDuration)

            TimedWord(
                text = word,
                startMs = cursor,
                endMs = maxOf(wordEnd, cursor + 1L)
            ).also {
                cursor = it.endMs
            }
        }
    }

    private fun mergeTranslations(entries: List<RawLyricEntry>, mainArtists: List<String>): List<RawLyricEntry> {
        if (entries.isEmpty()) return emptyList()

        return entries
            .groupBy { it.timestampMs }
            .toSortedMap()
            .values
            .flatMap { timestampGroup ->
                val backgroundTexts = timestampGroup
                    .filter { resolveLane(it.speaker, mainArtists) == LyricLane.BACKGROUND }
                    .map { it.text.trim() }
                    .filter { it.isNotBlank() }

                timestampGroup
                    .filter { resolveLane(it.speaker, mainArtists) != LyricLane.BACKGROUND }
                    .groupBy { it.speaker }
                    .toSortedMap(compareBy<String?> { it.orEmpty() })
                    .values
                    .map { group ->
                        val ordered = group.sortedWith(
                            compareBy<RawLyricEntry>(
                                { preferredLanguageRank(it.language) },
                                { it.sortOrder }
                            )
                        )
                        val prioritized = when {
                            ordered.size >= 3 -> listOf(ordered[1], ordered[0], ordered[2]) + ordered.drop(3)
                            else -> ordered
                        }
                        val primary = prioritized.first()
                        val secondaryTexts = (prioritized
                            .drop(1)
                            .map { it.text.trim() } + if (prioritized.first() == primary) backgroundTexts else emptyList())
                            .filter { it.isNotBlank() && !it.equals(primary.text, ignoreCase = false) }
                            .distinct()
                            .take(2)
                        val translation = secondaryTexts.firstOrNull()

                        primary.copy(
                            translation = translation,
                            secondaryTexts = secondaryTexts
                        )
                    }
            }
    }

    private fun preferredLanguageRank(language: String?): Int = when (language?.lowercase()) {
        "orig", "original", "default", "ja", "jp" -> 0
        null, "" -> 1
        "en" -> 2
        "es" -> 3
        else -> 4
    }

    private fun resolveTimedFormat(formatHint: LyricsFormat, entries: List<RawLyricEntry>): LyricsFormat {
        if (formatHint == LyricsFormat.TTML) return LyricsFormat.TTML
        val hasSpeakers = entries.any { !it.speaker.isNullOrBlank() }
        val hasExplicitWordTiming = entries.any { it.hasExplicitWordTiming }
        return when {
            hasExplicitWordTiming && hasSpeakers -> LyricsFormat.ELRC_MULTI_PERSON
            hasExplicitWordTiming -> LyricsFormat.ELRC
            hasSpeakers -> LyricsFormat.LRC_MULTI_PERSON
            formatHint == LyricsFormat.OTHER -> LyricsFormat.OTHER
            else -> LyricsFormat.LRC
        }
    }

    private fun resolveTimingMode(
        structuredLines: List<StructuredLyricLine>,
        syncedLines: List<LyricLine>
    ): LyricsTimingMode {
        if (structuredLines.isEmpty() && syncedLines.isEmpty()) return LyricsTimingMode.NONE
        if (structuredLines.any { it.words.isNotEmpty() }) {
            val hasExplicit = structuredLines.any { line ->
                line.words.zipWithNext().any { (current, next) -> current.endMs == next.startMs } ||
                    line.words.any { word -> word.startMs > line.startMs }
            }
            return if (hasExplicit) LyricsTimingMode.WORD else LyricsTimingMode.GENERATED_WORD
        }
        return LyricsTimingMode.LINE
    }

    private fun parseTtmlTime(raw: String?): Long? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            value.endsWith("ms", ignoreCase = true) -> value.dropLast(2).toDoubleOrNull()?.toLong()
            value.endsWith("s", ignoreCase = true) -> (value.dropLast(1).toDoubleOrNull()?.times(1000.0))?.toLong()
            value.contains(':') -> {
                val parts = value.split(':')
                when (parts.size) {
                    3 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].replace(',', '.').toDoubleOrNull() ?: 0.0
                        ((hours * 3600 + minutes * 60 + seconds) * 1000.0).toLong()
                    }
                    2 -> {
                        val minutes = parts[0].toDoubleOrNull() ?: 0.0
                        val seconds = parts[1].replace(',', '.').toDoubleOrNull() ?: 0.0
                        ((minutes * 60 + seconds) * 1000.0).toLong()
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun parseTimestamp(minutes: String, seconds: String, fraction: String): Long {
        val mins = minutes.toLongOrNull() ?: 0L
        val secs = seconds.toLongOrNull() ?: 0L
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> (fraction.toLongOrNull() ?: 0L) * 100L
            2 -> (fraction.toLongOrNull() ?: 0L) * 10L
            else -> fraction.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        }
        return ((mins * 60L) + secs) * 1_000L + millis
    }

    private fun estimateLineDuration(text: String): Long =
        (text.length.coerceAtLeast(8) * 90L).coerceIn(1_500L, 4_500L)

    private fun estimateWordDuration(word: String): Long =
        (word.length.coerceAtLeast(2) * 80L).coerceIn(140L, 900L)

    private fun appendInlineText(builder: StringBuilder, fragment: String?) {
        val normalized = fragment
            ?.replace('\u00A0', ' ')
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            .orEmpty()
        if (normalized.isBlank()) return
        if (builder.isNotEmpty() && builder.last() != '\n') {
            builder.append(' ')
        }
        builder.append(normalized)
    }

    private fun String.normalizeWhitespace(): String =
        lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private fun extractSpeaker(text: String): SpeakerInfo {
        bracketSpeakerRegex.matchEntire(text)?.let { match ->
            val normalizedSpeaker = normalizeSpeaker(match.groupValues[1])
            return SpeakerInfo(
                speaker = normalizedSpeaker,
                text = match.groupValues[2].trim()
            )
        }
        colonSpeakerRegex.matchEntire(text)?.let { match ->
            val speaker = normalizeSpeaker(match.groupValues[1])
            if ((speaker?.length ?: 0) <= 24) {
                return SpeakerInfo(
                    speaker = speaker,
                    text = match.groupValues[2].trim()
                )
            }
        }
        return SpeakerInfo(null, text.trim())
    }

    private fun normalizeSpeaker(rawSpeaker: String): String? {
        val speaker = rawSpeaker.trim()
        if (speaker.isBlank()) return null
        return when {
            variantSpeakerRegex.matches(speaker) -> speaker.lowercase()
            backgroundSpeakerRegex.matches(speaker) -> "BG"
            else -> speaker
        }
    }

    private fun XmlPullParser.attributeNames(): List<String> =
        (0 until attributeCount).mapNotNull { getAttributeName(it) }

    private fun List<StructuredLyricLine>.toKaraokeTrack(): KaraokeTrack =
        KaraokeTrack(
            lines = map { line ->
                KaraokeLine(
                    text = line.text,
                    translation = line.translation,
                    secondaryTexts = line.secondaryLines.map { it.text },
                    startMs = line.startMs,
                    endMs = line.endMs,
                    syllables = line.words.map { word ->
                        KaraokeSyllable(
                            text = word.text,
                            startMs = word.startMs,
                            endMs = word.endMs
                        )
                    },
                    speaker = line.speaker,
                    lane = line.lane
                )
            }
        )

    private fun resolveLane(speaker: String?, mainArtists: List<String> = emptyList()): LyricLane {
        if (speaker == null) return LyricLane.CENTER
        val normalizedSpeaker = speaker.trim().lowercase().removeSurrounding("[", "]").removeSurrounding("(", ")")
        
        // 1. Prioridad a tags explícitos de posición
        when (normalizedSpeaker) {
            "v1", "left", "l", "singer1" -> return LyricLane.LEFT
            "v2", "right", "r", "singer2" -> return LyricLane.RIGHT
            "v3", "duet", "both", "all", "together" -> return LyricLane.DUET
            "bg", "background", "chorus", "harmony" -> return LyricLane.BACKGROUND
        }

        // 2. Mapeo inteligente por nombre de artista (Fuzzy Matching)
        if (mainArtists.size >= 2) {
            val artists = mainArtists.map { it.lowercase().trim() }
            val a1 = artists[0]
            val a2 = artists[1]
            
            // Caso: El nombre del speaker es parte del nombre del artista 1 (o viceversa)
            val isA1 = normalizedSpeaker.length > 2 && (a1.contains(normalizedSpeaker) || normalizedSpeaker.contains(a1))
            val isA2 = normalizedSpeaker.length > 2 && (a2.contains(normalizedSpeaker) || normalizedSpeaker.contains(a2))

            if (isA1 && isA2) return LyricLane.DUET
            if (isA1) return LyricLane.LEFT
            if (isA2) return LyricLane.RIGHT
            
            // Caso: Comparación por primera palabra (ej. "Tito Double P" -> "Tito")
            val firstWordA1 = a1.substringBefore(" ")
            val firstWordA2 = a2.substringBefore(" ")
            if (firstWordA1.length > 2 && (normalizedSpeaker.contains(firstWordA1) || firstWordA1.contains(normalizedSpeaker))) return LyricLane.LEFT
            if (firstWordA2.length > 2 && (normalizedSpeaker.contains(firstWordA2) || firstWordA2.contains(normalizedSpeaker))) return LyricLane.RIGHT
        }

        return LyricLane.CENTER
    }

    private data class RawLyricEntry(
        val timestampMs: Long,
        val text: String,
        val translation: String? = null,
        val secondaryTexts: List<String> = emptyList(),
        val language: String? = null,
        val speaker: String? = null,
        val hasExplicitWordTiming: Boolean = false,
        val sortOrder: Int = 0,
    )

    private data class SpeakerInfo(
        val speaker: String?,
        val text: String,
    )
}
