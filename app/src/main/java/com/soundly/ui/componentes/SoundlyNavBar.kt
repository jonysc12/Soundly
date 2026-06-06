package com.soundly.ui.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.soundly.ui.componentes.MiniPlayerState
import com.soundly.ui.componentes.NavDimens
import com.soundly.ui.componentes.rememberPressState
import com.soundly.ui.componentes.rememberAnimatedDominant
import com.soundly.ui.componentes.blendOnSurface
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SEARCH_PAGE_INDEX = 3

private val SpringSnappy = spring<Float>(dampingRatio = 0.90f, stiffness = 550f)
private val SpringBouncy = spring<Float>(dampingRatio = 0.58f, stiffness = 370f)
private val SpringSearchIn = spring<Float>(dampingRatio = 0.62f, stiffness = 350f)
private val SpringSearchOut = spring<Float>(dampingRatio = 0.82f, stiffness = 520f)
private val PagerSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 420f)
private val NavEnterFade = fadeIn(tween(220))
private val NavEnterScale = scaleIn(initialScale = 0.86f, animationSpec = SpringSnappy)
private val NavExitFade = fadeOut(tween(160))
private val NavExitScale = scaleOut(targetScale = 0.86f, animationSpec = tween(160))
private val SearchExpandEnterFade = fadeIn(tween(270))
private val SearchExpandEnterScale = scaleIn(initialScale = 0.80f, animationSpec = SpringSearchIn)
private val SearchExpandEnterHoriz = expandHorizontally(expandFrom = Alignment.Start, animationSpec = spring(dampingRatio = 0.60f, stiffness = 360f))
private val SearchExpandExitFade = fadeOut(tween(140))
private val SearchExpandExitScale = scaleOut(targetScale = 0.80f, animationSpec = tween(130))
private val SearchExpandExitHoriz = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(165, easing = FastOutSlowInEasing))
private val SearchCollapseEnterFade = fadeIn(tween(210))
private val SearchCollapseEnterScale = scaleIn(initialScale = 0.70f, animationSpec = SpringBouncy)
private val SearchCollapseExitFade = fadeOut(tween(180))
private val SearchCollapseExitScale = scaleOut(targetScale = 0.70f, animationSpec = tween(175))
private val SearchCollapseExitHoriz = shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = spring(dampingRatio = 0.68f, stiffness = 490f))
private val TabSelectedEnterScale = scaleIn(initialScale = 0.55f, animationSpec = SpringBouncy)
private val TabSelectedEnterFade = fadeIn(tween(160))
private val TabSelectedExitFade = fadeOut(tween(60))
private val TabUnselectedEnterFade = fadeIn(tween(60))
private val TabUnselectedExitScale = scaleOut(targetScale = 0.55f, animationSpec = tween(140))
private val TabUnselectedExitFade = fadeOut(tween(120))
private val SearchCircleSpring = spring<Float>(dampingRatio = 0.40f, stiffness = 300f)

@Immutable
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val index: Int,
)

val navItems = listOf(
    NavItem("Inicio", Icons.Rounded.Home, 0),
    NavItem("Librería", Icons.Rounded.MusicNote, 1),
    NavItem("Biblioteca", Icons.Rounded.LibraryMusic, 2),
)

private val NAV_LAST_INDEX = navItems.lastIndex

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundlyNavStack(
    pagerState: PagerState,
    miniPlayerState: MiniPlayerState,
    onPlayPause: () -> Unit,
    onSearchToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onMiniPlayerClick: () -> Unit = {},
    miniPlayerModifier: Modifier = Modifier,
    showMini: Boolean = true,
    accentColor: Color = Color.Unspecified,
) {
    var navWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val miniWidthDp = with(density) { navWidthPx.toDp() }

        if (showMini) {
            CollapsedMiniPlaceholder(
                modifier = miniPlayerModifier.then(
                    if (navWidthPx > 0) Modifier.width(miniWidthDp) else Modifier.fillMaxWidth()
                )
            )
        } else {
            Spacer(Modifier.height(0.dp))
        }

        SoundlyBottomNav(
            pagerState = pagerState,
            accentColor = accentColor,
            onSearchToggle = onSearchToggle,
            modifier = Modifier.onSizeChanged { size ->
                if (size.width != navWidthPx) navWidthPx = size.width
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundlyBottomNav(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onSearchToggle: () -> Unit = {},
    accentColor: Color = Color.Unspecified,
) {
    val scope = rememberCoroutineScope()
    val isSearchExpanded by remember(pagerState) { derivedStateOf { pagerState.currentPage == SEARCH_PAGE_INDEX } }
    val lastContentPage by remember(pagerState) { derivedStateOf { pagerState.currentPage.coerceAtMost(NAV_LAST_INDEX) } }

    val navigateTo: (Int) -> Unit = remember(scope, pagerState) {
        { page -> scope.launch { pagerState.animateScrollToPage(page = page, animationSpec = PagerSpring) } }
    }
    val baseSurface = MaterialTheme.colorScheme.surface
    val isDark = baseSurface.luminance() < 0.5f
    val accentInstant = adaptDominantInstant(
        rawColor = accentColor.takeIf { it != Color.Unspecified } ?: Color.Transparent,
        isDarkTheme = isDark,
        fallback = baseSurface
    )
    val navBg = blendOnSurface(accentInstant, baseSurface, 0.25f)
    val navPill = blendOnSurface(accentInstant, MaterialTheme.colorScheme.surfaceVariant, 0.40f)
    val onNav = if (navBg.luminance() < 0.35f) Color.White else Color.Black
    val onAccent = if (navPill.luminance() < 0.35f) Color.White else Color.Black
    val accentVibrant = blendOnSurface(accentInstant, onAccent, 0.70f)
    val mutedTint = blendOnSurface(accentInstant, onNav, 0.25f)
    val onNavMuted = mutedTint

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = isSearchExpanded,
            transitionSpec = { (NavEnterFade + NavEnterScale) togetherWith (NavExitFade + NavExitScale) },
            label = "navOrDot",
        ) { expanded ->
            if (!expanded) {
                GooglePhotosNavBar(
                    pagerState = pagerState,
                    onItemSelected = navigateTo,
                    navBg = navBg,
                    pillColor = navPill,
                    textColor = accentVibrant,
                    mutedColor = onNavMuted,
                )
            } else {
                CollapsedNavDot(
                    currentPage = lastContentPage,
                    onClick = onSearchToggle,
                    bg = navBg,
                    contentColor = accentVibrant
                )
            }
        }
        AnimatedContent(
            targetState = isSearchExpanded,
            transitionSpec = {
                if (targetState) {
                    (SearchExpandEnterFade + SearchExpandEnterScale + SearchExpandEnterHoriz) togetherWith (SearchExpandExitFade + SearchExpandExitScale + SearchExpandExitHoriz)
                } else {
                    (SearchCollapseEnterFade + SearchCollapseEnterScale) togetherWith (SearchCollapseExitFade + SearchCollapseExitScale + SearchCollapseExitHoriz)
                }
            },
            label = "searchToggle",
        ) { expanded ->
            if (!expanded) SearchCircle(onClick = onSearchToggle, bg = navBg, contentColor = onNavMuted)
            else SearchBar(onClose = onSearchToggle, bg = navBg, contentColor = accentVibrant, onColor = onNav)
        }
    }
}

@Composable
private fun CollapsedNavDot(currentPage: Int, onClick: () -> Unit, bg: Color, contentColor: Color) {
    val currentItem = remember(currentPage) { navItems.getOrElse(currentPage) { navItems[0] } }
    val haptic = LocalHapticFeedback.current
    val (isPressed, triggerPress) = rememberPressState(duration = 90L)
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = SpringSnappy,
        label = "dotScale",
    )
    Surface(
        modifier = Modifier
            .size(NavDimens.TOTAL_HEIGHT_DP.dp)
            .graphicsLayer { val s = scale; scaleX = s; scaleY = s }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    triggerPress()
                    onClick()
                },
            ),
        shape = CircleShape,
        color = bg,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.padding(NavDimens.NAV_PADDING_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(NavDimens.PILL_HEIGHT_DP.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = currentItem.icon,
                    contentDescription = currentItem.label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchBar(onClose: () -> Unit, bg: Color, contentColor: Color, onColor: Color) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val (isPressed, triggerPress) = rememberPressState(duration = 75L)
    val interactionSource = remember { MutableInteractionSource() }
    var query by remember { mutableStateOf("") }

    val surfaceScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = SpringSnappy,
        label = "searchBarScale",
    )
    var closeRotation by remember { mutableIntStateOf(0) }
    val animatedRotation by animateFloatAsState(
        targetValue = closeRotation.toFloat(),
        animationSpec = SpringBouncy,
        label = "closeRot",
    )
    val surfVarColor = contentColor.copy(alpha = 0.25f)
    val onSurfVarColor = contentColor
    val onSurfColor = onColor

    // Pedir foco automáticamente al expandirse
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .height(NavDimens.TOTAL_HEIGHT_DP.dp)
            .graphicsLayer { val s = surfaceScale; scaleX = s; scaleY = s },
        shape = RoundedCornerShape(50.dp),
        color = bg,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(bg)
                .padding(horizontal = 20.dp)
                .widthIn(min = 250.dp, max = 340.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = onSurfVarColor,
                modifier = Modifier.size(22.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurfColor),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar canciones...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = onSurfColor.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { rotationZ = animatedRotation }
                    .clip(CircleShape)
                    .background(surfVarColor.copy(alpha = 0.50f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            triggerPress()
                            closeRotation += 90
                            query = ""
                            focusManager.clearFocus()
                            onClose()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cerrar búsqueda",
                    tint = onSurfVarColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GooglePhotosNavBar(
    pagerState: PagerState,
    onItemSelected: (Int) -> Unit,
    navBg: Color,
    pillColor: Color,
    textColor: Color,
    mutedColor: Color,
) {
    val scrollPos by remember(pagerState) {
        derivedStateOf {
            val page = pagerState.currentPage.coerceAtMost(NAV_LAST_INDEX)
            val offset = if (pagerState.currentPage >= SEARCH_PAGE_INDEX) 0f else pagerState.currentPageOffsetFraction.coerceIn(-0.5f, 0.5f)
            (page + offset).coerceIn(0f, NAV_LAST_INDEX.toFloat())
        }
    }
    val overshoot = rememberPillOvershoot(scrollPos)
    val surfColor = navBg

    Surface(
        shape = RoundedCornerShape(50.dp),
        color = surfColor,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        NavBarLayout(
            scrollPos = scrollPos,
            overshoot = overshoot,
            pillColor = pillColor,
            modifier = Modifier.padding(NavDimens.NAV_PADDING_DP.dp),
        ) {
            navItems.forEachIndexed { index, item ->
                val selected by remember(pagerState, index) {
                    derivedStateOf { index == pagerState.currentPage.coerceAtMost(NAV_LAST_INDEX) }
                }
            NavTabItem(
                item = item,
                selected = selected,
                pillTextColor = textColor,
                inactiveColor = mutedColor,
                onClick = onItemSelected,
            )
        }
    }
    }
}

@Composable
private fun SearchCircle(onClick: () -> Unit, bg: Color, contentColor: Color) {
    val haptic = LocalHapticFeedback.current
    val (isPressed, triggerPress) = rememberPressState(duration = 110L)
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = SearchCircleSpring,
        label = "searchCircleScale",
    )
    val onSurfVarColor = contentColor
    val surfColor = bg

    Surface(
        modifier = Modifier
            .size(NavDimens.TOTAL_HEIGHT_DP.dp)
            .graphicsLayer { val s = scale; scaleX = s; scaleY = s }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    triggerPress()
                    onClick()
                },
            ),
        shape = CircleShape,
        color = surfColor,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Abrir búsqueda",
                tint = onSurfVarColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun rememberPillOvershoot(scrollPos: Float, sensitivity: Float = 9f): Float {
    val lastPos = remember { FloatArray(1) { scrollPos } }
    var velocity by remember { mutableFloatStateOf(0f) }
    SideEffect {
        val prev = lastPos[0]
        lastPos[0] = scrollPos
        velocity = velocity * 0.80f + (scrollPos - prev) * 0.20f
    }
    val overshoot by animateFloatAsState(
        targetValue = velocity * sensitivity,
        animationSpec = spring(dampingRatio = 0.34f, stiffness = 200f),
        label = "pillOvershoot",
    )
    return overshoot.coerceIn(-22f, 22f)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private class TabMeasurements(val count: Int = navItems.size) {
    val lefts = FloatArray(count)
    val widths = FloatArray(count)
}

@Composable
private fun NavBarLayout(
    scrollPos: Float,
    overshoot: Float,
    pillColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val measurements = remember { TabMeasurements() }
    val leftIdx = scrollPos.toInt().coerceIn(0, measurements.count - 1)
    val rightIdx = (leftIdx + 1).coerceIn(0, measurements.count - 1)
    val fraction = (scrollPos - leftIdx).coerceIn(0f, 1f)

    Layout(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .height(NavDimens.PILL_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(pillColor),
            )
            content()
        },
    ) { measurables, _ ->
        if (measurables.size < 2) return@Layout layout(0, 0) {}

        val pillMeasurable = measurables[0]
        val tabMeasurables = measurables.drop(1)
        var xCursor = 0
        var maxHeight = 0
        val tabPlaceables = Array(tabMeasurables.size) { i ->
            val p = tabMeasurables[i].measure(Constraints())
            measurements.lefts[i] = xCursor.toFloat()
            measurements.widths[i] = p.width.toFloat()
            xCursor += p.width
            maxHeight = maxOf(maxHeight, p.height)
            p
        }
        val totalWidth = xCursor
        val pillHeightPx = NavDimens.PILL_HEIGHT_DP.dp.roundToPx()
        val totalHeight = maxOf(maxHeight, pillHeightPx)
        val pillLeft = lerpF(measurements.lefts[leftIdx], measurements.lefts[rightIdx], fraction) + overshoot
        val pillWidth = lerpF(measurements.widths[leftIdx], measurements.widths[rightIdx], fraction)
        val pillPlaceable = pillMeasurable.measure(
            Constraints.fixed(
                width = pillWidth.roundToInt().coerceAtLeast(1),
                height = totalHeight,
            )
        )
        layout(totalWidth, totalHeight) {
            pillPlaceable.placeRelative(
                x = pillLeft.roundToInt(),
                y = (totalHeight - pillPlaceable.height) / 2,
            )
            var x = 0
            tabPlaceables.forEach { p ->
                p.placeRelative(x = x, y = (totalHeight - p.height) / 2)
                x += p.width
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun NavTabItem(
    item: NavItem,
    selected: Boolean,
    pillTextColor: Color,
    inactiveColor: Color,
    onClick: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val (isPressed, triggerPress) = rememberPressState(duration = 85L)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = SpringSnappy,
        label = "tabPressScale",
    )
    val hPadding by animateFloatAsState(
        targetValue = if (selected) 16f else 12f,
        animationSpec = SpringBouncy,
        label = "tabHPad",
    )
    val onClickItem = remember(item.index, onClick) { { onClick(item.index) } }

    Row(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    triggerPress()
                    onClickItem()
                },
            )
            .graphicsLayer { val s = pressScale; scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(50.dp))
            .padding(horizontal = hPadding.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                if (targetState) {
                    (TabSelectedEnterScale + TabSelectedEnterFade) togetherWith TabSelectedExitFade
                } else {
                    TabUnselectedEnterFade togetherWith (TabUnselectedExitScale + TabUnselectedExitFade)
                }
            },
            label = "tabIcon_${item.index}",
        ) { isSelected ->
            if (isSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = pillTextColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
            } else {
                Spacer(Modifier.size(0.dp))
            }
        }
        Text(
            text = item.label,
            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (selected) pillTextColor else inactiveColor,
            maxLines = 1,
        )
    }
}
