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
    primary = JeduPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3F42A6),
    onPrimaryContainer = Color(0xFFE2E4FF),
    secondary = JeduSecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4C279E),
    onSecondaryContainer = Color(0xFFEADBFF),
    tertiary = JeduTertiaryDark,
    onTertiary = Color.Black,
    background = JeduBackgroundDark,
    onBackground = JeduTextPrimaryDark,
    surface = JeduSurfaceDark,
    onSurface = JeduTextPrimaryDark,
    surfaceVariant = JeduSurfaceVariantDark,
    onSurfaceVariant = JeduTextSecondaryDark,
    error = JeduError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = JeduPrimary,
    onPrimary = Color.White,
    primaryContainer = JeduLavender,
    onPrimaryContainer = JeduPrimaryVariant,
    secondary = JeduSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADBFF),
    onSecondaryContainer = Color(0xFF4C279E),
    tertiary = JeduTertiary,
    onTertiary = Color.Black,
    background = JeduBackgroundLight,
    onBackground = JeduTextPrimary,
    surface = JeduSurfaceLight,
    onSurface = JeduTextPrimary,
    surfaceVariant = Color(0xFFF1F3FB),
    onSurfaceVariant = JeduTextSecondary,
    error = JeduError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize cohesive JeduAI brand palette
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
