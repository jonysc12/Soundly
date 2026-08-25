package com.soundly.feature.home

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import com.soundly.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.carousel.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Album
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.data.repository.HomeSectionType
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.biblioteca.CreatePlaylistBottomSheet
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenu
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.agslFrostedGlass
import com.soundly.ui.componentes.edit.SongEditSheet
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.HomeItemArtistaList
import com.soundly.ui.componentes.listas.HomeItemCancion
import com.soundly.ui.componentes.listas.HomeItemCreatePlaylist
import com.soundly.ui.componentes.listas.ItemCancion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    viewModel: HomeViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onViewQueue: () -> Unit = {},
    onSearchClick: (com.soundly.feature.search.SearchMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val carouselState = rememberCarouselState { uiState.discoverAlbums.size }

    val userPlaylists by libraryViewModel.userPlaylists.collectAsStateWithLifecycle()
    val importState by libraryViewModel.importState.collectAsStateWithLifecycle()
    val playlistMembership by libraryViewModel.playlistMembershipBySong.collectAsStateWithLifecycle()
    val favoriteSongIds by libraryViewModel.favoriteSongIds.collectAsStateWithLifecycle()

    val onAlbumClickStable = remember(onAlbumClick) { { id: Long -> onAlbumClick(id) } }
    val onArtistClickStable = remember(onArtistClick) { { id: Long -> onArtistClick(id) } }
    val onPlaylistClickStable = remember(onPlaylistClick) { { id: String -> onPlaylistClick(id) } }
    val onViewQueueStable = remember(onViewQueue) { { onViewQueue() } }

    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistSheet by remember { mutableStateOf(false) }
    
    var overscrollOffsetValue by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    overscrollOffsetValue = (overscrollOffsetValue + available.y * 0.4f).coerceIn(-150f, 150f)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffsetValue != 0f) {
                    androidx.compose.animation.core.Animatable(overscrollOffsetValue).animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) {
                        overscrollOffsetValue = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val horizontalScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                return if (Math.abs(available.y) > Math.abs(available.x)) {
                    androidx.compose.ui.geometry.Offset.Zero
                } else {
                    super.onPreScroll(available, source)
                }
            }
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
    } else {
        val isScrolledProvider = remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
        val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current

        Box(modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .graphicsLayer { translationY = overscrollOffsetValue }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("home_page_list"),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = navStackHeight + 70.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior()
            ) {
                items(
                    items = uiState.sectionsOrder,
                    key = { it.name },
                    contentType = { it.name }
                ) { sectionType ->
                    when (sectionType) {
                        HomeSectionType.DISCOVER_ALBUMS -> {
                            if (uiState.discoverAlbums.isNotEmpty()) {
                                Column {
                                    HomeSectionTitle(stringResource(sectionType.titleRes))
                                    if (uiState.showSubtitles) {
                                        HomeSectionSubtitle(stringResource(R.string.home_section_discover_albums_desc))
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                        val carouselSpacing = 10.dp
                                        val onPlayClick = remember {
                                            { album: com.soundly.data.model.Album ->
                                                val albumSongs = viewModel.getAlbumSongs(album.id)
                                                if (albumSongs.isNotEmpty()) {
                                                    playbackViewModel.play(albumSongs.first(), albumSongs)
                                                }
                                                onAlbumClickStable(album.id)
                                            }
                                        }

                                        HorizontalMultiBrowseCarousel(
                                            state = carouselState,
                                            preferredItemWidth = 220.dp,
                                            itemSpacing = carouselSpacing,
                                            contentPadding = PaddingValues(
                                                start = 24.dp,
                                                end = 24.dp
                                            ),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(horizontalScrollConnection)
                                        ) { index ->
                                            val album = uiState.discoverAlbums[index]
                                            val artUri = remember(album.id) { viewModel.getAlbumArtUri(album.id) }
                                            HomeAlbumCarouselItem(
                                                album = album,
                                                artUri = artUri,
                                                viewModel = viewModel,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 2.dp)
                                                    .clickable { onAlbumClickStable(album.id) },
                                                onPlayClick = { onPlayClick(album) },
                                                selectedIndex = carouselState.currentItem,
                                                currentIndex = index
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        HomeSectionType.RECENTLY_PLAYED -> {
                            if (uiState.recentlyPlayed.isNotEmpty()) {
                                Column {
                                    HomeSectionTitle(stringResource(sectionType.titleRes))
                                    if (uiState.showSubtitles) {
                                        HomeSectionSubtitle(stringResource(R.string.home_section_recently_played_desc))
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                                        val windowInfo = LocalWindowInfo.current
                                        val density = LocalDensity.current
                                        val itemWidth = remember(windowInfo, density) {
                                            val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
                                            (screenWidth - 40.dp) / 2
                                        }

                                        val currentPlaylist = rememberUpdatedState(uiState.recentlyPlayed)

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(start = 24.dp, end = 16.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(horizontalScrollConnection)
                                        ) {
                                            items(
                                                items = uiState.recentlyPlayed,
                                                key = { it.id },
                                                contentType = { "home_song_item" }
                                            ) { song ->
                                                val artUri = remember(song.albumId) { viewModel.getAlbumArtUri(song.albumId) }
                                                val onClick = remember(song) { { playbackViewModel.play(song, currentPlaylist.value) } }
                                                val onLongClick = remember(song) { { menuSong = song } }

                                                val cancion = remember(artUri, song.title, song.artist) {
                                                    com.soundly.ui.componentes.listas.Cancion(
                                                        caratulaUri = artUri,
                                                        titulo = song.title,
                                                        artista = song.artist
                                                    )
                                                }

                                                Box(modifier = Modifier.width(itemWidth)) {
                                                    HomeItemCancion(
                                                        cancion = cancion,
                                                        onClick = onClick,
                                                        onLongClick = onLongClick
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        HomeSectionType.TOP_ARTISTS -> {
                            if (uiState.mostListenedArtists.isNotEmpty()) {
                                Column {
                                    HomeSectionTitle(stringResource(sectionType.titleRes))
                                    if (uiState.showSubtitles) {
                                        HomeSectionSubtitle(stringResource(R.string.home_section_top_artists_desc))
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                        val windowInfo = LocalWindowInfo.current
                                        val density = LocalDensity.current

                                        val itemWidth = remember(windowInfo.containerSize, density) {
                                            val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
                                            val horizontalPadding = 32.dp
                                            val spacing = 12.dp
                                            val availableWidth = screenWidth - horizontalPadding
                                            val preferredItemWidth = 140.dp
                                            val calculatedCount = ((availableWidth + spacing).value / (preferredItemWidth + spacing).value).toInt()
                                            val itemsCount = maxOf(2, calculatedCount)
                                            (availableWidth - (spacing * (itemsCount - 1))) / itemsCount
                                        }

                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .nestedScroll(horizontalScrollConnection),
                                            contentPadding = PaddingValues(start = 24.dp, end = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(
                                                items = uiState.mostListenedArtists,
                                                key = { it.id },
                                                contentType = { "home_artist_item" }
                                            ) { artist ->
                                                val onClick = remember(artist.id) { { onArtistClickStable(artist.id) } }
                                                HomeItemArtistaList(
                                                    artist = artist,
                                                    caratulaUri = artist.artworkUri,
                                                    modifier = Modifier.width(itemWidth),
                                                    onClick = onClick
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        HomeSectionType.RECENTLY_ADDED -> {
                            if (uiState.recentlyAdded.isNotEmpty()) {
                                Column {
                                    HomeSectionTitle(stringResource(sectionType.titleRes))
                                    if (uiState.showSubtitles) {
                                        HomeSectionSubtitle(stringResource(R.string.home_section_recently_added_desc))
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                        LazyRow(
                                            contentPadding = PaddingValues(start = 24.dp, end = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(horizontalScrollConnection)
                                        ) {
                                            items(
                                                items = uiState.recentlyAdded,
                                                key = { it.id },
                                                contentType = { "home_song_card" }
                                            ) { song ->
                                                val artUri = remember(song.albumId) { viewModel.getAlbumArtUri(song.albumId) }
                                                HomeSongCard(
                                                    song = song,
                                                    artUri = artUri,
                                                    onClick = { playbackViewModel.play(song, uiState.recentlyAdded) },
                                                    onLongClick = { menuSong = song }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        HomeSectionType.RECOMMENDED -> {
                            if (uiState.recommendedSongs.isNotEmpty()) {
                                Column {
                                    HomeSectionTitle(stringResource(sectionType.titleRes))
                                    if (uiState.showSubtitles) {
                                        HomeSectionSubtitle(stringResource(R.string.home_section_recommended_desc))
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                        val chunkedSongs = uiState.recommendedSongs.chunked(2)
                                        val currentPlaylist = rememberUpdatedState(uiState.recommendedSongs)

                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .nestedScroll(horizontalScrollConnection),
                                            contentPadding = PaddingValues(start = 24.dp, end = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(
                                                items = chunkedSongs,
                                                key = { chunk -> chunk.firstOrNull()?.id ?: 0L },
                                                contentType = { "home_song_chunk" }
                                            ) { chunk ->
                                                Column(
                                                    modifier = Modifier.width(320.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    chunk.forEach { song ->
                                                        val artUri = remember(song.albumId) { viewModel.getAlbumArtUri(song.albumId) }
                                                        val cancion = remember(artUri, song.title, song.artist) {
                                                            com.soundly.ui.componentes.listas.Cancion(
                                                                caratulaUri = artUri,
                                                                titulo = song.title,
                                                                artista = song.artist
                                                            )
                                                        }

                                                        ItemCancion(
                                                            cancion = cancion,
                                                            onClick = { playbackViewModel.play(song, currentPlaylist.value) },
                                                            onLongClick = { menuSong = song },
                                                            onMenuClick = { menuSong = song }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        HomeSectionType.CLOUD_RECOMMENDATIONS -> {
                            if (uiState.cloudRecommendations.isNotEmpty()) {
                                val context = LocalContext.current
                                Column {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Soundly Cloud",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.home_section_cloud_recommendations_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    
                                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                        LazyRow(
                                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(horizontalScrollConnection)
                                        ) {
                                            items(
                                                items = uiState.cloudRecommendations,
                                                key = { it.id },
                                                contentType = { "home_cloud_recommendation" }
                                            ) { cloudSong ->
                                                val progress = uiState.downloadProgress[cloudSong.id]
                                                HomeCloudDownloadItem(
                                                    song = cloudSong,
                                                    progress = progress,
                                                    onDownloadClick = {
                                                        viewModel.downloadCloudSong(context, cloudSong)
                                                    },
                                                    onTitleClick = {
                                                        viewModel.playCloudSong(cloudSong, playbackViewModel)
                                                    }
                                                )
                                            }
                                            
                                            // Último item: Buscar online
                                            item(key = "cloud_search_more") {
                                                HomeCloudSearchItem(
                                                    onClick = { 
                                                        onSearchClick(com.soundly.feature.search.SearchMode.ONLINE)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        HomeSectionType.MONTHLY_RECAP -> {
                            if (uiState.isRecapPeriod && uiState.monthlyRecapSongs.isNotEmpty()) {
                                HomeMonthlyRecapBanner(
                                    songs = uiState.monthlyRecapSongs,
                                    onClick = { onPlaylistClickStable(com.soundly.data.repository.MusicRepository.TOP_MONTH_RECAP_ID) },
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                            }
                        }

                        HomeSectionType.USER_PLAYLISTS -> {
                            val finalItems = uiState.userPlaylists
                                .filter { it.showOnHome }
                                .sortedWith(
                                    compareByDescending<Playlist> { it.isAutoGenerated }
                                        .thenByDescending { it.updatedAt }
                                )
                                .take(4)

                            if (finalItems.isNotEmpty() || uiState.userPlaylists.isEmpty()) {
                                Column {
                                    HomeSectionTitle(stringResource(sectionType.titleRes))
                                    if (uiState.showSubtitles) {
                                        HomeSectionSubtitle(stringResource(R.string.home_section_user_playlists_desc))
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    val windowInfo = LocalWindowInfo.current
                                    val density = LocalDensity.current

                                    val itemWidth = remember(windowInfo.containerSize, density) {
                                        val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
                                        (screenWidth - 60.dp) / 2
                                    }

                                    val context = LocalContext.current
                                    val autoGeneratedArt = remember { Uri.parse("android.resource://${context.packageName}/${R.drawable.playlist_favicon}") }
                                    val likedTitle = stringResource(R.string.liked_songs_title)
                                    val autoGenSubtitle = stringResource(R.string.playlist_auto_generated)
                                    val songsCountText = stringResource(R.string.songs_count)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val itemsWithCreate = remember(finalItems) {
                                            if (finalItems.size < 4) {
                                                finalItems + null
                                            } else {
                                                finalItems
                                            }
                                        }

                                        val rows = remember(itemsWithCreate) { itemsWithCreate.chunked(2) }

                                        rows.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowItems.forEach { playlist ->
                                                    val onClick = remember(playlist?.id) {
                                                        if (playlist != null) {
                                                            { onPlaylistClickStable(playlist.id) }
                                                        } else {
                                                            { showCreatePlaylistSheet = true }
                                                        }
                                                    }

                                                    Box(modifier = Modifier.width(itemWidth)) {
                                                        if (playlist != null) {
                                                            val cancion = remember(playlist, likedTitle, autoGenSubtitle, songsCountText) {
                                                                val playlistName = if (playlist.id == com.soundly.data.repository.MusicRepository.LIKED_SONGS_PLAYLIST_ID) {
                                                                    likedTitle
                                                                } else {
                                                                    playlist.name
                                                                }
                                                                Cancion(
                                                                    caratulaUri = if (playlist.isAutoGenerated) {
                                                                        autoGeneratedArt
                                                                    } else {
                                                                        playlist.artworkUri
                                                                    },
                                                                    titulo = playlistName,
                                                                    artista = if (playlist.isAutoGenerated) autoGenSubtitle else songsCountText.format(playlist.songCount)
                                                                )
                                                            }
                                                            HomeItemCancion(
                                                                cancion = cancion,
                                                                onClick = onClick
                                                            )
                                                        } else {
                                                            HomeItemCreatePlaylist(
                                                                onClick = onClick
                                                            )
                                                        }
                                                    }
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.width(itemWidth))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            val isDark = LocalIsDarkTheme.current
            val alphaProvider = remember(isScrolledProvider) {
                { if (isScrolledProvider.value) 1f else 0f }
            }

            if (isScrolledProvider.value) {
                val tintColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
                val gradient = remember(tintColor) {
                    Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.95f),
                            tintColor.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            this.alpha = alphaProvider()
                        }
                        .drawBehind {
                            drawRect(brush = gradient)
                        }
                )
            }
        }
    }

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
            onOpenAlbum = onAlbumClick,
            onOpenArtist = onArtistClick,
            onAddToPlaylist = { libraryViewModel.addSongToPlaylist(it, song.id) },
            onToggleFavorite = { libraryViewModel.toggleSongFavorite(song.id) },
            onDeleteSong = { libraryViewModel.hideSong(song.id) },
            onViewQueue = onViewQueue,
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

    CreatePlaylistBottomSheet(
        visible = showCreatePlaylistSheet,
        onDismiss = { showCreatePlaylistSheet = false },
        onCreatePlaylist = libraryViewModel::createPlaylist,
        onImportPlaylist = libraryViewModel::importPlaylist,
        importState = importState,
        onClearImportState = libraryViewModel::clearImportState,
        onCreatePlaylistWithSongs = libraryViewModel::createPlaylistWithSongs
    )
}

@Composable
fun HomeSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 4.dp)
    )
}

@Composable
fun HomeSectionSubtitle(subtitle: String) {
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 4.dp)
    )
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CarouselItemScope.HomeAlbumCarouselItem(
    album: Album,
    artUri: Uri,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit = {},
    selectedIndex: Int = 0,
    currentIndex: Int = 0
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color.Black) }
    val scope = rememberCoroutineScope()

    val imageRequest = remember(artUri) {
        ImageRequest.Builder(context)
            .data(artUri)
            .crossfade(true)
            .allowHardware(true)
            .size(600)
            .build()
    }

    val isExpanded = currentIndex == selectedIndex

    Box(
        modifier = modifier
            .maskClip(RoundedCornerShape(33.dp))
            .background(Color.Black)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                placeholder = painterResource(R.drawable.carga),
                error = painterResource(R.drawable.carga),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { result ->
                    val bitmap = (result.result.drawable as BitmapDrawable).bitmap
                    scope.launch {
                        dominantColor = viewModel.getDominantColor(album.id, bitmap)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val color = dominantColor
                        val brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = 0.2f),
                                color.copy(alpha = 0.8f)
                            )
                        )
                        onDrawBehind {
                            drawRect(brush = brush)
                        }
                    }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .size(24.dp)
            )

            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val typo = MaterialTheme.typography
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = album.name,
                            style = typo.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            style = typo.bodyMedium.copy(fontSize = 14.sp),
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialShapes.Square.toShape())
                            .agslFrostedGlass(
                                radius = 16f,
                                tint = if (dominantColor == Color.Black) Color(0xFF666666).copy(alpha = 0.45f) else dominantColor.copy(alpha = 0.45f)
                            )
                            .clickable { onPlayClick() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeSongCard(
    song: Song,
    artUri: Uri,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val imageRequest = remember(artUri) {
        ImageRequest.Builder(context)
            .data(artUri)
            .crossfade(true)
            .size(280) // Optimized size
            .build()
    }

    Column(
        modifier = Modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp)),
            placeholder = painterResource(R.drawable.carga),
            error = painterResource(R.drawable.carga)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeCloudSearchItem(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = "Encuentra lo que buscas",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeCloudDownloadItem(
    song: com.soundly.cloud.Song,
    progress: Int?,
    onDownloadClick: () -> Unit,
    onTitleClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(song.thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(song.thumbnailUrl)
            .crossfade(true)
            .size(320)
            .build()
    }

    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onDownloadClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.carga),
                error = painterResource(R.drawable.carga)
            )
            
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (progress == -1) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.size(56.dp),
                                color = Color.White,
                                strokeWidth = 4.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = "${progress}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Indicador visual de que es descargable al tocar
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Column(
            modifier = Modifier.clickable(onClick = onTitleClick)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeMonthlyRecapBanner(
    songs: List<Song>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = java.util.Calendar.getInstance()
    val monthName = remember {
        calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()) ?: ""
    }
    
    val artUris = remember(songs) {
        songs.take(4).map { song ->
            Uri.parse("content://media/external/audio/albumart/${song.albumId}")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                artUris.forEach { uri ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .alpha(0.3f)
                            .graphicsLayer { renderEffect = if (android.os.Build.VERSION.SDK_INT >= 31) android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect() else null },
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.recap_banner_title, monthName),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.recap_banner_desc, songs.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.recap_explore_now))
                }
            }
        }
    }
}
