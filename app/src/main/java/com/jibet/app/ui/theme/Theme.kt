package com.jibet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** همه‌ی توکن‌های رنگ جیبت — روشن و تیره؛ تنها منبع رنگ برای کل UI */
data class JibetColors(
    val isDark: Boolean = false,
    val accentA: Color,
    val accentB: Color,
    val expense: Color,
    val expenseSoft: Color,
    val income: Color,
    val incomeSoft: Color,
    val ink: Color,
    val inkSoft: Color,
    val textMid: Color,
    val faint: Color,
    val bgA: Color,
    val bgB: Color,
    val card: Color,
    val cardSoft: Color,
    val inputBg: Color,
    val iconBubbleBg: Color,
    val divider: Color,
    val border: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color,
    val skeleton: Color,
    val iconMuted: Color,
    val scrim: Color,
    val track: Color,
)

val LocalJibetColors = staticCompositionLocalOf {
    jibetColors(accentOf("blue"), dark = false)
}

/** ساخت مجموعه توکن‌ها برای حالت روشن/تیره با اکسنت دلخواه */
fun jibetColors(accent: Accent, dark: Boolean): JibetColors = if (dark) JibetColors(
    isDark = true,
    accentA = accent.a, accentB = accent.b,
    expense = ExpenseD, expenseSoft = ExpenseSoftD,
    income = IncomeD, incomeSoft = IncomeSoftD,
    ink = InkD, inkSoft = InkSoftD, textMid = TextMidD, faint = FaintD,
    bgA = BgAD, bgB = BgBD,
    card = CardD, cardSoft = CardSoftD, inputBg = InputBgD, iconBubbleBg = IconBubbleBgD,
    divider = DividerD, border = BorderD,
    glassTop = GlassTopD, glassBottom = GlassBottomD, glassBorder = GlassBorderD,
    skeleton = SkeletonD, iconMuted = IconMutedD, scrim = ScrimD, track = TrackD,
) else JibetColors(
    isDark = false,
    accentA = accent.a, accentB = accent.b,
    expense = ExpenseL, expenseSoft = ExpenseSoftL,
    income = IncomeL, incomeSoft = IncomeSoftL,
    ink = InkL, inkSoft = InkSoftL, textMid = TextMidL, faint = FaintL,
    bgA = BgAL, bgB = BgBL,
    card = CardL, cardSoft = CardSoftL, inputBg = InputBgL, iconBubbleBg = IconBubbleBgL,
    divider = DividerL, border = BorderL,
    glassTop = GlassTopL, glassBottom = GlassBottomL, glassBorder = GlassBorderL,
    skeleton = SkeletonL, iconMuted = IconMutedL, scrim = ScrimL, track = TrackL,
)

/** تم جیبت — روشن یا تیره (انتخاب کاربر در «بیشتر») */
@Composable
fun JibetTheme(
    accentKey: String = "blue",
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accent = accentOf(accentKey)
    val jc = jibetColors(accent, dark)
    val scheme = if (dark) darkColorScheme(
        primary = accent.a,
        onPrimary = Color.White,
        secondary = accent.b,
        background = BgAD,
        onBackground = InkD,
        surface = CardD,
        onSurface = InkD,
        surfaceVariant = BgBD,
        onSurfaceVariant = InkSoftD,
        error = ExpenseD,
        outline = DividerD,
    ) else lightColorScheme(
        primary = accent.a,
        onPrimary = Color.White,
        secondary = accent.b,
        background = BgAL,
        onBackground = InkL,
        surface = CardL,
        onSurface = InkL,
        surfaceVariant = BgBL,
        onSurfaceVariant = InkSoftL,
        error = ExpenseL,
        outline = InkSoftL.copy(alpha = 0.18f),
    )
    CompositionLocalProvider(
        LocalJibetColors provides jc,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = JibetTypography,
            content = content,
        )
    }
}
