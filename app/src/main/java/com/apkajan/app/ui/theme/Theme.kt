package com.apkajan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF4F46E5)
val IndigoLight = Color(0xFFA5B4FC)
val Danger = Color(0xFFD32F2F)
val Warn = Color(0xFFE08600)
val Ok = Color(0xFF2E9D5B)
val Tomato = Color(0xFFE8572A)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Tomato,
    error = Danger,
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEEF5)
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFFF8A65),
    error = Color(0xFFFF6B6B),
    background = Color(0xFF0F1117),
    surface = Color(0xFF171A22),
    surfaceVariant = Color(0xFF232733)
)

@Composable
fun ApkAjanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
