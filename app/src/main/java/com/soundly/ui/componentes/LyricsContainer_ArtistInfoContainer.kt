@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.soundly.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.carousel.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.soundly.R
import com.soundly.player.ArtistUiState
import com.soundly.player.LyricsUiState
import com.soundly.ui.componentes.Lyrics_fullscreen.LyricLineItem
import com.soundly.ui.componentes.Lyrics_fullscreen.resolveActiveLine
import com.soundly.ui.componentes.Lyrics_fullscreen.toStructuredFallback
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistInfoContainer(
    modifier: Modifier = Modifier,
    artistName: String,
    artistDescription: String,
    imageUrl: String,
    onColor: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(onColor.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "artistInfoLoading"
        ) { loading ->
            if (loading) {
                LoadingIndicator(
                    color = onColor.copy(alpha = 0.7f)
                )
            } else {
                ArtistCardContent(
                    artistName = artistName,
                    artistDescription = artistDescription,
                    imageUrl = imageUrl
                )
            }
        }
    }
}

@Composable
private fun ArtistCardContent(
    artistName: String,
    artistDescription: String,
    imageUrl: String,
    showText: Boolean = true
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .allowHardware(true)
                .build(),
            contentDescription = artistName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.05f),
                            0.5f to Color.Black.copy(alpha = 0.45f),
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = showText,
            enter = fadeIn(tween(400)) + expandVertically(tween(400)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = artistDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsCarouselContainer(
    artists: List<ArtistUiState>,
    onColor: Color,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onArtistClick: (String) -> Unit = {}
) {
    if (isLoading) {
        ArtistInfoContainer(
            artistName = stringResource(R.string.loading),
            artistDescription = "",
            imageUrl = "",
            onColor = onColor,
            isLoading = true,
            modifier = modifier
        )
        return
    }

    if (artists.isEmpty()) return

    if (artists.size == 1) {
        val artist = artists[0]
        ArtistInfoContainer(
            artistName = artist.name,
            artistDescription = artist.description,
            imageUrl = artist.imageUrl,
            onColor = onColor,
            onClick = { onArtistClick(artist.name) },
            modifier = modifier
        )
    } else {
        val carouselState = rememberCarouselState { artists.size }
        
        // Ajustamos el ancho preferido para forzar el efecto de "Grande, Mediano, Chico"
        // 220.dp suele permitir ver el primero grande, el segundo mediano y el tercero asomando (chico) en la mayoría de pantallas.
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 240.dp,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
        ) { index ->
            val artist = artists[index]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(RoundedCornerShape(24.dp))
                    .background(onColor.copy(alpha = 0.05f))
                    .clickable { onArtistClick(artist.name) },
                contentAlignment = Alignment.Center
            ) {
                val isFocused = index == carouselState.currentItem
                ArtistCardContent(
                    artistName = artist.name,
                    artistDescription = artist.description,
                    imageUrl = artist.imageUrl,
                    showText = isFocused
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsContainer(
    lyrics: LyricsUiState,
    positionMs: Long,
    onColor: Color,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onExpand: () -> Unit = {}
) {
    val lines = remember(lyrics) { lyrics.structuredLines.ifEmpty { lyrics.toStructuredFallback() } }
    val lyricsContentKey = remember(lyrics.rawContent, lyrics.plainText) {
        lyrics.rawContent ?: lyrics.plainText.orEmpty()
    }
    val listState = rememberLazyListState()
    val activeIndex = remember(lines, positionMs) { resolveActiveLine(lines, positionMs) }

    val room = 0.55f
    val viewportHeight = lerp(170.dp, 320.dp, room)
    val horizontalPadding = lerp(16.dp, 24.dp, room)
    val verticalPadding = lerp(14.dp, 20.dp, room)
    val lineSpacing = lerp(12.dp, 18.dp, room)

    val topPaddingDp = 16.dp
    
    var lastCenteredIndex by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(lyricsContentKey) {
        lastCenteredIndex = -1
        listState.scrollToItem(0)
    }

    LaunchedEffect(activeIndex, lines.size) {
        if (activeIndex < 0 || lines.isEmpty()) return@LaunchedEffect
        if (activeIndex == lastCenteredIndex) return@LaunchedEffect
        listState.animateScrollToItem(index = activeIndex)
        lastCenteredIndex = activeIndex
    }

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "lyrics_container"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .then(sharedModifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(onColor.copy(alpha = 0.07f))
            .clickable { onExpand() }
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.lyrics_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = onColor,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Button(
                    onClick = onExpand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onColor.copy(alpha = 0.10f),
                        contentColor = onColor
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(28.dp)
                ) {
                    Text(text = stringResource(R.string.button_show_more))
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = lyrics.isLoading,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "lyricsContentTransition"
            ) { loading ->
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(viewportHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(color = onColor.copy(alpha = 0.7f))
                    }
                } else if (lines.isNotEmpty()) {
                    val isTransitioning = animatedVisibilityScope?.transition?.let {
                        it.currentState != it.targetState
                    } ?: false

                    val contentAlpha by animateFloatAsState(
                        targetValue = if (isTransitioning) 0f else 1f,
                        animationSpec = tween(durationMillis = 400),
                        label = "lyricsFade"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(viewportHeight)
                            .graphicsLayer { alpha = contentAlpha }
                    ) {
                        if (contentAlpha > 0f || !isTransitioning) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = topPaddingDp, bottom = viewportHeight / 2),
                                verticalArrangement = Arrangement.spacedBy(lineSpacing),
                                userScrollEnabled = false
                            ) {
                                itemsIndexed(lines) { index, line ->
                                    val distanceFromActive = if (activeIndex >= 0) abs(index - activeIndex) else Int.MAX_VALUE
                                    LyricLineItem(
                                        line = line,
                                        positionMs = positionMs,
                                        distanceFromActive = distanceFromActive,
                                        onColor = onColor,
                                        room = room,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(viewportHeight))
                }
            }
        }
    }
}
