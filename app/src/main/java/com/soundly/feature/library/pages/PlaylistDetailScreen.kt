package com.soundly.feature.library.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.PlaylistExportResult
import com.soundly.feature.library.components.*
import com.soundly.feature.library.utils.*
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.SoundlyToast
import com.soundly.ui.componentes.SoundlyToastState
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.ItemCancionAlbum
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    songs: List<Song>,
    ownerName: String,
    viewModel: LibraryViewModel,
    ownerImageUri: Uri?,
    albumArtProvider: (Long) -> Uri,
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onViewQueue: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayCollection: (List<Song>, Boolean) -> Unit = { _, _ -> }
) {
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val density = LocalDensity.current
    val accentUri = playlist.artworkUri ?: songs.firstOrNull()?.let { albumArtProvider(it.albumId) }
    val playlistMeta = stringResource(if (songs.size == 1) R.string.song_count_singular else R.string.songs_count, songs.size)

    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistMembershipBySong by viewModel.playlistMembershipBySong.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val plColors = rememberAlbumColors(uri = accentUri, isDark = isDark)
    val rawColor = plColors[0]
    val dominantColor by animateColorAsState(targetValue = rawColor, animationSpec = SPRING_COLOR_SLOW, label = "plColor")
    val hasColor = dominantColor != Color.Transparent

    val plNameColor = remember(dominantColor, isDark) { getDetailNameColor(dominantColor, isDark) }
    val accentColor = rememberAdaptedAccentColor(dominantColor, isDark)
    val actionColor = if (hasColor) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val topBarHeightPx = statusBarHeightPx + with(density) { 56.dp.toPx() }

    val scrollProgressLambda = remember(listState, headerHeightPx, topBarHeightPx) {
        {
            if (headerHeightPx == 0) 0f
            else {
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "pl_header" }
                if (item == null) 1f
                else {
                    val scrolled = -item.offset.toFloat()
                    (scrolled / (headerHeightPx - topBarHeightPx)).coerceIn(0f, 1f)
                }
            }
        }
    }

    val backIconOnImage = remember(dominantColor) {
        if (dominantColor == Color.Transparent || dominantColor.luminance() < 0.4f) Color.White else Color.Black
    }

    var overscrollPx by remember { mutableFloatStateOf(0f) }
    val maxOverscrollPx = with(density) { 140.dp.toPx() }

    val elasticScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollPx <= 0f || available.y >= 0f) return Offset.Zero
                val consumed = available.y.coerceAtLeast(-overscrollPx)
                overscrollPx = (overscrollPx + consumed).coerceAtLeast(0f)
                return Offset(0f, consumed)
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                overscrollPx = (overscrollPx + available.y * 0.4f).coerceIn(0f, maxOverscrollPx)
                return Offset(0f, available.y)
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                animate(overscrollPx, 0f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) { v, _ -> overscrollPx = v }
                return Velocity.Zero
            }
        }
    }

    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("m3u") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportPlaylist(uri, songs, selectedFormat)
            }
        }
    )

    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val contentWidth = with(density) { windowInfo.containerSize.width.toDp() }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(elasticScroll).graphicsLayer { translationY = overscrollPx }) {
        DetailBackgroundHeader(
            artUri = accentUri,
            colors = plColors,
            hasColor = hasColor,
            isDark = isDark,
            backgroundColor = MaterialTheme.colorScheme.background,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            isLandscape = false
        )

        // --- TOAST DE EXPORTACIÓN ---
        SoundlyToast(
            isVisible = exportState != PlaylistExportResult.Idle,
            message = when (exportState) {
                is PlaylistExportResult.Loading -> stringResource(R.string.toast_importing) 
                is PlaylistExportResult.Success -> stringResource(R.string.toast_export_success)
                is PlaylistExportResult.Error -> (exportState as PlaylistExportResult.Error).message
                else -> ""
            },
            state = when (exportState) {
                is PlaylistExportResult.Loading -> SoundlyToastState.LOADING
                is PlaylistExportResult.Success -> SoundlyToastState.SUCCESS
                is PlaylistExportResult.Error -> SoundlyToastState.ERROR
                else -> SoundlyToastState.INFO
            },
            onDismiss = { viewModel.clearExportState() }
        )

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = navStackHeight + 16.dp)) {
            item(key = "pl_header") {
                Box(
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned { headerHeightPx = it.size.height }
                ) {
                    Spacer(modifier = Modifier.aspectRatio(0.95f).fillMaxWidth())

                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp).graphicsLayer {
                            alpha = (1f - scrollProgressLambda() * 2.5f).coerceIn(0f, 1f)
                        }
                    ) {
                        // Dejamos un espacio proporcional al título que dibuja la TopBar
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = playlistMeta,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = plNameColor.copy(alpha = 0.82f)
                        )
                    }
                }
            }
            item(key = "pl_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { /* Favorito de playlist no implementado o usar otro ícono */ },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(actionColor.copy(alpha = 0.16f))
                    ) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, tint = actionColor, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(24.dp))

                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                    Button(
                        onClick = { if (songs.isNotEmpty()) onPlayCollection(songs, false) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(48.dp).padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(24.dp))

                    IconButton(
                        onClick = { if (songs.isNotEmpty()) onPlayCollection(songs, true) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(actionColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            itemsIndexed(items = songs, key = { index, s -> "playlist_song_${s.id}_$index" }, contentType = { _, _ -> "playlist_song" }) { index, s ->
                ItemCancionAlbum(cancion = Cancion(caratulaUri = albumArtProvider(s.albumId), titulo = s.title, artista = s.artist), trackNumber = index + 1, onClick = { onPlaySong(s, songs) }, menuContent = {
                    SongOverflowMenuButton(song = s, source = SongMenuSource.Playlist(playlist.id, playlist.isAutoGenerated), userPlaylists = userPlaylists, playlistIdsContainingSong = playlistMembershipBySong[s.id].orEmpty(), isFavorite = s.id in favoriteSongIds, onPlayNext = { playbackViewModel.playNext(s) }, onAddToQueue = { playbackViewModel.addToQueue(s) }, onOpenAlbum = onAlbumClick, onOpenArtist = onArtistClick, onAddToPlaylist = { viewModel.addSongToPlaylist(it, s.id) }, onToggleFavorite = { viewModel.toggleSongFavorite(s.id) }, onDeleteSong = { viewModel.removeSongFromPlaylist(playlist.id, s.id) }, onViewQueue = onViewQueue)
                })
            }
        }
        
        AnimatedDetailTopBar(
            title = if (playlist.id == MusicRepository.LIKED_SONGS_PLAYLIST_ID) stringResource(R.string.liked_songs_title) else playlist.name,
            subtitle = ownerName.ifBlank { if (playlist.isAutoGenerated) stringResource(R.string.app_name) else stringResource(R.string.label_your_profile) },
            backIconOnImage = backIconOnImage,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            statusBarHeightPx = statusBarHeightPx,
            titleColor = plNameColor,
            onBack = onBack,
            onEdit = if (!playlist.isAutoGenerated) { { showEditSheet = true } } else null,
            onMoreOptions = { showPlaylistOptions = true },
            leadingSubtitle = {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ownerImageUri ?: if (playlist.isAutoGenerated) R.drawable.logo_soundly else R.drawable.playlist_favicon)
                        .crossfade(true)
                        .allowHardware(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            },
            contentWidth = contentWidth,
            isLandscape = false
        )
    }

    if (showPlaylistOptions) {
        com.soundly.ui.componentes.PlaylistOptionsSheet(
            playlist = playlist,
            songs = songs,
            artUri = accentUri,
            userPlaylists = userPlaylists,
            onDismissRequest = { showPlaylistOptions = false },
            onPlayNext = { playbackViewModel.playNext(songs) },
            onAddToQueue = { playbackViewModel.addToQueue(songs) },
            onTransferToPlaylist = { targetId ->
                viewModel.addSongsToPlaylist(targetId, songs.map { it.id })
            },
            onExportClick = { format ->
                selectedFormat = format
                exportLauncher.launch("${playlist.name}.${format}")
            },
            onSaveAsPlaylist = {
                scope.launch {
                    viewModel.createPlaylistWithSongs(
                        name = playlist.name,
                        artworkSourceUri = accentUri,
                        songIds = songs.map { it.id }
                    )
                }
            }
        )
    }

    com.soundly.feature.biblioteca.CreatePlaylistBottomSheet(
        visible = showEditSheet,
        onDismiss = { showEditSheet = false },
        onCreatePlaylist = { _, _ -> Result.success("") },
        initialPlaylistId = playlist.id,
        initialName = playlist.name,
        initialArtworkUri = playlist.artworkUri,
        onUpdatePlaylist = { id, name, uri ->
            viewModel.updatePlaylist(id, name, uri)
        }
    )
}
