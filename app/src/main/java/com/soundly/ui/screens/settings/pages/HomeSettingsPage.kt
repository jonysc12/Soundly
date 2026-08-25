package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.data.repository.HomeSectionType
import com.soundly.ui.componentes.SoundlyToast
import com.soundly.ui.screens.settings.SettingsLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.soundly.R

@Composable
fun HomeSettingsPage(
    viewModel: HomeSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val sections by viewModel.homeSectionsOrder.collectAsState()
    val showSubtitles by viewModel.showSubtitles.collectAsState()
    val list = remember { mutableStateListOf<HomeSectionType>() }
    
    var showToast by remember { mutableStateOf(false) }

    LaunchedEffect(sections) {
        if (list.toList() != sections) {
            list.clear()
            list.addAll(sections)
        }
    }

    SettingsLayout(
        title = stringResource(R.string.interface_customize_home_title),
        onBack = onBack,
        scrollable = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_settings_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_settings_subtitles_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.home_settings_subtitles_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showSubtitles,
                    onCheckedChange = { viewModel.updateShowSubtitles(it) }
                )
            }

            Text(
                text = stringResource(R.string.home_settings_help_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                ReorderableHomeSections(
                    items = list,
                    onOrderChanged = { newList ->
                        viewModel.updateOrder(newList)
                    }
                )
            }

            Button(
                onClick = {
                    val defaultOrder = HomeSectionType.entries
                    list.clear()
                    list.addAll(defaultOrder)
                    viewModel.updateOrder(defaultOrder)
                    showToast = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    stringResource(R.string.home_settings_restore_button),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    SoundlyToast(
        message = stringResource(R.string.home_settings_restore_toast),
        isVisible = showToast,
        onDismiss = { showToast = false }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReorderableHomeSections(
    items: SnapshotStateList<HomeSectionType>,
    onOrderChanged: (List<HomeSectionType>) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    
    // Animatable para el "aterrizaje"
    val releaseAnimatable = remember { Animatable(0f) }
    var isReleasing by remember { mutableStateOf(false) }

    val itemPositions = remember { mutableStateMapOf<String, Float>() }
    val itemHeights = remember { mutableStateMapOf<String, Float>() }

    // Lógica de Auto-Scroll suave
    LaunchedEffect(draggingIndex, dragOffsetY) {
        if (draggingIndex != null) {
            while (true) {
                val layoutInfo = lazyListState.layoutInfo
                val draggingItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingIndex }
                if (draggingItem != null) {
                    val viewPortHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                    val itemTop = draggingItem.offset + dragOffsetY
                    val itemBottom = itemTop + draggingItem.size
                    
                    val scrollThreshold = 100f
                    if (itemTop < scrollThreshold) {
                        val scrollAmount = (scrollThreshold - itemTop) / 5f
                        lazyListState.scrollBy(-scrollAmount)
                    } else if (itemBottom > viewPortHeight - scrollThreshold) {
                        val scrollAmount = (itemBottom - (viewPortHeight - scrollThreshold)) / 5f
                        lazyListState.scrollBy(scrollAmount)
                    }
                }
                delay(16) // ~60fps
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(items) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val sectionName = itemPositions.entries.firstOrNull { entry ->
                            val pos = entry.value
                            val height = itemHeights[entry.key] ?: 0f
                            offset.y >= pos && offset.y <= pos + height
                        }?.key
                        draggingIndex = items.indexOfFirst { it.name == sectionName }.takeIf { it != -1 }
                        dragOffsetY = 0f
                        isReleasing = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                        dragOffsetY += dragAmount.y
                        
                        val currentSection = items.getOrNull(currentIdx) ?: return@detectDragGesturesAfterLongPress
                        val currentItemHeight = itemHeights[currentSection.name] ?: 0f
                        val threshold = currentItemHeight * 0.6f

                        if (dragOffsetY > threshold && currentIdx < items.size - 1) {
                            val nextIdx = currentIdx + 1
                            val nextSection = items[nextIdx]
                            val nextItemHeight = itemHeights[nextSection.name] ?: 0f
                            
                            val item = items.removeAt(currentIdx)
                            items.add(nextIdx, item)
                            
                            dragOffsetY -= (nextItemHeight + 8.dp.toPx())
                            draggingIndex = nextIdx
                        } else if (dragOffsetY < -threshold && currentIdx > 0) {
                            val prevIdx = currentIdx - 1
                            val prevSection = items[prevIdx]
                            val prevItemHeight = itemHeights[prevSection.name] ?: 0f
                            
                            val item = items.removeAt(currentIdx)
                            items.add(prevIdx, item)
                            
                            dragOffsetY += (prevItemHeight + 8.dp.toPx())
                            draggingIndex = prevIdx
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            isReleasing = true
                            releaseAnimatable.snapTo(dragOffsetY)
                            releaseAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            onOrderChanged(items.toList())
                            draggingIndex = null
                            dragOffsetY = 0f
                            isReleasing = false
                        }
                    },
                    onDragCancel = {
                        draggingIndex = null
                        dragOffsetY = 0f
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        itemsIndexed(items, key = { _, section -> section.name }) { index, section ->
            val isDragging = draggingIndex == index
            
            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.05f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "scale"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutNodes ->
                        itemPositions[section.name] = layoutNodes.positionInParent().y
                        itemHeights[section.name] = layoutNodes.size.height.toFloat()
                    }
                    .zIndex(if (isDragging) 2f else 1f)
                    .then(if (isDragging) {
                        Modifier.graphicsLayer { 
                            translationY = if (isReleasing) releaseAnimatable.value else dragOffsetY 
                        }
                    } else {
                        Modifier.animateItem()
                    })
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(section.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    HomeSectionSkeleton(type = section)
                }
            }
        }
    }
}

@Composable
fun HomeSectionSkeleton(type: HomeSectionType) {
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    when (type) {
        HomeSectionType.DISCOVER_ALBUMS -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(skeletonColor)
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(skeletonColor)
                )
            }
        }
        
        HomeSectionType.RECENTLY_PLAYED, HomeSectionType.RECENTLY_ADDED -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(skeletonColor)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(skeletonColor)
                        )
                    }
                }
            }
        }

        HomeSectionType.USER_PLAYLISTS -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            Row(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(skeletonColor)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(skeletonColor.copy(alpha = 0.2f))
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(skeletonColor.copy(alpha = 0.2f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(skeletonColor.copy(alpha = 0.2f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        HomeSectionType.TOP_ARTISTS -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(skeletonColor)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(skeletonColor)
                        )
                    }
                }
            }
        }
        
        HomeSectionType.RECOMMENDED -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(skeletonColor)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(skeletonColor)
                            )
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(skeletonColor)
                            )
                        }
                    }
                }
            }
        }
        
        HomeSectionType.MONTHLY_RECAP -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(skeletonColor)
            )
        }

        HomeSectionType.CLOUD_RECOMMENDATIONS -> {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(2) {
                    Column(
                        modifier = Modifier.width(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(4) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(skeletonColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(skeletonColor)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(skeletonColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
