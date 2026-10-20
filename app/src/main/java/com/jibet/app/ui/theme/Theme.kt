package com.jibet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** رنگ‌های جیبت با رنگ اصلی قابل تعویض (مثل data-accent وب) */
data class JibetColors(
    val accentA: Color,
    val accentB: Color,
    val expense: Color = Expense,
    val income: Color = Income,
    val ink: Color = Ink,
    val inkSoft: Color = InkSoft,
)

val LocalJibetColors = staticCompositionLocalOf {
    JibetColors(accentOf("blue").a, accentOf("blue").b)
}

/** تم همیشه روشن — مثل نسخه وب */
@Composable
fun JibetTheme(
    accentKey: String = "blue",
    content: @Composable () -> Unit,
) {
    val accent = accentOf(accentKey)
    val scheme = lightColorScheme(
        primary = accent.a,
        onPrimary = Color.White,
        secondary = accent.b,
        background = BgA,
        onBackground = Ink,
        surface = CardBg,
        onSurface = Ink,
        surfaceVariant = BgB,
        onSurfaceVariant = InkSoft,
        error = Expense,
        outline = InkSoft.copy(alpha = 0.18f),
    )
    CompositionLocalProvider(
        LocalJibetColors provides JibetColors(accent.a, accent.b),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = JibetTypography,
            content = content,
        )
    }
}
