package com.soundly.feature.library.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.data.model.Song
import com.soundly.debug.DebugRecompose
import com.soundly.feature.library.LibrarySortOption
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenuButton
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.listas.Cancion
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundly.ui.componentes.listas.FastScrollIndex
import com.soundly.ui.componentes.listas.ItemCancion
import kotlinx.coroutines.launch

@Composable
fun SongsScreen(
    viewModel: LibraryViewModel,
    songs: LazyPagingItems<Song>,
    listState: LazyListState,
    onPlaySong: (Song, List<Song>) -> Unit,
    sortOption: LibrarySortOption,
    onOpenAlbum: (Long) -> Unit = {},
    onOpenArtist: (Long) -> Unit = {},
    onViewQueue: () -> Unit = {},
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    DebugRecompose("SongsScreen", logEvery = 20)
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistMembershipBySong by viewModel.playlistMembershipBySong.collectAsStateWithLifecycle()
    val allSongs by viewModel.songs.collectAsStateWithLifecycle()
    
    // OPTIMIZACIÓN: Pre-calcular la cola de reproducción completa para evitar 
    // iteraciones pesadas en el hilo de UI durante el clic.
    val fullQueue by remember(allSongs, sortOption) {
        derivedStateOf {
            viewModel.sortSongsForDisplay(allSongs, sortOption)
        }
    }

    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        ScrollFadeContainer(
            listState = listState,
            bottomPadding = navStackHeight + 16.dp
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = topPadding,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = navStackHeight
                )
            ) {
                items(
                    count = songs.itemCount,
                    key = songs.itemKey { it.id },
                    contentType = songs.itemContentType { "song_item" }
                ) { index ->
                    val song = songs[index] ?: return@items
                    val artUri = remember(song.albumId) { viewModel.getAlbumArtUri(song.albumId) }
                    val cancion = remember(artUri, song.title, song.artist) {
                        Cancion(
                            caratulaUri = artUri,
                            titulo = song.title,
                            artista = song.artist
                        )
                    }
                    ItemCancion(
                        cancion = cancion,
                        onClick = {
                            onPlaySong(song, fullQueue)
                        },
                        menuContent = {
                            SongOverflowMenuButton(
                                song = song,
                                source = SongMenuSource.Library,
                                userPlaylists = userPlaylists,
                                playlistIdsContainingSong = playlistMembershipBySong[song.id] ?: emptySet(),
                                isFavorite = song.id in favoriteSongIds,
                                onPlayNext = { playbackViewModel.playNext(song) },
                                onAddToQueue = { playbackViewModel.addToQueue(song) },
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                onAddToPlaylist = { playlistId ->
                                    viewModel.addSongToPlaylist(playlistId, song.id)
                                },
                                onToggleFavorite = {
                                    viewModel.toggleSongFavorite(song.id)
                                },
                                onDeleteSong = {
                                    viewModel.hideSong(song.id)
                                },
                                onViewQueue = onViewQueue
                            )
                        }
                    )
                }
            }

/*
            if ((sortOption == LibrarySortOption.TitleAsc) || (sortOption == LibrarySortOption.TitleDesc)) {
                val scrollProgress by remember(listState, songs.itemCount) {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val totalItems = songs.itemCount
                        if (totalItems == 0) 0f
                        else {
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
                FastScrollIndex(
                    items = emptyList(), // Temporalmente deshabilitado para Paging
                    itemToName = { "" },
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
            }
*/
        }
    }
}
