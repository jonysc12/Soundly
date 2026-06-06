package com.soundly.feature.home

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.*
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.soundly.data.model.Album
import com.soundly.data.model.Playlist
import com.soundly.data.model.Song
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.HomeItemArtistaList
import com.soundly.ui.componentes.listas.HomeItemCancion
import com.soundly.ui.componentes.listas.ItemCancion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val listState = rememberLazyListState()
        val isScrolled by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
        val alpha by animateFloatAsState(targetValue = if (isScrolled) 1f else 0f, label = "blurAlpha")
        val hazeState = remember { HazeState() }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // 1. Descubrir álbumes
            if (uiState.discoverAlbums.isNotEmpty()) {
                item {
                    Column {
                        HomeSectionTitle("Descubrir álbumes")
                        HomeSectionSubtitle("Explora música increible ")
                    }

                    val carouselState = rememberCarouselState { uiState.discoverAlbums.size }
                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = 310.dp, // Hace que 1 ítem ocupe casi todo el ancho, y el resto se minimice
                        itemSpacing = 8.dp, // Reducimos espacio para que encaje mejor la composición
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp) // Volvemos a un alto proporcionado para hero layout
                    ) { index ->
                        val album = uiState.discoverAlbums[index]
                        HomeAlbumCarouselItem(
                            album = album,
                            artUri = viewModel.getAlbumArtUri(album.id),
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onAlbumClick(album.id) }
                        )
                    }
                }
            }

            // 2. Reproducidos recientemente
            item {
                HomeSectionTitle("Reproducido\nrecientemente")
                HomeSectionSubtitle("Continua donde te quedaste")

                BoxWithConstraints {
                    val itemWidth = (maxWidth - 16.dp * 2 - 8.dp) / 2
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.recentlyPlayed) { song ->
                            Box(modifier = Modifier.width(itemWidth)) {
                                HomeItemCancion(
                                    cancion = Cancion(
                                        caratulaUri = viewModel.getAlbumArtUri(song.albumId),
                                        titulo = song.title,
                                        artista = song.artist
                                    ),
                                    onClick = {
                                        playbackViewModel.play(song, uiState.recentlyPlayed)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Artistas más escuchados
            if (uiState.mostListenedArtists.isNotEmpty()) {
                item {
                    val windowInfo = LocalWindowInfo.current
                    val density = LocalDensity.current
                    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
                    val horizontalPadding = 32.dp
                    val spacing = 12.dp
                    val availableWidth = screenWidth - horizontalPadding

                    val preferredItemWidth = 180.dp
                    val calculatedCount = ((availableWidth + spacing).value / (preferredItemWidth + spacing).value).toInt()
                    val itemsCount = maxOf(2, calculatedCount)
                    val itemWidth = (availableWidth - (spacing * (itemsCount - 1))) / itemsCount

                    HomeSectionTitle("Top Artistas")
                    HomeSectionSubtitle("Tu artistas más escuchados")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.mostListenedArtists) { artist ->
                            HomeItemArtistaList(
                                artist = artist,
                                caratulaUri = viewModel.getArtistArtUri(artist.id),
                                modifier = Modifier.width(itemWidth),
                                onClick = { onArtistClick(artist.id) }
                            )
                        }
                    }
                }
            }

            // 4. Añadidos recientemente
            if (uiState.recentlyAdded.isNotEmpty()) {
                item {
                    HomeSectionTitle("Añadidos recientemente")
                    HomeSectionSubtitle("Tus ultimas añadidas")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recentlyAdded) { song ->
                            HomeSongCard(
                                song = song,
                                artUri = viewModel.getAlbumArtUri(song.albumId),
                                onClick = { playbackViewModel.play(song, uiState.recentlyAdded) }
                            )
                        }
                    }
                }
            }


            // 5. Recomendado para ti
            if (uiState.recommendedSongs.isNotEmpty()) {
                item {
                    HomeSectionTitle("Recomendado para ti")
                    HomeSectionSubtitle("Basado en lo que escuchas")

                    val chunkedSongs = uiState.recommendedSongs.chunked(3)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chunkedSongs) { chunk ->
                            Column(
                                modifier = Modifier.width(320.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                chunk.forEach { song ->
                                    ItemCancion(
                                        cancion = Cancion(
                                            caratulaUri = viewModel.getAlbumArtUri(song.albumId),
                                            titulo = song.title,
                                            artista = song.artist
                                        ),
                                        onClick = { playbackViewModel.play(song, uiState.recommendedSongs) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Tus Playlists
            if (uiState.userPlaylists.isNotEmpty()) {
                item {
                    HomeSectionTitle("Tus Playlists")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.userPlaylists) { playlist ->
                            HomePlaylistItem(
                                playlist = playlist,
                                artUri = playlist.artworkUri,
                                onClick = { onPlaylistClick(playlist.id) }
                            )
                        }
                    }
                }
            }
        }

        if (alpha > 0f) {
            val isDark = isSystemInDarkTheme()
            val tintColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent)
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            tint = HazeTint(tintColor.copy(alpha = 1.0f)),
                            blurRadius = 45.dp
                        )
                    )
            )
        }
    }
    }
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
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
    )
}

@Composable
fun HomeSectionSubtitle(subtitle: String) {
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CarouselItemScope.HomeAlbumCarouselItem(
    album: Album,
    artUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color.Black) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .maskClip(RoundedCornerShape(33.dp))
            .background(Color.Black)
            .fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artUri)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.carga),
                error = painterResource(R.drawable.carga),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { result ->
                    val bitmap = (result.result.drawable as BitmapDrawable).bitmap
                    scope.launch {
                        dominantColor = extractDominantColorSync(bitmap)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                dominantColor.copy(alpha = 0.2f),
                                dominantColor.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            if (width > 80.dp) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(33.dp)
                        .size(24.dp)
                )
            }

            if (width > 160.dp) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(33.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,

                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                    Box(
                        modifier = Modifier
                            .size(59.dp)
                            // 1. Usar la forma expresiva Square oficial para la sombra
                            .shadow(
                                elevation = 8.dp,
                                shape = MaterialShapes.Square.toShape()
                            )
                            // 2. Aplicar exactamente la misma forma al fondo
                            .background(
                                color = dominantColor,
                                shape = MaterialShapes.Square.toShape()
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSongCard(
    song: Song,
    artUri: Uri,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = artUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .shadow(4.dp)
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
fun HomePlaylistItem(
    playlist: Playlist,
    artUri: Uri?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artUri ?: R.drawable.carga,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

suspend fun extractDominantColorSync(bitmap: Bitmap): Color {
    return withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()
        val colorInt = palette.getDominantColor(0)
        if (colorInt == 0) Color.Black else Color(colorInt)
    }
}
