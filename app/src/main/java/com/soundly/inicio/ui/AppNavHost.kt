package com.soundly.inicio.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soundly.data.repository.MusicRepository
import com.soundly.home.ui.HomeScreen

sealed class InicioRoute(val route: String) {
    object Launcher : InicioRoute("inicio_launcher")
    object Onboarding : InicioRoute("inicio_onboarding")
    object Permissions : InicioRoute("inicio_permissions")
    object Profile : InicioRoute("inicio_profile")
    object MediaScanner : InicioRoute("inicio_media_scanner")
    object Home : InicioRoute("home")
}

@Composable
fun AppNavHost(navController: NavHostController, repository: MusicRepository) {

    NavHost(
        navController = navController,
        startDestination = InicioRoute.Launcher.route
    ) {

        composable(InicioRoute.Launcher.route) {
            LauncherScreen(navController)
        }

        composable(InicioRoute.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(InicioRoute.Permissions.route) {
                        popUpTo(InicioRoute.Launcher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(InicioRoute.Permissions.route) {
            PermissionScreen(
                navController = navController,
                onPermissionsGranted = {
                    navController.navigate(InicioRoute.Profile.route) {
                        popUpTo(InicioRoute.Launcher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(InicioRoute.Profile.route) {
            ProfileScreen(
                navController = navController,
                onProfileCreated = {
                    navController.navigate(InicioRoute.MediaScanner.route) {
                        popUpTo(InicioRoute.Launcher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(InicioRoute.MediaScanner.route) {
            MediaScannerScreen(
                onBack = { navController.popBackStack() },
                onScanConfirmed = {
                    navController.navigate(InicioRoute.Home.route) {
                        popUpTo(InicioRoute.Launcher.route) { inclusive = true }
                    }
                },
                repository = repository
            )
        }

        composable(InicioRoute.Home.route) {
            HomeScreen(repository)
        }
    }
}
