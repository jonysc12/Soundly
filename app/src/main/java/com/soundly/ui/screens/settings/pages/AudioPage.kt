package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.R
import com.soundly.data.repository.NormalizationLevel
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.SoundlyTheme
import kotlin.math.roundToInt

@Composable
fun AudioPage(
    onBack: () -> Unit,
    onEqualizerClick: () -> Unit,
    onConnectionClick: () -> Unit,
    onSafePlaybackClick: () -> Unit,
    viewModel: AudioViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    SettingsLayout(
        title = "",
        onBack = onBack
    ) {
        // Hero Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.settings_audio_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_audio_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Action: Safe Playback
        AudioHeroCard(
            title = stringResource(R.string.audio_safe_playback_title),
            description = stringResource(R.string.audio_safe_playback_desc),
            icon = Icons.Rounded.GppGood,
            onClick = onSafePlaybackClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Playback Group
        AudioGroupHeader(stringResource(R.string.audio_section_playback))
        
        AudioSwitchCard(
            title = stringResource(R.string.audio_gapless_title),
            description = stringResource(R.string.audio_gapless_desc),
            icon = Icons.Rounded.Timer,
            checked = settings.gaplessEnabled,
            onCheckedChange = { viewModel.toggleGapless(it) }
        )

        AudioTransformingCard(
            title = stringResource(R.string.audio_crossfade_title),
            description = stringResource(R.string.audio_crossfade_desc),
            icon = Icons.Rounded.SlowMotionVideo,
            checked = settings.crossfadeEnabled,
            onCheckedChange = { viewModel.toggleCrossfade(it) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.audio_crossfade_duration), style = MaterialTheme.typography.labelLarge)
                    Badge(containerColor = MaterialTheme.colorScheme.primary) { 
                        Text("${settings.crossfadeDuration}s", modifier = Modifier.padding(horizontal = 4.dp)) 
                    }
                }
                Slider(
                    value = settings.crossfadeDuration.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.roundToInt()) },
                    valueRange = 1f..12f,
                    steps = 10
                )
            }
        }

        AudioTransformingCard(
            title = stringResource(R.string.audio_normalization_title),
            description = stringResource(R.string.audio_normalization_desc),
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            checked = settings.normalizationEnabled,
            onCheckedChange = { viewModel.toggleNormalization(it) }
             ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NormalizationLevel.entries.forEach { level ->
                    val label = when(level) {
                        NormalizationLevel.LOW -> stringResource(R.string.audio_level_low)
                        NormalizationLevel.NORMAL -> stringResource(R.string.audio_level_normal)
                        NormalizationLevel.HIGH -> stringResource(R.string.audio_level_high)
                    }
                    FilterChip(
                        selected = settings.normalizationLevel == level,
                        onClick = { viewModel.setNormalizationLevel(level) },
                        label = { Text(label) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }
        }

        AudioSwitchCard(
            title = stringResource(R.string.audio_focus_title),
            description = stringResource(R.string.audio_focus_desc),
            icon = Icons.Rounded.Hearing,
            checked = settings.audioFocusEnabled,
            onCheckedChange = { viewModel.toggleAudioFocus(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sound Group
        AudioGroupHeader(stringResource(R.string.audio_section_sound))

        AudioHeroCard(
            title = stringResource(R.string.audio_equalizer_title),
            description = stringResource(R.string.audio_equalizer_desc),
            icon = Icons.Rounded.GraphicEq,
            onClick = onEqualizerClick
        )

        AudioHeroCard(
            title = stringResource(R.string.audio_connection_features),
            description = stringResource(R.string.audio_connection_desc),
            icon = Icons.Rounded.Bluetooth,
            onClick = onConnectionClick
        )

        AudioSwitchCard(
            title = stringResource(R.string.audio_mono_title),
            description = stringResource(R.string.audio_mono_desc),
            icon = Icons.Rounded.Audiotrack,
            checked = settings.monoEnabled,
            onCheckedChange = { viewModel.toggleMono(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AudioGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )
}

@Composable
private fun AudioHeroCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AudioSwitchCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)

    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun AudioTransformingCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)

    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!checked) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = if (checked) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            AnimatedVisibility(
                visible = checked,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioPagePreview() {
    SoundlyTheme {
        AudioPage(
            onBack = {},
            onEqualizerClick = {},
            onConnectionClick = {},
            onSafePlaybackClick = {}
        )
    }
}
