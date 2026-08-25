package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.SoundlyTheme
import androidx.compose.ui.res.stringResource
import com.soundly.R
import com.soundly.data.repository.ThemeMode

@Composable
fun InterfacePage(
    onBack: () -> Unit,
    onPlayerClick: () -> Unit = {},
    onMiniPlayerClick: () -> Unit = {},
    onHomeSettingsClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    viewModel: InterfaceViewModel = hiltViewModel()
) {
    val showHomePage by viewModel.showHomePage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
    val vividColors by viewModel.vividColors.collectAsState()

    InterfacePageContent(
        showHomePage = showHomePage,
        themeMode = themeMode,
        dynamicColorsEnabled = dynamicColorsEnabled,
        vividColors = vividColors,
        onBack = onBack,
        onPlayerClick = onPlayerClick,
        onMiniPlayerClick = onMiniPlayerClick,
        onHomeSettingsClick = onHomeSettingsClick,
        onLyricsClick = onLyricsClick,
        onSetShowHomePage = { viewModel.setShowHomePage(it) },
        onSetThemeMode = { viewModel.setThemeMode(it) },
        onSetDynamicColorsEnabled = { viewModel.setDynamicColorsEnabled(it) },
        onSetVividColors = { viewModel.setVividColors(it) }
    )
}

@Composable
fun InterfacePageContent(
    showHomePage: Boolean,
    themeMode: ThemeMode,
    dynamicColorsEnabled: Boolean,
    vividColors: Boolean,
    onBack: () -> Unit,
    onPlayerClick: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onHomeSettingsClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSetShowHomePage: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColorsEnabled: (Boolean) -> Unit,
    onSetVividColors: (Boolean) -> Unit
) {
    var showThemeSheet by remember { mutableStateOf(false) }

    val themeDescription = when (themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.interface_theme_light)
        ThemeMode.DARK -> stringResource(R.string.interface_theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.interface_theme_system)
    }

    val themeIcon = when (themeMode) {
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
        ThemeMode.SYSTEM -> Icons.Rounded.Android
    }

    if (showThemeSheet) {
        ThemeSelectionSheet(
            currentTheme = themeMode,
            onThemeSelected = {
                onSetThemeMode(it)
                showThemeSheet = false
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    SettingsLayout(title = stringResource(R.string.settings_interface_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.interface_section_universal)) {
                M3ListItem(
                    icon = themeIcon,
                    title = stringResource(R.string.interface_theme_title),
                    description = themeDescription,
                    color = Color.Transparent,
                    drawDivider = true,
                    onClick = { showThemeSheet = true }
                )
                M3ListItem(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.interface_dynamic_colors_title),
                    description = stringResource(R.string.interface_dynamic_colors_desc),
                    color = Color.Transparent,
                    drawDivider = true,
                    onClick = { onSetDynamicColorsEnabled(!dynamicColorsEnabled) },
                    trailingContent = {
                        Switch(
                            checked = dynamicColorsEnabled,
                            onCheckedChange = { onSetDynamicColorsEnabled(it) }
                        )
                    }
                )
                M3ListItem(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.interface_vivid_colors_title),
                    description = stringResource(R.string.interface_vivid_colors_desc),
                    color = Color.Transparent,
                    drawDivider = false,
                    onClick = { onSetVividColors(!vividColors) },
                    trailingContent = {
                        Switch(
                            checked = vividColors,
                            onCheckedChange = { onSetVividColors(it) }
                        )
                    }
                )
            }

            SettingsGroup(title = stringResource(R.string.interface_section_home)) {
                M3ListItem(
                    title = stringResource(R.string.interface_enable_home_title),
                    description = stringResource(R.string.interface_enable_home_desc),
                    color = Color.Transparent,
                    drawDivider = showHomePage,
                    onClick = { onSetShowHomePage(!showHomePage) },
                    trailingContent = {
                        Switch(
                            checked = showHomePage,
                            onCheckedChange = { onSetShowHomePage(it) }
                        )
                    }
                )
                AnimatedVisibility(
                    visible = showHomePage,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    M3ListItem(
                        icon = Icons.Rounded.Home,
                        title = stringResource(R.string.interface_customize_home_title),
                        description = stringResource(R.string.interface_customize_home_desc),
                        color = Color.Transparent,
                        onClick = onHomeSettingsClick
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.interface_section_player)) {
                PlayerSettings(
                    onPlayerClick = onPlayerClick,
                    onMiniPlayerClick = onMiniPlayerClick,
                    onLyricsClick = onLyricsClick
                )
            }
        }
    }
}

@Composable
private fun PlayerSettings(
    onPlayerClick: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onLyricsClick: () -> Unit
) {
    Column {
        M3ListItem(
            icon = Icons.Rounded.PlayCircle,
            title = stringResource(R.string.interface_player_title),
            description = stringResource(R.string.interface_player_desc),
            color = Color.Transparent,
            drawDivider = true,
            onClick = onPlayerClick
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubSettingItem(
                title = stringResource(R.string.interface_mini_player_title),
                modifier = Modifier.weight(1f),
                onClick = onMiniPlayerClick
            )
            SubSettingItem(
                title = stringResource(R.string.interface_lyrics_title),
                modifier = Modifier.weight(1f),
                onClick = onLyricsClick
            )
        }
    }
}

@Composable
private fun SubSettingItem(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionSheet(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.interface_theme_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
            )

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ThemeOption(
                        text = stringResource(R.string.interface_theme_system),
                        icon = Icons.Rounded.Android,
                        selected = currentTheme == ThemeMode.SYSTEM,
                        onClick = { onThemeSelected(ThemeMode.SYSTEM) },
                        drawDivider = true
                    )
                    ThemeOption(
                        text = stringResource(R.string.interface_theme_light),
                        icon = Icons.Rounded.LightMode,
                        selected = currentTheme == ThemeMode.LIGHT,
                        onClick = { onThemeSelected(ThemeMode.LIGHT) },
                        drawDivider = true
                    )
                    ThemeOption(
                        text = stringResource(R.string.interface_theme_dark),
                        icon = Icons.Rounded.DarkMode,
                        selected = currentTheme == ThemeMode.DARK,
                        onClick = { onThemeSelected(ThemeMode.DARK) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    drawDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        if (drawDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InterfacePagePreview() {
    SoundlyTheme {
        InterfacePageContent(
            showHomePage = true,
            themeMode = ThemeMode.SYSTEM,
            dynamicColorsEnabled = true,
            vividColors = false,
            onBack = {},
            onPlayerClick = {},
            onMiniPlayerClick = {},
            onHomeSettingsClick = {},
            onLyricsClick = {},
            onSetShowHomePage = {},
            onSetThemeMode = {},
            onSetDynamicColorsEnabled = {},
            onSetVividColors = {}
        )
    }
}
