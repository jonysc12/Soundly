package com.soundly.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width


import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speaker

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp
private val CONTROL_HEIGHT = 40.dp

@Composable
fun PlayerExtraControls(
    onOpenDevice: () -> Unit,
    onShare: () -> Unit,
    onOpenQueue: () -> Unit,
    onColor: Color
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // DISPOSITIVO
        Row(
            modifier = Modifier
                .height(CONTROL_HEIGHT)
                .clip(RoundedCornerShape(20.dp))
                .background(onColor.copy(alpha = 0.08f))
                .clickable(onClick = onOpenDevice)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Rounded.Speaker,
                contentDescription = "Dispositivo",
                tint = onColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = "Dispositivo",
                color = onColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // SHARE
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(CONTROL_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            bottomStart = 20.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )
                    .background(onColor.copy(alpha = 0.10f))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Compartir",
                    tint = onColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // QUEUE
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(CONTROL_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .background(onColor.copy(alpha = 0.10f))
                    .clickable(onClick = onOpenQueue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Cola",
                    tint = onColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}