package com.soundly.feature.library.pages

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundly.data.model.Artist
import com.soundly.debug.DebugRecompose
import com.soundly.debug.perfMark
import com.soundly.feature.library.ArtistsLayoutMode
import com.soundly.feature.library.LibraryViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.listas.FastScrollIndex
import com.soundly.ui.componentes.listas.ItemArtista
import com.soundly.ui.componentes.listas.ItemArtistaList
import kotlinx.coroutines.launch

@Composable
fun ArtistsScreen(
    viewModel: LibraryViewModel,
    artists: LazyPagingItems<Artist>,
    onArtistClick: (Long) -> Unit,
    gridState: LazyGridState,
    listState: LazyListState,
    layoutMode: ArtistsLayoutMode,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    DebugRecompose("ArtistsScreen", logEvery = 20)
    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    val artistPrimaryAlbumId by viewModel.artistPrimaryAlbumId.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        when (layoutMode) {
            ArtistsLayoutMode.Grid -> {
                ScrollFadeContainer(
                    gridState = gridState,
                    bottomPadding = navStackHeight + 16.dp
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = navStackHeight, 
                            start = 20.dp, 
                            end = 20.dp, 
                            top = topPadding
                        )
                    ) {
                        items(
                            count = artists.itemCount,
                            key = artists.itemKey { it.id },
                            contentType = artists.itemContentType { "artist_grid_item" }
                        ) { index ->
                            val artist = artists[index] ?: return@items
                            
                            ItemArtista(
                                artist = artist,
                                caratulaUri = artist.artworkUri
                            ) { onArtistClick(artist.id) }
                        }
                    }

                    val scrollProgress by remember(layoutMode, artists.itemCount, gridState) {
                        derivedStateOf {
                            val totalItems = artists.itemCount
                            if (totalItems == 0) 0f
                            else {
                                val layoutInfo = gridState.layoutInfo
                                val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                                if (firstVisibleItem == null) 0f
                                else {
                                    val itemFraction = if (firstVisibleItem.size.height > 0) {
                                        (-firstVisibleItem.offset.y).toFloat() / firstVisibleItem.size.height
                                    } else 0f
                                    (firstVisibleItem.index / 2f + itemFraction) / (totalItems / 2f)
                                }
                            }
                        }
                    }

                    /*
                    FastScrollIndex(
                        items = artists,
                        itemToName = { (it as Artist).name },
                        scrollProgress = scrollProgress.coerceIn(0f, 1f),
                        onScrollRequest = { index ->
                            coroutineScope.launch {
                                gridState.scrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                    */
                }
            }

            ArtistsLayoutMode.List -> {
                ScrollFadeContainer(
                    listState = listState,
                    bottomPadding = navStackHeight + 16.dp
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = navStackHeight, 
                            start = 20.dp, 
                            end = 20.dp, 
                            top = topPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            count = artists.itemCount,
                            key = artists.itemKey { it.id },
                            contentType = artists.itemContentType { "artist_list_item" }
                        ) { index ->
                            val artist = artists[index] ?: return@items

                            ItemArtistaList(
                                artist = artist,
                                caratulaUri = artist.artworkUri,
                                onClick = { onArtistClick(artist.id) }
                            )
                        }
                    }

                    val scrollProgress by remember(layoutMode, artists.itemCount, listState) {
                        derivedStateOf {
                            val totalItems = artists.itemCount
                            if (totalItems == 0) 0f
                            else {
                                val layoutInfo = listState.layoutInfo
                                val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                                if (firstVisibleItem == null) 0f
                                else {
                                    val itemFraction = if (firstVisibleItem.size > 0) {
                                        (-firstVisibleItem.offset).toFloat() / firstVisibleItem.size
                                    } else 0f
                                    (firstVisibleItem.index + itemFraction) / totalItems.toFloat()
                                }
                            }
                        }
                    }

                    /*
                    FastScrollIndex(
                        items = artists,
                        itemToName = { (it as Artist).name },
                        scrollProgress = scrollProgress.coerceIn(0f, 1f),
                        onScrollRequest = { index ->
                            coroutineScope.launch {
                                listState.scrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                    */
                }
            }
        }
    }
}
