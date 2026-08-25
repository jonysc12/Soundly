package com.soundly.ui.navigation

import androidx.compose.runtime.compositionLocalOf

data class BackStackCoordinator(
    val isOverlayActive: Boolean = false
)

val LocalBackStackCoordinator = compositionLocalOf { BackStackCoordinator() }
