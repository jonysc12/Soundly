package com.soundly.cloud

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundly.cloud.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization

@Composable
fun SoundlyCloudHeader(
    onBack: () -> Unit,
    title: String = stringResource(R.string.soundly_cloud),
    onDownloadManagerClick: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        val bgColor = MaterialTheme.colorScheme.background
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
        
        LegacyBlurView(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            blurRadius = 15f,
            tintColor = bgColor.copy(alpha = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Black,
                                0.5f to Color.Black,
                                0.85f to Color.Black.copy(alpha = 0.3f),
                                1.0f to Color.Transparent,
                                startY = 0f,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 23.dp, vertical = 8.dp)
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            LegacyBlurView(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(32.dp)
                    .clip(CircleShape),
                blurRadius = 10f,
                tintColor = surfaceVariantColor.copy(alpha = 0.4f)
            ) {
                IconButton(
                    onClick = onBack,
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier.fillMaxSize(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (onDownloadManagerClick != null) {
                LegacyBlurView(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape),
                    blurRadius = 10f,
                    tintColor = surfaceVariantColor.copy(alpha = 0.4f)
                ) {
                    IconButton(
                        onClick = onDownloadManagerClick,
                        modifier = Modifier.fillMaxSize(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.downloads),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundlyCloudScreen(viewModel: SoundlyCloudViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val overscrollOffset = remember { Animatable(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (overscrollOffset.value != 0f && source == NestedScrollSource.UserInput) {
                    val sign = Math.signum(overscrollOffset.value)
                    val availableSign = Math.signum(available.y)
                    if (sign != availableSign && availableSign != 0f) {
                        val newOffset = overscrollOffset.value + available.y * 0.45f
                        val newSign = Math.signum(newOffset)
                        if (newSign != sign && newSign != 0f) {
                            scope.launch { overscrollOffset.snapTo(0f) }
                            return androidx.compose.ui.geometry.Offset(0f, available.y - (newOffset - 0f) / 0.45f)
                        } else {
                            scope.launch { overscrollOffset.snapTo(newOffset) }
                            return androidx.compose.ui.geometry.Offset(0f, available.y)
                        }
                    }
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val resistance = 1f - Math.abs(overscrollOffset.value) / 1000f
                    val multiplier = (0.45f * resistance).coerceAtLeast(0.1f)
                    val newOffset = overscrollOffset.value + available.y * multiplier
                    scope.launch {
                        overscrollOffset.snapTo(newOffset)
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    var downloadProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try { NewPipe.init(NewPipeDownloader(), Localization.DEFAULT) } catch (_: Exception) {}
            try { 
                // La mayoría del trabajo ya se hizo en AppGlobal
                YoutubeDL.getInstance().init(context.applicationContext)
                com.yausername.aria2c.Aria2c.getInstance().init(context.applicationContext)
                
                // Actualizar binarios de yt-dlp en segundo plano
                YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("SoundlyCloud", "YoutubeDL Background Tasks Failed", e)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .graphicsLayer { translationY = overscrollOffset.value }
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(70.dp))

                if (!uiState.isSearchCommitted) {
                    OutlinedTextField(
                        value = uiState.query, 
                        onValueChange = { viewModel.onQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) IconButton({ 
                                viewModel.onQueryChanged("")
                            }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.clear))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(32.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.search(uiState.query) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    if (uiState.query.isEmpty()) {
                        WelcomeSuggestions { suggestion ->
                            keyboard?.hide()
                            viewModel.onQueryChanged(suggestion)
                            viewModel.search(suggestion)
                        }
                    }

                    if (uiState.suggestions.isNotEmpty()) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(uiState.suggestions) { suggestion ->
                               Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            keyboard?.hide() // Ocultamos el teclado al elegir una sugerencia
                                            viewModel.search(suggestion) 
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(suggestion, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                } else {
                    ScrollableTabRow(
                        selectedTabIndex = uiState.selectedCategory.ordinal,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        edgePadding = 0.dp,
                        divider = {},
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[uiState.selectedCategory.ordinal]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        SearchCategory.entries.forEach { category ->
                            Tab(
                                selected = uiState.selectedCategory == category,
                                onClick = { viewModel.onCategorySelected(category) },
                                text = { Text(stringResource(category.labelResId)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    when {
                        uiState.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(12.dp))
                                    Text(stringResource(R.string.searching), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        uiState.results.isEmpty() && uiState.detailState == null -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.no_results), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        uiState.artistDetailState != null -> {
                            ArtistDetailScreen(uiState.artistDetailState!!, viewModel)
                        }
                        uiState.detailState != null -> {
                            DetailScreen(uiState.detailState!!, viewModel)
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.results) { item ->
                                    when (item) {
                                        is Song -> {
                                            val progress = downloadProgress[item.id]
                                            SongItem(item, progress) {
                                                if (progress == null) {
                                                    downloadProgress = downloadProgress + (item.id to -1)
                                                    scope.launch {
                                                        downloadSong(context, item) { p -> 
                                                            downloadProgress = downloadProgress + (item.id to p) 
                                                        }
                                                        downloadProgress = downloadProgress - item.id
                                                    }
                                                }
                                            }
                                        }
                                        is Artist -> ArtistItem(item) {
                                            viewModel.loadArtistDetail(item)
                                        }
                                        is Album -> AlbumItem(item) {
                                            viewModel.loadDetail(item.id, item.title, item.artist, item.thumbnailUrl, ResultType.ALBUM)
                                        }
                                        is Playlist -> PlaylistItem(item) {
                                            viewModel.loadDetail(item.id, item.title, item.uploader, item.thumbnailUrl, ResultType.PLAYLIST)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SoundlyCloudHeader(
                onBack = { 
                    if (uiState.detailState != null || uiState.isSearchCommitted) {
                        viewModel.onBackToSearch()
                    } else {
                        (context as? ComponentActivity)?.finish() 
                    }
                },
                title = if (uiState.artistDetailState != null) uiState.artistDetailState!!.name else if (uiState.detailState != null) uiState.detailState!!.title else if (uiState.isSearchCommitted) uiState.query else stringResource(R.string.soundly_cloud),
                onDownloadManagerClick = {
                    val intent = Intent(context, DownloadManagerActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun ArtistItem(artist: Artist, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.thumbnailUrl)
                    .crossfade(true)
                    .allowHardware(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Icon(
                Icons.Default.Person, null, 
                modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(2.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumItem(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.thumbnailUrl)
                    .crossfade(true)
                    .allowHardware(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Icon(
                Icons.Default.Album, null,
                modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)).padding(2.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.artist} • Álbum",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playlist.thumbnailUrl)
                    .crossfade(true)
                    .allowHardware(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Icon(
                Icons.Default.PlaylistPlay, null,
                modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)).padding(2.dp),
                tint = MaterialTheme.colorScheme.onTertiary
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Playlist • ${playlist.uploader}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongItem(song: Song, downloadProgress: Int?, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDownload, enabled = downloadProgress == null)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.thumbnailUrl)
                .crossfade(true)
                .allowHardware(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • ${song.duration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            if (downloadProgress != null) {
                if (downloadProgress == -1) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    CircularProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text("${downloadProgress}%", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.download),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun DetailScreen(state: DetailUiState, viewModel: SoundlyCloudViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.thumbnailUrl)
                    .crossfade(true)
                    .allowHardware(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.uploader,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        state.items.forEach { song ->
                            if (downloadProgress[song.id] == null) {
                                downloadProgress = downloadProgress + (song.id to -1)
                                scope.launch {
                                    downloadSong(context, song) { p ->
                                        downloadProgress = downloadProgress + (song.id to p)
                                    }
                                    downloadProgress = downloadProgress - song.id
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.download_all))
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_songs_in_content), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.items) { song ->
                    val progress = downloadProgress[song.id]
                    SongItem(song, progress) {
                        if (progress == null) {
                            downloadProgress = downloadProgress + (song.id to -1)
                            scope.launch {
                                downloadSong(context, song) { p ->
                                    downloadProgress = downloadProgress + (song.id to p)
                                }
                                downloadProgress = downloadProgress - song.id
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    state: ArtistDetailUiState,
    viewModel: SoundlyCloudViewModel
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.loading_artist_profile, state.name), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (state.bannerUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.bannerUrl)
                            .crossfade(true)
                            .allowHardware(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                startY = 100f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.avatarUrl)
                            .crossfade(true)
                            .allowHardware(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.subscriberCount.isNotEmpty()) {
                        Text(
                            text = "${state.subscriberCount} ${stringResource(R.string.subscribers)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.description.isNotEmpty()) {
            item {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.songs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.popular_songs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(state.songs) { song ->
                SongItem(song, null) {
                    // Clic en la canción
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.albums_playlists),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(state.playlists) { playlist ->
                PlaylistItem(playlist) {
                    viewModel.loadDetail(playlist.id, playlist.title, playlist.uploader, playlist.thumbnailUrl, ResultType.PLAYLIST)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeSuggestions(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf("Pop Hits", "Lo-Fi Beats", "Rock Classics", "Jazz Vibes", "Electronic Dance", "Indie Pop")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = stringResource(R.string.explore),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
