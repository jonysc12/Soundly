package com.soundly.ui.screens.settings.pages

import androidx.compose.ui.res.stringResource
import com.soundly.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.data.repository.AnimationSpeed
import com.soundly.data.repository.MiniPlayerStyle
import com.soundly.data.repository.PlayerExpansionMode
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.componentes.blendOnSurface
import com.soundly.ui.componentes.ProgressiveEasing
import com.soundly.ui.theme.SoundlyTheme
import kotlin.math.roundToInt

@Composable
fun PlayerAnimationsPage(
    onBack: () -> Unit,
    viewModel: AnimationsViewModel = hiltViewModel()
) {
    val currentMode by viewModel.expansionMode.collectAsState()
    val expansionSpeed by viewModel.expansionSpeed.collectAsState()
    val elevationSpeed by viewModel.elevationSpeed.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.setMiniPlayerStyle(MiniPlayerStyle.TINTED)
    }

    PlayerAnimationsContent(
        onBack = onBack,
        currentMode = currentMode,
        expansionSpeed = expansionSpeed,
        elevationSpeed = elevationSpeed,
        onModeSelected = viewModel::setExpansionMode,
        onExpansionSpeedSelected = viewModel::setExpansionSpeed,
        onElevationSpeedSelected = viewModel::setElevationSpeed
    )
}

@Composable
fun PlayerAnimationsContent(
    onBack: () -> Unit,
    currentMode: PlayerExpansionMode,
    expansionSpeed: AnimationSpeed,
    elevationSpeed: AnimationSpeed,
    onModeSelected: (PlayerExpansionMode) -> Unit,
    onExpansionSpeedSelected: (AnimationSpeed) -> Unit,
    onElevationSpeedSelected: (AnimationSpeed) -> Unit
) {
    SettingsLayout(title = stringResource(R.string.animations_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.animations_expansion_mode_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreviewCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.animations_mode_expansion),
                        mode = PlayerExpansionMode.EXPANSION,
                        isSelected = currentMode == PlayerExpansionMode.EXPANSION,
                        speed = expansionSpeed,
                        onClick = { onModeSelected(PlayerExpansionMode.EXPANSION) }
                    )
                    PreviewCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.animations_mode_elevation),
                        mode = PlayerExpansionMode.ELEVATION,
                        isSelected = currentMode == PlayerExpansionMode.ELEVATION,
                        speed = elevationSpeed,
                        onClick = { onModeSelected(PlayerExpansionMode.ELEVATION) }
                    )
                }
            }

            val selectedSpeed = if (currentMode == PlayerExpansionMode.EXPANSION) expansionSpeed else elevationSpeed

            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.animations_speed_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpeedSlider(
                        currentSpeed = selectedSpeed,
                        onSpeedSelected = { speed ->
                            if (currentMode == PlayerExpansionMode.EXPANSION) {
                                onExpansionSpeedSelected(speed)
                            } else {
                                onElevationSpeedSelected(speed)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    modifier: Modifier = Modifier,
    title: String,
    mode: PlayerExpansionMode,
    isSelected: Boolean,
    speed: AnimationSpeed,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PreviewContainer(
            modifier = Modifier.fillMaxWidth(),
            mode = mode,
            isSelected = isSelected,
            speed = speed,
            onClick = onClick
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SpeedSlider(
    currentSpeed: AnimationSpeed,
    onSpeedSelected: (AnimationSpeed) -> Unit
) {
    val speeds = AnimationSpeed.entries
    val currentIndex = speeds.indexOf(currentSpeed).toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = currentIndex,
            onValueChange = { 
                val index = it.roundToInt().coerceIn(0, speeds.size - 1)
                onSpeedSelected(speeds[index])
            },
            valueRange = 0f..(speeds.size - 1).toFloat(),
            steps = speeds.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            speeds.forEach { speed ->
                val speedLabel = when(speed) {
                    AnimationSpeed.NORMAL -> stringResource(R.string.speed_normal)
                    AnimationSpeed.SLOW -> stringResource(R.string.speed_slow)
                }
                Text(
                    text = speedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (speed == currentSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun PreviewContainer(
    modifier: Modifier = Modifier,
    mode: PlayerExpansionMode,
    isSelected: Boolean,
    speed: AnimationSpeed,
    onClick: () -> Unit
) {
    var manualProgress by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableIntStateOf(1) }
    
    val targetDuration = speed.duration.toFloat()

    LaunchedEffect(targetDuration, isSelected) {
        if (!isSelected) {
            manualProgress = 0f
            return@LaunchedEffect
        }
        var lastTime = withFrameNanos { it }
        while (isSelected) {
            val currentTime = withFrameNanos { it }
            val deltaTimeMs = (currentTime - lastTime) / 1_000_000f
            lastTime = currentTime
            manualProgress += (deltaTimeMs / targetDuration) * direction
            if (manualProgress >= 1f) { manualProgress = 1f; direction = -1 }
            else if (manualProgress <= 0f) { manualProgress = 0f; direction = 1 }
        }
    }

    val progress = ProgressiveEasing.transform(manualProgress)

    Box(
        modifier = modifier
            .height(220.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = MaterialTheme.shapes.extraLarge
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                )
            }
        }

        val containerHeight = 220.dp
        
        val currentYOffset: Dp
        val currentHeight: Dp
        val currentWidthFraction: Float
        
        if (mode == PlayerExpansionMode.ELEVATION) {
            val elevationThreshold = 0.20f
            val jumpAmount = 32f
            
            if (progress < elevationThreshold) {
                currentYOffset = (progress / elevationThreshold).dp * jumpAmount
            } else {
                val ep = (progress - elevationThreshold) / (1f - elevationThreshold)
                val currentBottom = lerp(containerHeight - jumpAmount.dp, containerHeight, ep)
                currentYOffset = containerHeight - currentBottom
            }

            if (progress < 0.10f) {
                currentHeight = 64.dp
                currentWidthFraction = 0.85f
            } else {
                val ep = (progress - 0.10f) / (0.90f)
                val startHeight = 64.dp
                currentHeight = lerp(startHeight, containerHeight, ep)
                currentWidthFraction = 0.85f + (0.15f * ep)
            }
        } else {
            currentYOffset = lerp(16.dp, 0.dp, progress)
            currentHeight = lerp(64.dp, containerHeight, progress)
            currentWidthFraction = 0.85f + (0.15f * progress)
        }

        val baseColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val pillBg = blendOnSurface(baseColor, surfaceColor, 0.25f)
        
        val cornerRadius = lerp(20.dp, 0.dp, if (progress > 0.95f) (progress - 0.95f) / 0.05f else 0f)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -currentYOffset)
                .fillMaxWidth(currentWidthFraction)
                .height(currentHeight)
                .clip(object : Shape {
                    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                        val r = with(density) { cornerRadius.toPx() }
                        return Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(r)))
                    }
                })
                .background(pillBg),
            contentAlignment = Alignment.Center
        ) {
            if (progress > 0.30f) {
                val fullAlpha = ((progress - 0.30f) / 0.40f).coerceIn(0f, 1f)
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onPrimary.copy(alpha = ProgressiveEasing.transform(fullAlpha) * 0.1f)))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerAnimationsPagePreview() {
    SoundlyTheme {
        PlayerAnimationsContent(
            onBack = {},
            currentMode = PlayerExpansionMode.EXPANSION,
            expansionSpeed = AnimationSpeed.NORMAL,
            elevationSpeed = AnimationSpeed.NORMAL,
            onModeSelected = {},
            onExpansionSpeedSelected = {},
            onElevationSpeedSelected = {}
        )
    }
}
