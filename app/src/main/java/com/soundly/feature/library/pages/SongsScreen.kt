package com.soundly.feature.library.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.data.model.Song
import com.soundly.debug.DebugRecompose
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenuButton
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.ScrollFadeContainer
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.ItemCancion

@Composable
fun SongsScreen(
    viewModel: LibraryViewModel,
    songs: List<Song>,
    listState: LazyListState,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAlbum: (Long) -> Unit = {},
    onOpenArtist: (Long) -> Unit = {}
) {
    DebugRecompose("SongsScreen", logEvery = 20)
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val favoriteSongIds = viewModel.favoriteSongIds.collectAsState().value
    val userPlaylists = viewModel.userPlaylists.collectAsState().value
    val playlistMembershipBySong = viewModel.playlistMembershipBySong.collectAsState().value

    ScrollFadeContainer(listState = listState) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(songs, key = { it.id }) { song ->
                val artUri = remember(song.albumId) { viewModel.getAlbumArtUri(song.albumId) }
                ItemCancion(
                    cancion = Cancion(
                        caratulaUri = artUri,
                        titulo = song.title,
                        artista = song.artist
                    ),
                    onClick = { onPlaySong(song, songs) },
                    menuContent = {
                        SongOverflowMenuButton(
                            song = song,
                            source = SongMenuSource.Library,
                            userPlaylists = userPlaylists,
                            playlistIdsContainingSong = playlistMembershipBySong[song.id].orEmpty(),
                            isFavorite = song.id in favoriteSongIds,
                            onPlayNext = { playbackViewModel.playNext(song) },
                            onAddToQueue = { playbackViewModel.addToQueue(song) },
                            onOpenAlbum = onOpenAlbum,
                            onOpenArtist = onOpenArtist,
                            onAddToPlaylist = { playlistId ->
                                viewModel.addSongToPlaylist(playlistId, song.id)
                            },
                            onAddToFavorites = {
                                viewModel.toggleSongFavorite(song.id)
                            },
                            onDeleteSong = {
                                viewModel.hideSong(song.id)
                            }
                        )
                    }
                )
            }
        }
    }
}
