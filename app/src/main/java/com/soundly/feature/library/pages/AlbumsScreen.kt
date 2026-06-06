package com.soundly.feature.library.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundly.data.model.Album
import com.soundly.debug.DebugRecompose
import com.soundly.feature.library.LibraryViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.listas.ItemAlbum

@Composable
fun AlbumsScreen(
    viewModel: LibraryViewModel,
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    gridState: LazyGridState
) {
    DebugRecompose("AlbumsScreen", logEvery = 20)
    ScrollFadeContainer(gridState = gridState) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                val artUri = remember(album.id) { viewModel.getAlbumArtUri(album.id) }
                ItemAlbum(
                    album = album,
                    caratulaUri = artUri,
                    onClick = { onAlbumClick(album.id) }
                )
            }
        }
    }
}
