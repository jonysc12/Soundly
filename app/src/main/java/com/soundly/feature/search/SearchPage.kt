package com.soundly.feature.search

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.soundly.ui.theme.SoundlyTheme
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.cloud.SoundlyCloudActivity
import com.soundly.cloud.Song as CloudSong
import com.soundly.cloud.Artist as CloudArtist
import com.soundly.cloud.Album as CloudAlbum
import com.soundly.cloud.Playlist as CloudPlaylist
import com.soundly.cloud.ResultType
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenu
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.edit.SongEditSheet
import com.soundly.ui.componentes.listas.*
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.navigation.LocalBackStackCoordinator

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    viewModel: SearchViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onBackToBiblioteca: () -> Unit = {},
    onViewQueue: () -> Unit = {},
    isHostPageVisible: Boolean = true
) {
    val searchResult by viewModel.searchResult.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val isSongsExpanded by viewModel.isSongsExpanded.collectAsState()
    val isCloudEnabled by viewModel.isCloudEnabled.collectAsState()

    val cloudResults by viewModel.cloudResults.collectAsState()
    val isCloudLoading by viewModel.isCloudLoading.collectAsState()
    val cloudDetailState by viewModel.cloudDetailState.collectAsState()
    val cloudArtistDetailState by viewModel.cloudArtistDetailState.collectAsState()
    val downloadProgressMapState = viewModel.downloadProgress.collectAsState()

    val context = LocalContext.current

    val onSongClickStable = remember(onSongClick) { { s: Song, q: List<Song> -> onSongClick(s, q) } }
    val onAlbumClickStable = remember(onAlbumClick) { { id: Long -> onAlbumClick(id) } }
    val onArtistClickStable = remember(onArtistClick) { { id: Long -> onArtistClick(id) } }
    val onPlaylistClickStable = remember(onPlaylistClick) { { id: String -> onPlaylistClick(id) } }
    val onViewQueueStable = remember(onViewQueue) { { onViewQueue() } }

    val userPlaylists by libraryViewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistMembership by libraryViewModel.playlistMembershipBySong.collectAsStateWithLifecycle()
    val favoriteSongIds by libraryViewModel.favoriteSongIds.collectAsStateWithLifecycle()

    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<Song?>(null) }

    val backStackCoordinator = LocalBackStackCoordinator.current
    val backHandlerEnabled by remember(query, isHostPageVisible, backStackCoordinator.isOverlayActive) {
        derivedStateOf { isHostPageVisible && query.isNotEmpty() && !backStackCoordinator.isOverlayActive }
    }

    // Manejo inteligente del botón Atrás del sistema (solo limpiar búsqueda)
    androidx.activity.compose.BackHandler(enabled = backHandlerEnabled) {
        viewModel.onQueryChange("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        SearchContent(
            searchResult = searchResult,
            searchMode = searchMode,
            isSongsExpanded = isSongsExpanded,
            isCloudEnabled = isCloudEnabled,
            cloudResults = cloudResults,
            isCloudLoading = isCloudLoading,
            downloadProgressMapProvider = { downloadProgressMapState.value },
            onModeChange = { viewModel.onModeChange(it) },
            onToggleSongs = { viewModel.toggleSongsExpansion() },
            onSongClick = onSongClickStable,
            onAlbumClick = onAlbumClickStable,
            onArtistClick = onArtistClickStable,
            onPlaylistClick = onPlaylistClickStable,
            onCloudSongClick = { viewModel.downloadSong(context, it) },
            onCloudArtistClick = { viewModel.loadCloudArtistDetail(it) },
            onCloudAlbumClick = { viewModel.loadCloudDetail(it.id, it.title, it.artist, it.thumbnailUrl, ResultType.ALBUM) },
            onCloudPlaylistClick = { viewModel.loadCloudDetail(it.id, it.title, it.uploader, it.thumbnailUrl, ResultType.PLAYLIST) },
            libraryViewModel = libraryViewModel,
            query = query,
            onSongLongClick = { menuSong = it }
        )
    }

    CloudDetailSheet(
        state = cloudDetailState,
        artistState = cloudArtistDetailState,
        downloadProgressMapProvider = { downloadProgressMapState.value },
        onDismiss = { 
            viewModel.dismissCloudDetail()
            viewModel.dismissCloudArtistDetail()
        },
        onSongClick = { viewModel.downloadSong(context, it) },
        onDownloadAll = { /* Implementar si es necesario */ },
        onAlbumClick = { viewModel.loadCloudDetail(it.id, it.title, it.artist, it.thumbnailUrl, ResultType.ALBUM) },
        onPlaylistClick = { viewModel.loadCloudDetail(it.id, it.title, it.uploader, it.thumbnailUrl, ResultType.PLAYLIST) }
    )

    menuSong?.let { song ->
        SongOverflowMenu(
            song = song,
            source = SongMenuSource.Library,
            userPlaylists = userPlaylists,
            playlistIdsContainingSong = playlistMembership[song.id] ?: emptySet(),
            isFavorite = song.id in favoriteSongIds,
            showMenu = true,
            onDismissRequest = { menuSong = null },
            onPlayNext = { playbackViewModel.playNext(song) },
            onAddToQueue = { playbackViewModel.addToQueue(song) },
            onOpenAlbum = onAlbumClickStable,
            onOpenArtist = onArtistClickStable,
            onAddToPlaylist = { libraryViewModel.addSongToPlaylist(it, song.id) },
            onToggleFavorite = { libraryViewModel.toggleSongFavorite(song.id) },
            onDeleteSong = { libraryViewModel.hideSong(song.id) },
            onViewQueue = onViewQueueStable,
            onEditClick = {
                songToEdit = song
                showEditSheet = true
            }
        )
    }

    if (showEditSheet && songToEdit != null) {
        SongEditSheet(
            song = songToEdit!!,
            onDismissRequest = {
                showEditSheet = false
                songToEdit = null
            }
        )
    }
}

@Composable
private fun SearchModePills(
    currentMode: SearchMode,
    onModeChange: (SearchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchPill(
            text = stringResource(R.string.search_mode_offline),
            selected = currentMode == SearchMode.OFFLINE,
            onClick = { onModeChange(SearchMode.OFFLINE) }
        )
        SearchPill(
            text = stringResource(R.string.search_mode_online),
            selected = currentMode == SearchMode.ONLINE,
            onClick = { onModeChange(SearchMode.ONLINE) }
        )
    }
}

@Composable
private fun SearchPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "pillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillContent"
    )

    // Lógica para el efecto AGSL
    var effectActive by remember { mutableStateOf(false) }
    val shaderAlphaState = animateFloatAsState(
        targetValue = if (effectActive) 1f else 0f,
        animationSpec = if (effectActive) tween(300) else tween(2500),
        label = "shaderAlpha"
    )

    LaunchedEffect(selected) {
        if (selected) {
            effectActive = true
            delay(2000)
            effectActive = false
        } else {
            effectActive = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pillWaves")
    val timeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pillTime"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // OPTIMIZACIÓN: Mover el dibujado del shader fuera de la fase de composición
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader = remember { RuntimeShader(PILL_WAVE_SHADER) }
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .drawWithCache {
                            onDrawWithContent {
                                // Solo leemos el tiempo si el alpha es mayor a 0 para evitar redibujados innecesarios
                                val alpha = shaderAlphaState.value
                                if (alpha > 0.01f) {
                                    val time = timeState.value
                                    val drawSize = this.size
                                    shader.setFloatUniform("size", drawSize.width, drawSize.height)
                                    shader.setFloatUniform("time", time)
                                    shader.setFloatUniform("alpha", alpha)
                                    drawRect(brush = ShaderBrush(shader))
                                }
                                drawContent()
                            }
                        }
                )
            }

            // CAPA DE TEXTO
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    result: SearchResult.Success, 
    songsPaging: LazyPagingItems<Song>?,
    albumsPaging: LazyPagingItems<Album>?,
    artistsPaging: LazyPagingItems<Artist>?,
    cloudResults: List<Any>,
    isCloudLoading: Boolean,
    searchMode: SearchMode,
    isCloudEnabled: Boolean,
    cloudDownloadProgressProvider: () -> Map<String, Int>,
    scrollState: LazyListState,
    isSongsExpanded: Boolean,
    onToggleSongs: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onCloudSongClick: (CloudSong) -> Unit,
    onCloudArtistClick: (CloudArtist) -> Unit,
    onCloudAlbumClick: (CloudAlbum) -> Unit,
    onCloudPlaylistClick: (CloudPlaylist) -> Unit,
    libraryViewModel: LibraryViewModel
) {
    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = navStackHeight + 16.dp, 
            top = 0.dp,
            start = 20.dp, 
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (isCloudEnabled) {
                SearchModePills(
                    currentMode = searchMode,
                    onModeChange = onModeChange
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (searchMode == SearchMode.ONLINE) {
            cloudSection(cloudResults, isCloudLoading, cloudDownloadProgressProvider, onCloudSongClick, onCloudArtistClick, onCloudAlbumClick, onCloudPlaylistClick)
        } else {
            val sections = when (result.priority) {
                SearchPriority.ARTIST -> listOf("artists", "songs", "albums", "playlists")
                SearchPriority.ALBUM -> listOf("albums", "artists", "playlists", "songs")
                SearchPriority.SONG -> listOf("songs", "albums", "playlists", "artists")
                SearchPriority.PLAYLIST -> listOf("playlists", "albums", "artists", "songs")
            }

            sections.forEach { section ->
                when (section) {
                    "artists" -> artistsSection(artistsPaging, onArtistClick, libraryViewModel)
                    "songs" -> songsSection(songsPaging, isSongsExpanded, onToggleSongs, onSongClick, onSongLongClick)
                    "albums" -> albumsSection(albumsPaging, onAlbumClick)
                    "playlists" -> playlistsSection(result, onPlaylistClick)
                }
            }
        }
    }
}

private fun LazyListScope.cloudSection(
    results: List<Any>,
    isLoading: Boolean,
    downloadProgressProvider: () -> Map<String, Int>,
    onSongClick: (CloudSong) -> Unit,
    onArtistClick: (CloudArtist) -> Unit,
    onAlbumClick: (CloudAlbum) -> Unit,
    onPlaylistClick: (CloudPlaylist) -> Unit
) {
    val songs = results.filterIsInstance<CloudSong>()
    val albums = results.filterIsInstance<CloudAlbum>()
    val artists = results.filterIsInstance<CloudArtist>()
    val playlists = results.filterIsInstance<CloudPlaylist>()

    if (isLoading) {
        item {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    if (results.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.search_no_results_cloud),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    // --- ARTISTAS ---
    if (artists.isNotEmpty()) {
        item {
            var isArtistsExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(stringResource(R.string.search_section_cloud_artists))
                if (artists.size > 2) {
                    TextButton(onClick = { isArtistsExpanded = !isArtistsExpanded }) {
                        Text(
                            text = if (isArtistsExpanded) stringResource(R.string.button_see_less) else stringResource(R.string.button_see_all),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = if (isArtistsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            val displayArtists = if (isArtistsExpanded) artists else artists.take(2)
            Column {
                displayArtists.forEach { artist ->
                    ItemArtistaList(
                        artist = artist.toLocalArtist(),
                        caratulaUri = Uri.parse(artist.thumbnailUrl),
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }

    // --- CANCIONES ---
    if (songs.isNotEmpty()) {
        item {
            var isExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(stringResource(R.string.search_section_cloud_songs))
                if (songs.size > 5) {
                    TextButton(onClick = { isExpanded = !isExpanded }) {
                        Text(
                            text = if (isExpanded) stringResource(R.string.button_see_less) else stringResource(R.string.button_see_all),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            val displaySongs = if (isExpanded) songs else songs.take(5)
            Column {
                displaySongs.forEach { item ->
                    val progressState = remember { derivedStateOf { downloadProgressProvider()[item.id] } }
                    ItemCancion(
                        cancion = item.toCancionForUI(),
                        onClick = { onSongClick(item) },
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
                                        imageVector = Icons.Default.Download,
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

    // --- ÁLBUMES (SCROLL HORIZONTAL) ---
    if (albums.isNotEmpty()) {
        item {
            SectionHeader(stringResource(R.string.search_section_cloud_albums))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(albums) { album ->
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

    // --- PLAYLISTS ---
    if (playlists.isNotEmpty()) {
        item {
            var isExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(stringResource(R.string.search_section_cloud_playlists))
                if (playlists.size > 5) {
                    TextButton(onClick = { isExpanded = !isExpanded }) {
                        Text(
                            text = if (isExpanded) stringResource(R.string.button_see_less) else stringResource(R.string.button_see_all),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            val displayPlaylists = if (isExpanded) playlists else playlists.take(5)
            Column {
                displayPlaylists.forEach { playlist ->
                    ItemPlaylist(
                        playlist = playlist.toLocalPlaylist(),
                        onClick = { onPlaylistClick(playlist) }
                    )
                }
            }
        }
    }
}

private fun CloudSong.toCancionForUI(): Cancion {
    return Cancion(
        caratulaUri = Uri.parse(thumbnailUrl),
        titulo = title,
        artista = artist
    )
}

private fun CloudArtist.toLocalArtist(): Artist {
    android.util.Log.d("SearchPage", "Mapeando artista: $name, songs: $songCount, albums: $albumCount")
    return Artist(
        id = 0L,
        name = name,
        songCount = songCount.coerceAtLeast(0),
        albumCount = albumCount.coerceAtLeast(0)
    )
}

private fun CloudAlbum.toLocalAlbum(): Album {
    return Album(
        id = 0L,
        name = title,
        artist = artist,
        songCount = songCount.coerceAtLeast(0)
    )
}

private fun CloudPlaylist.toLocalPlaylist(): Playlist {
    return Playlist(
        id = id,
        name = title,
        songCount = songCount.coerceAtLeast(0),
        isAutoGenerated = false,
        artworkUri = Uri.parse(thumbnailUrl)
    )
}

private fun LazyListScope.songsSection(
    songs: LazyPagingItems<Song>?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    if (songs != null && songs.itemCount > 0) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(stringResource(R.string.search_section_songs))
                if (songs.itemCount > 5) {
                    TextButton(onClick = onToggle) {
                        Text(
                            text = if (isExpanded) stringResource(R.string.button_see_less) else stringResource(R.string.button_see_all),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        val count = if (isExpanded) songs.itemCount else minOf(songs.itemCount, 5)
        items(
            count = count,
            key = songs.itemKey { "song_${it.id}" },
            contentType = songs.itemContentType { "song_item" }
        ) { index ->
            val song = songs[index] ?: return@items
            ItemCancion(
                cancion = song.toCancion(),
                onClick = { 
                    val fullList = mutableListOf<Song>()
                    for (i in 0 until songs.itemCount) {
                        songs[i]?.let { fullList.add(it) }
                    }
                    onSongClick(song, fullList) 
                },
                onLongClick = { onSongLongClick(song) },
                onMenuClick = { onSongLongClick(song) }
            )
        }
    }
}

private fun LazyListScope.albumsSection(albums: LazyPagingItems<Album>?, onAlbumClick: (Long) -> Unit) {
    if (albums != null && albums.itemCount > 0) {
        item {
            SectionHeader(stringResource(R.string.search_section_albums))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = albums.itemCount,
                    key = albums.itemKey { "album_${it.id}" },
                    contentType = albums.itemContentType { "album_item" }
                ) { index ->
                    val album = albums[index] ?: return@items
                    Box(modifier = Modifier.width(160.dp)) {
                        ItemAlbum(
                            album = album,
                            caratulaUri = Uri.parse("content://media/external/audio/albumart/${album.id}"),
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.artistsSection(
    artists: LazyPagingItems<Artist>?, 
    onArtistClick: (Long) -> Unit,
    libraryViewModel: LibraryViewModel
) {
    if (artists != null && artists.itemCount > 0) {
        item { SectionHeader(stringResource(R.string.search_section_artists)) }
        items(
            count = artists.itemCount,
            key = artists.itemKey { "artist_${it.id}" },
            contentType = artists.itemContentType { "artist_item" }
        ) { index ->
            val artist = artists[index] ?: return@items
            ItemArtistaList(
                artist = artist,
                caratulaUri = libraryViewModel.getArtistArtUri(artist.id),
                onClick = { onArtistClick(artist.id) }
            )
        }
    }
}

private fun LazyListScope.playlistsSection(result: SearchResult.Success, onPlaylistClick: (String) -> Unit) {
    if (result.playlists.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.search_section_playlists)) }
        items(result.playlists, key = { "playlist_${it.id}" }) { playlist ->
            ItemPlaylist(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.search_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NoResultsState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.search_no_results_title, query),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.search_no_results_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private fun Song.toCancion(): Cancion {
    return Cancion(
        caratulaUri = Uri.parse("content://media/external/audio/media/$id/albumart"),
        titulo = title,
        artista = artist
    )
}

private fun <T : Any> emptyFlow(): kotlinx.coroutines.flow.Flow<PagingData<T>> = kotlinx.coroutines.flow.flowOf(PagingData.empty())

private const val PILL_WAVE_SHADER = """
    uniform float2 size;
    uniform float time;
    uniform float alpha;

    float2 hash(float2 p) {
        p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
        return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(dot(hash(i + float2(0.0, 0.0)), f - float2(0.0, 0.0)),
                       dot(hash(i + float2(1.0, 0.0)), f - float2(1.0, 1.0)), u.x),
                   mix(dot(hash(i + float2(0.0, 1.0)), f - float2(0.0, 1.0)),
                       dot(hash(i + float2(1.0, 1.0)), f - float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / size;
        float t = time * 0.35;
        
        // --- LÓGICA DE LAS OLAS (MOVIMIENTO BASE) ---
        float wave1 = sin(uv.x * 4.0 + t) * 0.25 + sin(uv.x * 8.0 - t * 0.7) * 0.08;
        float wave2 = sin(uv.x * 3.5 - t * 0.4) * 0.2 + cos(uv.x * 6.0 + t * 0.5) * 0.1;
        float mask = smoothstep(0.35, 0.95, uv.y + wave1 * 0.5 + wave2 * 0.3);
        
        // --- EFECTO MONOCROMÁTICO (UNIFICADO) ---
        float gray = 0.25 + 0.15 * noise(uv * 3.0 - t * 0.05);
        return half4(gray, gray, gray, 0.6 * alpha * mask);
    }
"""

@Preview(showBackground = true)
@Composable
fun SearchPagePreview() {
    SoundlyTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // MOCK DEL USER HEADER (Solo título)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            SearchContent(
                searchResult = SearchResult.Idle,
                searchMode = SearchMode.OFFLINE,
                isSongsExpanded = false,
                isCloudEnabled = true,
                cloudResults = emptyList(),
                isCloudLoading = false,
                downloadProgressMapProvider = { emptyMap<String, Int>() },
                onModeChange = {},
                onToggleSongs = {}
            )
        }
    }
}

@Composable
private fun SearchContent(
    searchResult: SearchResult,
    searchMode: SearchMode,
    isSongsExpanded: Boolean,
    isCloudEnabled: Boolean,
    cloudResults: List<Any>,
    isCloudLoading: Boolean,
    downloadProgressMapProvider: () -> Map<String, Int>,
    onModeChange: (SearchMode) -> Unit,
    onToggleSongs: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onCloudSongClick: (CloudSong) -> Unit = {},
    onCloudArtistClick: (CloudArtist) -> Unit = {},
    onCloudAlbumClick: (CloudAlbum) -> Unit = {},
    onCloudPlaylistClick: (CloudPlaylist) -> Unit = {},
    libraryViewModel: LibraryViewModel? = null,
    query: String = "", // Query solo para NoResultsState si es necesario
    onSongLongClick: (Song) -> Unit = {}
) {
    val scrollState = rememberLazyListState()
    
    val songsPagingItems = (searchResult as? SearchResult.Success)?.songs?.collectAsLazyPagingItems()
    val albumsPagingItems = (searchResult as? SearchResult.Success)?.albums?.collectAsLazyPagingItems()
    val artistsPagingItems = (searchResult as? SearchResult.Success)?.artists?.collectAsLazyPagingItems()

    ScrollFadeContainer(listState = scrollState) {
        AnimatedContent(
            targetState = if (searchMode == SearchMode.OFFLINE) searchResult else SearchResult.Success(emptyFlow(), emptyFlow(), emptyFlow(), emptyList(), SearchPriority.SONG),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "searchContent",
            modifier = Modifier.fillMaxSize()
        ) { result ->
            when (result) {
                is SearchResult.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        if (isCloudEnabled) {
                            SearchModePills(
                                currentMode = searchMode,
                                onModeChange = onModeChange
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        EmptySearchState()
                    }
                }
                is SearchResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is SearchResult.Success -> {
                    val hasSongs = (songsPagingItems?.itemCount ?: 0) > 0
                    val hasAlbums = (albumsPagingItems?.itemCount ?: 0) > 0
                    val hasArtists = (artistsPagingItems?.itemCount ?: 0) > 0
                    val hasLocalResults = hasSongs || hasAlbums || hasArtists || result.playlists.isNotEmpty()
                    
                    if (searchMode == SearchMode.OFFLINE && !hasLocalResults && query.isNotEmpty()) {
                        Column {
                            if (isCloudEnabled) {
                                SearchModePills(
                                    currentMode = searchMode,
                                    onModeChange = onModeChange,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                            NoResultsState(query)
                        }
                    } else {
                        SearchResultsList(
                            result = result, 
                            songsPaging = songsPagingItems,
                            albumsPaging = albumsPagingItems,
                            artistsPaging = artistsPagingItems,
                            cloudResults = cloudResults,
                            isCloudLoading = isCloudLoading,
                            searchMode = searchMode,
                            isCloudEnabled = isCloudEnabled,
                            cloudDownloadProgressProvider = downloadProgressMapProvider,
                            scrollState = scrollState,
                            isSongsExpanded = isSongsExpanded,
                            onToggleSongs = onToggleSongs,
                            onSongClick = onSongClick,
                            onSongLongClick = onSongLongClick,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onPlaylistClick = onPlaylistClick,
                            onModeChange = onModeChange,
                            onCloudSongClick = onCloudSongClick,
                            onCloudArtistClick = onCloudArtistClick,
                            onCloudAlbumClick = onCloudAlbumClick,
                            onCloudPlaylistClick = onCloudPlaylistClick,
                            libraryViewModel = libraryViewModel ?: hiltViewModel()
                        )
                    }
                }
            }
        }
    }
}
