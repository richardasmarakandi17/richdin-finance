package com.richdin.finance.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FarmGreenContainer,
    onPrimaryContainer = FarmGreenText,
    secondary = BarnWoodMedium,
    onSecondary = Color.White,
    secondaryContainer = FarmCardSurfaceVariant,
    onSecondaryContainer = BarnWoodDark,
    tertiary = FarmStrawWarning,
    onTertiary = Color.White,
    tertiaryContainer = FarmStrawContainer,
    onTertiaryContainer = FarmStrawText,
    error = FarmBarnRed,
    onError = Color.White,
    errorContainer = FarmBarnRedContainer,
    onErrorContainer = FarmBarnRedText,
    background = FarmCreamBg,
    onBackground = FarmTextPrimary,
    surface = FarmCardSurface,
    onSurface = FarmTextPrimary,
    surfaceVariant = FarmCardSurfaceVariant,
    onSurfaceVariant = FarmTextSecondary,
    outline = FarmDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = FarmGreenSecondary,
    onPrimary = Color.Black,
    primaryContainer = FarmGreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = BarnWoodLight,
    onSecondary = Color.White,
    secondaryContainer = FarmDarkSurfaceVariant,
    onSecondaryContainer = FarmDarkTextPrimary,
    tertiary = FarmStrawWarning,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5D4037),
    onTertiaryContainer = Color.White,
    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFF5C0000),
    onErrorContainer = Color.White,
    background = FarmDarkBg,
    onBackground = FarmDarkTextPrimary,
    surface = FarmDarkSurface,
    onSurface = FarmDarkTextPrimary,
    surfaceVariant = FarmDarkSurfaceVariant,
    onSurfaceVariant = FarmDarkTextSecondary,
    outline = Color(0xFF4A4944)
)

@Composable
fun RichdinFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
