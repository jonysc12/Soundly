package com.soundly.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.soundly.data.repository.ArtworkShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberArtworkShape(artworkShape: ArtworkShape): Shape {
    val polygon = remember(artworkShape) {
        when (artworkShape) {
            ArtworkShape.CIRCLE -> MaterialShapes.Circle
            ArtworkShape.SQUARE -> MaterialShapes.Square
            ArtworkShape.ARCH -> MaterialShapes.Arch
            ArtworkShape.PILL -> MaterialShapes.Pill
            ArtworkShape.ARROW -> MaterialShapes.Arrow
            ArtworkShape.PENTAGON -> MaterialShapes.Pentagon
            ArtworkShape.COOKIE_4 -> MaterialShapes.Cookie4Sided
            ArtworkShape.COOKIE_6 -> MaterialShapes.Cookie6Sided
            ArtworkShape.COOKIE_7 -> MaterialShapes.Cookie7Sided
            ArtworkShape.CLOVER_4 -> MaterialShapes.Clover4Leaf
            else -> null
        }
    }

    return if (polygon != null) {
        polygon.toShape()
    } else {
        remember { RoundedCornerShape(24.dp) }
    }
}
