package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkPrimary,
    onTertiary = DarkBackground,
    onBackground = DarkText,
    onSurface = DarkText,
    primaryContainer = Color(0xFF2E4572), // Muted dark indigo
    onPrimaryContainer = DarkPrimary, // Crisp style
    secondaryContainer = Color(0xFF242730),
    onSecondaryContainer = Color(0xFFE2E2E6)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = LightPrimary,
    onTertiary = LightSurface,
    onBackground = LightText,
    onSurface = LightText,
    primaryContainer = LightSecondary, // #D9E2FF
    onPrimaryContainer = Color(0xFF001945), // Deep navy
    secondaryContainer = Color(0xFFF2F0F4), // Muted card background
    onSecondaryContainer = Color(0xFF1D1B20)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color setting
    dynamicColor: Boolean = false, // Set to false by default so our custom beautiful colors are prioritize over standard Android wallpaper colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
