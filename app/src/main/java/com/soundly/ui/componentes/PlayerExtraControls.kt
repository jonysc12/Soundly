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

import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.soundly.R

private val CONTROL_HEIGHT = 36.dp

@Composable
fun PlayerExtraControls(
    onOpenDevice: () -> Unit,
    onShare: () -> Unit,
    onOpenQueue: () -> Unit,
    onColor: Color,
    modifier: Modifier = Modifier,
    deviceName: String = stringResource(R.string.device_name_default),
    deviceIcon: ImageVector = Icons.Rounded.Speaker,
    onOpenLyrics: (() -> Unit)? = null
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // DISPOSITIVO
        Row(
            modifier = Modifier
                .height(CONTROL_HEIGHT)
                .clip(RoundedCornerShape(18.dp))
                .background(onColor.copy(alpha = 0.08f))
                .clickable(onClick = onOpenDevice)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = deviceIcon,
                contentDescription = stringResource(R.string.cd_device),
                tint = onColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = deviceName,
                color = onColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // SHARE
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(CONTROL_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            bottomStart = 18.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(onColor.copy(alpha = 0.08f))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    tint = onColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (onOpenLyrics != null) {
                Spacer(Modifier.width(4.dp))
                // LYRICS
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(CONTROL_HEIGHT)
                        .clip(RoundedCornerShape(4.dp))
                        .background(onColor.copy(alpha = 0.08f))
                        .clickable(onClick = onOpenLyrics),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = stringResource(R.string.lyrics_title),
                        tint = onColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // QUEUE
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(CONTROL_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(onColor.copy(alpha = 0.08f))
                    .clickable(onClick = onOpenQueue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.cd_queue),
                    tint = onColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerExtraControlsPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(16.dp)
        ) {
            PlayerExtraControls(
                onOpenDevice = {},
                onShare = {},
                onOpenQueue = {},
                onColor = Color.White
            )
        }
    }
}