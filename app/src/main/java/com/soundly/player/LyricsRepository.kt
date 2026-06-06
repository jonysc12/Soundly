package com.soundly.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit

class LyricsRepository(private val context: Context) {

    companion object {
        private const val TAG = "LyricsRepository"
        private const val LRCLIB_BASE_URL = "https://lrclib.net/api"
        private const val MAX_MEMORY_CACHE_SIZE = 24
        private val SUPPORTED_SIDECAR_EXTENSIONS = listOf("lrc", "elrc", "ttml", "txt")
        private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "mp4", "alac")
        private val TEXT_LYRICS_KEYS = listOf(
            "lyrics",
            "unsyncedlyrics",
            "syncedlyrics",
            "lyric",
            "unsynced lyric",
            "synced lyric",
            "lrc",
            "elrc",
            "karaoke"
        )
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val memoryCache = LinkedHashMap<String, LyricsUiState>(MAX_MEMORY_CACHE_SIZE, 0.75f, true)
    private val memoryCacheLock = Mutex()

    suspend fun loadLyrics(
        audioFile: File?,
        audioUri: Uri?,
        title: String,
        artist: String,
        album: String? = null,
        duration: Long? = null
    ): LyricsUiState = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(audioFile, audioUri, title, artist)
        memoryCacheLock.withLock {
            memoryCache[cacheKey]
        }?.let { return@withContext it }

        val lyrics = loadFromSeparateFile(audioFile)
            ?: loadFromLocalCache(audioFile, title, artist)
            ?: loadFromEmbeddedAudio(audioFile, audioUri)
            ?: fetchFromLrclib(title, artist, album, duration)?.also { fetched ->
                persistApiResult(audioFile, title, artist, fetched)
            }
            ?: LyricsUiState()

        memoryCacheLock.withLock {
            memoryCache[cacheKey] = lyrics
            trimMemoryCacheLocked()
        }
        lyrics
    }

    suspend fun saveLyrics(
        filename: String,
        lrcContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val lyricsDir = ensureLyricsDir()
            File(lyricsDir, filename).writeText(lrcContent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar lyrics", e)
            false
        }
    }

    suspend fun fetchFromLrclib(
        title: String,
        artist: String,
        album: String? = null,
        duration: Long? = null
    ): LyricsUiState? = withContext(Dispatchers.IO) {
        try {
            val params = buildList {
                add("artist_name=${Uri.encode(artist)}")
                add("track_name=${Uri.encode(title)}")
                album?.takeIf { it.isNotBlank() }?.let { add("album_name=${Uri.encode(it)}") }
                duration?.takeIf { it > 0L }?.let { add("duration=${it / 1000.0}") }
            }.joinToString("&")

            val request = Request.Builder()
                .url("$LRCLIB_BASE_URL/get?$params")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "LRCLIB response no exitosa: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string().orEmpty()
                if (responseBody.isBlank()) return@withContext null

                val json = JSONObject(responseBody)
                if (json.optBoolean("instrumental", false)) {
                    return@withContext LyricsUiState(
                        plainText = "♪ Instrumental ♪",
                        rawContent = "♪ Instrumental ♪",
                        format = LyricsFormat.PLAIN,
                        retrievalMethod = LyricsRetrievalMethod.API,
                        provider = "LRCLIB"
                    )
                }

                val syncedLyrics = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
                val plainLyrics = json.optString("plainLyrics").takeIf { it.isNotBlank() }
                val payload = syncedLyrics ?: plainLyrics ?: return@withContext null
                val hint = when {
                    syncedLyrics != null -> LyricsFormat.LRC
                    else -> LyricsFormat.PLAIN
                }

                LyricsParser.parse(payload, hint).enrich(
                    retrievalMethod = LyricsRetrievalMethod.API,
                    provider = "LRCLIB",
                    rawContent = payload
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al fetch de LRCLIB", e)
            null
        }
    }

    private suspend fun loadFromSeparateFile(audioFile: File?): LyricsUiState? {
        val baseFile = audioFile ?: return null
        val parent = baseFile.parentFile ?: return null
        val matchingFile = findLyricsSidecarFiles(parent, baseFile.nameWithoutExtension)
            .firstOrNull()
            ?: return null

        return parseLyricsFile(matchingFile, LyricsRetrievalMethod.SEPARATE_FILE)
    }

    private suspend fun loadFromLocalCache(audioFile: File?, title: String, artist: String): LyricsUiState? {
        val lyricsDir = ensureLyricsDir()
        val candidates = buildList {
            audioFile?.let { file ->
                addAll(findLyricsSidecarFiles(lyricsDir, file.nameWithoutExtension))
            }
            val normalizedArtist = sanitizeFileSegment(artist)
            val normalizedTitle = sanitizeFileSegment(title)
            if (normalizedArtist.isNotBlank() && normalizedTitle.isNotBlank()) {
                addAll(findLyricsSidecarFiles(lyricsDir, "$normalizedArtist - $normalizedTitle"))
            }
        }

        return candidates.firstOrNull { it.exists() && it.isFile && it.canRead() }
            ?.let { parseLyricsFile(it, LyricsRetrievalMethod.CACHE) }
    }

    private suspend fun loadFromEmbeddedAudio(audioFile: File?, audioUri: Uri?): LyricsUiState? {
        val payload = when {
            audioFile?.exists() == true -> extractEmbeddedLyrics(audioFile)
            audioUri != null -> extractEmbeddedLyrics(audioUri)
            else -> null
        } ?: return null

        return LyricsParser.parse(payload.content, payload.formatHint).enrich(
            retrievalMethod = LyricsRetrievalMethod.EMBEDDED_AUDIO,
            rawContent = payload.content
        )
    }

    private fun extractEmbeddedLyrics(audioFile: File): EmbeddedLyricsPayload? =
        runCatching {
            extractEmbeddedLyricsFromStream(audioFile.inputStream())
                ?: extractEmbeddedLyricsFromMp4(audioFile)
                ?: extractEmbeddedLyricsFromFlac(audioFile)
                ?: extractEmbeddedLyricsFromOgg(audioFile)
                ?: extractEmbeddedLyricsFromRiff(audioFile)
                ?: extractEmbeddedLyricsWithRetriever(file = audioFile, uri = null)
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo lyrics incrustadas de archivo", error)
        }.getOrNull()

    private fun extractEmbeddedLyrics(audioUri: Uri): EmbeddedLyricsPayload? =
        runCatching {
            val bytes = context.contentResolver.openInputStream(audioUri)?.use { it.readBytes() }
            bytes?.inputStream()?.use(::extractEmbeddedLyricsFromStream)
                ?: bytes?.let(::extractEmbeddedLyricsFromMp4)
                ?: bytes?.let(::extractEmbeddedLyricsFromFlac)
                ?: bytes?.let(::extractEmbeddedLyricsFromOgg)
                ?: bytes?.let(::extractEmbeddedLyricsFromRiff)
                ?: extractEmbeddedLyricsWithRetriever(file = null, uri = audioUri)
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo lyrics incrustadas de uri $audioUri", error)
        }.getOrNull()

    private fun extractEmbeddedLyricsFromStream(stream: InputStream): EmbeddedLyricsPayload? =
        stream.use { input ->
            val header = ByteArray(10)
            if (!readFully(input, header, 0, header.size)) return@use null
            if (!header.copyOfRange(0, 3).contentEquals(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()))) {
                return@use null
            }

            val version = header[3].toInt() and 0xFF
            val flags = header[5].toInt() and 0xFF
            val tagSize = decodeSynchsafeInt(header, 6)
            if (tagSize <= 0) return@use null

            val tagData = ByteArray(tagSize)
            if (!readFully(input, tagData, 0, tagSize)) return@use null

            val normalizedTagData = if ((flags and 0x80) != 0) removeUnsynchronization(tagData) else tagData

            var offset = skipExtendedHeader(normalizedTagData, version, flags)
            var bestCandidate: EmbeddedLyricsPayload? = null
            while (offset + 10 <= normalizedTagData.size) {
                val frameId = normalizedTagData.decodeToString(offset, offset + 4)
                if (frameId.all { it == '\u0000' }) break

                val frameSize = decodeFrameSize(normalizedTagData, offset + 4, version)
                if (frameSize <= 0 || offset + 10 + frameSize > normalizedTagData.size) break
                val frameFlags = normalizedTagData.copyOfRange(offset + 8, offset + 10)
                val frameData = normalizedTagData.copyOfRange(offset + 10, offset + 10 + frameSize)
                val preparedFrameData = prepareFrameData(frameData, frameFlags, version)

                val candidate = when (frameId) {
                    "USLT" -> parseUsltFrame(preparedFrameData)
                    "SYLT" -> parseSyltFrame(preparedFrameData)
                    "TXXX" -> parseTxxxFrame(preparedFrameData)
                    "COMM" -> parseCommentFrame(preparedFrameData)
                    else -> null
                }

                if (candidate != null && candidate.score > (bestCandidate?.score ?: Int.MIN_VALUE)) {
                    bestCandidate = candidate
                }

                offset += 10 + frameSize
            }
            bestCandidate
        }

    private fun parseUsltFrame(frameData: ByteArray): EmbeddedLyricsPayload? {
        if (frameData.size < 5) return null
        val encoding = frameData[0].toInt() and 0xFF
        val descriptorStart = 4
        val descriptorEnd = findTerminator(frameData, descriptorStart, encoding) ?: return null
        val lyricsStart = descriptorEnd + terminatorLength(encoding)
        if (lyricsStart >= frameData.size) return null
        val lyrics = decodeText(frameData, lyricsStart, frameData.size, encoding).trim()
        if (lyrics.isBlank()) return null
        return EmbeddedLyricsPayload(
            content = lyrics,
            formatHint = detectEmbeddedFormatHint(lyrics),
            score = if (lineTimestampRegex.containsMatchIn(lyrics)) 3 else 2
        )
    }

    private fun parseTxxxFrame(frameData: ByteArray): EmbeddedLyricsPayload? {
        if (frameData.isEmpty()) return null
        val encoding = frameData[0].toInt() and 0xFF
        val descriptionEnd = findTerminator(frameData, 1, encoding) ?: return null
        val description = decodeText(frameData, 1, descriptionEnd, encoding).trim().lowercase(Locale.US)
        val valueStart = descriptionEnd + terminatorLength(encoding)
        if (valueStart >= frameData.size) return null
        val value = decodeText(frameData, valueStart, frameData.size, encoding).trim()
        if (value.isBlank()) return null
        val looksLikeLyrics = description.contains("lyric") || description.contains("lrc") || description.contains("ttml")
        if (!looksLikeLyrics) return null
        return EmbeddedLyricsPayload(
            content = value,
            formatHint = detectEmbeddedFormatHint(value),
            score = 1
        )
    }

    private fun parseCommentFrame(frameData: ByteArray): EmbeddedLyricsPayload? {
        if (frameData.size < 5) return null
        val encoding = frameData[0].toInt() and 0xFF
        val descriptionStart = 4
        val descriptionEnd = findTerminator(frameData, descriptionStart, encoding) ?: return null
        val description = decodeText(frameData, descriptionStart, descriptionEnd, encoding).trim().lowercase(Locale.US)
        val textStart = descriptionEnd + terminatorLength(encoding)
        if (textStart >= frameData.size) return null
        val text = decodeText(frameData, textStart, frameData.size, encoding).trim()
        if (text.isBlank()) return null
        val looksLikeLyrics = description.contains("lyric") || description.contains("karaoke") || lineTimestampRegex.containsMatchIn(text)
        if (!looksLikeLyrics) return null
        return EmbeddedLyricsPayload(
            content = text,
            formatHint = detectEmbeddedFormatHint(text),
            score = if (lineTimestampRegex.containsMatchIn(text)) 2 else 1
        )
    }

    private fun parseSyltFrame(frameData: ByteArray): EmbeddedLyricsPayload? {
        if (frameData.size < 7) return null
        val encoding = frameData[0].toInt() and 0xFF
        val timeStampFormat = frameData[4].toInt() and 0xFF
        val contentType = frameData[5].toInt() and 0xFF
        if (contentType != 1 && contentType != 0) return null

        val descriptionEnd = findTerminator(frameData, 6, encoding) ?: return null
        var cursor = descriptionEnd + terminatorLength(encoding)
        val builder = StringBuilder()

        while (cursor < frameData.size) {
            val textEnd = findTerminator(frameData, cursor, encoding) ?: break
            val chunk = decodeText(frameData, cursor, textEnd, encoding).trim()
            val timeStart = textEnd + terminatorLength(encoding)
            if (timeStart + 4 > frameData.size) break
            val timestampRaw = ByteBuffer.wrap(frameData, timeStart, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()
            val timestampMs = when (timeStampFormat) {
                1 -> timestampRaw
                2 -> timestampRaw
                else -> timestampRaw
            }
            if (chunk.isNotBlank()) {
                if (builder.isNotEmpty()) builder.append('\n')
                builder.append(formatAsLrcTimestamp(timestampMs)).append(chunk)
            }
            cursor = timeStart + 4
        }

        val content = builder.toString().trim()
        if (content.isBlank()) return null
        return EmbeddedLyricsPayload(
            content = content,
            formatHint = LyricsFormat.LRC,
            score = 4
        )
    }

    private fun extractEmbeddedLyricsWithRetriever(file: File?, uri: Uri?): EmbeddedLyricsPayload? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            when {
                file != null -> retriever.setDataSource(file.absolutePath)
                uri != null -> retriever.setDataSource(context, uri)
                else -> return null
            }
            val lyricKey = runCatching {
                MediaMetadataRetriever::class.java.getField("METADATA_KEY_LYRIC").getInt(null)
            }.getOrNull()
            val lyric = lyricKey
                ?.let(retriever::extractMetadata)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            if (lyric == null) {
                null
            } else {
                EmbeddedLyricsPayload(
                    content = lyric,
                    formatHint = detectEmbeddedFormatHint(lyric),
                    score = 5
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Fallo MediaMetadataRetriever al leer lyrics incrustadas", error)
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }

    private fun extractEmbeddedLyricsFromFlac(file: File): EmbeddedLyricsPayload? =
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                parseFlacMetadata(
                    readRange = { start, length ->
                        raf.seek(start)
                        ByteArray(length).also { raf.readFully(it) }
                    },
                    totalSize = raf.length()
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo metadata FLAC", error)
        }.getOrNull()

    private fun extractEmbeddedLyricsFromFlac(data: ByteArray): EmbeddedLyricsPayload? =
        runCatching {
            parseFlacMetadata(
                readRange = { start, length ->
                    data.copyOfRange(start.toInt(), (start + length).toInt())
                },
                totalSize = data.size.toLong()
            )
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo metadata FLAC desde bytes", error)
        }.getOrNull()

    private fun parseFlacMetadata(
        readRange: (start: Long, length: Int) -> ByteArray,
        totalSize: Long
    ): EmbeddedLyricsPayload? {
        if (totalSize < 4) return null
        val signature = readRange(0, 4).toString(StandardCharsets.ISO_8859_1)
        if (signature != "fLaC") return null
        var offset = 4L
        while (offset + 4 <= totalSize) {
            val header = readRange(offset, 4)
            val blockType = header[0].toInt() and 0x7F
            val isLast = (header[0].toInt() and 0x80) != 0
            val blockLength = ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)
            val payloadStart = offset + 4
            if (payloadStart + blockLength > totalSize) break
            if (blockType == 4) {
                val payload = readRange(payloadStart, blockLength)
                parseVorbisCommentBlock(payload, "FLAC")?.let { return it }
            }
            offset = payloadStart + blockLength
            if (isLast) break
        }
        return null
    }

    private fun extractEmbeddedLyricsFromOgg(file: File): EmbeddedLyricsPayload? =
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length()
                // Solo leer el principio del archivo para buscar tags de Ogg,
                // usualmente los tags de Vorbis están en las primeras páginas.
                // Limitamos a 64KB para evitar cargar archivos gigantes.
                val readSize = fileSize.coerceAtMost(65536L).toInt()
                val data = ByteArray(readSize)
                raf.readFully(data)
                extractEmbeddedLyricsFromOgg(data)
            }
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo metadata Ogg/Opus", error)
        }.getOrNull()

    private fun extractEmbeddedLyricsFromOgg(data: ByteArray): EmbeddedLyricsPayload? {
        if (data.size < 4) return null
        val signature = data.copyOfRange(0, 4).toString(StandardCharsets.ISO_8859_1)
        if (signature != "OggS") return null
        val packets = parseOggPackets(data)
        if (packets.isEmpty()) return null
        val commentPacketIndex = packets.indexOfFirst { packet ->
            packet.size >= 7 && packet.copyOfRange(0, 7).toString(StandardCharsets.ISO_8859_1) == "\u0003vorbis" ||
                    packet.size >= 8 && packet.copyOfRange(0, 8).toString(StandardCharsets.ISO_8859_1) == "OpusTags"
        }
        if (commentPacketIndex < 0) return null
        val packet = packets[commentPacketIndex]
        val payload = when {
            packet.size >= 7 && packet.copyOfRange(0, 7).toString(StandardCharsets.ISO_8859_1) == "\u0003vorbis" -> packet.copyOfRange(7, packet.size)
            packet.size >= 8 && packet.copyOfRange(0, 8).toString(StandardCharsets.ISO_8859_1) == "OpusTags" -> packet.copyOfRange(8, packet.size)
            else -> return null
        }
        return parseVorbisCommentBlock(payload, "OGG")
    }

    private fun extractEmbeddedLyricsFromRiff(file: File): EmbeddedLyricsPayload? =
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length()
                // Los chunks de metadatos en WAV suelen estar al principio o al final.
                // Intentamos leer los primeros 32KB.
                val readSize = fileSize.coerceAtMost(32768L).toInt()
                val data = ByteArray(readSize)
                raf.readFully(data)
                extractEmbeddedLyricsFromRiff(data)
            }
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo metadata RIFF/WAV", error)
        }.getOrNull()

    private fun extractEmbeddedLyricsFromRiff(data: ByteArray): EmbeddedLyricsPayload? {
        if (data.size < 12) return null
        val riff = data.copyOfRange(0, 4).toString(StandardCharsets.ISO_8859_1)
        val type = data.copyOfRange(8, 12).toString(StandardCharsets.ISO_8859_1)
        if (riff != "RIFF" || type != "WAVE") return null
        var offset = 12
        while (offset + 8 <= data.size) {
            val chunkId = data.copyOfRange(offset, offset + 4).toString(StandardCharsets.ISO_8859_1)
            val chunkSize = readLittleEndianInt32(data, offset + 4)
            val chunkStart = offset + 8
            val chunkEnd = (chunkStart + chunkSize).coerceAtMost(data.size)
            if (chunkStart > data.size || chunkEnd > data.size) break
            if (chunkId == "LIST" && chunkEnd - chunkStart >= 4) {
                val listType = data.copyOfRange(chunkStart, chunkStart + 4).toString(StandardCharsets.ISO_8859_1)
                if (listType == "INFO") {
                    parseRiffInfoLyrics(data, chunkStart + 4, chunkEnd)?.let { return it }
                }
            }
            offset = chunkEnd + (chunkSize % 2)
        }
        return null
    }

    private fun extractEmbeddedLyricsFromMp4(file: File): EmbeddedLyricsPayload? =
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length()
                parseMp4Atoms(
                    lengthProvider = { fileSize },
                    readRange = { start, length ->
                        raf.seek(start)
                        ByteArray(length).also { raf.readFully(it) }
                    }
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Error leyendo metadata MP4/M4A", error)
        }.getOrNull()

    private fun extractEmbeddedLyricsFromMp4(data: ByteArray): EmbeddedLyricsPayload? {
        if (data.isEmpty()) return null
        return parseMp4Atoms(
            lengthProvider = { data.size.toLong() },
            readRange = { start, length ->
                data.copyOfRange(start.toInt(), (start + length).toInt())
            }
        )
    }

    private fun parseMp4Atoms(
        lengthProvider: () -> Long,
        readRange: (start: Long, length: Int) -> ByteArray
    ): EmbeddedLyricsPayload? {
        val fileSize = lengthProvider()
        val ilstRange = findMp4AtomPath(
            totalSize = fileSize,
            readRange = readRange,
            path = listOf("moov", "udta", "meta", "ilst")
        ) ?: findMp4AtomPath(
            totalSize = fileSize,
            readRange = readRange,
            path = listOf("moov", "meta", "ilst")
        )

        if (ilstRange == null) return null
        return parseIlstAtom(ilstRange, readRange)
    }

    private fun parseIlstAtom(
        range: Mp4AtomRange,
        readRange: (start: Long, length: Int) -> ByteArray
    ): EmbeddedLyricsPayload? {
        var offset = range.start
        val end = range.start + range.size
        var bestCandidate: EmbeddedLyricsPayload? = null

        while (offset + 8 <= end) {
            val header = readRange(offset, 8)
            val atomSize = readMp4AtomSize(header, offset, end) ?: break
            if (atomSize < 8L || offset + atomSize > end) break
            val atomType = decodeMp4Type(header, 4)
            val contentStart = offset + 8
            val contentSize = (atomSize - 8L).toInt()

            val candidate = when (atomType) {
                "©lyr" -> parseMp4LyricAtom(
                    payload = readRange(contentStart, contentSize),
                    atomType = atomType
                )
                "----" -> parseMp4FreeformAtom(readRange(contentStart, contentSize))
                else -> null
            }

            if (candidate != null && candidate.score > (bestCandidate?.score ?: Int.MIN_VALUE)) {
                bestCandidate = candidate
            }

            offset += atomSize
        }

        return bestCandidate
    }

    private fun parseMp4LyricAtom(payload: ByteArray, atomType: String): EmbeddedLyricsPayload? {
        val text = extractMp4DataText(payload)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return EmbeddedLyricsPayload(
            content = text,
            formatHint = detectEmbeddedFormatHint(text),
            score = if (lineTimestampRegex.containsMatchIn(text)) 6 else 5
        )
    }

    private fun parseMp4FreeformAtom(payload: ByteArray): EmbeddedLyricsPayload? {
        var offset = 0
        var mean: String? = null
        var name: String? = null
        var dataText: String? = null

        while (offset + 8 <= payload.size) {
            val size = readInt32(payload, offset).toLong()
            if (size < 8L || offset + size > payload.size) break
            val type = decodeMp4Type(payload, offset + 4)
            val bodyStart = offset + 8
            val bodyEnd = offset + size.toInt()
            when (type) {
                "mean" -> mean = decodeMp4TextBlock(payload, bodyStart, bodyEnd)
                "name" -> name = decodeMp4TextBlock(payload, bodyStart, bodyEnd)
                "data" -> dataText = decodeMp4DataBlock(payload, bodyStart, bodyEnd)
            }
            offset += size.toInt()
        }

        val normalizedName = listOfNotNull(mean, name).joinToString("/").lowercase(Locale.US)
        val text = dataText?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val looksLikeLyrics = normalizedName.contains("lyric") ||
            normalizedName.contains("karaoke") ||
            normalizedName.contains("lrc") ||
            lineTimestampRegex.containsMatchIn(text)
        if (!looksLikeLyrics) return null
        return EmbeddedLyricsPayload(
            content = text,
            formatHint = detectEmbeddedFormatHint(text),
            score = if (lineTimestampRegex.containsMatchIn(text)) 7 else 5
        )
    }

    private fun parseVorbisCommentBlock(payload: ByteArray, containerLabel: String): EmbeddedLyricsPayload? {
        if (payload.size < 8) return null
        var offset = 0
        val vendorLength = readLittleEndianInt32(payload, offset)
        offset += 4
        if (vendorLength < 0 || offset + vendorLength > payload.size) return null
        offset += vendorLength
        if (offset + 4 > payload.size) return null
        val commentCount = readLittleEndianInt32(payload, offset)
        offset += 4
        for (index in 0 until commentCount.coerceAtLeast(0)) {
            if (offset + 4 > payload.size) break
            val length = readLittleEndianInt32(payload, offset)
            offset += 4
            if (length <= 0 || offset + length > payload.size) break
            val comment = payload.copyOfRange(offset, offset + length).toString(StandardCharsets.UTF_8)
            offset += length
            val separator = comment.indexOf('=')
            if (separator <= 0) continue
            val key = comment.substring(0, separator).trim().lowercase(Locale.US)
            val value = comment.substring(separator + 1).trim()
            if (value.isBlank()) continue
            if (TEXT_LYRICS_KEYS.any { key.contains(it) }) {
                return EmbeddedLyricsPayload(
                    content = value,
                    formatHint = detectEmbeddedFormatHint(value),
                    score = if (lineTimestampRegex.containsMatchIn(value)) 7 else 5
                )
            }
        }
        return null
    }

    private fun parseOggPackets(data: ByteArray): List<ByteArray> {
        val packets = mutableListOf<ByteArray>()
        var pageOffset = 0
        var currentPacket = mutableListOf<Byte>()
        while (pageOffset + 27 <= data.size) {
            if (data.copyOfRange(pageOffset, pageOffset + 4).toString(StandardCharsets.ISO_8859_1) != "OggS") break
            val segmentCount = data[pageOffset + 26].toInt() and 0xFF
            val segmentTableStart = pageOffset + 27
            val segmentTableEnd = segmentTableStart + segmentCount
            if (segmentTableEnd > data.size) break
            var payloadOffset = segmentTableEnd
            for (index in 0 until segmentCount) {
                val segmentSize = data[segmentTableStart + index].toInt() and 0xFF
                if (payloadOffset + segmentSize > data.size) return packets
                repeat(segmentSize) { byteIndex ->
                    currentPacket.add(data[payloadOffset + byteIndex])
                }
                payloadOffset += segmentSize
                if (segmentSize < 255) {
                    packets.add(currentPacket.toByteArray())
                    currentPacket = mutableListOf()
                }
            }
            pageOffset = payloadOffset
        }
        return packets
    }

    private fun parseRiffInfoLyrics(data: ByteArray, start: Int, end: Int): EmbeddedLyricsPayload? {
        var offset = start
        while (offset + 8 <= end) {
            val chunkId = data.copyOfRange(offset, offset + 4).toString(StandardCharsets.ISO_8859_1)
            val chunkSize = readLittleEndianInt32(data, offset + 4)
            val valueStart = offset + 8
            val valueEnd = (valueStart + chunkSize).coerceAtMost(end)
            if (valueStart > end || valueEnd > end) break
            val key = chunkId.trim().lowercase(Locale.US)
            val value = data.copyOfRange(valueStart, valueEnd)
                .toString(StandardCharsets.UTF_8)
                .replace("\u0000", "")
                .trim()
            if (value.isNotBlank() && (key == "ilyr" || key == "lyrc" || TEXT_LYRICS_KEYS.any { key.contains(it) })) {
                return EmbeddedLyricsPayload(
                    content = value,
                    formatHint = detectEmbeddedFormatHint(value),
                    score = if (lineTimestampRegex.containsMatchIn(value)) 6 else 4
                )
            }
            offset = valueEnd + (chunkSize % 2)
        }
        return null
    }

    private fun detectEmbeddedFormatHint(content: String): LyricsFormat = when {
        content.contains("<tt", ignoreCase = true) -> LyricsFormat.TTML
        lineTimestampRegex.containsMatchIn(content) && wordTimestampRegex.containsMatchIn(content) -> LyricsFormat.ELRC
        lineTimestampRegex.containsMatchIn(content) -> LyricsFormat.LRC
        else -> LyricsFormat.PLAIN
    }

    private suspend fun parseLyricsFile(file: File, retrievalMethod: LyricsRetrievalMethod): LyricsUiState? =
        runCatching {
            val content = file.readText()
            LyricsParser.parse(content, formatHintForFile(file)).enrich(
                retrievalMethod = retrievalMethod,
                rawContent = content
            )
        }.onFailure { error ->
            Log.e(TAG, "Error al leer lyrics desde ${file.absolutePath}", error)
        }.getOrNull()?.takeUnless { it.isEmpty }

    private fun persistApiResult(audioFile: File?, title: String, artist: String, lyrics: LyricsUiState) {
        val content = lyrics.rawContent?.takeIf { it.isNotBlank() } ?: return
        runCatching {
            val lyricsDir = ensureLyricsDir()
            val extension = extensionForFormat(lyrics.format)
            val primaryName = audioFile?.nameWithoutExtension?.takeIf { it.isNotBlank() }
                ?: "${sanitizeFileSegment(artist)} - ${sanitizeFileSegment(title)}"
            File(lyricsDir, "$primaryName.$extension").writeText(content)
        }.onFailure { error ->
            Log.e(TAG, "No se pudo guardar lyrics en cache", error)
        }
    }

    private fun ensureLyricsDir(): File =
        File(context.filesDir, "lyrics").apply {
            if (!exists()) mkdirs()
        }

    private fun formatHintForFile(file: File): LyricsFormat = when (file.extension.lowercase(Locale.US)) {
        "lrc" -> LyricsFormat.LRC
        "elrc" -> LyricsFormat.ELRC
        "ttml" -> LyricsFormat.TTML
        "txt" -> LyricsFormat.PLAIN
        else -> LyricsFormat.OTHER
    }

    private fun extensionForFormat(format: LyricsFormat): String = when (format) {
        LyricsFormat.TTML -> "ttml"
        LyricsFormat.PLAIN -> "txt"
        LyricsFormat.OTHER -> "txt"
        LyricsFormat.ELRC,
        LyricsFormat.ELRC_MULTI_PERSON -> "elrc"
        else -> "lrc"
    }

    private fun sanitizeFileSegment(value: String): String =
        value.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")

    private fun findLyricsSidecarFiles(directory: File, baseName: String): List<File> {
        val normalizedBase = baseName.lowercase(Locale.US)
        val files = directory.listFiles().orEmpty()

        val prioritized = files
            .filter { file ->
                file.isFile &&
                    file.canRead() &&
                    file.nameWithoutExtension.equals(baseName, ignoreCase = true) &&
                    file.extension.lowercase(Locale.US) in SUPPORTED_SIDECAR_EXTENSIONS
            }
            .sortedBy { SUPPORTED_SIDECAR_EXTENSIONS.indexOf(it.extension.lowercase(Locale.US)) }

        val others = files
            .filter { file ->
                file.isFile &&
                    file.canRead() &&
                    file.nameWithoutExtension.lowercase(Locale.US) == normalizedBase &&
                    file.extension.lowercase(Locale.US) !in SUPPORTED_SIDECAR_EXTENSIONS &&
                    file.extension.lowercase(Locale.US) !in AUDIO_EXTENSIONS
            }
            .sortedBy { it.extension.lowercase(Locale.US) }

        return prioritized + others
    }

    private fun buildCacheKey(audioFile: File?, audioUri: Uri?, title: String, artist: String): String =
        listOfNotNull(audioFile?.absolutePath, audioUri?.toString(), title, artist).joinToString("|")

    private fun trimMemoryCacheLocked() {
        while (memoryCache.size > MAX_MEMORY_CACHE_SIZE) {
            val eldestKey = memoryCache.entries.firstOrNull()?.key ?: return
            memoryCache.remove(eldestKey)
        }
    }

    private fun LyricsUiState.enrich(
        retrievalMethod: LyricsRetrievalMethod,
        provider: String? = null,
        rawContent: String? = this.rawContent
    ): LyricsUiState = copy(
        retrievalMethod = retrievalMethod,
        provider = provider,
        rawContent = rawContent
    )

    private fun decodeSynchsafeInt(data: ByteArray, start: Int): Int =
        ((data[start].toInt() and 0x7F) shl 21) or
            ((data[start + 1].toInt() and 0x7F) shl 14) or
            ((data[start + 2].toInt() and 0x7F) shl 7) or
            (data[start + 3].toInt() and 0x7F)

    private fun decodeFrameSize(data: ByteArray, start: Int, version: Int): Int =
        if (version >= 4) {
            decodeSynchsafeInt(data, start)
        } else {
            ByteBuffer.wrap(data, start, 4).order(ByteOrder.BIG_ENDIAN).int
        }

    private fun skipExtendedHeader(data: ByteArray, version: Int, flags: Int): Int {
        val hasExtendedHeader = when (version) {
            3 -> (flags and 0x40) != 0
            4 -> (flags and 0x40) != 0
            else -> false
        }
        if (!hasExtendedHeader || data.size < 4) return 0

        val headerSize = if (version >= 4) {
            decodeSynchsafeInt(data, 0)
        } else {
            ByteBuffer.wrap(data, 0, 4).order(ByteOrder.BIG_ENDIAN).int
        }
        return headerSize.coerceIn(0, data.size)
    }

    private fun prepareFrameData(frameData: ByteArray, frameFlags: ByteArray, version: Int): ByteArray {
        if (version < 4 || frameFlags.size < 2) return frameData
        val secondFlagByte = frameFlags[1].toInt() and 0xFF
        val unsynchronised = (secondFlagByte and 0x02) != 0
        return if (unsynchronised) removeUnsynchronization(frameData) else frameData
    }

    private fun removeUnsynchronization(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        var writeIndex = 0
        var index = 0
        while (index < data.size) {
            val current = data[index]
            if (current == 0xFF.toByte() && index + 1 < data.size && data[index + 1] == 0x00.toByte()) {
                result[writeIndex++] = current
                index += 2
            } else {
                result[writeIndex++] = current
                index += 1
            }
        }
        return result.copyOf(writeIndex)
    }

    private fun findTerminator(data: ByteArray, start: Int, encoding: Int): Int? {
        val terminatorLength = terminatorLength(encoding)
        var index = start
        while (index + terminatorLength <= data.size) {
            val isTerminator = when (terminatorLength) {
                2 -> data[index] == 0.toByte() && data.getOrElse(index + 1) { 1.toByte() } == 0.toByte()
                else -> data[index] == 0.toByte()
            }
            if (isTerminator) return index
            index += terminatorLength
        }
        return null
    }

    private fun terminatorLength(encoding: Int): Int =
        if (encoding == 1 || encoding == 2) 2 else 1

    private fun decodeText(data: ByteArray, start: Int, end: Int, encoding: Int): String {
        val safeStart = start.coerceAtLeast(0)
        val safeEnd = end.coerceAtMost(data.size)
        if (safeStart >= safeEnd) return ""
        val slice = data.copyOfRange(safeStart, safeEnd)
        val charset: Charset = when (encoding) {
            1 -> StandardCharsets.UTF_16
            2 -> Charset.forName("UTF-16BE")
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
        return slice.toString(charset).replace("\u0000", "")
    }

    private fun findMp4AtomPath(
        totalSize: Long,
        readRange: (start: Long, length: Int) -> ByteArray,
        path: List<String>
    ): Mp4AtomRange? {
        fun search(start: Long, end: Long, depth: Int): Mp4AtomRange? {
            if (depth >= path.size) return null
            var offset = start
            while (offset + 8 <= end) {
                val header = readRange(offset, 8)
                val atomSize = readMp4AtomSize(header, offset, end) ?: break
                if (atomSize < 8L || offset + atomSize > end) break
                val atomType = decodeMp4Type(header, 4)
                val headerSize = if (atomType == "meta") 12L else 8L
                val contentStart = offset + headerSize
                val contentSize = atomSize - headerSize
                if (atomType == path[depth]) {
                    if (depth == path.lastIndex) {
                        return Mp4AtomRange(contentStart, contentSize)
                    }
                    val result = search(contentStart, contentStart + contentSize, depth + 1)
                    if (result != null) return result
                }
                offset += atomSize
            }
            return null
        }

        return search(0L, totalSize, 0)
    }

    private fun readMp4AtomSize(header: ByteArray, offset: Long, end: Long): Long? {
        val size32 = readInt32(header, 0).toLong() and 0xFFFFFFFFL
        return when {
            size32 == 0L -> end - offset
            size32 == 1L -> null
            else -> size32
        }
    }

    private fun decodeMp4Type(data: ByteArray, offset: Int): String =
        String(data, offset, 4, Charsets.ISO_8859_1)

    private fun readInt32(data: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.BIG_ENDIAN).int

    private fun readLittleEndianInt32(data: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun extractMp4DataText(payload: ByteArray): String? {
        var offset = 0
        while (offset + 8 <= payload.size) {
            val size = readInt32(payload, offset).toLong()
            if (size < 8L || offset + size > payload.size) break
            val type = decodeMp4Type(payload, offset + 4)
            if (type == "data") {
                return decodeMp4DataBlock(payload, offset + 8, offset + size.toInt())
            }
            offset += size.toInt()
        }
        return null
    }

    private fun decodeMp4TextBlock(data: ByteArray, start: Int, end: Int): String {
        val payloadStart = (start + 4).coerceAtMost(end)
        return data.copyOfRange(payloadStart, end)
            .toString(StandardCharsets.UTF_8)
            .replace("\u0000", "")
    }

    private fun decodeMp4DataBlock(data: ByteArray, start: Int, end: Int): String {
        val payloadStart = (start + 8).coerceAtMost(end)
        if (payloadStart >= end) return ""
        val raw = data.copyOfRange(payloadStart, end)
        val utf8 = raw.toString(StandardCharsets.UTF_8).replace("\u0000", "").trim()
        if (utf8.isNotBlank()) return utf8
        return raw.toString(StandardCharsets.UTF_16).replace("\u0000", "").trim()
    }

    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int, length: Int): Boolean {
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(buffer, offset + totalRead, length - totalRead)
            if (read <= 0) return false
            totalRead += read
        }
        return true
    }

    private fun formatAsLrcTimestamp(timestampMs: Long): String {
        val safe = timestampMs.coerceAtLeast(0L)
        val totalSeconds = safe / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (safe % 1000) / 10
        return "[%02d:%02d.%02d]".format(minutes, seconds, centiseconds)
    }

    private data class EmbeddedLyricsPayload(
        val content: String,
        val formatHint: LyricsFormat,
        val score: Int,
    )

    private val lineTimestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
    private val wordTimestampRegex = Regex("""<(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?>""")

    private data class Mp4AtomRange(
        val start: Long,
        val size: Long,
    )
}
