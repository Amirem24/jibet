package com.jibet.app.ui.theme

import androidx.compose.ui.graphics.Color

/* پالت جیبت — قرارداد یکسان با globals.css نسخه وب */

val Expense = Color(0xFFF26D6D)
val ExpenseSoft = Color(0xFFFDEAEA)
val Income = Color(0xFF2FBF83)
val IncomeSoft = Color(0xFFE2F6EC)
val Ink = Color(0xFF232B3B)
val InkSoft = Color(0xFF8A92A6)
val TextMid = Color(0xFF5A6478)
val Faint = Color(0xFFB4BAC8)
val BgA = Color(0xFFEEF2FB)
val BgB = Color(0xFFE9EDF8)
val CardBg = Color(0xFFFFFFFF)
val Divider = Color(0xFFF1F3F9)
val ChartTrack = Color(0x1A8A92A6)

data class Accent(val key: String, val a: Color, val b: Color)

val ACCENTS = mapOf(
    "blue" to Accent("blue", Color(0xFF4F8CFF), Color(0xFF6E7BFF)),
    "green" to Accent("green", Color(0xFF2FBF83), Color(0xFF39C9A8)),
    "purple" to Accent("purple", Color(0xFF9B7BFF), Color(0xFFC77BFF)),
    "orange" to Accent("orange", Color(0xFFFF9F43), Color(0xFFFFB86B)),
)

fun accentOf(key: String?): Accent = ACCENTS[key] ?: ACCENTS.getValue("blue")

/** رنگ نوار پیشرفت بودجه — مثل budgetBarColor وب */
fun budgetBarColor(pct: Float): Color = when {
    pct < 70f -> Income
    pct < 100f -> Color(0xFFF5A524)
    else -> Expense
}
