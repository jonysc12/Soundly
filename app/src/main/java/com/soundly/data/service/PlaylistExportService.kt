package com.soundly.data.service

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.soundly.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlSerializer
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportPlaylist(uri: Uri, songs: List<Song>, format: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver
            val outputStream = contentResolver.openOutputStream(uri) ?: throw Exception("Cannot open output stream")
            
            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                when (format.lowercase()) {
                    "m3u", "m3u8" -> writeM3U(writer, songs)
                    "xspf" -> writeXSPF(writer, songs)
                    else -> throw Exception("Unsupported format: $format")
                }
            }
        }
    }

    private fun writeM3U(writer: BufferedWriter, songs: List<Song>) {
        writer.write("#EXTM3U")
        writer.newLine()
        
        songs.forEach { song ->
            val durationInSeconds = song.duration / 1000
            writer.write("#EXTINF:$durationInSeconds,${song.artist} - ${song.title}")
            writer.newLine()
            writer.write(song.path)
            writer.newLine()
        }
    }

    private fun writeXSPF(writer: BufferedWriter, songs: List<Song>) {
        val serializer: XmlSerializer = Xml.newSerializer()
        val stringWriter = StringWriter()
        
        serializer.setOutput(stringWriter)
        serializer.startDocument("UTF-8", true)
        serializer.startTag("", "playlist")
        serializer.attribute("", "version", "1")
        serializer.attribute("", "xmlns", "http://xspf.org/ns/0/")
        
        serializer.startTag("", "trackList")
        songs.forEach { song ->
            serializer.startTag("", "track")
            
            serializer.startTag("", "location")
            serializer.text("file://${song.path}")
            serializer.endTag("", "location")
            
            serializer.startTag("", "title")
            serializer.text(song.title)
            serializer.endTag("", "title")
            
            serializer.startTag("", "creator")
            serializer.text(song.artist)
            serializer.endTag("", "creator")
            
            serializer.startTag("", "album")
            serializer.text(song.album)
            serializer.endTag("", "album")
            
            serializer.startTag("", "duration")
            serializer.text(song.duration.toString())
            serializer.endTag("", "duration")
            
            serializer.endTag("", "track")
        }
        serializer.endTag("", "trackList")
        serializer.endTag("", "playlist")
        serializer.endDocument()
        
        writer.write(stringWriter.toString())
    }
}
