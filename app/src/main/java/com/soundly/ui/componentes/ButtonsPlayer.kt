package com.soundly.ui.componentes

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Material 3 Motion Springs
// ─────────────────────────────────────────────

private fun <T> m3Accelerate(): SpringSpec<T> = spring(
    dampingRatio = 1f,
    stiffness    = 3000f
)

private fun <T> m3Decelerate(): SpringSpec<T> = spring(
    dampingRatio = 0.50f,
    stiffness    = 460f
)

private fun <T> m3Emphasized(): SpringSpec<T> = spring(
    dampingRatio = 0.68f,
    stiffness    = 380f
)

/* ───────────────────── SIDE BUTTON ───────────────────── */

@Composable
fun PlayerSideButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    onClick: () -> Unit,
    iconColor: Color,
    onPressChange: (Boolean) -> Unit = {},
    buttonSize: Dp = 64.dp,
    iconSize: Dp = 28.dp,
    horizontalPadding: Dp = 12.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) { onPressChange(pressed) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.82f else 1f,
        animationSpec = if (pressed) m3Accelerate() else m3Decelerate(),
        label         = "sideButtonScale"
    )

    Box(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .scale(scale)
            .size(buttonSize)
            .clip(RoundedCornerShape(50))
            .background(iconColor.copy(alpha = 0.10f))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ───────────────────── MAIN BUTTON ───────────────────── */

@Composable
fun PlayerMainButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    iconColor: Color,
    onPressChange: (Boolean) -> Unit = {},
    buttonSize: Dp = 72.dp,
    iconSize: Dp = 36.dp,
    cornerRadius: Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) { onPressChange(pressed) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.84f else 1f,
        animationSpec = if (pressed) m3Accelerate() else m3Decelerate(),
        label         = "mainButtonScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(buttonSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(iconColor)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint     = containerColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ───────────────────── SECONDARY CONTROLS ───────────────────── */

@Composable
fun PlayerSecondaryControls(
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onColor: Color,
    compact: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    var shufflePressed by remember { mutableStateOf(false) }
    var heartPressed   by remember { mutableStateOf(false) }
    var repeatPressed  by remember { mutableStateOf(false) }

    val gapLeft by animateFloatAsState(
        targetValue = when {
            heartPressed   -> 10f
            shufflePressed -> 1f
            else           -> 6f
        },
        animationSpec = if (heartPressed || shufflePressed) m3Accelerate() else m3Decelerate(),
        label = "secGapLeft"
    )

    val gapRight by animateFloatAsState(
        targetValue = when {
            heartPressed  -> 10f
            repeatPressed -> 1f
            else          -> 6f
        },
        animationSpec = if (heartPressed || repeatPressed) m3Accelerate() else m3Decelerate(),
        label = "secGapRight"
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(if (compact) 22.dp else 28.dp))
                .background(onColor.copy(alpha = 0.10f))
                .padding(
                    horizontal = if (compact) 6.dp else 8.dp,
                    vertical = if (compact) 4.dp else 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerSideOvalLeft(
                icon          = Icons.Rounded.Shuffle,
                active        = isShuffleEnabled,
                onClick       = onToggleShuffle,
                onColor       = onColor,
                onPressChange = { shufflePressed = it },
                compact       = compact
            )

            Spacer(Modifier.width((if (compact) gapLeft * 0.7f else gapLeft).dp))

            PlayerHeartButton(
                isFavorite    = isFavorite,
                onClick       = onToggleFavorite,
                onColor       = onColor,
                onPressChange = { heartPressed = it },
                compact       = compact
            )

            Spacer(Modifier.width((if (compact) gapRight * 0.7f else gapRight).dp))

            val repeatIcon = when (repeatMode) {
                androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                else -> Icons.Rounded.Repeat
            }

            PlayerSideOvalRight(
                icon          = repeatIcon,
                active        = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
                onClick       = onCycleRepeat,
                onColor       = onColor,
                onPressChange = { repeatPressed = it },
                compact       = compact
            )
        }
    }
}

/* ───────────────────── HEART BUTTON ───────────────────── */

@Composable
fun PlayerHeartButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    onColor: Color,
    onPressChange: (Boolean) -> Unit = {},
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) { onPressChange(pressed) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.78f else 1f,
        animationSpec = if (pressed) m3Accelerate() else m3Decelerate(),
        label         = "heartScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(if (compact) 38.dp else 44.dp)
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(onColor.copy(alpha = if (isFavorite) 0.20f else 0.08f))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint     = onColor,
            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
        )
    }
}

/* ───────────────────── OVAL RIGHT ───────────────────── */

@Composable
fun PlayerSideOvalRight(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    onColor: Color,
    onPressChange: (Boolean) -> Unit = {},
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) { onPressChange(pressed) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.85f else 1f,
        animationSpec = if (pressed) m3Accelerate() else m3Decelerate(),
        label         = "ovalRightScale"
    )

    val bgAlpha by animateFloatAsState(
        targetValue   = if (active) 0.20f else 0.08f,
        animationSpec = m3Emphasized(),
        label         = "ovalRightBg"
    )

    val iconAlpha by animateFloatAsState(
        targetValue   = if (active) 1f else 0.55f,
        animationSpec = m3Emphasized(),
        label         = "ovalRightIconAlpha"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .width(if (compact) 56.dp else 66.dp)
            .height(if (compact) 38.dp else 44.dp)
            .clip(
                RoundedCornerShape(
                    topStart    = if (compact) 8.dp else 10.dp,
                    bottomStart = if (compact) 8.dp else 10.dp,
                    topEnd      = if (compact) 18.dp else 22.dp,
                    bottomEnd   = if (compact) 18.dp else 22.dp
                )
            )
            .background(onColor.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = onColor.copy(alpha = iconAlpha),
            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
        )
    }
}

/* ───────────────────── OVAL LEFT ───────────────────── */

@Composable
fun PlayerSideOvalLeft(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    onColor: Color,
    onPressChange: (Boolean) -> Unit = {},
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) { onPressChange(pressed) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.85f else 1f,
        animationSpec = if (pressed) m3Accelerate() else m3Decelerate(),
        label         = "ovalLeftScale"
    )

    val bgAlpha by animateFloatAsState(
        targetValue   = if (active) 0.20f else 0.08f,
        animationSpec = m3Emphasized(),
        label         = "ovalLeftBg"
    )

    val iconAlpha by animateFloatAsState(
        targetValue   = if (active) 1f else 0.55f,
        animationSpec = m3Emphasized(),
        label         = "ovalLeftIconAlpha"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .width(if (compact) 56.dp else 66.dp)
            .height(if (compact) 38.dp else 44.dp)
            .clip(
                RoundedCornerShape(
                    topStart    = if (compact) 18.dp else 22.dp,
                    bottomStart = if (compact) 18.dp else 22.dp,
                    topEnd      = if (compact) 8.dp else 10.dp,
                    bottomEnd   = if (compact) 8.dp else 10.dp
                )
            )
            .background(onColor.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = onColor.copy(alpha = iconAlpha),
            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
        )
    }
}
