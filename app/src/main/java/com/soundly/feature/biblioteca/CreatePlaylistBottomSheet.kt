package com.soundly.feature.biblioteca

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreatePlaylist: suspend (String, Uri) -> Result<String>,
    initialPlaylistId: String? = null,
    initialName: String? = null,
    initialArtworkUri: Uri? = null,
    onUpdatePlaylist: suspend (String, String, Uri?) -> Result<Unit> = { _, _, _ -> Result.success(Unit) }
) {
    if (!visible) return

    val isEditMode = initialPlaylistId != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var playlistName by remember(initialName) { mutableStateOf(initialName ?: "") }
    var selectedArtworkUri by remember(initialArtworkUri) { mutableStateOf(initialArtworkUri) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (!visible) {
            if (!isEditMode) {
                playlistName = ""
                selectedArtworkUri = null
            }
            isSaving = false
            errorMessage = null
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedArtworkUri = uri
                errorMessage = null
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Logra que use la pantalla completa
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditMode) "Editar playlist" else "Crear nueva playlist",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isEditMode) "Cambia el nombre o la portada de tu colección para mantenerla actualizada." else "Dale personalidad a tu playlist con una foto increíble y un nombre único para guardarla en tu biblioteca.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !isSaving) {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedArtworkUri != null) {
                    AsyncImage(
                        model = selectedArtworkUri,
                        contentDescription = "Portada de la playlist",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Añadir foto",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            OutlinedTextField(
                value = playlistName,
                onValueChange = {
                    playlistName = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Mi Playlist Favorita", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                singleLine = true,
                enabled = !isSaving,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val artworkUri = selectedArtworkUri
                        if (playlistName.isBlank()) {
                            errorMessage = "Escribe un nombre para la playlist."
                            return@FilledTonalButton
                        }
                        if (artworkUri == null) {
                            errorMessage = "Selecciona una portada para continuar."
                            return@FilledTonalButton
                        }

                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            if (isEditMode) {
                                val changedArtwork = if (artworkUri == initialArtworkUri) null else artworkUri
                                onUpdatePlaylist(initialPlaylistId!!, playlistName.trim(), changedArtwork)
                                    .onSuccess { onDismiss() }
                                    .onFailure { error ->
                                        errorMessage = error.message ?: "No se pudo actualizar la playlist."
                                    }
                            } else {
                                onCreatePlaylist(playlistName.trim(), artworkUri)
                                    .onSuccess { onDismiss() }
                                    .onFailure { error ->
                                        errorMessage = error.message ?: "No se pudo crear la playlist."
                                    }
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (isEditMode) "Guardar" else "Crear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
