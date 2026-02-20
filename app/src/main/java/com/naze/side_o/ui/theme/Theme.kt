package com.naze.side_o.ui.theme

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
import com.naze.side_o.data.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA0A7FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6B73CC),
    onPrimaryContainer = Color.White,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1A1D2E),
    onBackground = Color(0xFFE8E9F0),
    surface = Color(0xFF252838),
    onSurface = Color(0xFFE8E9F0),
    surfaceVariant = Color(0xFF2D3250),
    onSurfaceVariant = Color(0xFF9EA1B1),
    outline = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E2FF),
    onPrimaryContainer = TextPrimary,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0F2F8),
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary
)

@Composable
fun SideOTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val resolvedDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        resolvedDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
