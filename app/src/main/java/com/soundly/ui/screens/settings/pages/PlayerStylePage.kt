package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.soundly.R
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.PlayerType
import com.soundly.data.repository.ProgressBarType
import com.soundly.player.PlayerUiState
import com.soundly.ui.screens.settings.SettingsLayout

import com.soundly.ui.theme.SoundlyTheme
import com.soundly.ui.theme.rememberArtworkShape
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import com.soundly.ui.componentes.SoundlyWavySlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

@Composable
fun PlayerStylePage(
    onBack: () -> Unit,
    viewModel: AnimationsViewModel = hiltViewModel()
) {
    val progressBarType by viewModel.progressBarType.collectAsState()
    val showThumb by viewModel.showThumb.collectAsState()
    val thickness by viewModel.progressBarThickness.collectAsState()
    val artworkShape by viewModel.artworkShape.collectAsState()
    val textAlignCentered by viewModel.textAlignCentered.collectAsState()
    val marqueeTextEnabled by viewModel.marqueeTextEnabled.collectAsState()
    val carouselEnabled by viewModel.carouselEnabled.collectAsState()
    val playerType by viewModel.playerType.collectAsState()

    PlayerStyleContent(
        progressBarType = progressBarType,
        showThumb = showThumb,
        thickness = thickness,
        artworkShape = artworkShape,
        textAlignCentered = textAlignCentered,
        marqueeTextEnabled = marqueeTextEnabled,
        carouselEnabled = carouselEnabled,
        playerType = playerType,
        onBack = onBack,
        onTypeSelected = viewModel::setProgressBarType,
        onShowThumbChanged = viewModel::setShowThumb,
        onThicknessChanged = viewModel::setProgressBarThickness,
        onArtworkShapeSelected = viewModel::setArtworkShape,
        onTextAlignCenteredChanged = viewModel::setTextAlignCentered,
        onMarqueeTextEnabledChanged = viewModel::setMarqueeTextEnabled,
        onCarouselEnabledChanged = viewModel::setCarouselEnabled
    )
}

@Composable
fun PlayerStyleContent(
    progressBarType: ProgressBarType,
    showThumb: Boolean,
    thickness: Float,
    artworkShape: ArtworkShape,
    textAlignCentered: Boolean,
    marqueeTextEnabled: Boolean,
    carouselEnabled: Boolean,
    playerType: PlayerType,
    onBack: () -> Unit,
    onTypeSelected: (ProgressBarType) -> Unit,
    onShowThumbChanged: (Boolean) -> Unit,
    onThicknessChanged: (Float) -> Unit,
    onArtworkShapeSelected: (ArtworkShape) -> Unit,
    onTextAlignCenteredChanged: (Boolean) -> Unit,
    onMarqueeTextEnabledChanged: (Boolean) -> Unit,
    onCarouselEnabledChanged: (Boolean) -> Unit
) {
    val isModern = playerType == PlayerType.MODERN

    SettingsLayout(title = stringResource(R.string.settings_style_design_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section
            PlayerPreview(
                progressBarType = progressBarType,
                showThumb = showThumb,
                thickness = thickness,
                artworkShape = artworkShape,
                textAlignCentered = textAlignCentered,
                playerType = playerType
            )

            // Options Section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                if (!isModern) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500, delayMillis = 100))
                    ) {
                        ProgressBarSelector(
                            selectedType = progressBarType,
                            onTypeSelected = onTypeSelected,
                            showThumb = showThumb,
                            onShowThumbChanged = onShowThumbChanged,
                            thickness = thickness,
                            onThicknessChanged = onThicknessChanged,
                            isModern = isModern
                        )
                    }
                }

                if (!isModern) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500, delayMillis = 200))
                    ) {
                        ArtworkShapeSelector(
                            selectedShape = artworkShape,
                            onShapeSelected = onArtworkShapeSelected
                        )
                    }
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500, delayMillis = 300))
                ) {
                    PlayerFunctionalitySettings(
                        textAlignCentered = textAlignCentered,
                        onTextAlignCenteredChanged = onTextAlignCenteredChanged,
                        marqueeTextEnabled = marqueeTextEnabled,
                        onMarqueeTextEnabledChanged = onMarqueeTextEnabledChanged,
                        carouselEnabled = carouselEnabled,
                        onCarouselEnabledChanged = onCarouselEnabledChanged,
                        isModern = isModern
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlayerFunctionalitySettings(
    textAlignCentered: Boolean,
    onTextAlignCenteredChanged: (Boolean) -> Unit,
    marqueeTextEnabled: Boolean,
    onMarqueeTextEnabledChanged: (Boolean) -> Unit,
    carouselEnabled: Boolean,
    onCarouselEnabledChanged: (Boolean) -> Unit,
    isModern: Boolean = false
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (!isModern) {
                ListItem(
                    leadingContent = {
                        Icon(Icons.Rounded.FormatAlignCenter, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    headlineContent = { Text(stringResource(R.string.settings_center_text_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_center_text_desc)) },
                    trailingContent = {
                        Switch(
                            checked = textAlignCentered,
                            onCheckedChange = onTextAlignCenteredChanged
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onTextAlignCenteredChanged(!textAlignCentered) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            ListItem(
                leadingContent = {
                    Icon(Icons.Rounded.TextRotationNone, null, tint = MaterialTheme.colorScheme.primary)
                },
                headlineContent = { Text(stringResource(R.string.settings_marquee_title)) },
                supportingContent = { Text(stringResource(R.string.settings_marquee_desc_player)) },
                trailingContent = {
                    Switch(
                        checked = marqueeTextEnabled,
                        onCheckedChange = onMarqueeTextEnabledChanged
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onMarqueeTextEnabledChanged(!marqueeTextEnabled) }
            )

            if (!isModern) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    leadingContent = {
                        Icon(Icons.Rounded.ViewCarousel, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    headlineContent = { Text(stringResource(R.string.settings_carousel_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_carousel_desc)) },
                    trailingContent = {
                        Switch(
                            checked = carouselEnabled,
                            onCheckedChange = onCarouselEnabledChanged
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onCarouselEnabledChanged(!carouselEnabled) }
                )
            }
        }
    }
}


@Composable
private fun PlayerPreview(
    progressBarType: ProgressBarType,
    showThumb: Boolean,
    thickness: Float,
    artworkShape: ArtworkShape,
    textAlignCentered: Boolean,
    playerType: PlayerType = PlayerType.CLASSIC
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_player_preview_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            PlayerSkeleton(
                progressBarType = progressBarType,
                showThumb = showThumb,
                thickness = thickness,
                artworkShape = artworkShape,
                textAlignCentered = textAlignCentered,
                playerType = playerType
            )
        }
    }
}

@Composable
private fun PlayerSkeleton(
    progressBarType: ProgressBarType,
    showThumb: Boolean,
    thickness: Float,
    artworkShape: ArtworkShape,
    textAlignCentered: Boolean,
    playerType: PlayerType = PlayerType.CLASSIC
) {
    if (playerType == PlayerType.MODERN) {
        ModernPlayerSkeleton(thickness)
    } else {
        ClassicPlayerSkeleton(
            progressBarType = progressBarType,
            showThumb = showThumb,
            thickness = thickness,
            artworkShape = artworkShape,
            textAlignCentered = textAlignCentered
        )
    }
}

@Composable
private fun ModernPlayerSkeleton(thickness: Float) {
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(520.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bgColor)
    ) {
        // Artwork Background Effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startY = size.height * 0.5f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = subColor.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp).align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Song Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onColor)
                    Text("Artist Name", style = MaterialTheme.typography.bodySmall, color = subColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = onColor.copy(alpha = 0.08f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MoreVert, null, tint = onColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(shape = CircleShape, color = onColor.copy(alpha = 0.08f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.FavoriteBorder, null, tint = onColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar (Forced Modern look: 11dp, plane, no thumb)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(11.dp)
                        .clip(CircleShape)
                        .background(onColor.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight()
                            .background(onColor)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1:24", style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 8.sp)
                    Text("3:45", style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 8.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = onColor, modifier = Modifier.size(32.dp))
                    Icon(Icons.Rounded.PlayArrow, null, tint = onColor, modifier = Modifier.size(44.dp))
                    Icon(Icons.Rounded.SkipNext, null, tint = onColor, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Extra Controls (Scaled down for preview)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // DISPOSITIVO
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(onColor.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Speaker, null, tint = onColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Phone", color = onColor, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // SHARE
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 28.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(onColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Share, null, tint = onColor, modifier = Modifier.size(12.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    // LYRICS
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(onColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, null, tint = onColor, modifier = Modifier.size(12.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    // QUEUE
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 28.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp))
                            .background(onColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.QueueMusic, null, tint = onColor, modifier = Modifier.size(12.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ClassicPlayerSkeleton(
    progressBarType: ProgressBarType,
    showThumb: Boolean,
    thickness: Float,
    artworkShape: ArtworkShape,
    textAlignCentered: Boolean
) {
    val bgColor = MaterialTheme.colorScheme.surface
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    val buttonSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val currentArtworkShape = rememberArtworkShape(artworkShape)

    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bgColor)
            .padding(
                top = 56.dp,
                bottom = 32.dp,
                start = 8.dp,
                end = 8.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = subColor, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.player_header_playing), style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 7.sp, letterSpacing = 1.sp)
            Icon(Icons.Default.MoreVert, null, tint = subColor, modifier = Modifier.size(20.dp))
        }

        // Artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .aspectRatio(1f)
                .clip(currentArtworkShape)
                .background(surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = subColor.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Info
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (textAlignCentered) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = "Song Title",
                style = MaterialTheme.typography.bodyMedium,
                color = onColor,
                fontWeight = FontWeight.Bold,
                textAlign = if (textAlignCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Artist Name",
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                textAlign = if (textAlignCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Progress Section
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (progressBarType == ProgressBarType.DEFAULT) {
                Slider(
                    value = 0.4f,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = onColor,
                        activeTrackColor = onColor,
                        inactiveTrackColor = onColor.copy(alpha = 0.25f)
                    )
                )
            } else {
                SoundlyWavySlider(
                    value = 0.4f,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    activeColor = onColor,
                    inactiveColor = onColor.copy(alpha = 0.25f),
                    thumbColor = onColor,
                    showThumb = showThumb,
                    isWave = progressBarType == ProgressBarType.WAVE,
                    waveHeight = 7.dp,
                    waveLength = 32.dp,
                    waveThickness = (thickness * 0.85f).dp,
                    trackThickness = (thickness * 0.85f).dp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1:24", style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 8.sp)
                Text("3:45", style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 8.sp)
            }
        }

        // Main Controls Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.SkipPrevious, null, tint = onColor, modifier = Modifier.size(28.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = buttonSurface, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = onColor, modifier = Modifier.size(24.dp))
                }
            }
            Icon(Icons.Rounded.SkipNext, null, tint = onColor, modifier = Modifier.size(28.dp))
        }

        // SECONDARY CONTROLS (Pill style)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(onColor.copy(alpha = 0.10f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier.size(width = 40.dp, height = 28.dp).clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 6.dp, bottomEnd = 6.dp)).background(onColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Shuffle, null, tint = onColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(onColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = onColor, modifier = Modifier.size(14.dp))
                }
                Box(
                    modifier = Modifier.size(width = 40.dp, height = 28.dp).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 14.dp, bottomEnd = 14.dp)).background(onColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Repeat, null, tint = onColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressBarSelector(
    selectedType: ProgressBarType,
    onTypeSelected: (ProgressBarType) -> Unit,
    showThumb: Boolean,
    onShowThumbChanged: (Boolean) -> Unit,
    thickness: Float,
    onThicknessChanged: (Float) -> Unit,
    isModern: Boolean = false
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_progressbar_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                ProgressBarType.entries.forEachIndexed { index, type ->
                    val label = when(type) {
                        ProgressBarType.DEFAULT -> stringResource(R.string.pb_type_default).split(" ")[0]
                        ProgressBarType.WAVE -> stringResource(R.string.pb_type_wave).split(" ")[0]
                        ProgressBarType.PLANE -> stringResource(R.string.pb_type_plane).split(" ")[0]
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ProgressBarType.entries.size),
                        onClick = { onTypeSelected(type) },
                        selected = selectedType == type,
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = {
                            val icon = when(type) {
                                ProgressBarType.DEFAULT -> Icons.Rounded.LinearScale
                                ProgressBarType.WAVE -> Icons.Rounded.Waves
                                ProgressBarType.PLANE -> Icons.Rounded.Maximize
                            }
                            Icon(icon, null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedType != ProgressBarType.DEFAULT,
                transitionSpec = {
                    (expandVertically(animationSpec = tween(400)) + fadeIn())
                        .togetherWith(shrinkVertically(animationSpec = tween(400)) + fadeOut())
                },
                label = "ProgressBarSettings"
            ) { hasSubSettings ->
                if (hasSubSettings) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        // Show Thumb option (disabled if Modern + Plane as it's forced off)
                        val isThumbDisabled = isModern && selectedType == ProgressBarType.PLANE
                        
                        ListItem(
                            headlineContent = { 
                                Text(
                                    text = stringResource(R.string.settings_show_thumb_title),
                                    color = if (isThumbDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.Unspecified
                                ) 
                            },
                            supportingContent = { 
                                Text(
                                    text = if (isThumbDisabled) "No disponible en estilo Moderno + Plano" else stringResource(R.string.settings_show_thumb_desc),
                                    color = if (isThumbDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.Unspecified
                                ) 
                            },
                            trailingContent = {
                                Switch(
                                    checked = if (isThumbDisabled) false else showThumb,
                                    onCheckedChange = if (isThumbDisabled) null else onShowThumbChanged,
                                    enabled = !isThumbDisabled,
                                    thumbContent = if (showThumb && !isThumbDisabled) {
                                        { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                                    } else null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = if (isThumbDisabled) Modifier else Modifier.clickable { onShowThumbChanged(!showThumb) }
                        )

                        // Hide thickness slider if Modern (forced to 11dp)
                        if (!isModern) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LineWeight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_bar_thickness_title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${thickness.toInt()}dp", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Slider(
                                    value = thickness,
                                    onValueChange = onThicknessChanged,
                                    valueRange = 2f..12f,
                                    steps = 9,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.settings_thickness_normal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(R.string.settings_thickness_bold), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtworkShapeSelector(
    selectedShape: ArtworkShape,
    onShapeSelected: (ArtworkShape) -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_artwork_shape_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val shapes = ArtworkShape.entries
                val chunks = shapes.chunked(3) // 3 items per row for better grid
                chunks.forEach { rowShapes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowShapes.forEach { shape ->
                            ShapeItem(
                                shapeType = shape,
                                isSelected = shape == selectedShape,
                                onClick = { onShapeSelected(shape) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowShapes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapeItem(
    shapeType: ArtworkShape,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = rememberArtworkShape(shapeType)

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .height(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(shape)
                    .background(contentColor)
                    .then(
                        if (isSelected) Modifier.background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        ) else Modifier
                    )
            )
            Spacer(modifier = Modifier.height(10.dp))
            val shapeLabel = when(shapeType) {
                ArtworkShape.DEFAULT -> stringResource(R.string.shape_default)
                ArtworkShape.CIRCLE -> stringResource(R.string.shape_circle)
                ArtworkShape.SQUARE -> stringResource(R.string.shape_square)
                ArtworkShape.ARCH -> stringResource(R.string.shape_arch)
                ArtworkShape.PILL -> stringResource(R.string.shape_pill)
                ArtworkShape.ARROW -> stringResource(R.string.shape_arrow)
                ArtworkShape.PENTAGON -> stringResource(R.string.shape_pentagon)
                ArtworkShape.COOKIE_4 -> stringResource(R.string.shape_cookie_4)
                ArtworkShape.COOKIE_6 -> stringResource(R.string.shape_cookie_6)
                ArtworkShape.COOKIE_7 -> stringResource(R.string.shape_cookie_7)
                ArtworkShape.CLOVER_4 -> stringResource(R.string.shape_clover_4)
            }
            Text(
                text = shapeLabel,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp
            )
        }
    }
}

@Preview(showBackground = true, name = "Wave Style")
@Composable
fun PlayerStylePageWavePreview() {
    SoundlyTheme {
        PlayerStyleContent(
            progressBarType = ProgressBarType.WAVE,
            showThumb = true,
            thickness = 7f,
            artworkShape = ArtworkShape.DEFAULT,
            textAlignCentered = false,
            marqueeTextEnabled = false,
            carouselEnabled = false,
            playerType = PlayerType.CLASSIC,
            onBack = {},
            onTypeSelected = {},
            onShowThumbChanged = {},
            onThicknessChanged = {},
            onArtworkShapeSelected = {},
            onTextAlignCenteredChanged = {},
            onMarqueeTextEnabledChanged = {},
            onCarouselEnabledChanged = {}
        )
    }
}

@Preview(showBackground = true, name = "Material 3 Style")
@Composable
fun PlayerStylePageDefaultPreview() {
    SoundlyTheme {
        PlayerStyleContent(
            progressBarType = ProgressBarType.DEFAULT,
            showThumb = false,
            thickness = 7f,
            artworkShape = ArtworkShape.COOKIE_6,
            textAlignCentered = true,
            marqueeTextEnabled = true,
            carouselEnabled = true,
            playerType = PlayerType.CLASSIC,
            onBack = {},
            onTypeSelected = {},
            onShowThumbChanged = {},
            onThicknessChanged = {},
            onArtworkShapeSelected = {},
            onTextAlignCenteredChanged = {},
            onMarqueeTextEnabledChanged = {},
            onCarouselEnabledChanged = {}
        )
    }
}
