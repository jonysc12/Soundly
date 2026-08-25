package com.soundly.ui.screens.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.SoundlyTheme

import androidx.compose.ui.res.stringResource
import com.soundly.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundly.data.repository.PlayerType

@Composable
fun MiniPlayerAnimationsPage(
    onBack: () -> Unit,
    onAnimationsClick: () -> Unit = {},
    onStyleClick: () -> Unit = {},
    viewModel: AnimationsViewModel = hiltViewModel()
) {
    val playerType by viewModel.playerType.collectAsStateWithLifecycle()

    SettingsLayout(title = stringResource(R.string.player_settings_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PlayerTypeSelector(
                selectedOption = if (playerType == PlayerType.CLASSIC) "Clasico" else "Moderno",
                onOptionSelected = { 
                    viewModel.setPlayerType(if (it == "Clasico") PlayerType.CLASSIC else PlayerType.MODERN)
                }
            )

            SettingsGroup(title = stringResource(R.string.player_section_customization)) {
                M3ListItem(
                    icon = Icons.Rounded.Animation,
                    title = stringResource(R.string.player_animation_title),
                    description = stringResource(R.string.player_animation_desc),
                    color = Color.Transparent,
                    drawDivider = true,
                    onClick = onAnimationsClick
                )
                M3ListItem(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.player_style_title),
                    description = stringResource(R.string.player_style_desc),
                    color = Color.Transparent,
                    onClick = onStyleClick
                )
            }
        }
    }
}

@Composable
private fun PlayerTypeSelector(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val options = listOf("Clasico", "Moderno")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onOptionSelected(option) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = backgroundColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MiniPlayerAnimationsPagePreview() {
    SoundlyTheme {
        MiniPlayerAnimationsPage(onBack = {})
    }
}
