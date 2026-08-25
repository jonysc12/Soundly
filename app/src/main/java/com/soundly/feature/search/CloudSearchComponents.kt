package com.soundly.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.cloud.*
import com.soundly.ui.componentes.listas.*
import com.soundly.data.model.Album as LocalAlbum
import com.soundly.data.model.Artist as LocalArtist
import com.soundly.data.model.Playlist as LocalPlaylist
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDetailSheet(
    state: DetailUiState?,
    artistState: ArtistDetailUiState?,
    downloadProgressMapProvider: () -> Map<String, Int>,
    onDismiss: () -> Unit,
    onSongClick: (Song) -> Unit,
    onDownloadAll: (List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    if (state != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                DetailContent(state, downloadProgressMapProvider, onSongClick, onDownloadAll)
            }
        }
    }

    if (artistState != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ArtistDetailContent(artistState, downloadProgressMapProvider, onSongClick, onAlbumClick, onPlaylistClick)
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    downloadProgressMapProvider: () -> Map<String, Int>,
    onSongClick: (Song) -> Unit,
    onDownloadAll: (List<Song>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.uploader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDownloadAll(state.items) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cloud_download_all))
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(state.items) { song ->
                    val progressState = remember { derivedStateOf { downloadProgressMapProvider()[song.id] } }
                    ItemCancion(
                        cancion = song.toCancionForUI(),
                        onClick = { onSongClick(song) },
                        menuContent = {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                                val progress = progressState.value
                                if (progress != null) {
                                    if (progress == -1) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        CircularProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 3.dp,
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailContent(
    state: ArtistDetailUiState,
    downloadProgressMapProvider: () -> Map<String, Int>,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    val combinedExtras = remember(state.singles, state.playlists) {
        state.singles + state.playlists
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 800.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                // El fondo es la carátula del artista con un gradiente premium
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.5f)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    // Avatar circular con borde
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier.size(94.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Título limpio
                    Text(
                        text = state.name.replace(" - Topic", "", ignoreCase = true).trim(),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                }
            }
        }

        if (state.songs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.cloud_popular_songs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            itemsIndexed(state.songs.take(10)) { index, song ->
                val progressState = remember { derivedStateOf { downloadProgressMapProvider()[song.id] } }
                ItemCancion(
                    cancion = song.toCancionForUI(),
                    index = index + 1,
                    onClick = { onSongClick(song) },
                    menuContent = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            val progress = progressState.value
                            if (progress != null) {
                                if (progress == -1) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    CircularProgressIndicator(
                                        progress = { progress / 100f },
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        if (state.albums.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.library_label_albums),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.albums) { album ->
                        Box(modifier = Modifier.width(160.dp)) {
                            ItemAlbum(
                                album = album.toLocalAlbum(),
                                caratulaUri = Uri.parse(album.thumbnailUrl),
                                onClick = { onAlbumClick(album) }
                            )
                        }
                    }
                }
            }
        }

        if (combinedExtras.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_section_cloud_extras),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(combinedExtras) { item ->
                        Box(modifier = Modifier.width(160.dp)) {
                            when (item) {
                                is Album -> {
                                    ItemAlbum(
                                        album = item.toLocalAlbum(),
                                        caratulaUri = Uri.parse(item.thumbnailUrl),
                                        onClick = { onAlbumClick(item) }
                                    )
                                }
                                is Playlist -> {
                                    ItemAlbum(
                                        album = com.soundly.data.model.Album(
                                            id = 0L,
                                            name = item.title,
                                            artist = item.uploader,
                                            songCount = item.songCount
                                        ),
                                        caratulaUri = Uri.parse(item.thumbnailUrl),
                                        onClick = { onPlaylistClick(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.videos.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.cloud_videos),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            itemsIndexed(state.videos) { index, video ->
                val progressState = remember { derivedStateOf { downloadProgressMapProvider()[video.id] } }
                ItemCancion(
                    cancion = video.toCancionForUI(),
                    index = index + 1,
                    onClick = { onSongClick(video) },
                    menuContent = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            val progress = progressState.value
                            if (progress != null) {
                                if (progress == -1) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    CircularProgressIndicator(
                                        progress = { progress / 100f },
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        if (combinedExtras.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Helpers for UI mapping
private fun Song.toCancionForUI(): Cancion {
    return Cancion(
        caratulaUri = Uri.parse(thumbnailUrl),
        titulo = title,
        artista = artist
    )
}

private fun Album.toLocalAlbum(): LocalAlbum {
    return LocalAlbum(id = 0L, name = title, artist = artist, songCount = songCount)
}

private fun Playlist.toLocalPlaylist(): LocalPlaylist {
    return LocalPlaylist(
        id = id,
        name = title,
        songCount = songCount,
        isAutoGenerated = false,
        artworkUri = Uri.parse(thumbnailUrl)
    )
}
