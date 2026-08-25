package com.soundly

import android.os.Bundle
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouterParams
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.ThemeMode
import com.soundly.data.repository.UserSettingsRepository
import com.soundly.ui.theme.SoundlyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: MusicRepository

    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)

        // El Splash Screen se queda hasta que el repositorio cargue la caché inicial
        splashScreen.setKeepOnScreenCondition {
            !repository.isReady.value
        }

        // Habilitar transferencia de medios (Output Switcher / Cast) de forma diferida
        // para no bloquear el arranque
        lifecycleScope.launch {
            val router = MediaRouter.getInstance(this@MainActivity)
            router.routerParams = MediaRouterParams.Builder()
                .setMediaTransferReceiverEnabled(true)
                .build()
        }

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
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
                )
                window.isNavigationBarContrastEnforced = false
            }

            SoundlyTheme(themeMode = themeMode, dynamicColor = dynamicColorsEnabled) {
                ReportDrawn()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 🔥 Quita el glow clásico y permite stretch moderno
                    CompositionLocalProvider(
                        LocalOverscrollConfiguration provides null
                    ) {
                        App(repository)
                    }
                }
            }
        }
    }
}