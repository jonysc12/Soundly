package com.soundly.inicio.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.soundly.R
import com.soundly.ui.componentes.agslFrostedGlass
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.LocalIsDarkTheme
import com.soundly.ui.theme.SoundlyTheme
import com.soundly.data.preferences.LanguagePreferences
import com.soundly.ui.utils.LocaleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LanguageItem(val name: String, val nativeName: String, val code: String)

@Composable
fun LanguageSelectionScreen(
    navController: NavHostController,
    onLanguageSelected: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Lista base de idiomas
    val baseLanguages = remember {
        listOf(
            LanguageItem(context.getString(R.string.language_selection_system), context.getString(R.string.language_selection_system_desc), ""),
            LanguageItem("Inglés", "English", "en"),
            LanguageItem("Español", "Español", "es"),
            LanguageItem("Chinese (Simplified)", "简体中文", "zh-CN"),
            LanguageItem("Indonesio", "Bahasa Indonesia", "in"),
            LanguageItem("Alemán", "Deutsch", "de"),
            LanguageItem("Portugués (Brasil)", "Português (Brasil)", "pt-BR"),
            LanguageItem("Ruso", "Русский", "ru")
        )
    }

    // Estado persistente del idioma seleccionado
    var selectedLanguage by rememberSaveable { mutableStateOf(baseLanguages[0].name) }
    
    // Lista mutable que se reordena
    val languages = remember {
        val current = baseLanguages.find { it.name == selectedLanguage } ?: baseLanguages[0]
        val rest = baseLanguages.filter { it.name != current.name }
        mutableStateListOf<LanguageItem>().apply { 
            add(current)
            addAll(rest) 
        }
    }
    
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsLayout(
            title = stringResource(R.string.language_selection_title),
            onBack = { navController.popBackStack() },
            scrollable = false
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.language_selection_header),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.language_selection_description),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp,
                            letterSpacing = 0.25.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .alpha(0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text(
                        text = stringResource(R.string.language_selection_select),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                val visibleLanguages = if (isExpanded) languages else languages.take(3)
                
                itemsIndexed(
                    items = visibleLanguages,
                    key = { _, lang -> lang.name }
                ) { index, lang ->
                    val isFirst = index == 0
                    val isLast = index == visibleLanguages.size - 1 && !(!isExpanded && languages.size > 3)
                    
                    val shape = when {
                        isFirst && isLast -> MaterialTheme.shapes.extraLarge
                        isFirst -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        isLast -> RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                        else -> RoundedCornerShape(0.dp)
                    }

                    LanguageRow(
                        lang = lang,
                        isSelected = selectedLanguage == lang.name,
                        showDivider = index < visibleLanguages.size - 1 || (!isExpanded && languages.size > 3),
                        shape = shape,
                        modifier = Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        onClick = {
                            if (selectedLanguage != lang.name) {
                                moveLanguageToTop(languages, lang)
                                selectedLanguage = lang.name
                                
                                scope.launch {
                                    LanguagePreferences.setLanguageCode(context, lang.code)
                                    // Delay para ver la animación de reordenamiento antes del cambio de texto
                                    delay(180)
                                    val activity = context as? android.app.Activity
                                    LocaleManager.applyLanguageSeamless(lang.code, activity)
                                }
                            }
                        }
                    )
                }

                if (languages.size > 3) {
                    item(key = "expand_button") {
                        M3ListItem(
                            title = if (isExpanded) stringResource(R.string.language_selection_see_less) else stringResource(R.string.language_selection_see_more),
                            description = if (isExpanded) stringResource(R.string.language_selection_see_less_desc) else stringResource(R.string.language_selection_see_more_desc),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            onClick = { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        ContinueButton(
            onContinueClick = onLanguageSelected,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 48.dp)
        )
    }
}

@Composable
private fun LanguageRow(
    lang: LanguageItem,
    isSelected: Boolean,
    showDivider: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    M3ListItem(
        icon = if (lang.code == "") Icons.Rounded.SettingsBackupRestore else Icons.Rounded.Translate,
        title = lang.name,
        description = lang.nativeName,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        drawDivider = showDivider,
        shape = shape,
        modifier = modifier,
        onClick = onClick,
        trailingContent = {
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

private fun moveLanguageToTop(languages: SnapshotStateList<LanguageItem>, lang: LanguageItem) {
    val index = languages.indexOf(lang)
    if (index > 0) {
        val item = languages.removeAt(index)
        languages.add(0, item)
    }
}

@Composable
private fun ContinueButton(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val borderAlpha = if (isDark) 0.3f else 0.05f
    val containerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size(90.dp)
            .clip(CircleShape)
            .agslFrostedGlass(
                radius = 25f,
                tint = Color.Transparent
            )
            .border(
                1.dp,
                contentColor.copy(alpha = borderAlpha),
                CircleShape
            )
            .background(containerColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onContinueClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "Continuar",
            modifier = Modifier.size(36.dp),
            tint = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionScreenPreview() {
    SoundlyTheme {
        LanguageSelectionScreen(
            navController = rememberNavController(),
            onLanguageSelected = {}
        )
    }
}
