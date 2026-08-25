package com.soundly.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true, name = "Widget Alargado (Wide)")
@Composable
fun WideWidgetPreview() {
    Box(
        modifier = Modifier
            .width(350.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF4A2C2C)) // Simulado color de palette
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulación de carátula
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu Boda",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Oscar Maydon",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White)
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black)
                    }
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Widget Cuadrado (Big)")
@Composable
fun BigWidgetPreview() {
    Box(
        modifier = Modifier
            .size(250.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E3A5F)) // Simulado color de palette
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Starboy",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "The Weeknd",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(12.dp))
            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black)
                    }
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
