package com.soundly.feature.library.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundly.data.model.Album
import com.soundly.debug.DebugRecompose
import com.soundly.feature.library.LibrarySortOption
import com.soundly.feature.library.LibraryViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.listas.FastScrollIndex
import com.soundly.ui.componentes.listas.ItemAlbum
import kotlinx.coroutines.launch

@Composable
fun AlbumsScreen(
    viewModel: LibraryViewModel,
    albums: LazyPagingItems<Album>,
    onAlbumClick: (Long) -> Unit,
    gridState: LazyGridState,
    sortOption: LibrarySortOption,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    DebugRecompose("AlbumsScreen", logEvery = 20)
    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        ScrollFadeContainer(
            gridState = gridState,
            bottomPadding = navStackHeight + 16.dp
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topPadding, 
                    bottom = navStackHeight, 
                    start = 20.dp, 
                    end = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = albums.itemCount,
                    key = albums.itemKey { it.id },
                    contentType = albums.itemContentType { "album_item" }
                ) { index ->
                    val album = albums[index] ?: return@items
                    val artUri = remember(album.id) { viewModel.getAlbumArtUri(album.id) }
                    ItemAlbum(
                        album = album,
                        caratulaUri = artUri
                    ) { onAlbumClick(album.id) }
                }
            }

/*
            if ((sortOption == LibrarySortOption.TitleAsc) || (sortOption == LibrarySortOption.TitleDesc)) {
                val scrollProgress by remember(gridState, albums.itemCount) {
                    derivedStateOf {
                        val layoutInfo = gridState.layoutInfo
                        val totalItems = albums.itemCount
                        if (totalItems == 0) 0f
                        else {
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
                FastScrollIndex(
                    items = emptyList(),
                    itemToName = { "" },
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
            }
*/
        }
    }
}
