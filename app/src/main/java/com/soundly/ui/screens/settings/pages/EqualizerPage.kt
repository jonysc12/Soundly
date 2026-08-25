package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.R
import com.soundly.ui.screens.settings.SettingsLayout
import kotlin.math.roundToInt

@Composable
fun EqualizerPage(
    onBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val frequencies = remember { viewModel.getBandFrequencies() }
    val range = remember { viewModel.getBandLevelRange() }
    val minLevel = range[0].toFloat()
    val maxLevel = range[1].toFloat()

    SettingsLayout(title = stringResource(R.string.equalizer_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Integrated Card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            ) {
                Column {
                    // Master Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.equalizer_enable_title),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.equalizerEnabled,
                            onCheckedChange = { viewModel.toggleEqualizer(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                                checkedTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Content that expands when enabled
                    AnimatedVisibility(visible = settings.equalizerEnabled) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Graphic Equalizer Bands
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                Text(
                                    text = stringResource(R.string.equalizer_section_bands),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp) // Mayor altura para llenar el espacio
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    frequencies.forEachIndexed { index, freq ->
                                        val currentLevel = settings.equalizerBandLevels[index] ?: 0
                                        VerticalBandSlider(
                                            frequency = freq,
                                            level = currentLevel,
                                            min = minLevel,
                                            max = maxLevel,
                                            onValueChange = { viewModel.updateBandLevel(index, it.roundToInt()) }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Audio Effects
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.equalizer_section_effects),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                EffectSlider(
                                    title = stringResource(R.string.equalizer_bass_boost),
                                    value = settings.bassBoostStrength.toFloat(),
                                    max = 1000f,
                                    onValueChange = { viewModel.updateBassBoost(it.roundToInt()) }
                                )
                                
                                EffectSlider(
                                    title = stringResource(R.string.equalizer_virtualizer),
                                    value = settings.virtualizerStrength.toFloat(),
                                    max = 1000f,
                                    onValueChange = { viewModel.updateVirtualizer(it.roundToInt()) }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Control Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openSystemEqualizer() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(
                                        text = stringResource(R.string.equalizer_system),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Button(
                                    onClick = { viewModel.resetEqualizer() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(
                                        text = stringResource(R.string.equalizer_reset),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalBandSlider(
    frequency: Int,
    level: Int,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    
    var internalValue by remember { mutableFloatStateOf(level.toFloat()) }
    
    LaunchedEffect(level) {
        if (!isDragged) {
            internalValue = level.toFloat()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .width(56.dp)
    ) {
        Text(
            text = "${internalValue.roundToInt() / 100}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Fondo del "Slot" del ecualizador - Más gordo (20dp)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Slider(
                value = internalValue,
                onValueChange = {
                    internalValue = it
                    onValueChange(it)
                },
                valueRange = min..max,
                interactionSource = interactionSource,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                        transformOrigin = TransformOrigin.Center
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.placeRelative(
                                x = -(placeable.width - placeable.height) / 2,
                                y = -(placeable.height - placeable.width) / 2
                            )
                        }
                    }
                    .size(width = this.maxHeight, height = 64.dp), // Área táctil más amplia (64dp)
                track = {
                    // Slot personalizado ya dibujado
                    Spacer(modifier = Modifier.fillMaxWidth())
                },
                thumb = {
                    // Thumb tipo "píldora" más delgada (10dp) y redondeada
                    Surface(
                        modifier = Modifier
                            .size(width = 44.dp, height = 10.dp) // Rotado: ancho 10, alto 44
                            .graphicsLayer { rotationZ = 90f },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onBackground,
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.5f)
                                    .width(2.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (frequency >= 1000) "${frequency / 1000}k" else "$frequency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectSlider(
    title: String,
    value: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    var internalValue by remember { mutableFloatStateOf(value) }

    LaunchedEffect(value) {
        if (!isDragged) internalValue = value
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(internalValue / max * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Riel horizontal tipo Slot - Más gordo (20dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Slider(
                value = internalValue,
                onValueChange = {
                    internalValue = it
                    onValueChange(it)
                },
                valueRange = 0f..max,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                track = {
                    Spacer(modifier = Modifier.fillMaxSize())
                },
                thumb = {
                    // Thumb tipo "píldora" más delgada (10dp) y redondeada
                    Surface(
                        modifier = Modifier.size(width = 10.dp, height = 44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onBackground,
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                            )
                        }
                    }
                }
            )
        }
    }
}
