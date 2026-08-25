package com.soundly.ui.screens.settings.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.R
import com.soundly.data.repository.ThemeMode
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.SoundlyTheme

@Composable
fun GeneralPage(
    onBack: () -> Unit,
    onLanguageClick: () -> Unit = {},
    viewModel: GeneralViewModel = hiltViewModel()
) {
    val selectedLanguageCode by viewModel.selectedLanguageCode.collectAsState(initial = "")

    GeneralPageContent(
        selectedLanguageCode = selectedLanguageCode,
        onBack = onBack,
        onLanguageClick = onLanguageClick
    )
}

@Composable
fun GeneralPageContent(
    selectedLanguageCode: String?,
    onBack: () -> Unit,
    onLanguageClick: () -> Unit
) {
    val systemLabel = stringResource(R.string.language_selection_system)
    val languageName = remember(selectedLanguageCode, systemLabel) {
        when (selectedLanguageCode) {
            "en" -> "English"
            "es" -> "Español"
            "zh-CN" -> "简体中文"
            "in" -> "Bahasa Indonesia"
            "de" -> "Deutsch"
            "pt-BR" -> "Português (Brasil)"
            "ru" -> "Русский"
            else -> systemLabel
        }
    }

    SettingsLayout(title = stringResource(R.string.settings_general_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.interface_section_universal)) {
                M3ListItem(
                    icon = Icons.Rounded.Translate,
                    title = stringResource(R.string.interface_language_title),
                    description = languageName,
                    color = Color.Transparent,
                    onClick = onLanguageClick
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_about_title)) {
                M3ListItem(
                    icon = Icons.Rounded.SystemUpdate,
                    title = stringResource(R.string.settings_check_updates_title),
                    description = stringResource(R.string.settings_check_updates_desc),
                    color = Color.Transparent,
                    drawDivider = true,
                    onClick = { /* TODO: Check for updates */ }
                )
                M3ListItem(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_about_title),
                    description = stringResource(R.string.settings_about_desc),
                    color = Color.Transparent,
                    onClick = { /* TODO: About dialog/page */ }
                )
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
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeneralPagePreview() {
    SoundlyTheme {
        GeneralPageContent(
            selectedLanguageCode = "es",
            onBack = {},
            onLanguageClick = {}
        )
    }
}
