package com.soundly.cloud

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.aria2c.Aria2c
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.nio.ByteBuffer

// Función nativa para reparar el archivo de YouTube y hacerlo compatible con metadatos
private fun remuxToM4A(inputFile: File, outputFile: File) {
    val extractor = MediaExtractor()
    extractor.setDataSource(inputFile.absolutePath)
    
    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    
    var trackIndex = -1
    for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("audio/") == true) {
            trackIndex = i
            break
        }
    }
    
    if (trackIndex == -1) {
        extractor.release()
        muxer.release()
        throw Exception("No se encontró pista de audio")
    }
    
    val format = extractor.getTrackFormat(trackIndex)
    val mainTrack = muxer.addTrack(format)
    
    muxer.start()
    
    val buffer = ByteBuffer.allocate(1024 * 1024)
    val bufferInfo = android.media.MediaCodec.BufferInfo()
    
    extractor.selectTrack(trackIndex)
    while (true) {
        bufferInfo.offset = 0
        bufferInfo.size = extractor.readSampleData(buffer, 0)
        if (bufferInfo.size < 0) break
        
        bufferInfo.presentationTimeUs = extractor.sampleTime
        bufferInfo.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) 
            android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
        muxer.writeSampleData(mainTrack, buffer, bufferInfo)
        extractor.advance()
    }
    
    muxer.stop()
    muxer.release()
    extractor.release()
}

suspend fun downloadSong(context: Context, song: Song, onProgress: (Int) -> Unit) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "soundly_downloads"
    val notificationId = song.id.hashCode()

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Descargando...")
        .setContentText(song.title)
        .setOngoing(true)
        .setProgress(100, 0, false)
    notificationManager.notify(notificationId, builder.build())

    try {
        DownloadTracker.updateProgress(song, -1)
        withContext(Dispatchers.IO) {
            // 1. OBTENER METADATOS OFICIALES (YouTube Music -> iTunes -> MusicBrainz)
            val officialMeta = getRealMetadata(song.title, song.artist, song.thumbnailUrl, song.id)
            val ytTrack = officialMeta.title
            val ytArtist = officialMeta.artist
            val ytAlbum = officialMeta.album.ifBlank { song.album }.ifBlank { "YouTube Music" }
            val artworkUrl = officialMeta.artworkUrl

            // 2. CONFIGURACIÓN DE DESCARGA
            try {
                // Nos aseguramos de que YoutubeDL esté listo. 
                YoutubeDL.getInstance().init(context.applicationContext)
                Aria2c.getInstance().init(context.applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("SoundlyCloud", "Error crítico de inicialización: ${e.message}", e)
                // Si falla la inicialización, no podemos continuar
                throw Exception("La librería de descarga no pudo inicializarse: ${e.message}")
            }

            val outputDir = context.cacheDir
            val tempFile = File(outputDir, "raw_${song.id}.m4a")
            if (tempFile.exists()) tempFile.delete()

            val request = YoutubeDLRequest(song.videoUrl)
            // Formato más flexible para evitar errores de disponibilidad
            request.addOption("-f", "bestaudio/best") 
            request.addOption("-o", tempFile.absolutePath)
            request.addOption("--no-check-certificate")
            request.addOption("--force-overwrites")
            request.addOption("--no-part")
            request.addOption("--no-cache-dir")
            
            // MODO SIGILO: Usamos una combinación de clientes oficiales (iOS es el más estable)
            // Eliminamos aria2c temporalmente para que YouTube deje de bloquear la IP
            request.addOption("--extractor-args", "youtube:player_client=ios,android,web_safari")
            
            android.util.Log.d("SoundlyCloud", "Iniciando descarga en modo sigilo para: $ytTrack")
            
            val response = YoutubeDL.getInstance().execute(request, null) { progress, _, _ ->
                val p = progress.toInt()
                if (p >= 0) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onProgress(p)
                        DownloadTracker.updateProgress(song, p)
                        builder.setProgress(100, p, false)
                        notificationManager.notify(notificationId, builder.build())
                    }
                }
            }

            if (response.exitCode != 0) throw Exception("Error yt-dlp: ${response.out}")

            // 3. REPARACIÓN Y ETIQUETADO
            val downloadedFile = outputDir.listFiles()?.find { it.name.startsWith("raw_${song.id}") && it.length() > 1024 } 
                ?: throw Exception("Archivo no encontrado")

            val fixedFile = File(outputDir, "fixed_${song.id}.m4a")
            try {
                remuxToM4A(downloadedFile, fixedFile)
                downloadedFile.delete()
            } catch (e: Exception) {
                if (!downloadedFile.renameTo(fixedFile)) {
                    downloadedFile.inputStream().use { input ->
                        fixedFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    downloadedFile.delete()
                }
            }

            // Aplicar etiquetas físicas con carátula cuadrada de alta resolución
            try {
                val audioFile = AudioFileIO.read(fixedFile)
                val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }
                tag.setField(FieldKey.TITLE, ytTrack)
                tag.setField(FieldKey.ARTIST, ytArtist)
                tag.setField(FieldKey.ALBUM, ytAlbum)
                officialMeta.year.let { if (it.isNotBlank()) tag.setField(FieldKey.YEAR, it) }
                
                sharedHttpClient.newCall(Request.Builder().url(artworkUrl).build()).execute().use { res ->
                    res.body?.bytes()?.let { bytes ->
                        val artwork = StandardArtwork()
                        artwork.binaryData = bytes
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    }
                }
                audioFile.commit()
            } catch (e: Exception) { android.util.Log.e("Soundly", "Error tags: ${e.message}") }

            // 5. GUARDADO FINAL
            val safeName = ytTrack.replace(Regex("[^a-zA-Z0-9 ]"), "_").trim()
            val finalFileName = "$safeName.m4a"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, finalFileName)
                put(MediaStore.Audio.Media.TITLE, ytTrack)
                put(MediaStore.Audio.Media.ARTIST, ytArtist)
                put(MediaStore.Audio.Media.ALBUM, ytAlbum)
                officialMeta.year.let { if (it.isNotBlank()) put(MediaStore.Audio.Media.YEAR, it.toIntOrNull() ?: 0) }
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/SoundlyCloud")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { targetUri ->
                context.contentResolver.openOutputStream(targetUri)?.use { out -> fixedFile.inputStream().use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(targetUri, values, null, null)
                val publicFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SoundlyCloud/$finalFileName")
                android.media.MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), arrayOf("audio/mp4")) { _, _ -> }
                kotlinx.coroutines.delay(1000)
            }
            fixedFile.delete()
            
            // NOTIFICAR AL TRACKER QUE TERMINÓ
            DownloadTracker.removeActiveDownload(song.id)
        }

        withContext(Dispatchers.Main) {
            onProgress(100)
            builder.setContentTitle("¡Descarga completa!").setContentText(song.title)
                .setOngoing(false).setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
            notificationManager.notify(notificationId, builder.build())
            android.util.Log.d("SoundlyCloud", "Descarga completada con éxito: ${song.title} (${song.id})")
        }
    } catch (e: Exception) {
        DownloadTracker.removeActiveDownload(song.id)
        withContext(Dispatchers.Main) {
            builder.setContentTitle("Fallo en descarga").setContentText(e.message).setOngoing(false)
            notificationManager.notify(notificationId, builder.build())
            android.util.Log.e("SoundlyCloud", "Error en descarga de ${song.title}: ${e.message}", e)
        }
    }
}
