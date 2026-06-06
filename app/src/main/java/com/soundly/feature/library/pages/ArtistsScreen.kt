package com.soundly.feature.library.pages

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundly.data.model.Artist
import com.soundly.debug.DebugRecompose
import com.soundly.debug.perfMark
import com.soundly.feature.library.ArtistsLayoutMode
import com.soundly.feature.library.LibraryViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.listas.ItemArtista
import com.soundly.ui.componentes.listas.ItemArtistaList
import android.os.SystemClock

@Composable
fun ArtistsScreen(
    viewModel: LibraryViewModel,
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    gridState: LazyGridState,
    listState: LazyListState,
    layoutMode: ArtistsLayoutMode
) {
    DebugRecompose("ArtistsScreen", logEvery = 20)
    when (layoutMode) {
        ArtistsLayoutMode.Grid -> {
            ScrollFadeContainer(gridState = gridState) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        val start = SystemClock.elapsedRealtimeNanos()
                        val caratulaUri = remember(artist.id) {
                            viewModel.getArtistArtUri(artist.id)
                        }
                        val durationMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                        perfMark("ArtistsScreen.getArtistArtUri.grid id=${artist.id} took ${"%.2f".format(durationMs)} ms")

                        ItemArtista(
                            artist = artist,
                            caratulaUri = caratulaUri,
                            onClick = { onArtistClick(artist.id) }
                        )
                    }
                }
            }
        }

        ArtistsLayoutMode.List -> {
            ScrollFadeContainer(listState = listState) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        val start = SystemClock.elapsedRealtimeNanos()
                        val caratulaUri = remember(artist.id) {
                            viewModel.getArtistArtUri(artist.id)
                        }
                        val durationMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                        perfMark("ArtistsScreen.getArtistArtUri.list id=${artist.id} took ${"%.2f".format(durationMs)} ms")

                        ItemArtistaList(
                            artist = artist,
                            caratulaUri = caratulaUri,
                            onClick = { onArtistClick(artist.id) }
                        )
                    }
                }
            }
        }
    }
}
