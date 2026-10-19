package com.example.languagebridge.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.languagebridge.data.Language

object AppColors {
    val ZoneBackground = Color(0xFF1C1C1E)
    val BubbleBackground = Color(0xFF2E2E32)
    val MiddlePanelBackground = Color(0xFF101012)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFA0A0A5)
    val AccentBlue = Color(0xFF4E8DF5)
    val ErrorRed = Color(0xFFFF6B6B)

    private val RussiaFlagColors = listOf(
        Color(0xFFFFFFFF), // белый
        Color(0xFF0039A6), // синий
        Color(0xFFD52B1E), // красный
    )
    private val ArmeniaFlagColors = listOf(
        Color(0xFFD90012), // красный
        Color(0xFF0033A0), // синий
        Color(0xFFF2A800), // оранжевый (абрикосовый)
    )

    fun flagBrush(language: Language): Brush {
        val colors = if (language == Language.RUSSIAN) RussiaFlagColors else ArmeniaFlagColors
        return Brush.verticalGradient(
            colorStops = arrayOf(
                0f to colors[0],
                0.33f to colors[0],
                0.34f to colors[1],
                0.66f to colors[1],
                0.67f to colors[2],
                1f to colors[2],
            )
        )
    }
}
