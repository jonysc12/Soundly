package com.soundly.ui.componentes

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Album
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumOptionsSheet(
    album: Album,
    songs: List<Song>,
    artUri: Uri?,
    isFavorite: Boolean,
    userPlaylists: List<Playlist>,
    onDismissRequest: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddSongsToPlaylist: (String) -> Unit,
    onOpenArtist: (Long) -> Unit
) {
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showArtistChoiceDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playlistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val artistNames = remember(album.artist) {
        com.soundly.data.model.splitArtistNames(album.artist)
    }

    if (!showPlaylistSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                item {
                    AlbumSheetHeader(album = album, artUri = artUri, onClose = onDismissRequest)
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                item {
                    QuickActionsRow(
                        onPlayNext = { onDismissRequest(); onPlayNext() },
                        onAddToQueue = { onDismissRequest(); onAddToQueue() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                item {
                    MenuItemWithIcon(
                        text = if (isFavorite) stringResource(R.string.library_remove_album) else stringResource(R.string.library_add_album),
                        icon = if (isFavorite) Icons.Rounded.DeleteOutline else Icons.Rounded.AddCircleOutline,
                        onClick = { onToggleFavorite(); onDismissRequest() }
                    )
                }

                item {
                    MenuItemWithIcon(
                        text = stringResource(R.string.menu_add_to_playlist),
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        onClick = { showPlaylistSheet = true }
                    )
                }

                item {
                    MenuItemWithIcon(
                        text = if (artistNames.size > 1) stringResource(R.string.menu_go_to_artist_multiple) else stringResource(R.string.menu_go_to_artist),
                        icon = Icons.Rounded.Person,
                        onClick = {
                            if (artistNames.size > 1) {
                                showArtistChoiceDialog = true
                            } else {
                                onDismissRequest()
                                onOpenArtist(com.soundly.data.model.generateArtistId(artistNames.first()))
                            }
                        }
                    )
                }
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            sheetState = playlistSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showPlaylistSheet = false }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                    Text(
                        text = stringResource(R.string.menu_add_album_to_playlist_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (userPlaylists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.menu_no_playlists),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(userPlaylists, key = { it.id }) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = {
                                    onAddSongsToPlaylist(playlist.id)
                                    showPlaylistSheet = false
                                    onDismissRequest()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showArtistChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showArtistChoiceDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showArtistChoiceDialog = false }) { Text(stringResource(R.string.button_cancel)) }
            },
            title = { Text(stringResource(R.string.dialog_select_artist_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    artistNames.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showArtistChoiceDialog = false
                                    onDismissRequest()
                                    onOpenArtist(com.soundly.data.model.generateArtistId(name))
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun AlbumSheetHeader(album: Album, artUri: Uri?, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artUri)
                    .crossfade(true)
                    .allowHardware(true)
                    .build(),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = album.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.info_label_album),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp).clip(CircleShape)
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuickActionsRow(
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickActionChip(label = stringResource(R.string.quick_action_play_next), icon = Icons.Rounded.PlayArrow, modifier = Modifier.weight(1f), onClick = onPlayNext)
        QuickActionChip(label = stringResource(R.string.quick_action_in_queue), icon = Icons.AutoMirrored.Rounded.QueueMusic, modifier = Modifier.weight(1f), onClick = onAddToQueue)
    }
}

@Composable
private fun QuickActionChip(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MenuItemWithIcon(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            if (playlist.artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.artworkUri)
                        .crossfade(true)
                        .allowHardware(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(imageVector = Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val playlistName = if (playlist.id == com.soundly.data.repository.MusicRepository.LIKED_SONGS_PLAYLIST_ID) {
                stringResource(R.string.liked_songs_title)
            } else {
                playlist.name
            }
            Text(text = playlistName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = stringResource(R.string.playlist_item_add_here), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
