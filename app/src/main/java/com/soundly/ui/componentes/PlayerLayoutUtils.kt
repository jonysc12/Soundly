package com.soundly.ui.componentes

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.soundly.R

val ProgressiveEasing = CubicBezierEasing(0.48f, 0.15f, 0.25f, 1f)

data class DesignParams(
    val isVeryShortScreen: Boolean,
    val isTinyScreen: Boolean,
    val isUltraShortScreen: Boolean,
    val isNarrowScreen: Boolean,
    val layoutRoom: Float,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val heroSectionMinHeight: Dp,
    val artworkMaxHeight: Dp,
    val artworkWidthFraction: Float,
    val artworkShape: Shape,
    val compactControls: Boolean,
    val heroElementGap: Dp,
    val titleBlockGap: Dp,
    val headerVerticalPadding: Dp,
    val titleBlockVerticalPadding: Dp,
    val sectionSpacer: Dp,
    val extraControlsSpacer: Dp,
    val sideButtonSize: Dp,
    val sideButtonIconSize: Dp,
    val sideButtonHorizontalPadding: Dp,
    val mainButtonSize: Dp,
    val mainButtonIconSize: Dp,
    val mainButtonCornerRadius: Dp,
    val controlRowHeight: Dp,
    val titleStyle: TextStyle,
    val artistStyle: TextStyle,
    val timeStyle: TextStyle,
    val mainSliderSpacing: Dp,
    val playbackBandGap: Dp,
    val secondaryControlsPadding: Dp
)

data class PlayerColorScheme(
    val containerColor: Color,
    val onColor: Color,
    val subColor: Color,
    val tertiaryColor: Color,
    val buttonSurface: Color
)

@Composable
fun rememberCurrentRoute(): MediaRouter.RouteInfo? {
    if (LocalInspectionMode.current) return null
    
    val context = LocalContext.current
    val mediaRouter = try {
        MediaRouter.getInstance(context)
    } catch (_: Exception) {
        null
    } ?: return null
    
    var currentRoute by remember { mutableStateOf(mediaRouter.selectedRoute) }

    DisposableEffect(Unit) {
        val selector = MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
            .build()
        val callback = object : MediaRouter.Callback() {
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                currentRoute = route
            }
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                if (route.id == currentRoute.id) {
                    currentRoute = route
                }
            }
            override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                currentRoute = router.selectedRoute
            }
        }
        mediaRouter.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        onDispose {
            mediaRouter.removeCallback(callback)
        }
    }
    return currentRoute
}

fun getRouteIcon(route: MediaRouter.RouteInfo?): ImageVector {
    if (route == null) return Icons.Rounded.PhoneAndroid
    val name = route.name.lowercase()
    return when {
        route.isDefault -> Icons.Rounded.PhoneAndroid
        name.contains("bluetooth") || name.contains("bt") ||
                route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP -> Icons.Rounded.Bluetooth
        name.contains("headphone") || name.contains("headset") ||
                name.contains("auricular") || name.contains("audifono") -> Icons.Rounded.Headphones
        name.contains("speaker") || name.contains("altavoz") -> Icons.Rounded.Speaker
        name.contains("tv") || name.contains("cast") || name.contains("chromecast") -> Icons.Rounded.Cast
        else -> Icons.Rounded.PhoneAndroid
    }
}

fun getRouteName(context: android.content.Context, route: MediaRouter.RouteInfo?): String {
    if (route == null || route.isDefault || route.name.lowercase().contains("dispositivo")) {
        return context.getString(R.string.device_name_default)
    } else {
        return route.name
    }
}
