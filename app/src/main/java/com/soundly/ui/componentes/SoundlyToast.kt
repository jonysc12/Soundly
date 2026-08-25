package com.soundly.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.soundly.ui.componentes.agslFrostedGlass
import kotlinx.coroutines.delay

enum class SoundlyToastState {
    SUCCESS, ERROR, LOADING, INFO
}

@Composable
fun SoundlyToast(
    message: String,
    isVisible: Boolean,
    state: SoundlyToastState = SoundlyToastState.INFO,
    onDismiss: () -> Unit
) {
    LaunchedEffect(isVisible, state) {
        if (isVisible && state != SoundlyToastState.LOADING) {
            delay(3000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .agslFrostedGlass(
                            radius = 24f,
                            tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (state) {
                            SoundlyToastState.LOADING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            else -> {
                                val icon = when (state) {
                                    SoundlyToastState.SUCCESS -> Icons.Rounded.CheckCircle
                                    SoundlyToastState.ERROR -> Icons.Rounded.Error
                                    else -> Icons.Rounded.Info
                                }
                                val iconColor = when (state) {
                                    SoundlyToastState.SUCCESS -> Color(0xFF4CAF50)
                                    SoundlyToastState.ERROR -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
