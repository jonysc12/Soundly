package com.soundly.ui.componentes

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.soundly.R
import com.soundly.viewmodel.HomeScreenViewModel
import com.soundly.viewmodel.HomeUiState

enum class HeaderMode {
    HOME,
    LIBRARY,
    BIBLIOTECA,
    SEARCH,
    SETTINGS,
}

@Composable
fun SoundlyUserHeader(
    mode: HeaderMode,
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    useStatusBarsPadding: Boolean = true,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    showIcon: Boolean = true,
    onSettingsClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    iconOverride: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    SoundlyUserHeaderContent(
        uiState = uiState,
        mode = mode,
        modifier = modifier,
        useStatusBarsPadding = useStatusBarsPadding,
        horizontalPadding = horizontalPadding,
        showIcon = showIcon,
        onSettingsClick = onSettingsClick,
        onBackClick = onBackClick,
        iconOverride = iconOverride
    )
}

@Composable
fun SoundlyUserHeaderContent(
    uiState: HomeUiState,
    mode: HeaderMode,
    modifier: Modifier = Modifier,
    useStatusBarsPadding: Boolean = true,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    showIcon: Boolean = true,
    onSettingsClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    iconOverride: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val context = LocalContext.current
    val isHome = mode == HeaderMode.HOME
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isHome) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "header_content_alpha"
    )

    val imageRequest = remember(uiState.imageUri, uiState.lastUpdated) {
        uiState.imageUri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .crossfade(false)
                .allowHardware(true)
                .memoryCacheKey("${uri}_${uiState.lastUpdated}")
                .diskCacheKey("${uri}_${uiState.lastUpdated}")
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    LaunchedEffect(imageRequest) {
        imageRequest?.let(context.imageLoader::enqueue)
    }

    val profilePainter = rememberAsyncImagePainter(model = imageRequest)
    val imageIsReady = profilePainter.state is AsyncImagePainter.State.Success

    val titleText = when (mode) {
        HeaderMode.HOME -> stringResource(R.string.user_header_greeting, uiState.username)
        HeaderMode.LIBRARY -> stringResource(R.string.nav_music)
        HeaderMode.BIBLIOTECA -> stringResource(R.string.nav_library)
        HeaderMode.SEARCH -> stringResource(R.string.search_title)
        HeaderMode.SETTINGS -> stringResource(R.string.edit)
    }

    val titleScaleProvider = remember {
        { if (mode == HeaderMode.HOME) 1f else 1.35f }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(top = 8.dp, bottom = 12.dp)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val haptic = LocalHapticFeedback.current

        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.button_back),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp) // Tamaño un poco más generoso y consistente
                .graphicsLayer {
                    alpha = contentAlpha
                    // Escalamiento desde el centro para evitar sensación de "apachurrado"
                    val s = 0.7f + (0.3f * contentAlpha)
                    scaleX = s
                    scaleY = s
                    translationX = (-20f * (1f - contentAlpha)).dp.toPx()
                    transformOrigin = TransformOrigin.Center
                },
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.imageUri != null) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = profilePainter,
                            contentDescription = stringResource(R.string.cd_profile_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (imageIsReady) 1f else 0.2f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp).graphicsLayer {
            alpha = contentAlpha
            translationX = (-30f * (1f - contentAlpha)).dp.toPx()
        })

        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    translationX = (-64f * (1f - contentAlpha)).dp.toPx()
                }
                .padding(horizontal = 4.dp),
        ) {
            if (mode == HeaderMode.HOME) {
                Text(
                    text = "Soundly",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    ),
                )
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    val s = titleScaleProvider()
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
            )
        }

        if (showIcon) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsClick()
                }
            ) {
                Icon(
                    imageVector = iconOverride ?: if (mode == HeaderMode.SETTINGS) Icons.Rounded.Edit else Icons.Rounded.Settings,
                    contentDescription = if (mode == HeaderMode.SETTINGS) stringResource(R.string.edit) else stringResource(R.string.media_scanner_settings),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.width(24.dp).height(24.dp),
                )
            }
        }
    }
}
