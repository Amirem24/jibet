package com.jibet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    secondary = AmberSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = CoralTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = Color(0xFF4A150A),
    background = JibetBackground,
    onBackground = JibetOnSurface,
    surface = JibetSurface,
    onSurface = JibetOnSurface,
    surfaceVariant = JibetSurfaceVariant,
    onSurfaceVariant = JibetOnSurfaceVariant,
    outline = JibetOutline,
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealOnContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark,
    tertiary = CoralTertiaryDark,
    onTertiary = Color(0xFF4A150A),
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = Color(0xFFFFDBD1),
    background = JibetBackgroundDark,
    onBackground = JibetOnSurfaceDark,
    surface = JibetSurfaceDark,
    onSurface = JibetOnSurfaceDark,
    surfaceVariant = JibetSurfaceVariantDark,
    onSurfaceVariant = JibetOnSurfaceVariantDark,
    outline = JibetOutlineDark,
    error = Color(0xFFFFB4AB)
)

data class JibetColors(val income: Color, val expense: Color)

val LocalJibetColors = staticCompositionLocalOf {
    JibetColors(Color(0xFF2A9D8F), Color(0xFFE76F51))
}

@Composable
fun JibetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    val jc = if (darkTheme) {
        JibetColors(Color(0xFF63C7B8), Color(0xFFF09B7E))
    } else {
        JibetColors(Color(0xFF21867A), Color(0xFFD9603F))
    }
    CompositionLocalProvider(LocalJibetColors provides jc) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
