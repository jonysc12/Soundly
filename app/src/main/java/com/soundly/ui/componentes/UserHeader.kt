package com.soundly.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.soundly.viewmodel.HomeScreenViewModel

enum class HeaderMode {
    HOME,
    LIBRARY,
    BIBLIOTECA,
}

private const val HeaderMorphSegments = 4f
private const val HeaderAnimationDurationMillis = 3800
private const val HeaderFinalHoldMillis = 180L

private fun phasePulse(
    phaseFraction: Float,
    start: Float,
    peak: Float,
    end: Float,
): Float {
    val clampedFraction = phaseFraction.coerceIn(0f, 1f)
    return if (clampedFraction < 0.5f) {
        lerp(start, peak, clampedFraction / 0.5f)
    } else {
        lerp(peak, end, (clampedFraction - 0.5f) / 0.5f)
    }
}

private fun smoothStep(fraction: Float): Float {
    val t = fraction.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun softPulse(
    phaseFraction: Float,
    start: Float,
    peak: Float,
    end: Float,
): Float {
    val t = smoothStep(phaseFraction)
    return if (t < 0.5f) {
        lerp(start, peak, t / 0.5f)
    } else {
        lerp(peak, end, (t - 0.5f) / 0.5f)
    }
}

private class MorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val composePath = morph.toPath(progress = progress).asComposePath()
        val bounds = composePath.getBounds()
        val safeWidth = if (bounds.width == 0f) 1f else bounds.width
        val safeHeight = if (bounds.height == 0f) 1f else bounds.height
        val scale = minOf(size.width / safeWidth, size.height / safeHeight)
        val scaledWidth = safeWidth * scale
        val scaledHeight = safeHeight * scale
        val offsetX = (size.width - scaledWidth) / 2f
        val offsetY = (size.height - scaledHeight) / 2f

        matrix.reset()
        matrix.translate(-bounds.left, -bounds.top)
        matrix *= Matrix().apply { scale(scale, scale) }
        matrix *= Matrix().apply { translate(offsetX, offsetY) }
        composePath.transform(matrix)

        return Outline.Generic(composePath)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundlyUserHeader(
    mode: HeaderMode,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    triggerAnimation: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val showProfileImage = mode == HeaderMode.HOME

    val imageWidth by animateDpAsState(
        targetValue = if (showProfileImage) 80.dp else 0.dp,
        animationSpec = keyframes {
            durationMillis = 250
            80.dp at 250
        },
        label = "image_width_anim",
    )

    val spacerWidth by animateDpAsState(
        targetValue = if (showProfileImage) 12.dp else 0.dp,
        animationSpec = keyframes {
            durationMillis = 250
            if (showProfileImage) {
                0.dp at 80
                12.dp at 250
            } else {
                12.dp at 0
                0.dp at 250
            }
        },
        label = "spacer_anim",
    )

    val circleShape = remember { MaterialShapes.Circle }
    val cookie7SidedShape = remember { MaterialShapes.Cookie7Sided }
    val pentagonShape = remember { MaterialShapes.Pentagon }
    val cookie4SidedShape = remember { MaterialShapes.Cookie4Sided }
    val squareShape = remember { MaterialShapes.Square }
    val shadowShape = remember { CircleShape }

    val morphCircleToCookie7 = remember { Morph(circleShape, cookie7SidedShape) }
    val morphCookie7ToPentagon = remember { Morph(cookie7SidedShape, pentagonShape) }
    val morphPentagonToCookie4 = remember { Morph(pentagonShape, cookie4SidedShape) }
    val morphCookie4ToSquare = remember { Morph(cookie4SidedShape, squareShape) }

    val globalProgress = remember { Animatable(if (triggerAnimation) 0f else HeaderMorphSegments) }
    val imageRequest = remember(uiState.imageUri) {
        uiState.imageUri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .crossfade(false)
                .memoryCacheKey(uri.toString())
                .diskCacheKey(uri.toString())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    LaunchedEffect(imageRequest) {
        imageRequest?.let(context.imageLoader::enqueue)
    }

    LaunchedEffect(triggerAnimation, mode) {
        if (triggerAnimation && mode == HeaderMode.HOME) {
            globalProgress.snapTo(0f)
            globalProgress.animateTo(
                targetValue = HeaderMorphSegments,
                animationSpec = keyframes {
                    durationMillis = HeaderAnimationDurationMillis
                    0f at 0 using FastOutSlowInEasing
                    0.9f at 760 using LinearOutSlowInEasing
                    1f at 980 using FastOutSlowInEasing
                    1.9f at 1700 using LinearOutSlowInEasing
                    2f at 1940 using FastOutSlowInEasing
                    2.9f at 2680 using LinearOutSlowInEasing
                    3f at 2920 using FastOutSlowInEasing
                    3.92f at 3520 using LinearOutSlowInEasing
                    HeaderMorphSegments at HeaderAnimationDurationMillis using FastOutSlowInEasing
                },
            )
            delay(HeaderFinalHoldMillis)
            onAnimationComplete()
        }
    }

    val phase by remember {
        derivedStateOf { globalProgress.value.coerceIn(0f, HeaderMorphSegments) }
    }
    val phaseIndex by remember {
        derivedStateOf { phase.toInt().coerceIn(0, 3) }
    }
    val phaseFraction by remember {
        derivedStateOf { (phase - phaseIndex).coerceIn(0f, 1f) }
    }
    val activeMorph by remember(phaseIndex) {
        derivedStateOf {
            when (phaseIndex) {
                0 -> morphCircleToCookie7
                1 -> morphCookie7ToPentagon
                2 -> morphPentagonToCookie4
                else -> morphCookie4ToSquare
            }
        }
    }
    val currentShape by remember(activeMorph, phaseFraction) {
        derivedStateOf {
            MorphShape(
                morph = activeMorph,
                progress = phaseFraction,
            )
        }
    }
    val finalRotation by remember(phaseIndex, phaseFraction) {
        derivedStateOf {
            when (phaseIndex) {
                0 -> softPulse(phaseFraction, start = 0f, peak = 1.6f, end = -0.4f)
                1 -> softPulse(phaseFraction, start = -0.4f, peak = 3.5f, end = 0.6f)
                2 -> softPulse(phaseFraction, start = 0.6f, peak = -3f, end = -0.3f)
                else -> softPulse(phaseFraction, start = -0.3f, peak = 1.4f, end = 0f)
            }
        }
    }
    val containerScale by remember(phaseIndex, phaseFraction) {
        derivedStateOf {
            when (phaseIndex) {
                0 -> softPulse(phaseFraction, start = 0.988f, peak = 1.006f, end = 0.996f)
                1 -> softPulse(phaseFraction, start = 0.996f, peak = 1.012f, end = 0.998f)
                2 -> softPulse(phaseFraction, start = 0.998f, peak = 1.014f, end = 0.997f)
                else -> softPulse(phaseFraction, start = 0.997f, peak = 1.006f, end = 1f)
            }
        }
    }
    val imageScale by remember(phaseIndex, phaseFraction) {
        derivedStateOf {
            when (phaseIndex) {
                0 -> softPulse(phaseFraction, start = 1.13f, peak = 1.16f, end = 1.145f)
                1 -> softPulse(phaseFraction, start = 1.145f, peak = 1.18f, end = 1.16f)
                2 -> softPulse(phaseFraction, start = 1.16f, peak = 1.19f, end = 1.17f)
                else -> softPulse(phaseFraction, start = 1.17f, peak = 1.18f, end = 1.16f)
            }
        }
    }
    val containerAlpha by remember(phaseIndex, phaseFraction) {
        derivedStateOf {
            when (phaseIndex) {
                0 -> softPulse(phaseFraction, start = 0.97f, peak = 0.995f, end = 0.985f)
                1 -> softPulse(phaseFraction, start = 0.985f, peak = 1f, end = 0.99f)
                2 -> softPulse(phaseFraction, start = 0.99f, peak = 1f, end = 0.993f)
                else -> softPulse(phaseFraction, start = 0.993f, peak = 1f, end = 1f)
            }
        }
    }

    val rotationModifier = Modifier.graphicsLayer {
        rotationZ = finalRotation
        transformOrigin = TransformOrigin.Center
        scaleX = containerScale
        scaleY = containerScale
        alpha = containerAlpha
    }

    val profilePainter = rememberAsyncImagePainter(model = imageRequest)
    val imageIsReady = profilePainter.state is AsyncImagePainter.State.Success

    val elevation by remember(phaseIndex, phaseFraction) {
        derivedStateOf {
            when (phaseIndex) {
                0 -> softPulse(phaseFraction, start = 3.6f, peak = 4.6f, end = 4f).dp
                1 -> softPulse(phaseFraction, start = 4f, peak = 5.3f, end = 4.5f).dp
                2 -> softPulse(phaseFraction, start = 4.5f, peak = 5.7f, end = 4.8f).dp
                else -> softPulse(phaseFraction, start = 4.8f, peak = 5.1f, end = 4.2f).dp
            }
        }
    }
    val shadowAlpha = if (phaseIndex < 3) 0.16f else 0.2f

    val titleText = when (mode) {
        HeaderMode.HOME -> "Hola, ${uiState.username}!"
        HeaderMode.LIBRARY -> "Librería"
        HeaderMode.BIBLIOTECA -> "Biblioteca"
    }

    val titleScale by animateFloatAsState(
        targetValue = if (mode == HeaderMode.HOME) 1f else 1.35f,
        animationSpec = keyframes {
            durationMillis = 250
            if (mode == HeaderMode.HOME) {
                1.35f at 0
                1f at 250
            } else {
                1f at 0
                1.35f at 250
            }
        },
        label = "title_scale_anim",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(imageWidth)
                .height(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (showProfileImage && imageWidth > 0.dp) {
                uiState.imageUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(rotationModifier)
                            .graphicsLayer {
                                shadowElevation = elevation.toPx()
                                shape = shadowShape
                                clip = false
                                ambientShadowColor = Color.Black.copy(alpha = shadowAlpha)
                                spotShadowColor = Color.Black.copy(alpha = shadowAlpha)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            shape = currentShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Image(
                                    painter = profilePainter,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (imageIsReady) 1f else 0.2f)
                                        .graphicsLayer {
                                            rotationZ = -finalRotation
                                            transformOrigin = TransformOrigin.Center
                                            scaleX = imageScale
                                            scaleY = imageScale
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(spacerWidth))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            if (mode == HeaderMode.HOME) {
                Text(
                    text = "Soundly",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    ),
                )
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = titleScale
                    scaleY = titleScale
                    transformOrigin = TransformOrigin.Center
                },
            )
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSettingsClick()
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.width(24.dp).height(24.dp),
            )
        }
    }
}
