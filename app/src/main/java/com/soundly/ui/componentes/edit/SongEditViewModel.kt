package com.soundly.ui.componentes.edit

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.model.Song
import com.soundly.data.utils.fixEncoding
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.inject.Inject

data class SongEditState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val composer: String = "",
    val genre: String = "",
    val year: String = "",
    val artwork: Bitmap? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val pendingWriteRequest: IntentSenderRequest? = null,
    val saveCompleted: Boolean = false
)

@HiltViewModel
class SongEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SongEditState())
    val state: StateFlow<SongEditState> = _state.asStateFlow()

    private var currentSong: Song? = null
    private var newArtworkBytes: ByteArray? = null

    fun loadSong(song: Song) {
        if (currentSong?.id == song.id) return
        
        currentSong = song
        // Pre-cargamos los datos que ya tenemos para evitar el parpadeo de campos vacíos
        _state.update { 
            it.copy(
                title = song.title,
                artist = song.artist,
                album = song.album,
                isLoading = true, 
                error = null
            ) 
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(song.path)
                
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).fixEncoding().ifBlank { song.title }
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).fixEncoding().ifBlank { song.artist }
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).fixEncoding().ifBlank { song.album }
                val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER).fixEncoding()
                val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).fixEncoding()
                val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE).fixEncoding()
                
                val artworkBytes = retriever.embeddedPicture
                val artwork = artworkBytes?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }

                _state.update {
                    it.copy(
                        title = title,
                        artist = artist,
                        album = album,
                        composer = composer,
                        genre = genre,
                        year = year,
                        artwork = artwork,
                        isLoading = false,
                        error = null // Limpiar errores previos al cargar éxito
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }
    fun onArtistChange(value: String) = _state.update { it.copy(artist = value) }
    fun onAlbumChange(value: String) = _state.update { it.copy(album = value) }
    fun onComposerChange(value: String) = _state.update { it.copy(composer = value) }
    fun onGenreChange(value: String) = _state.update { it.copy(genre = value) }
    fun onYearChange(value: String) = _state.update { it.copy(year = value) }
    
    fun onArtworkChange(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    newArtworkBytes = bytes
                    _state.update { it.copy(artwork = bitmap) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error al cargar imagen: ${e.message}") }
            }
        }
    }

    fun saveChanges() {
        val song = currentSong ?: return
        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // En Android 10+, necesitamos permiso de escritura explícito para MediaStore
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id.toString())
                    val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    _state.update { it.copy(pendingWriteRequest = IntentSenderRequest.Builder(pendingIntent).build()) }
                } else {
                    performSave()
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Error al iniciar guardado: ${e.message}") }
            }
        }
    }

    fun onPermissionGranted() {
        _state.update { it.copy(pendingWriteRequest = null) }
        viewModelScope.launch(Dispatchers.IO) {
            performSave()
        }
    }

    fun onPermissionDenied() {
        _state.update { it.copy(isSaving = false, pendingWriteRequest = null, error = "Permiso denegado por el usuario") }
    }

    private suspend fun performSave() = withContext(Dispatchers.IO) {
        val song = currentSong ?: return@withContext
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            
            // 1. Preparar archivo temporal
            val originalFile = File(song.path)
            val ext = originalFile.extension.lowercase()
            val tempFile = File(context.cacheDir, "edit_${song.id}.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw Exception("Error al leer archivo original")

            // 2. Intentar editar el archivo físico mediante Jaudiotagger (Para formatos soportados)
            if (ext != "opus") {
                try {
                    val audioFile = AudioFileIO.read(tempFile)
                    val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }
                    val s = _state.value
                    
                    tag.setField(FieldKey.TITLE, s.title)
                    tag.setField(FieldKey.ARTIST, s.artist)
                    tag.setField(FieldKey.ALBUM, s.album)
                    tag.setField(FieldKey.COMPOSER, s.composer)
                    tag.setField(FieldKey.GENRE, s.genre)
                    tag.setField(FieldKey.YEAR, s.year)
                    
                    newArtworkBytes?.let { bytes ->
                        val artwork = StandardArtwork()
                        artwork.binaryData = bytes
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    }
                    
                    audioFile.commit()

                    // Escribir de vuelta al almacenamiento original
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SongEdit", "Edición física falló: ${e.message}")
                }
            }
            tempFile.delete()

            // 3. Sincronización Maestra (Universal)
            val s = _state.value
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, s.title)
                put(MediaStore.Audio.Media.ARTIST, s.artist)
                put(MediaStore.Audio.Media.ALBUM, s.album)
                put(MediaStore.Audio.Media.YEAR, s.year.toIntOrNull() ?: 0)
            }
            context.contentResolver.update(uri, values, null, null)
            
            // Forzar actualización inmediata en el sistema
            MediaScannerConnection.scanFile(context, arrayOf(song.path), null) { _, _ -> }

            // 4. Notificar éxito
            _state.update { it.copy(isSaving = false, saveCompleted = true) }
        } catch (e: Exception) {
            _state.update { it.copy(isSaving = false, error = "Error al guardar: ${e.message}") }
        }
    }

    fun resetState() {
        _state.value = SongEditState()
        newArtworkBytes = null
        currentSong = null
    }
}
