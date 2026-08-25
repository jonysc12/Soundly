package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.MiniProgressBarType
import com.soundly.data.repository.MiniProgressBarThickness
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.R
import com.soundly.ui.theme.SoundlyTheme
import com.soundly.ui.theme.rememberArtworkShape
import com.soundly.ui.componentes.MiniPlayer
import com.soundly.ui.componentes.MiniPlayerMetadata

@Composable
fun MiniPlayerStylePage(
    onBack: () -> Unit,
    viewModel: AnimationsViewModel = hiltViewModel()
) {
    val miniArtworkShape by viewModel.miniArtworkShape.collectAsState()
    val miniProgressBarType by viewModel.miniProgressBarType.collectAsState()
    val miniProgressBarThickness by viewModel.miniProgressBarThickness.collectAsState()
    val showMiniPrevious by viewModel.showMiniPrevious.collectAsState()
    val swipeToDismiss by viewModel.swipeToDismiss.collectAsState()
    val marqueeTextEnabled by viewModel.marqueeTextEnabled.collectAsState()

    MiniPlayerStyleContent(
        miniArtworkShape = miniArtworkShape,
        miniProgressBarType = miniProgressBarType,
        miniProgressBarThickness = miniProgressBarThickness,
        showMiniPrevious = showMiniPrevious,
        swipeToDismiss = swipeToDismiss,
        marqueeTextEnabled = marqueeTextEnabled,
        onBack = onBack,
        onArtworkShapeSelected = viewModel::setMiniArtworkShape,
        onMiniProgressBarTypeSelected = viewModel::setMiniProgressBarType,
        onMiniProgressBarThicknessSelected = viewModel::setMiniProgressBarThickness,
        onShowMiniPreviousChanged = viewModel::setShowMiniPrevious,
        onSwipeToDismissChanged = viewModel::setSwipeToDismiss,
        onMarqueeTextEnabledChanged = viewModel::setMarqueeTextEnabled
    )
}

@Composable
fun MiniPlayerStyleContent(
    miniArtworkShape: ArtworkShape,
    miniProgressBarType: MiniProgressBarType,
    miniProgressBarThickness: MiniProgressBarThickness,
    showMiniPrevious: Boolean,
    swipeToDismiss: Boolean,
    marqueeTextEnabled: Boolean,
    onBack: () -> Unit,
    onArtworkShapeSelected: (ArtworkShape) -> Unit,
    onMiniProgressBarTypeSelected: (MiniProgressBarType) -> Unit,
    onMiniProgressBarThicknessSelected: (MiniProgressBarThickness) -> Unit,
    onShowMiniPreviousChanged: (Boolean) -> Unit,
    onSwipeToDismissChanged: (Boolean) -> Unit,
    onMarqueeTextEnabledChanged: (Boolean) -> Unit
) {
    SettingsLayout(title = stringResource(R.string.settings_mini_player_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section
            MiniPlayerPreview(
                artworkShape = miniArtworkShape,
                miniProgressBarType = miniProgressBarType,
                miniProgressBarThickness = miniProgressBarThickness,
                showMiniPrevious = showMiniPrevious,
                swipeToDismiss = swipeToDismiss,
                marqueeTextEnabled = marqueeTextEnabled
            )

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
                    MiniPlayerStyleSettings(
                        miniProgressBarType = miniProgressBarType,
                        onMiniProgressBarTypeSelected = onMiniProgressBarTypeSelected,
                        miniProgressBarThickness = miniProgressBarThickness,
                        onMiniProgressBarThicknessSelected = onMiniProgressBarThicknessSelected,
                        showMiniPrevious = showMiniPrevious,
                        onShowMiniPreviousChanged = onShowMiniPreviousChanged
                    )
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500, delayMillis = 200))
                ) {
                    MiniArtworkShapeSelector(
                        selectedShape = miniArtworkShape,
                        onShapeSelected = onArtworkShapeSelected
                    )
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500, delayMillis = 300))
                ) {
                    MiniPlayerFunctionalitySettings(
                        swipeToDismiss = swipeToDismiss,
                        onSwipeToDismissChanged = onSwipeToDismissChanged,
                        marqueeTextEnabled = marqueeTextEnabled,
                        onMarqueeTextEnabledChanged = onMarqueeTextEnabledChanged
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MiniPlayerStyleSettings(
    miniProgressBarType: MiniProgressBarType,
    onMiniProgressBarTypeSelected: (MiniProgressBarType) -> Unit,
    miniProgressBarThickness: MiniProgressBarThickness,
    onMiniProgressBarThicknessSelected: (MiniProgressBarThickness) -> Unit,
    showMiniPrevious: Boolean,
    onShowMiniPreviousChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_style_design_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_progressbar_design_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                MiniProgressBarType.entries.forEachIndexed { index, type ->
                    val label = type.label.replace(" (Predeterminado)", "")
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = MiniProgressBarType.entries.size),
                        onClick = { onMiniProgressBarTypeSelected(type) },
                        selected = miniProgressBarType == type,
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = {
                            val icon = when(type) {
                                MiniProgressBarType.WAVE -> Icons.Rounded.Waves
                                MiniProgressBarType.PLANE -> Icons.Rounded.Maximize
                                MiniProgressBarType.NONE -> Icons.Rounded.Block
                            }
                            Icon(icon, null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = miniProgressBarType == MiniProgressBarType.PLANE,
                transitionSpec = {
                    (expandVertically(animationSpec = tween(400)) + fadeIn())
                        .togetherWith(shrinkVertically(animationSpec = tween(400)) + fadeOut())
                },
                label = "ThicknessSettings"
            ) { showThickness ->
                if (showThickness) {
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_progress_thickness_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            MiniProgressBarThickness.entries.forEachIndexed { index, thickness ->
                                val label = when(thickness) {
                                    MiniProgressBarThickness.NORMAL -> stringResource(R.string.mini_thickness_normal)
                                    MiniProgressBarThickness.MEDIANO -> stringResource(R.string.mini_thickness_medium)
                                    MiniProgressBarThickness.GORDO -> stringResource(R.string.mini_thickness_bold)
                                }
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = MiniProgressBarThickness.entries.size),
                                    onClick = { onMiniProgressBarThicknessSelected(thickness) },
                                    selected = miniProgressBarThickness == thickness,
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                leadingContent = { Icon(Icons.Rounded.SkipPrevious, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(stringResource(R.string.settings_show_previous_title)) },
                supportingContent = { Text(stringResource(R.string.settings_show_previous_desc)) },
                trailingContent = {
                    Switch(
                        checked = showMiniPrevious,
                        onCheckedChange = onShowMiniPreviousChanged,
                        thumbContent = if (showMiniPrevious) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onShowMiniPreviousChanged(!showMiniPrevious) }
            )
        }
    }
}

@Composable
private fun MiniPlayerFunctionalitySettings(
    swipeToDismiss: Boolean,
    onSwipeToDismissChanged: (Boolean) -> Unit,
    marqueeTextEnabled: Boolean,
    onMarqueeTextEnabledChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_functionality_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                leadingContent = { Icon(Icons.Rounded.SwipeRight, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(stringResource(R.string.settings_swipe_to_dismiss_title)) },
                supportingContent = { Text(stringResource(R.string.settings_swipe_to_dismiss_desc)) },
                trailingContent = {
                    Switch(
                        checked = swipeToDismiss,
                        onCheckedChange = onSwipeToDismissChanged,
                        thumbContent = if (swipeToDismiss) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onSwipeToDismissChanged(!swipeToDismiss) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ListItem(
                leadingContent = { Icon(Icons.Rounded.TextRotationNone, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(stringResource(R.string.settings_marquee_title)) },
                supportingContent = { Text(stringResource(R.string.settings_marquee_desc)) },
                trailingContent = {
                    Switch(
                        checked = marqueeTextEnabled,
                        onCheckedChange = onMarqueeTextEnabledChanged,
                        thumbContent = if (marqueeTextEnabled) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onMarqueeTextEnabledChanged(!marqueeTextEnabled) }
            )
        }
    }
}

@Composable
private fun MiniArtworkShapeSelector(
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
                val chunks = shapes.chunked(3)
                chunks.forEach { rowShapes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowShapes.forEach { shape ->
                            MiniShapeItem(
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

@Composable
private fun MiniShapeItem(
    shapeType: ArtworkShape,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = rememberArtworkShape(shapeType)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

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

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .height(100.dp)
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
                    .size(36.dp)
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
            Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun MiniPlayerPreview(
    artworkShape: ArtworkShape,
    miniProgressBarType: MiniProgressBarType = MiniProgressBarType.WAVE,
    miniProgressBarThickness: MiniProgressBarThickness = MiniProgressBarThickness.NORMAL,
    showMiniPrevious: Boolean = false,
    swipeToDismiss: Boolean = true,
    marqueeTextEnabled: Boolean = false
) {
    var isDismissed by remember(artworkShape, miniProgressBarType, miniProgressBarThickness, showMiniPrevious, swipeToDismiss, marqueeTextEnabled) { 
        mutableStateOf(false) 
    }

    val previewArtworkColor = MaterialTheme.colorScheme.surfaceVariant
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_preview_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .padding(vertical = 80.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isDismissed) {
                MiniPlayer(
                    metadata = MiniPlayerMetadata(
                        songName = if (marqueeTextEnabled) "Song Title with very long name to test marquee effect" else "Song Title",
                        artistName = "Artist Name",
                        isPlaying = true,
                        artwork = previewArtworkColor
                    ),
                    progress = { 0.4f },
                    onPlayPauseClick = {},
                    artworkShape = artworkShape,
                    accentColor = previewArtworkColor,
                    miniProgressBarType = miniProgressBarType,
                    miniProgressBarThickness = miniProgressBarThickness,
                    showPrevious = showMiniPrevious,
                    swipeToDismiss = swipeToDismiss,
                    marqueeTextEnabled = marqueeTextEnabled,
                    onDismiss = { isDismissed = true }
                )
            }
 else {
                Text(
                    text = stringResource(R.string.settings_mini_player_closed_preview),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { isDismissed = false }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MiniPlayerStylePagePreview() {
    SoundlyTheme {
        MiniPlayerStyleContent(
            miniArtworkShape = ArtworkShape.DEFAULT,
            miniProgressBarType = MiniProgressBarType.WAVE,
            miniProgressBarThickness = MiniProgressBarThickness.NORMAL,
            showMiniPrevious = true,
            swipeToDismiss = true,
            marqueeTextEnabled = false,
            onBack = {},
            onArtworkShapeSelected = {},
            onMiniProgressBarTypeSelected = {},
            onMiniProgressBarThicknessSelected = {},
            onShowMiniPreviousChanged = {},
            onSwipeToDismissChanged = {},
            onMarqueeTextEnabledChanged = {}
        )
    }
}
