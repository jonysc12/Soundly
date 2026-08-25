package com.soundly.data.service

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.soundly.data.model.Song
import com.soundly.data.utils.fixEncoding
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class PlaylistImportResult {
    object Idle : PlaylistImportResult()
    object Loading : PlaylistImportResult()
    data class Success(val data: ImportedPlaylist) : PlaylistImportResult()
    data class Error(val message: String) : PlaylistImportResult()
}

data class ImportedPlaylist(
    val name: String,
    val songIds: List<Long>,
    val totalInFile: Int,
    val foundCount: Int
)

@Singleton
class PlaylistImportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val MAX_FILE_SIZE = 512 * 1024 // 512 KB

    suspend fun importPlaylist(uri: Uri, librarySongs: List<Song>): Result<ImportedPlaylist> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver
            val fileName = getFileNameFromUri(uri) ?: "Imported Playlist"
            val pName = fileName.substringBeforeLast(".")

            // Check file size
            val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0
            if (fileSize > MAX_FILE_SIZE) {
                throw Exception("File too large (Max 512KB)")
            }

            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
            val extension = fileName.substringAfterLast(".").lowercase(Locale.ROOT)

            val entries = when (extension) {
                "m3u", "m3u8" -> parseM3U(inputStream)
                "xspf" -> parseXSPF(inputStream)
                else -> throw Exception("Unsupported format: $extension")
            }

            if (entries.isEmpty()) {
                throw Exception("File is empty or invalid")
            }

            val foundIds = mutableListOf<Long>()
            var foundCount = 0

            // Index library for faster fuzzy matching
            val songsByPath = librarySongs.associateBy { it.path }
            val songsByName = librarySongs.groupBy { getFileNameWithoutExtension(it.path).lowercase(Locale.ROOT) }
            val songsByMetadata = librarySongs.groupBy { 
                "${it.title.lowercase(Locale.ROOT)}|${it.artist.lowercase(Locale.ROOT)}" 
            }

            entries.forEach { entry ->
                val matchedSong = findMatch(entry, songsByPath, songsByName, songsByMetadata)
                if (matchedSong != null) {
                    foundIds.add(matchedSong.id)
                    foundCount++
                }
            }

            ImportedPlaylist(
                name = pName,
                songIds = foundIds,
                totalInFile = entries.size,
                foundCount = foundCount
            )
        }
    }

    private fun findMatch(
        entry: PlaylistEntry,
        songsByPath: Map<String, Song>,
        songsByName: Map<String, List<Song>>,
        songsByMetadata: Map<String, List<Song>>
    ): Song? {
        // 1. Exact path match
        songsByPath[entry.path]?.let { return it }

        // 2. Normalized path match (fix separators)
        val normalizedPath = entry.path.replace('\\', '/')
        songsByPath[normalizedPath]?.let { return it }

        // 3. File name match
        val entryFileName = getFileNameWithoutExtension(entry.path).lowercase(Locale.ROOT)
        songsByName[entryFileName]?.firstOrNull()?.let { return it }

        // 4. Metadata match (if available)
        if (entry.title != null && entry.artist != null) {
            val metaKey = "${entry.title.lowercase(Locale.ROOT)}|${entry.artist.lowercase(Locale.ROOT)}"
            songsByMetadata[metaKey]?.firstOrNull()?.let { return it }
        } else if (entry.title != null) {
            // Try title only if artist is missing
            songsByName[entry.title.lowercase(Locale.ROOT)]?.firstOrNull()?.let { return it }
        }

        return null
    }

    private fun parseM3U(inputStream: java.io.InputStream): List<PlaylistEntry> {
        val entries = mutableListOf<PlaylistEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line: String?
        var currentTitle: String? = null
        var currentArtist: String? = null

        while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line?.trim() ?: continue
            if (trimmedLine.isEmpty()) continue

            if (trimmedLine.startsWith("#EXTINF:")) {
                // Format: #EXTINF:duration,Artist - Title OR #EXTINF:duration,Title
                val info = trimmedLine.substringAfter("#EXTINF:").substringAfter(",")
                if (info.contains(" - ")) {
                    currentArtist = info.substringBefore(" - ").trim().fixEncoding()
                    currentTitle = info.substringAfter(" - ").trim().fixEncoding()
                } else {
                    currentTitle = info.trim().fixEncoding()
                }
            } else if (!trimmedLine.startsWith("#")) {
                entries.add(PlaylistEntry(path = trimmedLine, title = currentTitle, artist = currentArtist))
                currentTitle = null
                currentArtist = null
            }
        }
        return entries
    }

    private fun parseXSPF(inputStream: java.io.InputStream): List<PlaylistEntry> {
        val entries = mutableListOf<PlaylistEntry>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        
        var eventType = parser.eventType
        var currentPath: String? = null
        var currentTitle: String? = null
        var currentArtist: String? = null
        var inTrack = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "track") inTrack = true
                    if (inTrack) {
                        when (tagName) {
                            "location" -> currentPath = parser.nextText().trim().removePrefix("file://")
                            "title" -> currentTitle = parser.nextText().trim().fixEncoding()
                            "creator" -> currentArtist = parser.nextText().trim().fixEncoding()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "track") {
                        currentPath?.let {
                            entries.add(PlaylistEntry(it, currentTitle, currentArtist))
                        }
                        currentPath = null
                        currentTitle = null
                        currentArtist = null
                        inTrack = false
                    }
                }
            }
            eventType = parser.next()
        }
        return entries
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    private fun getFileNameWithoutExtension(path: String): String {
        val fileName = path.substringAfterLast('/').substringAfterLast('\\')
        return fileName.substringBeforeLast('.')
    }

    private data class PlaylistEntry(
        val path: String,
        val title: String? = null,
        val artist: String? = null
    )
}
