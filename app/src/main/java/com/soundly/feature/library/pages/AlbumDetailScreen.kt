package com.soundly.feature.library.pages

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.soundly.ui.theme.SoundlyTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Song
import com.soundly.data.model.Playlist
import com.soundly.data.model.splitArtistNames
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.components.*
import com.soundly.feature.library.utils.*
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.agslFrostedGlass
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.ItemAlbum
import com.soundly.ui.componentes.listas.ItemCancionAlbum

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: LibraryViewModel,
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onViewQueue: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayCollection: (List<Song>, Boolean) -> Unit = { _, _ -> }
) {
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val songs by viewModel.getSongsForAlbum(albumId).collectAsStateWithLifecycle(initialValue = emptyList())
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val album = remember(albums, albumId) { albums.find { it.id == albumId } }
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val favoriteAlbumIds by viewModel.favoriteAlbumIds.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistMembershipBySong by viewModel.playlistMembershipBySong.collectAsStateWithLifecycle()

    val albumArtistNames = remember(album?.artist) {
        album?.artist?.let { splitArtistNames(it) } ?: emptyList()
    }
    val albumArtists = remember(artists, albumArtistNames) {
        albumArtistNames.mapNotNull { name ->
            artists.find { it.name.equals(name, ignoreCase = true) }
        }
    }

    val isFavorite = albumId in favoriteAlbumIds
    val artUri = remember(album?.id) { album?.let { viewModel.getAlbumArtUri(it.id) } }

    var showAlbumOptions by remember { mutableStateOf(false) }

    AlbumDetailContent(
        album = album,
        albumArtists = albumArtists,
        songs = songs,
        otherAlbums = remember(albums, albumId, album?.artist) { albums.filter { it.artist == album?.artist && it.id != albumId } },
        artUri = artUri,
        isFavorite = isFavorite,
        favoriteSongIds = favoriteSongIds,
        userPlaylists = userPlaylists,
        playlistMembershipBySong = playlistMembershipBySong,
        getAlbumArtUri = { viewModel.getAlbumArtUri(it) },
        getArtistArtUri = { viewModel.getArtistArtUri(it) },
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        onBack = onBack,
        onToggleFavorite = { viewModel.toggleAlbumFavorite(albumId) },
        onShowAlbumOptions = { showAlbumOptions = true },
        onPlaySong = onPlaySong,
        onPlayCollection = onPlayCollection,
        onPlayNext = { playbackViewModel.playNext(it) },
        onAddToQueue = { playbackViewModel.addToQueue(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onToggleSongFavorite = { viewModel.toggleSongFavorite(it) },
        onHideSong = { viewModel.hideSong(it) },
        onViewQueue = onViewQueue
    )

    if (showAlbumOptions && album != null) {
        com.soundly.ui.componentes.AlbumOptionsSheet(
            album = album,
            songs = songs,
            artUri = artUri,
            isFavorite = isFavorite,
            userPlaylists = userPlaylists,
            onDismissRequest = { showAlbumOptions = false },
            onToggleFavorite = { viewModel.toggleAlbumFavorite(albumId) },
            onPlayNext = { playbackViewModel.playNext(songs) },
            onAddToQueue = { playbackViewModel.addToQueue(songs) },
            onAddSongsToPlaylist = { playlistId -> viewModel.addSongsToPlaylist(playlistId, songs.map { it.id }) },
            onOpenArtist = onArtistClick
        )
    }
}

@Composable
private fun AlbumDetailContent(
    album: Album?,
    albumArtists: List<Artist>,
    songs: List<Song>,
    otherAlbums: List<Album>,
    artUri: Uri?,
    isFavorite: Boolean,
    favoriteSongIds: Set<Long>,
    userPlaylists: List<Playlist>,
    playlistMembershipBySong: Map<Long, Set<String>>,
    getAlbumArtUri: (Long) -> Uri,
    getArtistArtUri: (Long) -> Uri?,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowAlbumOptions: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayCollection: (List<Song>, Boolean) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit,
    onToggleSongFavorite: (Long) -> Unit,
    onHideSong: (Long) -> Unit,
    onViewQueue: () -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val albumColors = rememberAlbumColors(uri = artUri, isDark = isDark)
    val rawColor = albumColors[0]
    val dominantColor by animateColorAsState(targetValue = rawColor, animationSpec = SPRING_COLOR_SLOW, label = "dominantColor")
    val hasColor = dominantColor != Color.Transparent

    val albumNameColor = remember(dominantColor, isDark) { getDetailNameColor(dominantColor, isDark) }
    val accentColor = rememberAdaptedAccentColor(dominantColor, isDark)
    val actionColor = if (hasColor) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val topBarHeightPx = statusBarHeightPx + with(density) { 56.dp.toPx() }

    val scrollProgressLambda = remember(listState, headerHeightPx, topBarHeightPx) {
        {
            if (headerHeightPx == 0) 0f
            else {
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "album_header" }
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

    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current

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

    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val contentWidth = with(density) { windowInfo.containerSize.width.toDp() }

    Box(
        modifier = Modifier.fillMaxSize().nestedScroll(elasticScroll).graphicsLayer { translationY = overscrollPx }
    ) {
        DetailBackgroundHeader(
            artUri = artUri,
            colors = albumColors,
            hasColor = hasColor,
            isDark = isDark,
            backgroundColor = MaterialTheme.colorScheme.background,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            isLandscape = false
        )

        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = navStackHeight + 16.dp)
        ) {
            item(key = "album_header") {
                Box(
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned { headerHeightPx = it.size.height }
                ) {
                    Spacer(modifier = Modifier.aspectRatio(0.95f).fillMaxWidth())

                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp).graphicsLayer {
                            alpha = (1f - scrollProgressLambda() * 2.5f).coerceIn(0f, 1f)
                        }
                    ) {
                        album?.let {
                            // Dejamos un espacio proporcional al título que dibuja la TopBar
                            Spacer(Modifier.height(32.dp)) 
                            val countRes = if (it.songCount == 1) R.string.song_count_singular else R.string.songs_count
                            Text(
                                text = stringResource(countRes, it.songCount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = albumNameColor.copy(alpha = 0.82f)
                            )
                        }
                    }
                }
            }

            item(key = "album_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(actionColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else actionColor,
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

            itemsIndexed(items = songs, key = { index, song -> "album_song_${song.id}_$index" }, contentType = { _, _ -> "album_song" }) { index, song ->
                ItemCancionAlbum(cancion = Cancion(caratulaUri = getAlbumArtUri(song.albumId), titulo = song.title, artista = song.artist), trackNumber = index + 1, onClick = { onPlaySong(song, songs) }, menuContent = {
                    SongOverflowMenuButton(song = song, source = SongMenuSource.Library, userPlaylists = userPlaylists, playlistIdsContainingSong = playlistMembershipBySong[song.id].orEmpty(), isFavorite = song.id in favoriteSongIds, onPlayNext = { onPlayNext(song) }, onAddToQueue = { onAddToQueue(song) }, onOpenAlbum = onAlbumClick, onOpenArtist = onArtistClick, onAddToPlaylist = { onAddSongToPlaylist(it, song.id) }, onToggleFavorite = { onToggleSongFavorite(song.id) }, onDeleteSong = { onHideSong(song.id) }, onViewQueue = onViewQueue)
                })
            }

            if (otherAlbums.isNotEmpty()) {
                item(key = "more_artist_header") {
                    Spacer(Modifier.height(32.dp))
                    Text(text = stringResource(R.string.library_option_albums), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 12.dp))
                }
                item(key = "more_artist_row") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items = otherAlbums, key = { it.id }, contentType = { "related_album" }) { otherAlbum ->
                            Box(Modifier.width(160.dp)) { ItemAlbum(album = otherAlbum, caratulaUri = getAlbumArtUri(otherAlbum.id), onClick = { onAlbumClick(otherAlbum.id) }) }
                        }
                    }
                }
            }
        }

        AnimatedDetailTopBar(
            title = album?.name ?: "",
            subtitle = album?.artist,
            backIconOnImage = backIconOnImage,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            statusBarHeightPx = statusBarHeightPx,
            titleColor = albumNameColor,
            onBack = onBack,
            onMoreOptions = { onShowAlbumOptions() },
            leadingSubtitle = {
                CollaborativeArtistsStack(
                    artists = albumArtists,
                    getArtistArtUri = getArtistArtUri
                )
            },
            contentWidth = contentWidth,
            isLandscape = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumDetailScreenPreview() {
    val dummyAlbum = Album(id = 1, name = "Album de Prueba", artist = "Artista de Prueba", songCount = 5)
    val dummySongs = listOf(
        Song(id = 1, title = "Canción 1", artist = "Artista de Prueba", artistId = 1, album = "Album de Prueba", albumId = 1, dateAdded = 0, duration = 180000, path = ""),
        Song(id = 2, title = "Canción 2", artist = "Artista de Prueba", artistId = 1, album = "Album de Prueba", albumId = 1, dateAdded = 0, duration = 200000, path = "")
    )
    SoundlyTheme(darkTheme = true) {
        AlbumDetailContent(
            album = dummyAlbum,
            albumArtists = emptyList(),
            songs = dummySongs,
            otherAlbums = emptyList(),
            artUri = null,
            isFavorite = true,
            favoriteSongIds = setOf(1),
            userPlaylists = emptyList(),
            playlistMembershipBySong = emptyMap(),
            getAlbumArtUri = { Uri.EMPTY },
            getArtistArtUri = { null },
            onAlbumClick = {},
            onArtistClick = {},
            onBack = {},
            onToggleFavorite = {},
            onShowAlbumOptions = {},
            onPlaySong = { _, _ -> },
            onPlayCollection = { _, _ -> },
            onPlayNext = {},
            onAddToQueue = {},
            onAddSongToPlaylist = { _, _ -> },
            onToggleSongFavorite = {},
            onHideSong = {},
            onViewQueue = {}
        )
    }
}
