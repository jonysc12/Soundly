package com.soundly.ui.screens.settings.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.HearingDisabled
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundly.R
import com.soundly.ui.screens.settings.SettingsLayout
import android.media.AudioManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun SafePlaybackPage(
    onBack: () -> Unit,
    viewModel: AudioViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Cálculo de límites y recomendaciones según la edad
    val limitMinutes = when {
        settings.userAge < 12 -> 60
        settings.userAge < 18 -> 90
        else -> 120
    }
    
    val currentExposure = settings.dailyExposureMinutes
    val progress = (currentExposure.toFloat() / limitMinutes).coerceIn(0f, 1f)
    val remainingMinutes = (limitMinutes - currentExposure).coerceAtLeast(0)

    // Detección de dB aproximados (Nivel de volumen sistema)
    val currentVolumePercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / 
                               audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) * 100
    val estimatedDb = 40 + (currentVolumePercent * 0.5f) // Estimación simple: 40dB base + 50dB de rango
    val highVolumeAlert = estimatedDb > 85 && settings.dbMonitoringEnabled

    SettingsLayout(
        title = stringResource(R.string.audio_safe_playback_title),
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Estado de Descanso Forzado
            AnimatedVisibility(
                visible = settings.isResting,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RestingCard(
                    endTime = settings.restEndTime,
                    onCancel = { viewModel.cancelRest() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Alerta de dB
            AnimatedVisibility(
                visible = highVolumeAlert,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                HighDbAlertCard()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tarjeta de Estadísticas de Exposición
            ExposureStatsCard(
                progress = progress,
                remainingMinutes = remainingMinutes
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Historial Semanal
            WeeklyHistoryCard(weeklyStats = settings.weeklyExposureMinutes, limit = limitMinutes)

            Spacer(modifier = Modifier.height(24.dp))

            // Interruptor Principal
            SettingsItemCard {
                M3SwitchItem(
                    icon = Icons.Rounded.HealthAndSafety,
                    title = stringResource(R.string.audio_safe_playback_title),
                    description = stringResource(R.string.audio_safe_playback_desc),
                    checked = settings.safePlaybackEnabled,
                    onCheckedChange = { viewModel.toggleSafePlayback(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Configuración Avanzada
            SettingsItemCard {
                Column {
                    M3SwitchItem(
                        icon = Icons.Rounded.HearingDisabled,
                        title = stringResource(R.string.audio_safe_playback_ignore_speaker),
                        description = stringResource(R.string.audio_safe_playback_ignore_speaker_desc),
                        checked = settings.ignoreSpeakerExposure,
                        onCheckedChange = { viewModel.toggleIgnoreSpeaker(it) },
                        drawDivider = true
                    )
                    M3SwitchItem(
                        icon = Icons.Rounded.AutoFixHigh,
                        title = stringResource(R.string.audio_safe_playback_intelligent_volume),
                        description = stringResource(R.string.audio_safe_playback_intelligent_volume_desc),
                        checked = settings.intelligentVolumeReduction,
                        onCheckedChange = { viewModel.toggleIntelligentVolume(it) },
                        drawDivider = true
                    )
                    M3SwitchItem(
                        icon = Icons.Rounded.Timer,
                        title = stringResource(R.string.audio_safe_playback_forced_rest),
                        description = stringResource(R.string.audio_safe_playback_forced_rest_desc),
                        checked = settings.forcedRestEnabled,
                        onCheckedChange = { viewModel.toggleForcedRest(it) },
                        drawDivider = true
                    )
                    M3SwitchItem(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        title = stringResource(R.string.audio_safe_playback_db_monitoring),
                        description = stringResource(R.string.audio_safe_playback_db_monitoring_desc),
                        checked = settings.dbMonitoringEnabled,
                        onCheckedChange = { viewModel.toggleDbMonitoring(it) },
                        drawDivider = true
                    )
                    M3SwitchItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.audio_safe_playback_notifications_title),
                        description = stringResource(R.string.audio_safe_playback_notifications_desc),
                        checked = settings.safePlaybackNotificationsEnabled,
                        onCheckedChange = { viewModel.toggleSafeNotifications(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Guía de Edad
            Text(
                text = stringResource(R.string.audio_safe_playback_age_guide),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp)
            )
            
            SettingsItemCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.audio_safe_playback_age_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AgeOption(
                            icon = Icons.Rounded.ChildCare,
                            label = stringResource(R.string.audio_safe_playback_age_kids),
                            selected = settings.userAge < 12,
                            onClick = { viewModel.setUserAge(10) }
                        )
                        AgeOption(
                            icon = Icons.Rounded.Face,
                            label = stringResource(R.string.audio_safe_playback_age_teens),
                            selected = settings.userAge in 12..17,
                            onClick = { viewModel.setUserAge(15) }
                        )
                        AgeOption(
                            icon = Icons.Rounded.Person,
                            label = stringResource(R.string.audio_safe_playback_age_adults),
                            selected = settings.userAge >= 18,
                            onClick = { viewModel.setUserAge(25) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recomendación de Salud Dinámica
            HealthRecommendationCard(age = settings.userAge)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExposureStatsCard(
    progress: Float,
    remainingMinutes: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.audio_safe_playback_exposure_stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = if (progress > 0.9f) Color.Red else primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$remainingMinutes",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (progress > 0.9f) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.audio_safe_playback_minutes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.audio_safe_playback_exposure_remaining),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyHistoryCard(weeklyStats: Map<Int, Int>, limit: Int) {
    val days = listOf(
        stringResource(R.string.audio_safe_playback_day_sun),
        stringResource(R.string.audio_safe_playback_day_mon),
        stringResource(R.string.audio_safe_playback_day_tue),
        stringResource(R.string.audio_safe_playback_day_wed),
        stringResource(R.string.audio_safe_playback_day_thu),
        stringResource(R.string.audio_safe_playback_day_fri),
        stringResource(R.string.audio_safe_playback_day_sat)
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.audio_safe_playback_weekly_history),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Generar barras para los 7 días (Calendar.SUNDAY = 1 a Calendar.SATURDAY = 7)
                for (i in 1..7) {
                    val minutes = weeklyStats[i] ?: 0
                    val barHeightProgress = (minutes.toFloat() / limit).coerceIn(0.05f, 1.2f)
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(80.dp * barHeightProgress)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (minutes >= limit) Color.Red else MaterialTheme.colorScheme.primary
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = days[i - 1],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestingCard(endTime: Long, onCancel: () -> Unit) {
    val remainingMins = ((endTime - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
    
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Red.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = Color.Red)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.audio_safe_playback_resting_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        text = stringResource(R.string.audio_safe_playback_resting_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.audio_safe_playback_resting_remaining, remainingMins),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.button_cancel), color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun HighDbAlertCard() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = stringResource(R.string.audio_safe_playback_db_alert),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AgeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun HealthRecommendationCard(age: Int) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = stringResource(R.string.audio_safe_playback_recommendation_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        age < 12 -> stringResource(R.string.audio_safe_playback_recommendation_kids)
                        age < 18 -> stringResource(R.string.audio_safe_playback_recommendation_teens)
                        else -> stringResource(R.string.audio_safe_playback_recommendation_adults)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsItemCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun M3SwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    drawDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (drawDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
