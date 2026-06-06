package com.soundly.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundly.R

/**
 * Objeto para manejar la extracción y adaptación de colores dinámicos
 * basados en el logo de Soundly.
 */
object SoundlyColors {

    /**
     * Adapta un color azul para que sea consistente en ambos temas.
     * Mismo azul en light y dark mode.
     */
    fun adaptBlueForTheme(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = hsv[1].coerceIn(0.6f, 1.0f)
        hsv[2] = hsv[2].coerceIn(0.55f, 0.85f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Intensifica el color azul para el efecto glow.
     */
    fun boostBlueForGlow(color: Color, isDark: Boolean): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = 1.0f
        hsv[2] = if (isDark) 1.0f else 0.75f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Extrae el azul más vibrante de un bitmap.
     */
    fun extractMostVibrantBlueFromBitmap(bitmap: android.graphics.Bitmap): Int? {
        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        var bestSaturation = 0f
        var bestArgb: Int? = null
        val hsv = FloatArray(3)

        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                android.graphics.Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]
                if (hue in 180f..260f && sat > bestSaturation && value > 0.2f) {
                    bestSaturation = sat
                    bestArgb = pixel
                }
            }
        }
        return bestArgb
    }
}

/**
 * Modificador para aplicar efecto glow a un botón.
 */
fun Modifier.soundlyGlowEffect(
    glowColor: Color,
    glowAlpha: Float = 0.5f,
    glowRadius: Dp = 32.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    glowRadius.toPx(),
                    0f,
                    0f,
                    android.graphics.Color.argb(
                        (glowAlpha * 255).toInt(),
                        (glowColor.red * 255).toInt(),
                        (glowColor.green * 255).toInt(),
                        (glowColor.blue * 255).toInt()
                    )
                )
            }
        }
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = 50.dp.toPx(),
            radiusY = 50.dp.toPx(),
            paint = paint
        )
    }
}

/**
 * Botón principal de Soundly con color dinámico extraído del logo.
 * Aplica automáticamente el color adaptado al tema y efecto glow.
 *
 * @param extractedColor Color extraído del logo (null usa primary del tema)
 * @param onClick Callback al hacer click
 * @param modifier Modificadores opcionales
 * @param text Texto del botón
 */
@Composable
fun SoundlyPrimaryButton(
    extractedColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    val buttonColor = extractedColor?.let { color ->
        SoundlyColors.adaptBlueForTheme(color)
    } ?: MaterialTheme.colorScheme.primary

    val glowColor = extractedColor?.let { color ->
        SoundlyColors.boostBlueForGlow(color, isDark)
    } ?: buttonColor

    val contentColor = if (buttonColor.luminance() > 0.4f) Color.Black else Color.White

    val glowAlpha = if (isDark) 0.55f else 0.22f
    val glowRadius = if (isDark) 24.dp else 14.dp

    Button(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .soundlyGlowEffect(
                glowColor = glowColor,
                glowAlpha = if (enabled) glowAlpha else glowAlpha * 0.35f,
                glowRadius = glowRadius
            ),
        contentPadding = PaddingValues(vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(text = text)
    }
}

/**
 * Botón secundario de Soundly con color dinámico extraído del logo.
 * Versión con fondo semi-transparente, ideal para botones con ícono.
 *
 * @param extractedColor Color extraído del logo (null usa primary del tema)
 * @param onClick Callback al hacer click
 * @param modifier Modificadores opcionales
 * @param content Contenido del botón
 */
@Composable
fun SoundlySecondaryButton(
    extractedColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val buttonColor = extractedColor?.let { color ->
        SoundlyColors.adaptBlueForTheme(color)
    } ?: MaterialTheme.colorScheme.primary

    Button(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor.copy(alpha = 0.12f),
            contentColor = buttonColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        content()
    }
}

@Composable
fun SoundlySecondaryButtonon(
    extractedColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val buttonColor = extractedColor?.let { color ->
        SoundlyColors.adaptBlueForTheme(color)
    } ?: MaterialTheme.colorScheme.primary

    Button(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor.copy(alpha = 0.12f),
            contentColor = buttonColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        content()
    }
}

/**
 * Helper para cargar el color del logo desde recursos.
 * Debe ser llamado dentro de un LaunchedEffect.
 */
@Composable
fun rememberLogoColor(): Color? {
    var extractedColor by remember { mutableStateOf<Color?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val resources = context.resources
        val inputStream = resources.openRawResource(R.drawable.logo_soundly)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        bitmap?.let { bmp ->
            val palette = androidx.palette.graphics.Palette.from(bmp).generate()
            palette?.let {
                val allSwatches = listOfNotNull(
                    it.vibrantSwatch,
                    it.lightVibrantSwatch,
                    it.darkVibrantSwatch,
                    it.dominantSwatch,
                    it.mutedSwatch,
                    it.lightMutedSwatch,
                    it.darkMutedSwatch
                )

                val blueSwatches = allSwatches.filter { swatch ->
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                    val hue = hsv[0]
                    val saturation = hsv[1]
                    val value = hsv[2]
                    hue in 180f..260f && saturation > 0.3f && value > 0.2f
                }

                val bestArgb: Int? = if (blueSwatches.isNotEmpty()) {
                    blueSwatches.maxByOrNull { swatch ->
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                        hsv[1]
                    }?.rgb
                } else {
                    SoundlyColors.extractMostVibrantBlueFromBitmap(bmp)
                }

                bestArgb?.let { argb ->
                    extractedColor = Color(argb)
                }
            }
        }
    }

    return extractedColor
}
