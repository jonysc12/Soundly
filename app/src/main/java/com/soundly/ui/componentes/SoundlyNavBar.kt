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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.soundly.R
import com.soundly.ui.componentes.MiniPlayerState
import com.soundly.ui.componentes.NavDimens
import com.soundly.ui.componentes.rememberPressState
import com.soundly.ui.componentes.rememberAnimatedDominant
import com.soundly.ui.componentes.adaptDominantInstant

import com.soundly.ui.componentes.blendOnSurface
import com.soundly.feature.search.SearchViewModel
import com.soundly.data.repository.MiniPlayerStyle
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.MiniProgressBarType
import com.soundly.data.repository.MiniProgressBarThickness
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val labelRes: Int,
    val icon: ImageVector,
    val index: Int,
)

val defaultNavItems = listOf(
    NavItem(R.string.nav_home, Icons.Rounded.Home, 0),
    NavItem(R.string.nav_music, Icons.Rounded.MusicNote, 1),
    NavItem(R.string.nav_library, Icons.Rounded.LibraryMusic, 2),
)

val LocalNavStackHeight = compositionLocalOf { 0.dp }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundlyNavStack(
    pagerState: PagerState,
    miniPlayerMetadata: MiniPlayerMetadata,
    miniPlayerProgress: () -> Float,
    onPlayPause: () -> Unit,
    onSearchToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onMiniPlayerClick: () -> Unit = {},
    miniPlayerModifier: Modifier = Modifier,
    onNext: () -> Unit = {},
    showMini: Boolean = true,
    accentColor: Color = Color.Unspecified,
    searchViewModel: SearchViewModel? = null,
    miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID,
    artworkShape: ArtworkShape = ArtworkShape.CIRCLE,
    miniProgressBarType: MiniProgressBarType = MiniProgressBarType.WAVE,
    miniProgressBarThickness: MiniProgressBarThickness = MiniProgressBarThickness.NORMAL,
    showMiniPrevious: Boolean = false,
    swipeToDismiss: Boolean = true,
    vividColors: Boolean = false,
    onPrevious: () -> Unit = {},
    onDismiss: () -> Unit = {},
    showHomePage: Boolean = true,
) {
    var navWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val currentNavItems = remember(showHomePage) {
        if (showHomePage) {
            defaultNavItems
        } else {
            defaultNavItems.filter { it.labelRes != R.string.nav_home }
                .mapIndexed { index, item -> item.copy(index = index) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMini) {
            val miniWidthDp = with(density) { navWidthPx.toDp() }
            MiniPlayer(
                metadata = miniPlayerMetadata,
                progress = miniPlayerProgress,
                onPlayPauseClick = onPlayPause,
                onNextClick = onNext,
                onPreviousClick = onPrevious,
                onClick = onMiniPlayerClick,
                accentColor = accentColor,
                miniPlayerStyle = miniPlayerStyle,
                artworkShape = artworkShape,
                miniProgressBarType = miniProgressBarType,
                miniProgressBarThickness = miniProgressBarThickness,
                showPrevious = showMiniPrevious,
                swipeToDismiss = swipeToDismiss,
                vividColors = vividColors,
                onDismiss = onDismiss,
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
            searchViewModel = searchViewModel,
            miniPlayerStyle = miniPlayerStyle,
            vividColors = vividColors,
            navItems = currentNavItems,
            modifier = Modifier
                .onSizeChanged { size -> 
                    if (navWidthPx != size.width) navWidthPx = size.width 
                }
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
    searchViewModel: SearchViewModel? = null,
    miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID,
    vividColors: Boolean = false,
    navItems: List<NavItem> = defaultNavItems,
) {
    val scope = rememberCoroutineScope()
    val navLastIndex = navItems.size - 1
    val searchPageIndex = navItems.size
    
    // Check if we are in the Search tab to expand the bar
    val isSearchExpanded by remember(pagerState, searchPageIndex) { derivedStateOf { pagerState.currentPage == searchPageIndex } }
    val lastContentPage by remember(pagerState, navLastIndex) { derivedStateOf { pagerState.currentPage.coerceAtMost(navLastIndex) } }

    val navigateTo: (Int) -> Unit = remember(scope, pagerState) {
        { page -> scope.launch { pagerState.animateScrollToPage(page = page, animationSpec = PagerSpring) } }
    }
    val baseSurface = MaterialTheme.colorScheme.surface
    val isDark = baseSurface.luminance() < 0.5f
    val accentInstant = adaptDominantInstant(
        rawColor = accentColor.takeIf { it != Color.Unspecified } ?: Color.Transparent,
        isDarkTheme = isDark,
        fallback = baseSurface,
        isVivid = vividColors
    )
    
    val navBg: Color
    val navPill: Color
    val onNavBase: Color
    val accentVibrant: Color
    val onNavMuted: Color

    if (!isDark && vividColors) {
        navBg = if (miniPlayerStyle == MiniPlayerStyle.BLUR) Color.Transparent else blendOnSurface(accentInstant, baseSurface, 0.12f)
        navPill = accentInstant.copy(alpha = 0.18f)
        onNavBase = accentInstant
        accentVibrant = accentInstant
        onNavMuted = accentInstant.copy(alpha = 0.60f)
    } else {
        navBg = when(miniPlayerStyle) {
            MiniPlayerStyle.SOLID -> baseSurface
            MiniPlayerStyle.TINTED -> blendOnSurface(accentInstant, baseSurface, if (vividColors) 0.65f else 0.25f)
            MiniPlayerStyle.BLUR -> Color.Transparent
        }
        navPill = blendOnSurface(accentInstant, MaterialTheme.colorScheme.surfaceVariant, if (vividColors) 0.65f else 0.40f)
        onNavBase = if (navBg.luminance() < 0.52f) Color.White else Color.Black
        val vividFactor = if (vividColors) 0.25f else 0.70f
        accentVibrant = blendOnSurface(accentInstant, onNavBase, vividFactor)
        val mutedFactor = if (vividColors) 0.45f else 0.25f
        onNavMuted = blendOnSurface(accentInstant, onNavBase, mutedFactor)
    }

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
                    miniPlayerStyle = miniPlayerStyle,
                    navItems = navItems,
                )
            } else {
                CollapsedNavDot(
                    currentPage = lastContentPage,
                    onClick = onSearchToggle,
                    bg = navBg,
                    contentColor = accentVibrant,
                    miniPlayerStyle = miniPlayerStyle,
                    navItems = navItems,
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
            if (!expanded) SearchCircle(onClick = onSearchToggle, bg = navBg, contentColor = onNavMuted, miniPlayerStyle = miniPlayerStyle)
            else SearchBar(onClose = onSearchToggle, bg = navBg, contentColor = accentVibrant, onColor = onNavBase, viewModel = searchViewModel, miniPlayerStyle = miniPlayerStyle)
        }
    }
}

@Composable
private fun CollapsedNavDot(
    currentPage: Int,
    onClick: () -> Unit,
    bg: Color,
    contentColor: Color,
    miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID,
    navItems: List<NavItem> = defaultNavItems
) {
    val currentItem = remember(currentPage, navItems) { navItems.getOrElse(currentPage) { navItems.firstOrNull() ?: defaultNavItems[0] } }
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
        tonalElevation = if (miniPlayerStyle == MiniPlayerStyle.BLUR) 0.dp else 3.dp,
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
                    contentDescription = stringResource(currentItem.labelRes),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchBar(onClose: () -> Unit, bg: Color, contentColor: Color, onColor: Color, viewModel: SearchViewModel? = null, miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val (isPressed, triggerPress) = rememberPressState(duration = 75L)
    val interactionSource = remember { MutableInteractionSource() }
    
    val query by viewModel?.query?.collectAsState() ?: remember { mutableStateOf("") }

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

    Surface(
        modifier = Modifier
            .height(NavDimens.TOTAL_HEIGHT_DP.dp)
            .graphicsLayer { val s = surfaceScale; scaleX = s; scaleY = s },
        shape = RoundedCornerShape(50.dp),
        color = bg,
        tonalElevation = if (miniPlayerStyle == MiniPlayerStyle.BLUR) 0.dp else 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
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
                onValueChange = { viewModel?.onQueryChange(it) },
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
                                text = stringResource(R.string.nav_search) + "...",
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
                            viewModel?.onQueryChange("")
                            focusManager.clearFocus()
                            onClose()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.button_close),
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
    miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID,
    navItems: List<NavItem> = defaultNavItems,
) {
    val navLastIndex = navItems.size - 1
    val scrollPosProvider = remember(pagerState, navLastIndex) {
        {
            val page = pagerState.currentPage.coerceAtMost(navLastIndex)
            val offset = if (pagerState.currentPage >= navItems.size) 0f else pagerState.currentPageOffsetFraction.coerceIn(-0.5f, 0.5f)
            (page + offset).coerceIn(0f, navLastIndex.toFloat())
        }
    }
    
    val overshootProvider = rememberPillOvershoot(scrollPosProvider)

    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(50.dp),
        color = navBg,
        tonalElevation = if (miniPlayerStyle == MiniPlayerStyle.BLUR) 0.dp else 3.dp,
        shadowElevation = 0.dp,
    ) {
        NavBarLayout(
            scrollPosProvider = scrollPosProvider,
            overshootProvider = overshootProvider,
            pillColor = pillColor,
            navItemsCount = navItems.size,
            modifier = Modifier.padding(NavDimens.NAV_PADDING_DP.dp),
        ) {
            navItems.forEachIndexed { index, item ->
                val selected by remember(pagerState, index, navLastIndex) {
                    derivedStateOf { index == pagerState.currentPage.coerceAtMost(navLastIndex) }
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
private fun SearchCircle(onClick: () -> Unit, bg: Color, contentColor: Color, miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID) {
    val haptic = LocalHapticFeedback.current
    val (isPressed, triggerPress) = rememberPressState(duration = 110L)
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = SearchCircleSpring,
        label = "searchCircleScale",
    )
    val onSurfVarColor = contentColor

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
        tonalElevation = if (miniPlayerStyle == MiniPlayerStyle.BLUR) 0.dp else 3.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.nav_search),
                tint = onSurfVarColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun rememberPillOvershoot(scrollPosProvider: () -> Float, sensitivity: Float = 9f): () -> Float {
    val scrollPos = scrollPosProvider()
    val lastPos = remember { FloatArray(1) { scrollPos } }
    var velocity by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(scrollPosProvider) {
        snapshotFlow { scrollPosProvider() }.collect { currentPos ->
            val prev = lastPos[0]
            lastPos[0] = currentPos
            velocity = velocity * 0.80f + (currentPos - prev) * 0.20f
        }
    }
    
    val overshoot by animateFloatAsState(
        targetValue = velocity * sensitivity,
        animationSpec = spring(dampingRatio = 0.34f, stiffness = 200f),
        label = "pillOvershoot",
    )
    return remember { { overshoot.coerceIn(-22f, 22f) } }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private class TabMeasurements(val count: Int) {
    val lefts = FloatArray(count)
    val widths = FloatArray(count)
}

@Composable
private fun NavBarLayout(
    scrollPosProvider: () -> Float,
    overshootProvider: () -> Float,
    pillColor: Color,
    navItemsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val measurements = remember(navItemsCount) { TabMeasurements(navItemsCount) }

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

        val scrollPos = scrollPosProvider()
        val overshoot = overshootProvider()
        val leftIdx = scrollPos.toInt().coerceIn(0, measurements.count - 1)
        val rightIdx = (leftIdx + 1).coerceIn(0, measurements.count - 1)
        val fraction = (scrollPos - leftIdx).coerceIn(0f, 1f)

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
                        contentDescription = stringResource(item.labelRes),
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
            text = stringResource(item.labelRes),
            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (selected) pillTextColor else inactiveColor,
            maxLines = 1,
        )
    }
}
