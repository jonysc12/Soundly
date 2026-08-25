package com.soundly.feature.library.pages

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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundly.R
import com.soundly.data.model.Song
import com.soundly.feature.biblioteca.BibliotecaViewModel
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenuButton
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.ItemCancionAlbum
import com.soundly.feature.library.components.*
import com.soundly.feature.library.utils.*
import com.soundly.ui.componentes.agslFrostedGlass

@Composable
fun FolderDetailScreen(
    folderPath: String,
    viewModel: BibliotecaViewModel,
    libraryViewModel: LibraryViewModel,
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onViewQueue: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayCollection: (List<Song>, Boolean) -> Unit = { _, _ -> }
) {
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val density = LocalDensity.current
    val songs by viewModel.getSongsForFolder(folderPath).collectAsStateWithLifecycle(initialValue = emptyList())
    val folderName = folderPath.substringAfterLast("/")
    val playlistMeta = stringResource(if (songs.size == 1) R.string.song_count_singular else R.string.songs_count, songs.size)
    val favoriteFolderPaths by viewModel.favoriteFolderPaths.collectAsStateWithLifecycle()
    val isFavoriteFolder = folderPath in favoriteFolderPaths
    val favoriteSongIds by libraryViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val userPlaylists by libraryViewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistMembershipBySong by libraryViewModel.playlistMembershipBySong.collectAsStateWithLifecycle()
    val accentUri = remember(songs) { songs.firstOrNull()?.let { libraryViewModel.getAlbumArtUri(it.albumId) } }
    
    val listState = rememberLazyListState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val folderColors = rememberAlbumColors(uri = accentUri, isDark = isDark)
    val rawColor = folderColors[0]
    val dominantColor by animateColorAsState(targetValue = rawColor, animationSpec = SPRING_COLOR_SLOW, label = "domColor")
    val hasColor = dominantColor != Color.Transparent

    val folderNameColor = remember(dominantColor, isDark) { getDetailNameColor(dominantColor, isDark) }
    val accentColor = rememberAdaptedAccentColor(dominantColor, isDark)
    val actionColor = if (hasColor) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val topBarHeightPx = statusBarHeightPx + with(density) { 56.dp.toPx() }

    val scrollProgressLambda = remember(listState, headerHeightPx, topBarHeightPx) {
        {
            if (headerHeightPx == 0) 0f
            else {
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "f_header" }
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

    val backIconColor by animateColorAsState(
        androidx.compose.ui.graphics.lerp(backIconOnImage, MaterialTheme.colorScheme.onSurface, scrollProgressLambda()),
        spring(stiffness = Spring.StiffnessLow),
        label = "backIconColor"
    )

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

    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    var showFolderOptions by remember { mutableStateOf(false) }

    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val contentWidth = with(density) { windowInfo.containerSize.width.toDp() }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(elasticScroll).graphicsLayer { translationY = overscrollPx }) {
        DetailBackgroundHeader(
            artUri = accentUri,
            colors = folderColors,
            hasColor = hasColor,
            isDark = isDark,
            backgroundColor = MaterialTheme.colorScheme.background,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            isLandscape = false
        )

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = navStackHeight + 16.dp)) {
            item(key = "f_header") {
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
                            color = folderNameColor.copy(alpha = 0.82f)
                        )
                    }
                }
            }
            item(key = "f_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { viewModel.toggleFavoriteFolder(folderPath) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(actionColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = if (isFavoriteFolder) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = if (isFavoriteFolder) MaterialTheme.colorScheme.primary else actionColor,
                            modifier = Modifier.size(20.dp)
                        )
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
            itemsIndexed(items = songs, key = { index, s -> "f_song_${s.id}_$index" }, contentType = { _, _ -> "f_song" }) { index, s ->
                ItemCancionAlbum(cancion = Cancion(caratulaUri = libraryViewModel.getAlbumArtUri(s.albumId), titulo = s.title, artista = s.artist), trackNumber = index + 1, onClick = { onPlaySong(s, songs) }, menuContent = {
                    SongOverflowMenuButton(song = s, source = SongMenuSource.Library, userPlaylists = userPlaylists, playlistIdsContainingSong = playlistMembershipBySong[s.id].orEmpty(), isFavorite = s.id in favoriteSongIds, onPlayNext = { playbackViewModel.playNext(s) }, onAddToQueue = { playbackViewModel.addToQueue(s) }, onOpenAlbum = onAlbumClick, onOpenArtist = onArtistClick, onAddToPlaylist = { libraryViewModel.addSongToPlaylist(it, s.id) }, onToggleFavorite = { libraryViewModel.toggleSongFavorite(s.id) }, onDeleteSong = { libraryViewModel.hideSong(s.id) }, onViewQueue = onViewQueue)
                })
            }
        }
        
        AnimatedDetailTopBar(
            title = folderName,
            subtitle = folderPath,
            backIconOnImage = backIconOnImage,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            statusBarHeightPx = statusBarHeightPx,
            titleColor = folderNameColor,
            onBack = onBack,
            onMoreOptions = { showFolderOptions = true },
            leadingSubtitle = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = backIconColor
                )
            },
            contentWidth = contentWidth,
            isLandscape = false
        )
    }

    if (showFolderOptions) {
        com.soundly.ui.componentes.FolderOptionsSheet(
            folderName = folderName,
            folderPath = folderPath,
            onDismissRequest = { showFolderOptions = false },
            onPlayNext = { playbackViewModel.playNext(songs) },
            onAddToQueue = { playbackViewModel.addToQueue(songs) },
            onToggleFavorite = { viewModel.toggleFavoriteFolder(folderPath) },
            isFavorite = isFavoriteFolder
        )
    }
}
