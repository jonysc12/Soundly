package com.soundly.ui.screens.settings.pages

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.R
import com.soundly.data.repository.AnimationSpeed
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.SoundlyTheme

private const val LYRICS_WAVE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 color1; // Normal
    layout(color) uniform half4 color2; // Vibrante
    layout(color) uniform half4 color3; // Claro
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
        float t = iTime * 0.12;
        
        float n = noise(uv * 1.0 + t * 0.15);
        float n2 = noise(uv * 1.8 - t * 0.25);
        float combined = (n + n2) * 0.5 + 0.5;
        float mixVal = smoothstep(0.3, 0.7, combined);
        half4 baseColor = mix(color1, color2, mixVal);
        
        float n3 = noise(uv * 2.0 + t * 0.3) * 0.5 + 0.5;
        float highlight = smoothstep(0.4, 0.8, n3);
        
        half4 finalColor = mix(baseColor, color3, highlight * 0.7);
        finalColor += color3 * highlight * 0.2;
        
        return mix(bgColor, finalColor, 0.95);
    }
"""

@Composable
fun LyricsPage(
    onBack: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel()
) {
    val expansionSpeed by viewModel.lyricsExpansionSpeed.collectAsState()
    val useAgsl by viewModel.useLyricsAgslAnimation.collectAsState()

    LyricsPageContent(
        expansionSpeed = expansionSpeed,
        useAgsl = useAgsl,
        onBack = onBack,
        onSpeedSelected = viewModel::setLyricsExpansionSpeed,
        onAgslToggle = viewModel::setUseLyricsAgslAnimation
    )
}

@Composable
fun LyricsPageContent(
    expansionSpeed: AnimationSpeed,
    useAgsl: Boolean,
    onBack: () -> Unit,
    onSpeedSelected: (AnimationSpeed) -> Unit,
    onAgslToggle: (Boolean) -> Unit
) {
    SettingsLayout(title = stringResource(R.string.lyrics_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section
            LyricsPreview(useAgsl = useAgsl)

            // Options Section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500, delayMillis = 100))
                ) {
                    LyricsAnimationSettings(
                        expansionSpeed = expansionSpeed,
                        onSpeedSelected = onSpeedSelected,
                        useAgsl = useAgsl,
                        onAgslToggle = onAgslToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsAnimationSettings(
    expansionSpeed: AnimationSpeed,
    onSpeedSelected: (AnimationSpeed) -> Unit,
    useAgsl: Boolean,
    onAgslToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.lyrics_section_animations),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.lyrics_expansion_speed_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AnimationSpeed.entries.forEachIndexed { index, speed ->
                        val label = when(speed) {
                            AnimationSpeed.NORMAL -> stringResource(R.string.speed_normal)
                            AnimationSpeed.SLOW -> stringResource(R.string.speed_slow)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AnimationSpeed.entries.size),
                            onClick = { onSpeedSelected(speed) },
                            selected = expansionSpeed == speed,
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.lyrics_expansion_speed_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                leadingContent = { 
                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) 
                },
                headlineContent = { Text(stringResource(R.string.lyrics_agsl_title)) },
                supportingContent = { Text(stringResource(R.string.lyrics_agsl_desc)) },
                trailingContent = {
                    Switch(
                        checked = useAgsl,
                        onCheckedChange = onAgslToggle,
                        thumbContent = if (useAgsl) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onAgslToggle(!useAgsl) }
            )
        }
    }
}

@Composable
private fun LyricsPreview(useAgsl: Boolean) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val vibrantColor = remember(containerColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(containerColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 2.2f).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 1.4f).coerceIn(0f, 1f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val lightColor = remember(containerColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(containerColor.toArgb(), hsv)
        hsv[2] = (hsv[2] * 2.5f).coerceIn(0f, 1f)
        hsv[1] = (hsv[1] * 0.35f).coerceIn(0f, 1f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "agsl_preview")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_preview_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(225.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            // Real AGSL Animation
            if (useAgsl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader = remember { RuntimeShader(LYRICS_WAVE_SHADER) }
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    shader.setFloatUniform("iTime", time)
                    shader.setColorUniform("color1", containerColor.toArgb())
                    shader.setColorUniform("color2", vibrantColor.toArgb())
                    shader.setColorUniform("color3", lightColor.toArgb())
                    shader.setColorUniform("bgColor", containerColor.toArgb())
                    drawRect(brush = ShaderBrush(shader))
                }
            } else if (useAgsl) {
                // Fallback for older Android versions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(vibrantColor.copy(alpha = 0.4f), lightColor.copy(alpha = 0.4f), Color.Transparent),
                                center = Offset(time * 5f, time * 2.5f),
                                radius = 600f
                            )
                        )
                )
            }

            // Lyrics Skeleton with improved animation
            val lines = listOf(
                "I remember when we used to sit",
                "In the government yard in Trenchtown",
                "Observing the hypocrites",
                "Mingle with the good people we meet"
            )
            
            val activeLineIndex by infiniteTransition.animateValue(
                initialValue = 0,
                targetValue = lines.size,
                typeConverter = Int.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "activeLine"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                lines.forEachIndexed { index, text ->
                    LyricsLinePreview(
                        text = text, 
                        active = index == activeLineIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsLinePreview(text: String, active: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.4f,
        animationSpec = tween(500),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val fillColor = MaterialTheme.colorScheme.onSurface

    if (active) {
        val infiniteTransition = rememberInfiniteTransition(label = "karaoke")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    val baseColor = fillColor.copy(alpha = 0.35f)
                    val brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to fillColor,
                            (progress - 0.02f).coerceAtLeast(0f) to fillColor,
                            (progress + 0.02f).coerceAtMost(1f) to baseColor,
                            1f to baseColor
                        )
                    )
                    drawContent()
                    drawRect(
                        brush = brush,
                        blendMode = BlendMode.SrcIn
                    )
                }
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LyricsPagePreview() {
    SoundlyTheme {
        LyricsPageContent(
            expansionSpeed = AnimationSpeed.NORMAL,
            useAgsl = true,
            onBack = {},
            onSpeedSelected = {},
            onAgslToggle = {}
        )
    }
}
