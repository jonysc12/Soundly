package com.soundly.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScrollFadeContainer(
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {

    val showTopShadow by remember {
        derivedStateOf {
            listState?.let {
                it.firstVisibleItemIndex > 0 ||
                        it.firstVisibleItemScrollOffset > 0
            } ?: gridState?.let {
                it.firstVisibleItemIndex > 0 ||
                        it.firstVisibleItemScrollOffset > 0
            } ?: false
        }
    }

    val showBottomShadow by remember {
        derivedStateOf {
            listState?.let {
                val total = it.layoutInfo.totalItemsCount
                if (total == 0) return@derivedStateOf false
                it.layoutInfo.visibleItemsInfo.lastOrNull()?.index != total - 1
            } ?: gridState?.let {
                val total = it.layoutInfo.totalItemsCount
                if (total == 0) return@derivedStateOf false
                it.layoutInfo.visibleItemsInfo.lastOrNull()?.index != total - 1
            } ?: false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        content()

        if (showTopShadow) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        if (showBottomShadow) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
    }
}