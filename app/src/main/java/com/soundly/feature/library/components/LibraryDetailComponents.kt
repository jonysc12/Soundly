package com.soundly.feature.library.components

import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Artist
import com.soundly.ui.componentes.agslFrostedGlass
import kotlin.math.PI
import kotlin.math.sin

val SPRING_COLOR_SLOW   = spring<Color>(stiffness = Spring.StiffnessVeryLow)

private const val DETAIL_WAVE_SHADER = """
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
        
        // Mezcla fluida de los 3 colores
        float n = noise(uv * 1.5 + t * 0.1);
        float n2 = noise(uv * 2.2 - t * 0.05);
        float mixVal = smoothstep(-0.2, 0.8, n + n2);
        half4 fluidColor = mix(color1, color2, mixVal);
        
        float n3 = noise(uv * 3.0 + t * 0.1);
        fluidColor = mix(fluidColor, color3, smoothstep(0.0, 1.0, n3) * 0.6);
        
        // Olas superiores
        float waveTop = sin(uv.x * 4.0 + t) * 0.1 + sin(uv.x * 8.0 - t * 2.0) * 0.03;
        float maskTop = smoothstep(0.0, 0.4, uv.y + waveTop - 0.2);
        
        // Olas inferiores
        float waveBot = sin(uv.x * 4.5 + t) * 0.09 + sin(uv.x * 9.0 - t * 2.0) * 0.03;
        float maskBot = smoothstep(0.5, 0.85, uv.y + waveBot - 0.05);
        
        half4 colorWithTopMask = fluidColor * maskTop;
        return mix(colorWithTopMask, bgColor, maskBot);
    }
"""

@Composable
fun DetailBackgroundHeader(
    artUri: Uri?,
    colors: List<Color>,
    hasColor: Boolean,
    isDark: Boolean,
    backgroundColor: Color,
    scrollProgress: () -> Float,
    headerHeightPx: Int,
    isLandscape: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val systemWaveHeightDp = 20.dp
    val extraHeight = 80.dp

    val effectsAlphaState = animateFloatAsState(
        targetValue = if (hasColor && scrollProgress() < 1f) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "effectsAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waves")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(12000, easing = LinearEasing)
        ),
        label = "time"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { headerHeightPx.toDp() } + extraHeight)
            .background(backgroundColor)
            .graphicsLayer {
                alpha = (1f - scrollProgress()).coerceIn(0f, 1f)
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artUri)
                .crossfade(true)
                .allowHardware(true)
                .build(),
            placeholder = painterResource(R.drawable.carga),
            error = painterResource(R.drawable.carga),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { headerHeightPx.toDp() })
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

        val dominantColor = colors[0]
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
                .offset(y = with(density) { (headerHeightPx - (if (isLandscape) 140.dp else 240.dp).toPx()).toDp() })
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, gradientBottomColor, gradientBottomColor)
                    )
                )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasColor) {
            val shader = remember { RuntimeShader(DETAIL_WAVE_SHADER) }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
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
                    shader.setColorUniform("color1", colors[0].toArgb())
                    shader.setColorUniform("color2", colors[1].toArgb())
                    shader.setColorUniform("color3", colors[2].toArgb())
                    shader.setColorUniform("bgColor", backgroundColor.toArgb())
                    drawRect(brush = ShaderBrush(shader))
                }
            }
        } else {
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
        val yOffset = sin((relativeX * 4.5f) + time) * waveHeight * 0.8f +
                      sin((relativeX * 9f) - (time * 2f)) * waveHeight * 0.2f
        path.lineTo(x, h - waveHeight + yOffset)
    }
    path.lineTo(w, h)
    path.close()
    drawPath(path = path, color = color)
}

@Composable
fun DetailHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    onEdit: (() -> Unit)? = null,
    onHeaderClick: (() -> Unit)? = null,
    hdrAlpha: () -> Float,
    smallAlpha: () -> Float
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // iOS Style Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .graphicsLayer {
                    alpha = hdrAlpha()
                }
                .agslFrostedGlass(
                    radius = 40f,
                    tint = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .agslFrostedGlass(
                        radius = 20f,
                        tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.button_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (onEdit != null) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.45f)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.menu_edit),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 108.dp)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = smallAlpha() }
                    .then(if (onHeaderClick != null) Modifier.clickable(onClick = onHeaderClick) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CollaborativeArtistsStack(
    artists: List<Artist>,
    getArtistArtUri: (Long) -> Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        artists.take(3).forEachIndexed { index, artist ->
            val artUri = remember(artist.id) { getArtistArtUri(artist.id) }
            Box(
                modifier = Modifier
                    .padding(start = (index * 10).dp)
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artUri)
                        .crossfade(true)
                        .allowHardware(true)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.carga),
                    error = painterResource(R.drawable.carga)
                )
            }
        }
    }
}

@Composable
fun AnimatedDetailTopBar(
    title: String,
    subtitle: String?,
    backIconOnImage: Color,
    scrollProgress: () -> Float,
    headerHeightPx: Int,
    statusBarHeightPx: Float,
    titleColor: Color,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onMoreOptions: (() -> Unit)? = null,
    leadingSubtitle: (@Composable () -> Unit)? = null,
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
    
    val targetY = statusBarHeightPx + with(density) { 28.dp.toPx() }
    val startY = headerHeightPx.toFloat() - with(density) { (if (isLandscape) 46.dp else 92.dp).toPx() }

    val scaleFactor = if (isLandscape) 1.25f else 1.55f
    
    // Calculamos el ancho base para que al escalar al máximo no se salga de la pantalla
    // El padding horizontal expandido es 24dp a cada lado (48dp total)
    val maxExpandedWidth = contentWidth - 48.dp
    val baseWidth = maxExpandedWidth / scaleFactor

    val collapsedX = with(density) { (if (onEdit != null) 104.dp else 74.dp).toPx() }
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
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .agslFrostedGlass(
                        radius = 60f,
                        tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f * (1f - scrollProgress()))
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.button_back),
                        tint = backIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (onEdit != null) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.45f)
                            .background(backIconColor.copy(alpha = 0.15f))
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.menu_edit),
                            tint = backIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (onMoreOptions != null) {
                IconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .agslFrostedGlass(
                            radius = 60f,
                            tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f * (1f - scrollProgress()))
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = backIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        val p = scrollProgress()
                        val currentScale = lerp(scaleFactor, 1.0f, p)
                        
                        translationX = lerp(expandedX, collapsedX, p)
                        translationY = lerp(startY - targetY, 0f, p)
                        scaleX = currentScale
                        scaleY = currentScale
                        transformOrigin = TransformOrigin(0f, 0.5f) // Escalar desde la izquierda
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .width(baseWidth)
                        .padding(end = 4.dp), 
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = lerpColor(titleColor, onSurface, scrollProgress()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    if (subtitle != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.graphicsLayer { alpha = 1f - scrollProgress() }
                        ) {
                            leadingSubtitle?.invoke()
                            if (leadingSubtitle != null) Spacer(Modifier.width(6.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = lerpColor(titleColor.copy(alpha = 0.7f), MaterialTheme.colorScheme.onSurfaceVariant, scrollProgress()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
