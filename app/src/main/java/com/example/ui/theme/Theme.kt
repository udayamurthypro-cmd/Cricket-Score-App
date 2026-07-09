package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryRedContainer,
    onPrimary = OnPrimaryContainer,
    primaryContainer = PrimaryRed,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryGreenContainer,
    onSecondary = OnSecondaryContainer,
    secondaryContainer = SecondaryGreen,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryBlueContainer,
    onTertiary = OnTertiaryContainer,
    background = OnBackground,
    onBackground = BackgroundColor,
    surface = OnBackground,
    onSurface = BackgroundColor,
    surfaceVariant = OnSurfaceVariantValue,
    onSurfaceVariant = BackgroundColor,
    error = ErrorRed,
    onError = OnErrorValue
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = OnErrorValue,
    primaryContainer = PrimaryRedContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryGreen,
    onSecondary = OnErrorValue,
    secondaryContainer = SecondaryGreenContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryBlue,
    onTertiary = OnErrorValue,
    background = BackgroundColor,
    onBackground = OnBackground,
    surface = SurfaceLowest,
    onSurface = OnSurfaceValue,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = OnSurfaceVariantValue,
    outline = OutlineValue,
    outlineVariant = OutlineVariantValue,
    error = ErrorRed,
    onError = OnErrorValue
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
