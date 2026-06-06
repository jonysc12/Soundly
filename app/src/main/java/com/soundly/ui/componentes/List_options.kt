package com.soundly.ui.componentes

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

data class ListOptionsLeadingAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

data class ListOptionsMenuItem(
    val id: String,
    val label: String
)

private data class LeadingButtonShape(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp
)

sealed interface ListOptionsTrailingAction {
    data class Menu(
        val label: String,
        val options: List<ListOptionsMenuItem>,
        val selectedOptionId: String,
        val onOptionSelected: (String) -> Unit,
        val icon: ImageVector = Icons.Rounded.Menu,
        val contentDescription: String = "Abrir opciones"
    ) : ListOptionsTrailingAction

    data class Toggle(
        val label: String,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val contentDescription: String
    ) : ListOptionsTrailingAction
}

private enum class BounceRole {
    Idle,
    Primary,
    Sibling,
}

private const val BounceResetDelayMillis = 110L

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun List_options(
    leadingActions: List<ListOptionsLeadingAction>,
    trailingAction: ListOptionsTrailingAction,
    modifier: Modifier = Modifier,
    onColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val haptic = LocalHapticFeedback.current

    var leadingPulse by remember { mutableIntStateOf(0) }
    var activeLeadingIndex by remember { mutableIntStateOf(-1) }
    var trailingPulse by remember { mutableIntStateOf(0) }
    var trailingActive by remember { mutableStateOf(false) }
    var showMenuPopup by remember { mutableStateOf(false) }

    LaunchedEffect(leadingPulse) {
        if (leadingPulse == 0) return@LaunchedEffect
        delay(BounceResetDelayMillis)
        activeLeadingIndex = -1
    }

    LaunchedEffect(trailingPulse) {
        if (trailingPulse == 0) return@LaunchedEffect
        delay(BounceResetDelayMillis)
        trailingActive = false
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 22.dp
            ),
        horizontalArrangement = if (leadingActions.isEmpty()) {
            Arrangement.End
        } else {
            Arrangement.SpaceBetween
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingActions.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingActions.forEachIndexed { index, action ->
                    AnimatedLeadingButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeLeadingIndex = index
                            leadingPulse++
                            action.onClick()
                        },
                        onColor = onColor,
                        role = when {
                            activeLeadingIndex == index -> BounceRole.Primary
                            activeLeadingIndex >= 0 && abs(activeLeadingIndex - index) == 1 -> BounceRole.Sibling
                            else -> BounceRole.Idle
                        },
                        modifier = Modifier
                            .width(50.dp)
                            .height(42.dp),
                        shape = leadingShapeFor(index, leadingActions.size)
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.contentDescription,
                            tint = onColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (index < leadingActions.lastIndex) {
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }

        Box {
            AnimatedTrailingButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    trailingActive = true
                    trailingPulse++
                    when (trailingAction) {
                        is ListOptionsTrailingAction.Menu -> {
                            showMenuPopup = !showMenuPopup
                        }

                        is ListOptionsTrailingAction.Toggle -> trailingAction.onClick()
                    }
                },
                onColor = onColor,
                active = trailingActive,
                icon = when (trailingAction) {
                    is ListOptionsTrailingAction.Menu -> trailingAction.icon
                    is ListOptionsTrailingAction.Toggle -> trailingAction.icon
                },
                label = when (trailingAction) {
                    is ListOptionsTrailingAction.Menu -> trailingAction.label
                    is ListOptionsTrailingAction.Toggle -> trailingAction.label
                },
                contentDescription = when (trailingAction) {
                    is ListOptionsTrailingAction.Menu -> trailingAction.contentDescription
                    is ListOptionsTrailingAction.Toggle -> trailingAction.contentDescription
                },
                modifier = Modifier.height(42.dp)
            )

            val menuAction = trailingAction as? ListOptionsTrailingAction.Menu
            if (menuAction != null) {
                DropdownMenu(
                    expanded = showMenuPopup,
                    onDismissRequest = { showMenuPopup = false },
                    containerColor = Color.Transparent, // 🔥 quitamos el fondo interno
                    shape = RoundedCornerShape(0.dp),   // 🔥 eliminamos la forma interna
                    shadowElevation = 0.dp              // 🔥 quitamos sombra default
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 6.dp)
                    ) {
                        Column {
                            menuAction.options.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.label,
                                            style = if (item.id == menuAction.selectedOptionId) {
                                                MaterialTheme.typography.titleMedium
                                            } else {
                                                MaterialTheme.typography.bodyLarge
                                            }
                                        )
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        menuAction.onOptionSelected(item.id)
                                        showMenuPopup = false
                                    },
                                    leadingIcon = {
                                        if (item.id == menuAction.selectedOptionId) {
                                            Icon(
                                                imageVector = Icons.Rounded.Done,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AnimatedLeadingButton(
    onClick: () -> Unit,
    onColor: Color,
    role: BounceRole,
    shape: LeadingButtonShape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val transition = updateTransition(targetState = role, label = "list_option_leading_transition")
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val cornerSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()

    val scale by transition.animateFloat(
        transitionSpec = { spatialSpec },
        label = "list_option_leading_scale"
    ) {
        when (it) {
            BounceRole.Primary -> 0.86f
            BounceRole.Sibling -> 1.08f
            BounceRole.Idle -> 1f
        }
    }
    val backgroundAlpha by transition.animateFloat(
        transitionSpec = { effectsSpec },
        label = "list_option_leading_alpha"
    ) {
        when (it) {
            BounceRole.Primary -> 0.18f
            BounceRole.Sibling -> 0.14f
            BounceRole.Idle -> 0.10f
        }
    }
    val topStart by transition.animateDp(
        transitionSpec = { cornerSpec },
        label = "list_option_leading_top_start"
    ) { if (it == BounceRole.Idle) shape.topStart else 22.dp }
    val topEnd by transition.animateDp(
        transitionSpec = { cornerSpec },
        label = "list_option_leading_top_end"
    ) { if (it == BounceRole.Idle) shape.topEnd else 22.dp }
    val bottomStart by transition.animateDp(
        transitionSpec = { cornerSpec },
        label = "list_option_leading_bottom_start"
    ) { if (it == BounceRole.Idle) shape.bottomStart else 22.dp }
    val bottomEnd by transition.animateDp(
        transitionSpec = { cornerSpec },
        label = "list_option_leading_bottom_end"
    ) { if (it == BounceRole.Idle) shape.bottomEnd else 22.dp }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(
                RoundedCornerShape(
                    topStart = topStart,
                    topEnd = topEnd,
                    bottomStart = bottomStart,
                    bottomEnd = bottomEnd
                )
            )
            .background(onColor.copy(alpha = backgroundAlpha))
            .combinedClickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = {}
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AnimatedTrailingButton(
    onClick: () -> Unit,
    onColor: Color,
    active: Boolean,
    icon: ImageVector,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val transition = updateTransition(targetState = active, label = "list_option_trailing_transition")
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val cornerSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()

    val scale by transition.animateFloat(
        transitionSpec = { spatialSpec },
        label = "list_option_trailing_scale"
    ) { if (it) 0.93f else 1f }
    val backgroundAlpha by transition.animateFloat(
        transitionSpec = { effectsSpec },
        label = "list_option_trailing_alpha"
    ) { if (it) 0.16f else 0.08f }
    val cornerRadius by transition.animateDp(
        transitionSpec = { cornerSpec },
        label = "list_option_trailing_corner"
    ) { if (it) 24.dp else 20.dp }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(onColor.copy(alpha = backgroundAlpha))
            .combinedClickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = {}
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = onColor,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = label,
            color = onColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun leadingShapeFor(index: Int, total: Int): LeadingButtonShape {
    return when {
        total <= 1 -> LeadingButtonShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )

        index == 0 -> LeadingButtonShape(
            topStart = 20.dp,
            topEnd = 8.dp,
            bottomStart = 20.dp,
            bottomEnd = 8.dp
        )

        index == total - 1 -> LeadingButtonShape(
            topStart = 8.dp,
            topEnd = 20.dp,
            bottomStart = 8.dp,
            bottomEnd = 20.dp
        )

        else -> LeadingButtonShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewListOptions() {
    List_options(
        leadingActions = listOf(
            ListOptionsLeadingAction(
                icon = Icons.Rounded.PlayArrow,
                contentDescription = "Reproducir",
                onClick = {}
            ),
            ListOptionsLeadingAction(
                icon = Icons.Rounded.Shuffle,
                contentDescription = "Aleatorio",
                onClick = {}
            )
        ),
        trailingAction = ListOptionsTrailingAction.Menu(
            label = "A-Z",
            options = listOf(
                ListOptionsMenuItem("a_z", "A-Z"),
                ListOptionsMenuItem("z_a", "Z-A")
            ),
            selectedOptionId = "a_z",
            onOptionSelected = {}
        )
    )
}
