package com.soundly.inicio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.soundly.data.preferences.LanguagePreferences
import com.soundly.inicio.viewmodel.LauncherViewModel
import com.soundly.ui.utils.LocaleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun LauncherScreen(
    navController: NavController,
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // Aplicar idioma guardado si existe
        val languageCode = LanguagePreferences.getLanguageCode(context).first()
        if (languageCode != null) {
            LocaleManager.applyLanguage(languageCode)
        }
        
        // Aseguramos una permanencia mínima para una transición visual suave
        delay(250)

        val onboardingSeen = viewModel.isOnboardingSeen()
        val permissionsGranted = viewModel.hasPermissions()
        val profileCreated = viewModel.isProfileCreated()
        val mediaScanConfirmed = viewModel.isMediaScanConfirmed()

        when {
            !onboardingSeen -> {
                navController.navigate(InicioRoute.Onboarding.route) {
                    popUpTo(InicioRoute.Launcher.route) {
                        inclusive = true
                    }
                }
            }

            !permissionsGranted -> {
                navController.navigate(InicioRoute.Permissions.route) {
                    popUpTo(InicioRoute.Launcher.route) {
                        inclusive = true
                    }
                }
            }

            !profileCreated -> {
                navController.navigate(InicioRoute.Profile.route) {
                    popUpTo(InicioRoute.Launcher.route) {
                        inclusive = true
                    }
                }
            }

            !mediaScanConfirmed -> {
                navController.navigate(InicioRoute.MediaScanner.route) {
                    popUpTo(InicioRoute.Launcher.route) {
                        inclusive = true
                    }
                }
            }

            else -> {
                navController.navigate(InicioRoute.Home.route) {
                    popUpTo(InicioRoute.Launcher.route) {
                        inclusive = true
                    }
                }
            }
        }
    }
}
