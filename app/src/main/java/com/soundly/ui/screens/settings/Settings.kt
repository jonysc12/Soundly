package com.soundly.ui.screens.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.res.stringResource
import com.soundly.R
import com.soundly.ui.componentes.SoundlyUserHeader
import com.soundly.ui.componentes.SoundlyUserHeaderContent
import com.soundly.ui.componentes.HeaderMode
import com.soundly.viewmodel.HomeScreenViewModel
import com.soundly.viewmodel.HomeUiState
import com.soundly.inicio.ui.ProfileScreen
import com.soundly.inicio.ui.MediaScannerScreen
import com.soundly.inicio.ui.LanguageSelectionScreen
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.ThemeMode
import com.soundly.data.repository.UserSettingsRepository
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.soundly.cloud.SoundlyCloudActivity
import com.soundly.ui.components.M3ListItem
import androidx.compose.ui.graphics.toArgb
import com.soundly.ui.theme.LocalIsDarkTheme
import com.soundly.ui.theme.SoundlyTheme
import com.soundly.ui.screens.settings.pages.GeneralPage
import com.soundly.ui.screens.settings.pages.AudioPage
import com.soundly.ui.screens.settings.pages.SafePlaybackPage
import com.soundly.ui.screens.settings.pages.AudioViewModel
import com.soundly.ui.screens.settings.pages.EqualizerPage
import com.soundly.ui.screens.settings.pages.InterfacePage
import com.soundly.ui.screens.settings.pages.HomeSettingsPage
import com.soundly.ui.screens.settings.pages.MiniPlayerStylePage
import com.soundly.ui.screens.settings.pages.MiniPlayerAnimationsPage
import com.soundly.ui.screens.settings.pages.PlayerAnimationsPage
import com.soundly.ui.screens.settings.pages.PlayerStylePage
import com.soundly.ui.screens.settings.pages.LyricsPage
import com.soundly.ui.screens.settings.pages.ConnectionPage
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class Settings : androidx.appcompat.app.AppCompatActivity() {

    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            val themeMode by userSettingsRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColorsEnabled by userSettingsRepository.dynamicColorsEnabledFlow.collectAsState(initial = false)

            // Actualización dinámica del fondo para evitar flashes en animaciones
            val isDark = when(themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { isDark }
                )
                window.isNavigationBarContrastEnforced = false
            }

            SoundlyTheme(themeMode = themeMode, dynamicColor = dynamicColorsEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeScreenViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
    val cloudEnabled by viewModel.cloudEnabled.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    SettingsScreenContent(
        currentSection = currentSection,
        themeMode = themeMode,
        dynamicColorsEnabled = dynamicColorsEnabled,
        cloudEnabled = cloudEnabled,
        homeUiState = homeUiState,
        onBack = onBack,
        onNavigate = { viewModel.navigateTo(it) },
        onHandleBack = { viewModel.handleBack() },
        onUpdateCloudEnabled = { viewModel.updateCloudEnabled(it) },
        repository = viewModel.repository
    )
}

@Composable
internal fun SettingsScreenContent(
    currentSection: SettingsSection,
    themeMode: ThemeMode,
    dynamicColorsEnabled: Boolean,
    cloudEnabled: Boolean,
    homeUiState: HomeUiState,
    onBack: () -> Unit,
    onNavigate: (SettingsSection) -> Unit,
    onHandleBack: () -> Unit,
    onUpdateCloudEnabled: (Boolean) -> Unit,
    repository: MusicRepository? = null
) {
    BackHandler(enabled = currentSection != SettingsSection.Main) {
        onHandleBack()
    }

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            if (targetState != SettingsSection.Main) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "SettingsTransition"
    ) { section ->
        when (section) {
            SettingsSection.Main -> {
                MainSettingsSection(
                    onBack = onBack,
                    onEditProfile = { onNavigate(SettingsSection.Profile) },
                    onScanLibrary = { onNavigate(SettingsSection.Scan) },
                    onGeneralClick = { onNavigate(SettingsSection.General) },
                    onAudioClick = { onNavigate(SettingsSection.Audio) },
                    onInterfaceClick = { onNavigate(SettingsSection.Interface) },
                    themeMode = themeMode,
                    dynamicColorsEnabled = dynamicColorsEnabled,
                    cloudEnabled = cloudEnabled,
                    homeUiState = homeUiState,
                    onUpdateCloudEnabled = onUpdateCloudEnabled
                )
            }

            SettingsSection.Profile -> ProfileScreen(
                navController = rememberNavController(),
                onProfileCreated = { onNavigate(SettingsSection.Main) },
                onBack = { onNavigate(SettingsSection.Main) }
            )

            SettingsSection.Scan -> {
                if (repository != null) {
                    MediaScannerScreen(
                        onBack = { onNavigate(SettingsSection.Main) },
                        onScanConfirmed = { onNavigate(SettingsSection.Main) },
                        repository = repository
                    )
                }
            }

            SettingsSection.General -> GeneralPage(
                onBack = { onNavigate(SettingsSection.Main) },
                onLanguageClick = { onNavigate(SettingsSection.Language) }
            )

            SettingsSection.Audio -> AudioPage(
                onBack = { onNavigate(SettingsSection.Main) },
                onEqualizerClick = { onNavigate(SettingsSection.Equalizer) },
                onConnectionClick = { onNavigate(SettingsSection.Connection) },
                onSafePlaybackClick = { onNavigate(SettingsSection.SafePlayback) }
            )

            SettingsSection.SafePlayback -> {
                SafePlaybackPage(
                    onBack = { onNavigate(SettingsSection.Audio) },
                    viewModel = hiltViewModel()
                )
            }

            SettingsSection.Connection -> ConnectionPage(
                onBack = { onNavigate(SettingsSection.Audio) }
            )

            SettingsSection.Equalizer -> EqualizerPage(
                onBack = { onNavigate(SettingsSection.Audio) }
            )

            SettingsSection.Interface -> InterfacePage(
                onBack = { onNavigate(SettingsSection.Main) },
                onPlayerClick = { onNavigate(SettingsSection.Player) },
                onMiniPlayerClick = { onNavigate(SettingsSection.MiniPlayer) },
                onHomeSettingsClick = { onNavigate(SettingsSection.Home) },
                onLyricsClick = { onNavigate(SettingsSection.Lyrics) }
            )

            SettingsSection.Language -> LanguageSelectionScreen(
                navController = rememberNavController(),
                onLanguageSelected = { onNavigate(SettingsSection.General) }
            )

            SettingsSection.Lyrics -> LyricsPage(
                onBack = { onNavigate(SettingsSection.Interface) }
            )

            SettingsSection.Home -> HomeSettingsPage(
                onBack = { onNavigate(SettingsSection.Interface) }
            )

            SettingsSection.Player -> MiniPlayerAnimationsPage(
                onBack = { onNavigate(SettingsSection.Interface) },
                onAnimationsClick = { onNavigate(SettingsSection.PlayerAnimations) },
                onStyleClick = { onNavigate(SettingsSection.PlayerStyle) }
            )

            SettingsSection.MiniPlayer -> MiniPlayerStylePage(
                onBack = { onNavigate(SettingsSection.Interface) }
            )

            SettingsSection.PlayerAnimations -> PlayerAnimationsPage(
                onBack = { onNavigate(SettingsSection.Player) }
            )

            SettingsSection.PlayerStyle -> PlayerStylePage(
                onBack = { onNavigate(SettingsSection.Player) }
            )
        }
    }
}

@Composable
private fun MainSettingsSection(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onScanLibrary: () -> Unit,
    onGeneralClick: () -> Unit,
    onAudioClick: () -> Unit,
    onInterfaceClick: () -> Unit,
    themeMode: ThemeMode,
    dynamicColorsEnabled: Boolean,
    cloudEnabled: Boolean,
    homeUiState: HomeUiState,
    onUpdateCloudEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    SettingsLayout(title = " ", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                placeholder = { Text(stringResource(R.string.search_title)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                )
            )

            SoundlyUserHeaderContent(
                uiState = homeUiState,
                mode = HeaderMode.HOME,
                useStatusBarsPadding = false,
                horizontalPadding = 0.dp,
                showIcon = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable { onEditProfile() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            M3ListItem(
                icon = Icons.Default.CloudQueue,
                title = stringResource(R.string.settings_enable_cloud_title),
                description = stringResource(R.string.settings_cloud_desc),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = {
                    if (cloudEnabled) {
                        val intent = Intent(context, SoundlyCloudActivity::class.java).apply {
                            putExtra("theme_mode", themeMode.name)
                            putExtra("dynamic_colors", dynamicColorsEnabled)
                        }
                        context.startActivity(intent)
                    }
                },
                trailingContent = {
                    Switch(
                        checked = cloudEnabled,
                        onCheckedChange = { onUpdateCloudEnabled(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                M3ListItem(
                    icon = Icons.Rounded.Android,
                    title = stringResource(R.string.settings_general_title),
                    description = stringResource(R.string.settings_general_desc),
                    color = Color.Transparent,
                    drawDivider = false,
                    onClick = { onGeneralClick() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    M3ListItem(
                        icon = Icons.Rounded.FolderOpen,
                        title = stringResource(R.string.settings_scan_title),
                        description = stringResource(R.string.settings_scan_desc),
                        color = Color.Transparent,
                        drawDivider = true,
                        onClick = { onScanLibrary() }
                    )
                    M3ListItem(
                        icon = Icons.Rounded.Headphones,
                        title = stringResource(R.string.settings_audio_title),
                        description = stringResource(R.string.settings_audio_desc),
                        color = Color.Transparent,
                        drawDivider = false,
                        onClick = { onAudioClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                M3ListItem(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_interface_title),
                    description = stringResource(R.string.settings_interface_desc),
                    color = Color.Transparent,
                    drawDivider = false,
                    onClick = { onInterfaceClick() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SoundlyTheme {
        SettingsScreenContent(
            currentSection = SettingsSection.Main,
            themeMode = ThemeMode.SYSTEM,
            dynamicColorsEnabled = false,
            cloudEnabled = true,
            homeUiState = HomeUiState(username = "Preview User"),
            onBack = {},
            onNavigate = {},
            onHandleBack = {},
            onUpdateCloudEnabled = {}
        )
    }
}
