package com.soundly.ui.componentes

import android.content.ContentUris
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Song
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class KeyedSong(val song: Song, val key: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerQueueSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    queue: List<Song>,
    currentSongId: Long?,
    currentSongIndex: Int,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    isPlaying: Boolean,
    title: String,
    artist: String,
    artworkUri: Uri?,
    durationMs: Long,
    positionMs: Long,
    containerColor: Color,
    onColor: Color
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = containerColor,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = 0.32f),
            modifier = Modifier.fillMaxSize()
        ) {
            val lazyListState = rememberLazyListState()
            
            LaunchedEffect(isOpen) {
                if (isOpen && currentSongId != null) {
                    val index = queue.indexOfFirst { it.id == currentSongId }
                    if (index >= 0) lazyListState.scrollToItem(index)
                }
            }

            // Usamos un estado local para la lista durante el arrastre
            var list by remember(queue) { 
                mutableStateOf(queue.mapIndexed { index, song -> KeyedSong(song, "${song.id}_$index") }) 
            }

            // Track the start and end of a drag operation to sync only once at the end
            var dragStartEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }
            
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val start = dragStartEnd?.first ?: from.index
                dragStartEnd = start to to.index
                
                list = list.toMutableList().apply { add(to.index, removeAt(from.index)) }
            }

            // Sincronizar con el ViewModel solo cuando el usuario suelta el item
            LaunchedEffect(reorderableState.isAnyItemDragging) {
                if (!reorderableState.isAnyItemDragging) {
                    dragStartEnd?.let { (from, to) ->
                        if (from != to) {
                            onMoveItem(from, to)
                        }
                    }
                    dragStartEnd = null
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                QueueHeader(onDismiss, onColor)

                QueueNowPlayingSection(
                    title = title,
                    artist = artist,
                    isPlaying = isPlaying,
                    artworkUri = artworkUri,
                    durationMs = durationMs,
                    positionMs = positionMs,
                    onPlayPause = onPlayPause,
                    onSkipNext = onSkipNext,
                    onColor = onColor,
                    containerColor = containerColor
                )

                QueueSecondarySection(
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = isFavorite,
                    onToggleShuffle = onToggleShuffle,
                    onToggleFavorite = onToggleFavorite,
                    onCycleRepeat = onCycleRepeat,
                    onColor = onColor
                )
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(list, key = { _, item -> item.key }) { index, item ->
                        // Usamos el ID de la canción para el resaltado actual, para que sea más estable durante el reordenamiento
                        val isCurrent = item.song.id == currentSongId
                        
                        ReorderableItem(reorderableState, key = item.key) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 16.dp else 0.dp,
                                label = "dragElevation"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .shadow(elevation, shape = RoundedCornerShape(16.dp))
                                    .background(if (isDragging) containerColor.copy(alpha = 0.9f) else containerColor)
                            ) {
                                QueueItem(
                                    song = item.song,
                                    isCurrent = isCurrent,
                                    onColor = onColor,
                                    onClick = { onPlaySong(item.song) },
                                    modifier = Modifier.longPressDraggableHandle()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(onDismiss: () -> Unit, onColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.button_close), tint = onColor)
        }
        Text(
            text = stringResource(R.string.menu_playback_queue),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onColor
        )
        Box(Modifier.size(48.dp))
    }
}

@Composable
private fun QueueNowPlayingSection(
    title: String,
    artist: String,
    isPlaying: Boolean,
    artworkUri: Uri?,
    durationMs: Long,
    positionMs: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onColor: Color,
    containerColor: Color
) {
    val progress = remember(positionMs, durationMs) {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Box(modifier = Modifier.height(72.dp).fillMaxWidth()) {
        MiniPlayerBody(
            metadata = MiniPlayerMetadata(
                songName = title,
                artistName = artist,
                isPlaying = isPlaying,
                artwork = artworkUri ?: R.drawable.carga
            ),
            onPlayPauseClick = onPlayPause,
            onNextClick = onSkipNext,
            textColor = onColor,
            subTextColor = onColor.copy(alpha = 0.7f),
            buttonBg = onColor.copy(alpha = 0.12f),
            buttonIconColor = onColor,
            progress = { progress },
        )
    }
}

@Composable
private fun QueueSecondarySection(
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onColor: Color
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        PlayerSecondaryControls(
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            isFavorite = isFavorite,
            onToggleShuffle = onToggleShuffle,
            onToggleFavorite = onToggleFavorite,
            onCycleRepeat = onCycleRepeat,
            onColor = onColor,
            compact = false,
            horizontalArrangement = Arrangement.Start
        )
    }
}

@Composable
fun QueueItem(
    song: Song,
    isCurrent: Boolean,
    onColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artworkUri = remember(song.albumId) {
        Uri.parse("content://media/external/audio/albumart/${song.albumId}")
    }

    Surface(
        onClick = onClick,
        color = if (isCurrent) onColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(onColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUri)
                        .crossfade(true)
                        .allowHardware(true)
                        .build(),
                    contentDescription = null,
                    error = painterResource(R.drawable.carga),
                    placeholder = painterResource(R.drawable.carga),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium, color = onColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = onColor.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Icon(Icons.Rounded.DragHandle, stringResource(R.string.cd_reorder), tint = onColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}
