package com.soundly.feature.library.pages

import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.soundly.R
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Song
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenuButton
import com.soundly.player.PlaybackViewModel
import com.soundly.ui.componentes.agslFrostedGlass
import com.soundly.ui.componentes.listas.Cancion
import com.soundly.ui.componentes.listas.ItemAlbum
import com.soundly.ui.componentes.listas.ItemCancionAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SONGS_PREVIEW_COUNT = 5

private const val ARTIST_WAVE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 color1;
    layout(color) uniform half4 color2;
    layout(color) uniform half4 color3;
    layout(color) uniform half4 bgColor;

    float2 hash(float2 p) {
        p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
        return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(dot(hash(i + float2(0.0, 0.0)), f - float2(0.0, 0.0)),
                       dot(hash(i + float2(1.0, 0.0)), f - float2(1.0, 0.0)), u.x),
                   mix(dot(hash(i + float2(0.0, 1.0)), f - float2(0.0, 1.0)),
                       dot(hash(i + float2(1.0, 1.0)), f - float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float t = iTime;
        
        // Mezcla fluida de los 3 colores del artista
        float n = noise(uv * 1.5 + t * 0.1);
        float n2 = noise(uv * 2.2 - t * 0.05);
        float mixVal = smoothstep(-0.2, 0.8, n + n2);
        half4 fluidColor = mix(color1, color2, mixVal);
        
        float n3 = noise(uv * 3.0 + t * 0.1);
        fluidColor = mix(fluidColor, color3, smoothstep(0.0, 1.0, n3) * 0.6);
        
        // Olas superiores: Multiplicadores enteros (1.0, 2.0) para loop perfecto
        float waveTop = sin(uv.x * 4.0 + t) * 0.1 + sin(uv.x * 8.0 - t * 2.0) * 0.03;
        float maskTop = smoothstep(0.0, 0.4, uv.y + waveTop - 0.2);
        
        // Olas inferiores: Multiplicadores enteros (1.0, 2.0) para loop perfecto
        float waveBot = sin(uv.x * 4.5 + t) * 0.09 + sin(uv.x * 9.0 - t * 2.0) * 0.03;
        // Rango de seguridad ampliado para evitar recortes
        float maskBot = smoothstep(0.5, 0.85, uv.y + waveBot - 0.05);
        
        // Composición final
        half4 colorWithTopMask = fluidColor * maskTop;
        return mix(colorWithTopMask, bgColor, maskBot);
    }
"""


@Composable
private fun rememberArtistColors(uri: Uri?): List<Color> {
    val context = LocalContext.current
    var colors by remember { mutableStateOf(listOf(Color.Transparent, Color.Transparent, Color.Transparent)) }

    LaunchedEffect(uri) {
        if (uri == null) {
            colors = listOf(Color.Transparent, Color.Transparent, Color.Transparent)
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(uri)
                    .size(128)
                    .allowHardware(false)
                    .build()
                val success = context.imageLoader.execute(req) as? SuccessResult ?: return@runCatching null
                val palette = Palette.Builder(success.drawable.toBitmap())
                    .maximumColorCount(16)
                    .generate()
                
                val dominant = palette.dominantSwatch?.rgb ?: palette.vibrantSwatch?.rgb ?: 0
                val hsl = FloatArray(3)
                androidx.core.graphics.ColorUtils.colorToHSL(dominant, hsl)
                
                val base = Color(dominant)
                
                // Variante clara: Aumentamos luminosidad significativamente
                val lightHsl = hsl.copyOf()
                lightHsl[2] = (lightHsl[2] + 0.35f).coerceAtMost(0.95f)
                lightHsl[1] = (lightHsl[1] * 0.7f) // Un poco más pastel
                val light = Color(androidx.core.graphics.ColorUtils.HSLToColor(lightHsl))
                
                // Variante saturada: Aumentamos saturación al máximo y ajustamos brillo
                val satHsl = hsl.copyOf()
                satHsl[1] = (satHsl[1] + 0.50f).coerceAtMost(1.0f)
                satHsl[2] = (satHsl[2] * 0.8f).coerceAtLeast(0.4f)
                val sat = Color(androidx.core.graphics.ColorUtils.HSLToColor(satHsl))
                
                listOf(base, light, sat)
            }.getOrNull()
        }
        result?.let { colors = it }
    }
    return colors
}

private fun artistNameGray(dominant: Color, isDark: Boolean): Color {
    if (dominant == Color.Transparent) return if (isDark) Color.White else Color.Black
    val lum = dominant.luminance()
    val grayValue = when {
        lum < 0.30f -> 0.92f
        lum > 0.65f -> 0.12f
        else -> 0.92f - ((lum - 0.30f) / (0.65f - 0.30f)) * (0.92f - 0.12f)
    }
    return Color(grayValue, grayValue, grayValue)
}

private fun adaptAccentColor(color: Color, isDark: Boolean): Color {
    val lum = color.luminance()
    return when {
        isDark && lum < 0.20f -> lerpColor(color, Color.White, 0.35f)
        !isDark && lum > 0.75f -> lerpColor(color, Color.Black, 0.35f)
        else -> color
    }
}

private fun DrawScope.drawAnimatedWave(color: Color, waveHeight: Float, time: Float) {
    val w = size.width
    val h = size.height
    val path = Path()
    
    path.moveTo(0f, h)
    path.lineTo(0f, h - waveHeight)
    
    val segments = 40
    val segmentWidth = w / segments
    for (i in 0..segments) {
        val x = i * segmentWidth
        val relativeX = i.toFloat() / segments
        
        // Multiplicadores enteros para loop perfecto en fallback
        val yOffset = kotlin.math.sin(relativeX * 4.5f + time) * waveHeight * 0.8f +
                      kotlin.math.sin(relativeX * 9f - time * 2f) * waveHeight * 0.2f
        
        path.lineTo(x, h - waveHeight + yOffset.toFloat())
    }
    
    path.lineTo(w, h)
    path.close()
    drawPath(path = path, color = color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    viewModel: LibraryViewModel,
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onViewQueue: () -> Unit = {},
    applySystemBarStyle: Boolean = true,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayCollection: (List<Song>, Boolean) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current

    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val favoriteArtistIds by viewModel.favoriteArtistIds.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistMembershipBySong by viewModel.playlistMembershipBySong.collectAsStateWithLifecycle()

    val artist by remember(artists, artistId) {
        derivedStateOf { artists.find { it.id == artistId } }
    }
    val artistSongs by viewModel.getSongsForArtist(artistId).collectAsStateWithLifecycle(initialValue = emptyList())

    val artistAlbums = remember(albums, artist?.name) {
        val currentArtistName = artist?.name ?: return@remember emptyList<Album>()
        albums.filter { album ->
            com.soundly.data.model.splitArtistNames(album.artist)
                .any { it.equals(currentArtistName, ignoreCase = true) }
        }.sortedBy { it.name.lowercase() }
    }
    val artistName = artist?.name ?: stringResource(R.string.library_label_artist)
    val isFavorite = artistId in favoriteArtistIds

    var songsExpanded by remember { mutableStateOf(false) }
    val hasMoreSongs = remember(artistSongs) { artistSongs.size > SONGS_PREVIEW_COUNT }
    val visibleSongs by remember(artistSongs, songsExpanded) {
        derivedStateOf {
            if (songsExpanded || !hasMoreSongs) artistSongs else artistSongs.take(SONGS_PREVIEW_COUNT)
        }
    }

    val artistArtUri = remember(artist, artists) { artist?.let { viewModel.getArtistArtUri(it.id) } }
    val artistInfo by viewModel.currentArtistInfo.collectAsStateWithLifecycle()

    var showArtistOptions by remember { mutableStateOf(false) }

    val unknownArtist = stringResource(R.string.unknown_artist)
    LaunchedEffect(artistName) {
        if (artistName.isNotBlank() && artistName != unknownArtist) {
            viewModel.fetchArtistInfo(artistName)
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val background = MaterialTheme.colorScheme.background

    val artistColors = rememberArtistColors(uri = artistArtUri)
    val rawDominant = artistColors[0]
    val dominantColor by animateColorAsState(
        rawDominant,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "dominantColor"
    )

    val hasColor = dominantColor != Color.Transparent
    val artistNameColor = remember(dominantColor, isDark) { artistNameGray(dominantColor, isDark) }
    val accentColor = remember(dominantColor, hasColor, isDark) {
        if (hasColor) adaptAccentColor(dominantColor, isDark) else null
    }

    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableIntStateOf(0) }

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val topBarHeightPx = statusBarHeightPx + with(density) { 56.dp.toPx() }

    val scrollProgressLambda = remember(listState, headerHeightPx, topBarHeightPx) {
        {
            if (headerHeightPx == 0) 0f
            else {
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "artist_header" }
                if (item == null) 1f
                else {
                    val scrolled = -item.offset.toFloat()
                    (scrolled / (headerHeightPx - topBarHeightPx)).coerceIn(0f, 1f)
                }
            }
        }
    }

    val backIconOnImage = remember(dominantColor) {
        if (dominantColor == Color.Transparent || dominantColor.luminance() < 0.4f) Color.White else Color.Black
    }

    // System bars logic
    val view = LocalView.current
    val activityWindow = remember(view) { (view.context as? android.app.Activity)?.window }
    val insetsController = remember(activityWindow, view) {
        activityWindow?.let { WindowCompat.getInsetsController(it, view) }
    }

    if (applySystemBarStyle && !view.isInEditMode && activityWindow != null && insetsController != null) {
        val wantLightIcons = remember(dominantColor, scrollProgressLambda(), hasColor, isDark) {
            if (scrollProgressLambda() >= 1f) isDark else if (hasColor) dominantColor.luminance() < 0.4f else true
        }

        val previousStatusBarColor = remember(activityWindow) { activityWindow.statusBarColor }
        val previousLightStatusBars = remember(insetsController) { insetsController.isAppearanceLightStatusBars }

        SideEffect {
            activityWindow.statusBarColor = android.graphics.Color.TRANSPARENT
            insetsController.isAppearanceLightStatusBars = !wantLightIcons
        }

        DisposableEffect(activityWindow, insetsController) {
            onDispose {
                activityWindow.statusBarColor = previousStatusBarColor
                insetsController.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
    }

    var overscrollPx by remember { mutableFloatStateOf(0f) }
    val maxOverscrollPx = with(density) { 140.dp.toPx() }

    val elasticScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollPx <= 0f || available.y >= 0f) return Offset.Zero
                val consumed = available.y.coerceAtLeast(-overscrollPx)
                overscrollPx = (overscrollPx + consumed).coerceAtLeast(0f)
                return Offset(0f, consumed)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                overscrollPx = (overscrollPx + available.y * 0.4f).coerceIn(0f, maxOverscrollPx)
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                animate(
                    overscrollPx,
                    0f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                ) { v, _ -> overscrollPx = v }
                return Velocity.Zero
            }
        }
    }

    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val contentWidth = with(density) { windowInfo.containerSize.width.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(elasticScroll)
            .graphicsLayer { translationY = overscrollPx }
    ) {
        val actualIsLandscape = isLandscape || contentWidth > 600.dp

        ArtistBackgroundHeader(
            artistArtUri = artistArtUri,
            artistColors = artistColors,
            dominantColor = dominantColor,
            hasColor = hasColor,
            isDark = isDark,
            backgroundColor = background,
            scrollProgress = scrollProgressLambda,
            headerHeightPx = headerHeightPx,
            contentWidth = contentWidth,
            isLandscape = actualIsLandscape
        )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = navStackHeight + 16.dp)
            ) {
                item(key = "artist_header", contentType = "header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { headerHeightPx = it.size.height }
                    ) {
                        Spacer(modifier = Modifier.aspectRatio(if (actualIsLandscape) 2.2f else 0.95f).fillMaxWidth())

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = if (actualIsLandscape) 12.dp else 28.dp)
                                .graphicsLayer {
                                    alpha = (1f - scrollProgressLambda() * 2.5f).coerceIn(0f, 1f)
                                }
                        ) {
                            val countRes = if (artistSongs.size == 1) R.string.song_count_singular else R.string.songs_count
                            Spacer(Modifier.height(if (actualIsLandscape) 16.dp else 48.dp))
                            Text(
                            text = stringResource(countRes, artistSongs.size),
                            style = if (actualIsLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = artistNameColor.copy(alpha = 0.82f)
                        )
                        }
                    }
                }

                item(key = "artist_actions", contentType = "actions") {
                    ArtistActionBar(
                        accentColor = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        isFavorite = isFavorite,
                        onToggleFavorite = { viewModel.toggleArtistFavorite(artistId) },
                        onShowOptions = { showArtistOptions = true },
                        onShuffle = { onPlayCollection(artistSongs, true) },
                        onPlay = { onPlayCollection(artistSongs, false) }
                    )
                }

                item(key = "songs_title", contentType = "section_title") {
                    SectionTitle(stringResource(R.string.library_option_songs))
                }

                itemsIndexed(
                    items = visibleSongs,
                    key = { index, s -> "${s.id}_$index" },
                    contentType = { _, _ -> "song_item" }
                ) { index, song ->
                    val cancion = remember(song.title, song.artist, song.albumId) {
                        Cancion(
                            caratulaUri = viewModel.getAlbumArtUri(song.albumId),
                            titulo = song.title,
                            artista = song.artist
                        )
                    }

                    ItemCancionAlbum(
                        cancion = cancion,
                        trackNumber = index + 1,
                        onClick = { onPlaySong(song, artistSongs) },
                        menuContent = {
                            SongOverflowMenuButton(
                                song = song,
                                source = SongMenuSource.Library,
                                userPlaylists = userPlaylists,
                                playlistIdsContainingSong = playlistMembershipBySong[song.id] ?: emptySet(),
                                isFavorite = song.id in favoriteSongIds,
                                onPlayNext = { playbackViewModel.playNext(song) },
                                onAddToQueue = { playbackViewModel.addToQueue(song) },
                                onOpenAlbum = onAlbumClick,
                                onOpenArtist = onArtistClick,
                                onAddToPlaylist = { playlistId ->
                                    viewModel.addSongToPlaylist(playlistId, song.id)
                                },
                                onToggleFavorite = { viewModel.toggleSongFavorite(song.id) },
                                onDeleteSong = { viewModel.hideSong(song.id) },
                                onViewQueue = onViewQueue
                            )
                        }
                    )
                }

                if (hasMoreSongs) {
                    item(key = "songs_expand_toggle", contentType = "expand_button") {
                        ShowMoreSongsButton(
                            expanded = songsExpanded,
                            remainingCount = artistSongs.size - SONGS_PREVIEW_COUNT,
                            accentColor = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            onToggle = { songsExpanded = !songsExpanded }
                        )
                    }
                }

                if (artistAlbums.isNotEmpty()) {
                    item(key = "albums_title", contentType = "section_title") {
                        SectionTitle(stringResource(R.string.library_option_albums), topPadding = 24.dp)
                    }
                    item(key = "albums_row", contentType = "albums_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = artistAlbums,
                                key = { it.id },
                                contentType = { "artist_album" }
                            ) { album ->
                                Box(Modifier.width(160.dp)) {
                                    ItemAlbum(
                                        album = album,
                                        caratulaUri = viewModel.getAlbumArtUri(album.id),
                                        onClick = { onAlbumClick(album.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (artistInfo.name.isNotBlank() && !artistInfo.isLoading) {
                    item(key = "artist_bio", contentType = "bio") {
                        Spacer(Modifier.height(24.dp))
                        com.soundly.feature.biblioteca.pages.FullArtistBioView(info = artistInfo)
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }

            ArtistTopBar(
                artistName = artistName,
                backIconOnImage = backIconOnImage,
                scrollProgress = scrollProgressLambda,
                headerHeightPx = headerHeightPx,
                statusBarHeightPx = statusBarHeightPx,
                artistNameColor = artistNameColor,
                onBack = onBack,
                contentWidth = contentWidth,
                isLandscape = actualIsLandscape
            )
        }

        val currentArtist = artist
        if (showArtistOptions && currentArtist != null) {
            com.soundly.ui.componentes.ArtistOptionsSheet(
                artist = currentArtist,
                artUri = artistArtUri,
                isFavorite = isFavorite,
                onDismissRequest = { showArtistOptions = false },
                onToggleFavorite = { viewModel.toggleArtistFavorite(artistId) }
            )
        }
    }

@Composable
private fun ArtistBackgroundHeader(
    artistArtUri: Uri?,
    artistColors: List<Color>,
    dominantColor: Color,
    hasColor: Boolean,
    isDark: Boolean,
    backgroundColor: Color,
    scrollProgress: () -> Float,
    headerHeightPx: Int,
    contentWidth: Dp,
    isLandscape: Boolean
) {
    val context = LocalContext.current
    val systemWaveHeightDp = 20.dp // Olas del sistema más grandes y uniformes
    val extraHeight = 80.dp // Espacio extra abajo para que las olas no se recorten

    // Optimización: Preparación de efectos. No inician hasta que los colores estén listos
    // y usamos un fade suave para que no "brinque" la UI.
    val effectsAlphaState = animateFloatAsState(
        targetValue = if (hasColor && scrollProgress() < 1f) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "effectsAlpha"
    )

    // Solo ejecutamos la animación si la cabecera es visible
    val infiniteTransition = rememberInfiniteTransition(label = "waves")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(12000, easing = LinearEasing)
        ),
        label = "time"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { headerHeightPx.toDp() } + extraHeight)
            .background(backgroundColor) // Seguridad: El fondo siempre es del color del sistema
            .graphicsLayer {
                alpha = (1f - scrollProgress()).coerceIn(0f, 1f)
            }
    ) {
        // La imagen se mantiene en su tamaño original y cubre todo el espacio
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artistArtUri)
                .crossfade(true)
                .allowHardware(true)
                .build(),
            placeholder = painterResource(R.drawable.carga),
            error = painterResource(R.drawable.carga),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { headerHeightPx.toDp() })
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )

        val gradientBottomColor = remember(dominantColor, hasColor, isDark, backgroundColor) {
            if (!hasColor) return@remember backgroundColor
            val alpha = if (isDark) 0.85f else 0.75f
            dominantColor.copy(alpha = alpha)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 140.dp else 240.dp)
                .align(Alignment.TopStart) 
                .offset(y = with(LocalDensity.current) { (headerHeightPx - (if (isLandscape) 140.dp else 240.dp).toPx()).toDp() })
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, gradientBottomColor, gradientBottomColor)
                    )
                )
        )

        // Un solo Shader que maneja todo: colores, olas superiores y olas de sistema inferiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasColor) {
            val shader = remember { RuntimeShader(ARTIST_WAVE_SHADER) }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    // Aumentamos la altura del Canvas para que las olas fluyan hacia abajo
                    .height(if (isLandscape) 200.dp else 320.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = effectsAlphaState.value }
                    .onSizeChanged { size ->
                        if (size.width > 0 && size.height > 0) {
                            shader.setFloatUniform("iResolution", size.width.toFloat(), size.height.toFloat())
                        }
                    }
            ) {
                val alpha = effectsAlphaState.value
                if (alpha > 0f && scrollProgress() < 1f) {
                    shader.setFloatUniform("iTime", time)
                    shader.setColorUniform("color1", artistColors[0].toArgb())
                    shader.setColorUniform("color2", artistColors[1].toArgb())
                    shader.setColorUniform("color3", artistColors[2].toArgb())
                    shader.setColorUniform("bgColor", backgroundColor.toArgb())
                    drawRect(brush = ShaderBrush(shader))
                }
            }
        } else {
            // Fallback para versiones antiguas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(systemWaveHeightDp * 8)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = effectsAlphaState.value }
            ) {
                val alpha = effectsAlphaState.value
                if (alpha > 0f && scrollProgress() < 1f) {
                    drawAnimatedWave(
                        color = backgroundColor,
                        waveHeight = systemWaveHeightDp.toPx(),
                        time = time
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowMoreSongsButton(
    expanded: Boolean,
    remainingCount: Int,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accentColor.copy(alpha = 0.18f))
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (expanded) stringResource(R.string.button_see_less) else stringResource(R.string.artist_detail_more_songs, remainingCount),
                color = accentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ArtistActionBar(
    accentColor: Color,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShowOptions: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = if (isFavorite) stringResource(R.string.cd_remove_from_favorites) else stringResource(R.string.cd_add_to_favorites)
                )
            }
            IconButton(onClick = onShowOptions) {
                Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.cd_more_options))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.16f))
                    .clickable(onClick = onShuffle)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.library_btn_shuffle),
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.width(12.dp))

            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
            FloatingActionButton(
                onClick = onPlay,
                shape = MaterialShapes.Cookie6Sided.toShape(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.library_btn_play))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, topPadding: Dp = 12.dp) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = topPadding, bottom = 12.dp)
    )
}

@Composable
private fun ArtistTopBar(
    artistName: String,
    backIconOnImage: Color,
    scrollProgress: () -> Float,
    headerHeightPx: Int,
    statusBarHeightPx: Float,
    artistNameColor: Color,
    onBack: () -> Unit,
    contentWidth: Dp,
    isLandscape: Boolean
) {
    val density = LocalDensity.current
    val onSurface = MaterialTheme.colorScheme.onSurface

    val backIconColor by animateColorAsState(
        lerpColor(backIconOnImage, MaterialTheme.colorScheme.onSurface, scrollProgress()),
        spring(stiffness = Spring.StiffnessLow),
        label = "backIconColor"
    )
    
    // Optimización: Usar el progreso de forma diferida
    val targetY = statusBarHeightPx + with(density) { 28.dp.toPx() }
    val startY = headerHeightPx.toFloat() - with(density) { (if (isLandscape) 46.dp else 92.dp).toPx() }

    val collapsedX = with(density) { 74.dp.toPx() }
    val expandedX = with(density) { 24.dp.toPx() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = scrollProgress().coerceIn(0f, 1f)
                }
                .agslFrostedGlass(
                    radius = 200f,
                    tint = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                )
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.7f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .agslFrostedGlass(
                        radius = 60f,
                        tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.button_back),
                    tint = backIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        val p = scrollProgress()
                        val scaleFactor = if (isLandscape) 1.25f else 1.55f
                        val currentScale = lerp(scaleFactor, 1.0f, p)
                        
                        translationX = lerp(expandedX, collapsedX, p)
                        translationY = lerp(startY - targetY, 0f, p)
                        scaleX = currentScale
                        scaleY = currentScale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = lerpColor(artistNameColor, onSurface, scrollProgress()),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 0.dp, end = 24.dp)
                )
            }
        }
    }
}
