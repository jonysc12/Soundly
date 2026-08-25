package com.soundly.feature.biblioteca

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.service.PlaylistImportResult
import com.soundly.ui.componentes.SoundlyToast
import com.soundly.ui.componentes.SoundlyToastState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreatePlaylist: suspend (String, Uri?) -> Result<String>,
    initialPlaylistId: String? = null,
    initialName: String? = null,
    initialArtworkUri: Uri? = null,
    onUpdatePlaylist: suspend (String, String, Uri?) -> Result<Unit> = { _, _, _ -> Result.success(Unit) },
    onImportPlaylist: (Uri) -> Unit = {},
    importState: PlaylistImportResult = PlaylistImportResult.Idle,
    onClearImportState: () -> Unit = {},
    onCreatePlaylistWithSongs: suspend (String, Uri?, List<Long>) -> Result<String> = { _, _, _ -> Result.success("") }
) {
    if (!visible) return

    val isEditMode = initialPlaylistId != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var playlistName by remember(initialName) { mutableStateOf(initialName ?: "") }
    var selectedArtworkUri by remember(initialArtworkUri) { mutableStateOf(initialArtworkUri) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Lista de IDs de canciones si viene de una importación
    var importedSongIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(visible) {
        if (!visible) {
            if (!isEditMode) {
                playlistName = ""
                selectedArtworkUri = null
                importedSongIds = emptyList()
            }
            isSaving = false
            errorMessage = null
            onClearImportState()
        }
    }

    // Selector de archivos para importación
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                onImportPlaylist(uri)
            }
        }
    )

    // Manejo de estados de importación
    LaunchedEffect(importState) {
        when (importState) {
            is PlaylistImportResult.Success -> {
                playlistName = importState.data.name
                importedSongIds = importState.data.songIds
                // Si hubo canciones que no se encontraron, el mensaje del toast informará al respecto
            }
            is PlaylistImportResult.Error -> {
                errorMessage = importState.message
            }
            else -> {}
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Toasts de estado de importación
            SoundlyToast(
                isVisible = importState != PlaylistImportResult.Idle,
                message = when (importState) {
                    is PlaylistImportResult.Loading -> stringResource(R.string.toast_importing)
                    is PlaylistImportResult.Success -> {
                        if (importState.data.foundCount < importState.data.totalInFile) {
                            stringResource(R.string.toast_import_partial, importState.data.foundCount, importState.data.totalInFile)
                        } else {
                            stringResource(R.string.toast_import_success)
                        }
                    }
                    is PlaylistImportResult.Error -> importState.message
                    else -> ""
                },
                state = when (importState) {
                    is PlaylistImportResult.Loading -> SoundlyToastState.LOADING
                    is PlaylistImportResult.Success -> SoundlyToastState.SUCCESS
                    is PlaylistImportResult.Error -> SoundlyToastState.ERROR
                    else -> SoundlyToastState.INFO
                },
                onDismiss = onClearImportState
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEditMode) stringResource(R.string.playlist_edit_title) else stringResource(R.string.playlist_create_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isEditMode) stringResource(R.string.playlist_edit_desc) else stringResource(R.string.playlist_create_desc),
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
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedArtworkUri)
                                    .allowHardware(true)
                                    .build(),
                                contentDescription = stringResource(R.string.playlist_artwork_cd),
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
                                    text = stringResource(R.string.playlist_add_photo),
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
                        placeholder = {
                            Text(
                                stringResource(R.string.playlist_name_placeholder),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
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
                    
                    // Espacio extra al final del scroll para que el TextField no quede pegado al borde
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (isEditMode) {
                                onDismiss()
                            } else {
                                importLauncher.launch(
                                    arrayOf("audio/x-mpegurl", "application/xspf+xml", "text/plain")
                                )
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isEditMode) stringResource(R.string.button_cancel) else stringResource(R.string.button_import),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val artworkUri = selectedArtworkUri
                            if (playlistName.isBlank()) {
                                errorMessage = context.getString(R.string.playlist_error_name_empty)
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
                                            errorMessage = error.message ?: context.getString(R.string.playlist_error_update_failed)
                                        }
                                } else {
                                    val result = if (importedSongIds.isNotEmpty()) {
                                        onCreatePlaylistWithSongs(playlistName.trim(), artworkUri, importedSongIds)
                                    } else {
                                        onCreatePlaylist(playlistName.trim(), artworkUri)
                                    }
                                    
                                    result.onSuccess { onDismiss() }
                                        .onFailure { error ->
                                            errorMessage = error.message ?: context.getString(R.string.playlist_error_create_failed)
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
                                text = if (isEditMode) stringResource(R.string.button_save) else stringResource(R.string.playlist_btn_create),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
