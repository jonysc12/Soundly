package com.soundly.ui.componentes.edit

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.soundly.R
import com.soundly.data.model.Song
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditSheet(
    song: Song,
    onDismissRequest: () -> Unit,
    viewModel: SongEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onArtworkChange(uri)
    }

    val writeRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(song.id) {
        viewModel.loadSong(song)
    }

    LaunchedEffect(state.pendingWriteRequest) {
        state.pendingWriteRequest?.let {
            writeRequestLauncher.launch(it)
        }
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onDismissRequest()
            viewModel.resetState()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null // Usaremos un header personalizado
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .imePadding()
        ) {
            // Header personalizado con acciones fijas
            SheetHeader(
                title = stringResource(R.string.edit_screen_title),
                onCancel = onDismissRequest,
                onSave = { viewModel.saveChanges() },
                isSaving = state.isSaving,
                canSave = state.title.isNotBlank() && !state.isLoading
            )

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Artwork Section
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.artwork != null) {
                            AsyncImage(
                                model = state.artwork,
                                contentDescription = stringResource(R.string.cd_artwork),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Image,
                                null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        
                        // Overlay de edición más elegante
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Surface(
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                Icon(
                                    Icons.Rounded.PhotoCamera,
                                    contentDescription = stringResource(R.string.cd_change_artwork),
                                    modifier = Modifier.padding(8.dp).size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Input Fields agrupados
                    Text(
                        text = stringResource(R.string.edit_section_details),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    EditTextField(label = stringResource(R.string.info_label_title), value = state.title, onValueChange = viewModel::onTitleChange, icon = Icons.Rounded.Title)
                    EditTextField(label = stringResource(R.string.info_label_artist), value = state.artist, onValueChange = viewModel::onArtistChange, icon = Icons.Rounded.Person)
                    EditTextField(label = stringResource(R.string.info_label_album), value = state.album, onValueChange = viewModel::onAlbumChange, icon = Icons.Rounded.Album)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.edit_section_additional),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    EditTextField(label = stringResource(R.string.info_label_genre), value = state.genre, onValueChange = viewModel::onGenreChange, icon = Icons.Rounded.Category)
                    EditTextField(label = stringResource(R.string.info_label_year), value = state.year, onValueChange = viewModel::onYearChange, icon = Icons.Rounded.CalendarToday)
                    EditTextField(label = stringResource(R.string.info_label_composer), value = state.composer, onValueChange = viewModel::onComposerChange, icon = Icons.Rounded.HistoryEdu)

                    state.error?.let {
                        Surface(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(text = it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }

                // Indicador de carga sutil
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    canSave: Boolean
) {
    Column {
        // Drag handle visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.button_cancel))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isSaving) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                TextButton(
                    onClick = onSave,
                    enabled = canSave,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.button_save), fontWeight = FontWeight.Bold)
                }
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    )
}
