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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.soundly.debug.DebugRecompose
import com.soundly.feature.library.pages.AlbumDetailScreen
import com.soundly.feature.library.pages.AlbumsScreen
import com.soundly.feature.library.pages.ArtistDetailScreen
import com.soundly.feature.library.pages.ArtistsScreen
import com.soundly.feature.library.pages.SongsScreen
import com.soundly.ui.componentes.ListOptionsLeadingAction
import com.soundly.ui.componentes.ListOptionsMenuItem
import com.soundly.ui.componentes.ListOptionsTrailingAction
import com.soundly.ui.componentes.List_options
import com.soundly.ui.componentes.PillTabSelector
import kotlinx.coroutines.launch

private val OPTIONS = listOf("Canciones", "Álbumes", "Artistas")

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
    isHostPageVisible: Boolean,
    onPlaySong: (com.soundly.data.model.Song, List<com.soundly.data.model.Song>) -> Unit,
    onPlayCollection: (List<com.soundly.data.model.Song>, Boolean) -> Unit
) {
    DebugRecompose("LibraryPage", logEvery = 15)
    val detailStack = rememberSaveable(
        saver = detailStackSaver
    ) {
        mutableStateListOf<LibraryDetailDestination>()
    }
    val pagerState = rememberPagerState { OPTIONS.size }
    val songsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val artistsListState = rememberLazyListState()
    val currentDetail = detailStack.lastOrNull()
    val isDetailVisible = currentDetail != null
    val isArtistDetailVisible = currentDetail is LibraryDetailDestination.Artist

    val popDetail: () -> Unit = remember(detailStack) {
        {
            if (detailStack.isNotEmpty()) {
                detailStack.removeAt(detailStack.lastIndex)
            }
        }
    }
    val openDetail: (LibraryDetailDestination) -> Unit = remember(detailStack) {
        { destination ->
            if (detailStack.lastOrNull() != destination) {
                detailStack.add(destination)
            }
        }
    }

    LaunchedEffect(isDetailVisible) {
        onAlbumDetailVisibilityChanged(isDetailVisible)
    }
    LaunchedEffect(isArtistDetailVisible) {
        onArtistEdgeToEdgeChanged(isArtistDetailVisible)
    }

    DisposableEffect(Unit) {
        onDispose {
            onAlbumDetailVisibilityChanged(false)
            onArtistEdgeToEdgeChanged(false)
        }
    }

    BackHandler(enabled = isDetailVisible, onBack = popDetail)

    AnimatedContent(
        targetState = currentDetail,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "LibraryContent"
    ) { destination ->
        when (destination) {
            is LibraryDetailDestination.Album -> {
                AlbumDetailScreen(
                    albumId = destination.id,
                    viewModel = viewModel,
                    onAlbumClick = { openDetail(LibraryDetailDestination.Album(it)) },
                    onArtistClick = { openDetail(LibraryDetailDestination.Artist(it)) },
                    onBack = popDetail,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection
                )
            }

            is LibraryDetailDestination.Artist -> {
                ArtistDetailScreen(
                    artistId = destination.id,
                    viewModel = viewModel,
                    onAlbumClick = { openDetail(LibraryDetailDestination.Album(it)) },
                    onArtistClick = { openDetail(LibraryDetailDestination.Artist(it)) },
                    onBack = popDetail,
                    applySystemBarStyle = isHostPageVisible,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection
                )
            }

            null -> {
                LibraryMainContent(
                    viewModel = viewModel,
                    pagerState = pagerState,
                    songsListState = songsListState,
                    albumsGridState = albumsGridState,
                    artistsGridState = artistsGridState,
                    artistsListState = artistsListState,
                    onAlbumClick = { openDetail(LibraryDetailDestination.Album(it)) },
                    onArtistClick = { openDetail(LibraryDetailDestination.Artist(it)) },
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryMainContent(
    viewModel: LibraryViewModel,
    pagerState: PagerState,
    songsListState: LazyListState,
    albumsGridState: LazyGridState,
    artistsGridState: LazyGridState,
    artistsListState: LazyListState,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaySong: (com.soundly.data.model.Song, List<com.soundly.data.model.Song>) -> Unit,
    onPlayCollection: (List<com.soundly.data.model.Song>, Boolean) -> Unit
) {
    DebugRecompose("LibraryMainContent", logEvery = 20)
    LaunchedEffect(Unit) {
        viewModel.refreshLibraryData()
    }
    val coroutineScope = rememberCoroutineScope()
    val songs by viewModel.librarySongs.collectAsState()
    val albums by viewModel.libraryAlbums.collectAsState()
    val artists by viewModel.libraryArtists.collectAsState()
    val albumsPlaybackQueue by viewModel.albumsPlaybackQueue.collectAsState()
    val songsSortOption by viewModel.songsSortOption.collectAsState()
    val albumsSortOption by viewModel.albumsSortOption.collectAsState()
    val artistsLayoutMode by viewModel.artistsLayoutMode.collectAsState()

    val displayedPage by remember {
        derivedStateOf {
            if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
        }
    }
    val optionsCollapseProgress by remember(
        displayedPage,
        songsListState,
        albumsGridState,
        artistsGridState,
        artistsListState,
        artistsLayoutMode
    ) {
        derivedStateOf {
            when (displayedPage) {
                0 -> songsListState.headerCollapseProgress()
                1 -> albumsGridState.headerCollapseProgress()
                2 -> when (artistsLayoutMode) {
                    ArtistsLayoutMode.Grid -> artistsGridState.headerCollapseProgress()
                    ArtistsLayoutMode.List -> artistsListState.headerCollapseProgress()
                }

                else -> 0f
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        PillTabSelector(
            options = OPTIONS,
            selectedIndex = displayedPage,
            onTabSelected = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        CollapsibleLibraryOptions(collapseProgress = optionsCollapseProgress) {
            when (displayedPage) {
                0 -> List_options(
                    leadingActions = listOf(
                        ListOptionsLeadingAction(
                            icon = Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir canciones"
                        ) {
                            onPlayCollection(songs, false)
                        },
                        ListOptionsLeadingAction(
                            icon = Icons.Rounded.Shuffle,
                            contentDescription = "Reproducir canciones en aleatorio"
                        ) {
                            onPlayCollection(songs, true)
                        }
                    ),
                    trailingAction = ListOptionsTrailingAction.Menu(
                        label = songsSortOption.label,
                        options = LibrarySortOption.entries.map { option ->
                            ListOptionsMenuItem(
                                id = option.storageValue,
                                label = option.label
                            )
                        },
                        selectedOptionId = songsSortOption.storageValue,
                        onOptionSelected = { optionId ->
                            val option = LibrarySortOption.fromStorage(optionId)
                            if (option != songsSortOption) {
                                viewModel.setSongsSort(option)
                            }
                        },
                        contentDescription = "Ordenar canciones"
                    )
                )

                1 -> List_options(
                    leadingActions = listOf(
                        ListOptionsLeadingAction(
                            icon = Icons.Rounded.Shuffle,
                            contentDescription = "Reproducir álbumes en aleatorio"
                        ) {
                            onPlayCollection(albumsPlaybackQueue, true)
                        }
                    ),
                    trailingAction = ListOptionsTrailingAction.Menu(
                        label = albumsSortOption.label,
                        options = LibrarySortOption.entries.map { option ->
                            ListOptionsMenuItem(
                                id = option.storageValue,
                                label = option.label
                            )
                        },
                        selectedOptionId = albumsSortOption.storageValue,
                        onOptionSelected = { optionId ->
                            val option = LibrarySortOption.fromStorage(optionId)
                            if (option != albumsSortOption) {
                                viewModel.setAlbumsSort(option)
                            }
                        },
                        contentDescription = "Ordenar álbumes"
                    )
                )

                2 -> List_options(
                    leadingActions = emptyList(),
                    trailingAction = ListOptionsTrailingAction.Toggle(
                        label = artistsLayoutMode.label,
                        icon = when (artistsLayoutMode) {
                            ArtistsLayoutMode.Grid -> Icons.Rounded.GridView
                            ArtistsLayoutMode.List -> Icons.Rounded.ViewAgenda
                        },
                        onClick = { viewModel.toggleArtistsLayout() },
                        contentDescription = "Cambiar formato de artistas"
                    )
                )

                else -> Unit
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> SongsScreen(
                    viewModel = viewModel,
                    songs = songs,
                    listState = songsListState,
                    onPlaySong = onPlaySong,
                    onOpenAlbum = onAlbumClick,
                    onOpenArtist = onArtistClick
                )

                1 -> AlbumsScreen(
                    viewModel = viewModel,
                    albums = albums,
                    onAlbumClick = onAlbumClick,
                    gridState = albumsGridState
                )

                2 -> ArtistsScreen(
                    viewModel = viewModel,
                    artists = artists,
                    onArtistClick = onArtistClick,
                    gridState = artistsGridState,
                    listState = artistsListState,
                    layoutMode = artistsLayoutMode
                )

                else -> Text("")
            }
        }
    }
}

@Composable
private fun CollapsibleLibraryOptions(
    collapseProgress: Float,
    content: @Composable () -> Unit
) {
    val easedProgress = collapseProgress
        .let { progress -> 1f - (1f - progress) * (1f - progress) }
        .coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = easedProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "library_options_progress"
    )
    val currentHeight by animateDpAsState(
        targetValue = lerp(88.dp, 0.dp, animatedProgress),
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "library_options_height"
    )
    val currentSpacerHeight by animateDpAsState(
        targetValue = lerp(18.dp, 6.dp, animatedProgress),
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "library_options_spacer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(currentHeight)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = 1f - (animatedProgress * 0.92f)
                    translationY = -18.dp.toPx() * animatedProgress
                    scaleX = 1f - (animatedProgress * 0.03f)
                    scaleY = 1f - (animatedProgress * 0.05f)
                }
        ) {
            content()
        }
    }

    Spacer(modifier = Modifier.height(currentSpacerHeight))
}

private fun LazyListState.headerCollapseProgress(
    collapseDistance: Dp = 72.dp
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    return (firstVisibleItemScrollOffset.toFloat() / collapseDistance.value)
        .coerceIn(0f, 1f)
}

private fun LazyGridState.headerCollapseProgress(
    collapseDistance: Dp = 72.dp
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    return (firstVisibleItemScrollOffset.toFloat() / collapseDistance.value)
        .coerceIn(0f, 1f)
}
