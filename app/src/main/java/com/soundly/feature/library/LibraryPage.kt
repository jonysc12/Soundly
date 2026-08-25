package com.soundly.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.soundly.R
import com.soundly.debug.DebugRecompose
import com.soundly.feature.library.pages.AlbumDetailScreen
import com.soundly.feature.library.pages.AlbumsScreen
import com.soundly.feature.library.pages.ArtistDetailScreen
import com.soundly.feature.library.pages.ArtistsScreen
import com.soundly.feature.library.pages.SongsScreen
import com.soundly.ui.componentes.*
import com.soundly.ui.navigation.LocalBackStackCoordinator
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private sealed interface LibraryDetailDestination {
    data class Album(val id: Long) : LibraryDetailDestination
    data class Artist(val id: Long) : LibraryDetailDestination
}

private fun LibraryDetailDestination.toPersistedValue(): String = when (this) {
    is LibraryDetailDestination.Album -> "album:$id"
    is LibraryDetailDestination.Artist -> "artist:$id"
}

private fun persistedValueToDestination(value: String): LibraryDetailDestination? {
    val parts = value.split(":", limit = 2)
    if (parts.size != 2) return null
    val id = parts[1].toLongOrNull() ?: return null
    return when (parts[0]) {
        "album" -> LibraryDetailDestination.Album(id)
        "artist" -> LibraryDetailDestination.Artist(id)
        else -> null
    }
}

private val detailStackSaver = listSaver<SnapshotStateList<LibraryDetailDestination>, String>(
    save = { stack -> stack.map { it.toPersistedValue() } },
    restore = { values ->
        mutableStateListOf<LibraryDetailDestination>().apply {
            values.mapNotNullTo(this, ::persistedValueToDestination)
        }
    }
)

@Composable
fun LibraryPage(
    viewModel: LibraryViewModel,
    onAlbumDetailVisibilityChanged: (Boolean) -> Unit,
    onArtistEdgeToEdgeChanged: (Boolean) -> Unit,
    onPlaySong: (com.soundly.data.model.Song, List<com.soundly.data.model.Song>) -> Unit,
    onPlayCollection: (List<com.soundly.data.model.Song>, Boolean) -> Unit,
    onViewQueue: () -> Unit = {},
    externalArtistRequest: Long? = null,
    onExternalRequestConsumed: () -> Unit = {},
    onCurrentArtistChanged: (Long?) -> Unit = {},
    isHostPageVisible: Boolean = true
) {
    DebugRecompose("LibraryPage", logEvery = 15)
    val detailStack = rememberSaveable(saver = detailStackSaver) { mutableStateListOf<LibraryDetailDestination>() }
    val currentDetail = detailStack.lastOrNull()

    val onAlbumDetailVisibilityChangedStable = remember(onAlbumDetailVisibilityChanged) { { v: Boolean -> onAlbumDetailVisibilityChanged(v) } }
    val onArtistEdgeToEdgeChangedStable = remember(onArtistEdgeToEdgeChanged) { { v: Boolean -> onArtistEdgeToEdgeChanged(v) } }
    val onPlaySongStable = remember(onPlaySong) { { s: com.soundly.data.model.Song, q: List<com.soundly.data.model.Song> -> onPlaySong(s, q) } }
    val onPlayCollectionStable = remember(onPlayCollection) { { q: List<com.soundly.data.model.Song>, b: Boolean -> onPlayCollection(q, b) } }
    val onViewQueueStable = remember(onViewQueue) { { onViewQueue() } }

    LaunchedEffect(currentDetail) {
        onCurrentArtistChanged((currentDetail as? LibraryDetailDestination.Artist)?.id)
    }

    val openDetail: (LibraryDetailDestination) -> Unit = remember(detailStack) {
        { destination -> if (detailStack.lastOrNull() != destination) detailStack.add(destination) }
    }

    val options = listOf(
        stringResource(R.string.library_option_songs),
        stringResource(R.string.library_option_albums),
        stringResource(R.string.library_option_artists)
    )
    val pagerState = rememberPagerState { options.size }
    val songsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val artistsListState = rememberLazyListState()
    
    val popDetail: () -> Unit = {
        if (detailStack.isNotEmpty()) {
            detailStack.removeAt(detailStack.lastIndex)
        }
    }

    LaunchedEffect(currentDetail != null) { onAlbumDetailVisibilityChangedStable(currentDetail != null) }
    LaunchedEffect(currentDetail is LibraryDetailDestination.Artist) { onArtistEdgeToEdgeChangedStable(currentDetail is LibraryDetailDestination.Artist) }

    val backStackCoordinator = LocalBackStackCoordinator.current
    val backHandlerEnabled = detailStack.isNotEmpty() && isHostPageVisible && !backStackCoordinator.isOverlayActive

    DisposableEffect(Unit) {
        onDispose {
            onAlbumDetailVisibilityChangedStable(false)
            onArtistEdgeToEdgeChangedStable(false)
        }
    }

    BackHandler(enabled = backHandlerEnabled, onBack = popDetail)

    AnimatedContent(
        targetState = currentDetail,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "LibraryContent"
    ) { destination ->
        when (destination) {
            is LibraryDetailDestination.Album -> AlbumDetailScreen(destination.id, viewModel, { openDetail(LibraryDetailDestination.Album(it)) }, { openDetail(LibraryDetailDestination.Artist(it)) }, popDetail, onViewQueueStable, onPlaySongStable, onPlayCollectionStable)
            is LibraryDetailDestination.Artist -> ArtistDetailScreen(
                artistId = destination.id,
                viewModel = viewModel,
                onAlbumClick = { openDetail(LibraryDetailDestination.Album(it)) },
                onArtistClick = { openDetail(LibraryDetailDestination.Artist(it)) },
                onBack = popDetail,
                onViewQueue = onViewQueueStable,
                onPlaySong = onPlaySongStable,
                onPlayCollection = onPlayCollectionStable
            )
            null -> LibraryMainContent(viewModel, options, pagerState, songsListState, albumsGridState, artistsGridState, artistsListState, { openDetail(LibraryDetailDestination.Album(it)) }, { openDetail(LibraryDetailDestination.Artist(it)) }, onViewQueueStable, onPlaySongStable, onPlayCollectionStable)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryMainContent(
    viewModel: LibraryViewModel,
    options: List<String>,
    pagerState: PagerState,
    songsListState: LazyListState,
    albumsGridState: LazyGridState,
    artistsGridState: LazyGridState,
    artistsListState: LazyListState,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onViewQueue: () -> Unit = {},
    onPlaySong: (com.soundly.data.model.Song, List<com.soundly.data.model.Song>) -> Unit,
    onPlayCollection: (List<com.soundly.data.model.Song>, Boolean) -> Unit
) {
    LaunchedEffect(Unit) { viewModel.refreshLibraryData() }
    
    val coroutineScope = rememberCoroutineScope()
    val songs = viewModel.librarySongs.collectAsLazyPagingItems()
    val albums = viewModel.libraryAlbums.collectAsLazyPagingItems()
    val artists = viewModel.libraryArtists.collectAsLazyPagingItems()
    val albumsPlaybackQueue by viewModel.albumsPlaybackQueue.collectAsStateWithLifecycle()
    val songsSortOption by viewModel.songsSortOption.collectAsStateWithLifecycle()
    val albumsSortOption by viewModel.albumsSortOption.collectAsStateWithLifecycle()
    val artistsLayoutMode by viewModel.artistsLayoutMode.collectAsStateWithLifecycle()

    val displayedPage by remember { derivedStateOf { if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage } }
    val density = LocalDensity.current
    val collapseDistancePx = remember(density) { with(density) { 88.dp.toPx() } }

    // OPTIMIZACIÓN: Lambda de progreso ultra-estable
    val optionsCollapseProgressProvider = remember {
        {
            val page = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
            val mode = viewModel.artistsLayoutMode.value
            
            val scrollOffset = when (page) {
                0 -> if (songsListState.firstVisibleItemIndex > 0) collapseDistancePx else songsListState.firstVisibleItemScrollOffset.toFloat()
                1 -> if (albumsGridState.firstVisibleItemIndex > 0) collapseDistancePx else albumsGridState.firstVisibleItemScrollOffset.toFloat()
                2 -> when (mode) {
                    ArtistsLayoutMode.Grid -> if (artistsGridState.firstVisibleItemIndex > 0) collapseDistancePx else artistsGridState.firstVisibleItemScrollOffset.toFloat()
                    ArtistsLayoutMode.List -> if (artistsListState.firstVisibleItemIndex > 0) collapseDistancePx else artistsListState.firstVisibleItemScrollOffset.toFloat()
                }
                else -> 0f
            }
            (scrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }

    val context = LocalContext.current
    // Eliminamos duplicidad de status bars (HomeScreen ya los aplica al Pager)
    val topPadding = 160.dp 

    Box(modifier = Modifier.fillMaxSize()) {
        // CAPA 1: Contenido (Pager que llena todo)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0
        ) { page ->
            when (page) {
                0 -> SongsScreen(viewModel, songs, songsListState, onPlaySong, sortOption = songsSortOption, onOpenAlbum = onAlbumClick, onOpenArtist = onArtistClick, onViewQueue = onViewQueue, topPadding = topPadding)
                1 -> AlbumsScreen(viewModel, albums, onAlbumClick, gridState = albumsGridState, sortOption = albumsSortOption, topPadding = topPadding)
                2 -> ArtistsScreen(viewModel, artists, onArtistClick, gridState = artistsGridState, listState = artistsListState, layoutMode = artistsLayoutMode, topPadding = topPadding)
            }
        }

        // CAPA 2: Elementos colapsables (Header dinámico)
        Column(modifier = Modifier.fillMaxSize()) {
            // Eliminamos statusBarsPadding()
            Spacer(modifier = Modifier.height(72.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    // El desplazamiento se hace por GPU sincronizado con el scroll
                    translationY = lerp(0.dp, (-88).dp, optionsCollapseProgressProvider()).toPx()
                }
            ) {
                CollapsibleLibraryOptions(progressProvider = optionsCollapseProgressProvider) {
                    when (displayedPage) {
                        0 -> List_options(
                            leadingActions = listOf(ListOptionsLeadingAction(Icons.Rounded.PlayArrow, stringResource(R.string.library_btn_play)) {
                                val fullList = mutableListOf<com.soundly.data.model.Song>()
                                for (i in 0 until songs.itemCount) {
                                    songs[i]?.let { fullList.add(it) }
                                }
                                onPlayCollection(fullList, false)
                            }, ListOptionsLeadingAction(Icons.Rounded.Shuffle, stringResource(R.string.library_btn_shuffle)) {
                                val fullList = mutableListOf<com.soundly.data.model.Song>()
                                for (i in 0 until songs.itemCount) {
                                    songs[i]?.let { fullList.add(it) }
                                }
                                onPlayCollection(fullList, true)
                            }),
                            trailingAction = ListOptionsTrailingAction.Menu(context.getString(songsSortOption.labelResId), LibrarySortOption.entries.map { ListOptionsMenuItem(it.storageValue, context.getString(it.labelResId)) }, songsSortOption.storageValue, { viewModel.setSongsSort(LibrarySortOption.fromStorage(it)) }, Icons.Rounded.Sort, context.getString(R.string.library_btn_sort))
                        )
                        1 -> List_options(
                            leadingActions = listOf(ListOptionsLeadingAction(Icons.Rounded.Shuffle, context.getString(R.string.library_btn_shuffle)) { onPlayCollection(albumsPlaybackQueue, true) }),
                            trailingAction = ListOptionsTrailingAction.Menu(context.getString(albumsSortOption.labelResId), LibrarySortOption.entries.map { ListOptionsMenuItem(it.storageValue, context.getString(it.labelResId)) }, albumsSortOption.storageValue, { viewModel.setAlbumsSort(LibrarySortOption.fromStorage(it)) }, Icons.Rounded.Sort, context.getString(R.string.library_btn_sort))
                        )
                        2 -> List_options(emptyList(), ListOptionsTrailingAction.Toggle(context.getString(artistsLayoutMode.labelResId), if (artistsLayoutMode == ArtistsLayoutMode.Grid) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda, { viewModel.toggleArtistsLayout() }, context.getString(R.string.library_btn_format)))
                    }
                }
            }
        }

        // CAPA 3: Pestañas fijas con gradiente
        Box(modifier = Modifier.fillMaxWidth()) {
            val bgColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp) // Cubre las pestañas y el inicio del gradiente
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                bgColor,
                                bgColor.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )

            PillTabSelector(
                options = options,
                selectedIndex = displayedPage,
                onTabSelected = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.padding(top = 8.dp).padding(horizontal = 20.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CollapsibleLibraryOptions(progressProvider: () -> Float, content: @Composable () -> Unit) {
    // OPTIMIZACIÓN: Layout fijo. Las animaciones son solo visuales (GPU).
    Box(modifier = Modifier.fillMaxWidth().height(88.dp).clipToBounds()) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().graphicsLayer { 
            val p = progressProvider()
            // animatedP con easing cuadrático para suavizar el inicio
            val animatedP = (1f - (1f - p) * (1f - p)).coerceIn(0f, 1f)
            alpha = 1f - (animatedP * 0.95f)
            
            // EFECTO "APACHURRADO": Se comprime en Y y se expande ligeramente en X
            scaleX = 1f + (animatedP * 0.05f)
            scaleY = 1f - (animatedP * 0.50f) 
            
            // Origen arriba para que parezca que se aplasta contra el selector
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
        }) {
            content()
        }
    }
}
