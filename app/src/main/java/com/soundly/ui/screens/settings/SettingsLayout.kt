package com.soundly.ui.screens.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.soundly.ui.componentes.agslFrostedGlass
import kotlinx.coroutines.launch

@Composable
fun SettingsLayout(
    title: String,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val overscrollOffset = remember { Animatable(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val newOffset = overscrollOffset.value + available.y * 0.45f
                    scope.launch { overscrollOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        containerColor = containerColor,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val contentModifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = overscrollOffset.value }
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = 16.dp)

            Column(
                modifier = contentModifier
            ) {
                // Espaciadores para que el contenido no empiece justo debajo del header
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(60.dp))

                content()

                // Espacio al final para el padding inferior del Scaffold
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
            }

            // iOS Style Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .agslFrostedGlass(
                            radius = 40f,
                            tint = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                        )
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.2f to Color.Black,
                                    0.5f to Color.Black.copy(alpha = 0.7f),
                                    0.8f to Color.Black.copy(alpha = 0.3f),
                                    1.0f to Color.Transparent,
                                    startY = 0f,
                                    endY = size.height
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 23.dp, vertical = 8.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(32.dp)
                            .clip(CircleShape)
                            .agslFrostedGlass(
                                radius = 20f,
                                tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.soundly.R.string.permission_screen_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
