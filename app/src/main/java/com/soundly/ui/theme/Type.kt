package com.soundly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.soundly.R

private val SoundlyFont = FontFamily(
    Font(R.font.handwriting, FontWeight.Normal),
    Font(R.font.handwriting, FontWeight.Bold)
)

val SoundlyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    displayMedium = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    displaySmall = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    headlineLarge = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    headlineMedium = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    headlineSmall = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    titleLarge = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    titleMedium = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    titleSmall = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    bodyLarge = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    bodyMedium = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    bodySmall = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    labelLarge = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    labelMedium = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        fontSynthesis = FontSynthesis.Weight
    ),
    labelSmall = TextStyle(
        fontFamily = SoundlyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        fontSynthesis = FontSynthesis.Weight
    )
)
