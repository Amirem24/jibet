package com.jibet.app.ui.theme

import androidx.compose.ui.graphics.Color

/* پالت جیبت — قرارداد یکسان با globals.css نسخه وب.
   هر رنگ یک جفت روشن/تیره دارد؛ توکن‌ها از طریق JibetColors توزیع می‌شوند. */

// ---------- روشن ----------
val ExpenseL = Color(0xFFF26D6D)
val ExpenseSoftL = Color(0xFFFDEAEA)
val IncomeL = Color(0xFF2FBF83)
val IncomeSoftL = Color(0xFFE2F6EC)
val InkL = Color(0xFF232B3B)
val InkSoftL = Color(0xFF8A92A6)
val TextMidL = Color(0xFF5A6478)
val FaintL = Color(0xFFB4BAC8)
val BgAL = Color(0xFFEEF2FB)
val BgBL = Color(0xFFE9EDF8)
val CardL = Color(0xFFFFFFFF)
val CardSoftL = Color(0xCCFFFFFF)      // white 80%
val InputBgL = Color(0xD9FFFFFF)       // white 85%
val IconBubbleBgL = Color(0xFFF1F3F9)
val DividerL = Color(0xFFF1F3F9)
val BorderL = Color(0xFFE3E8F4)
val GlassTopL = Color(0xEBFFFFFF)      // white 92%
val GlassBottomL = Color(0x9EFFFFFF)   // white 62%
val GlassBorderL = Color(0xE6FFFFFF)   // white 90%
val SkeletonL = Color(0xFFE4E8F4)
val IconMutedL = Color(0xFFA7AEC0)
val ScrimL = Color(0xB30F1420)         // تیره ۷۰٪ — شیت باید صفحه‌ی زیر را کامل محو کند
val TrackL = Color(0x1A8A92A6)

// ---------- تیره (هم‌خانواده با .dark وب) ----------
val ExpenseD = Color(0xFFFF8080)
val ExpenseSoftD = Color(0x29FF8080)   // ۱۶٪ روی کارت تیره
val IncomeD = Color(0xFF34CC8C)
val IncomeSoftD = Color(0x2934CC8C)
val InkD = Color(0xFFEEF1F9)
val InkSoftD = Color(0xFF9AA3B8)
val TextMidD = Color(0xFFB7C0D4)
val FaintD = Color(0xFF6E7891)
val BgAD = Color(0xFF0F1420)
val BgBD = Color(0xFF131A2A)
val CardD = Color(0xFF171E2E)
val CardSoftD = Color(0xFF1C2436)
val InputBgD = Color(0xFF1C2436)
val IconBubbleBgD = Color(0xFF222B40)
val DividerD = Color(0xFF263046)
val BorderD = Color(0xFF263046)
val GlassTopD = Color(0x1AFFFFFF)      // سفید ۱۰٪ — قرص/کارت شیشه‌ای در تیره باید از پس‌زمینه جداشود
val GlassBottomD = Color(0x0BFFFFFF)   // سفید ~۴٪
val GlassBorderD = Color(0x24FFFFFF)   // سفید ۱۴٪
val SkeletonD = Color(0xFF222B40)
val IconMutedD = Color(0xFF7C86A0)
val ScrimD = Color(0xCC000000)         // مشکی ۸۰٪ — شیت باید صفحه‌ی زیر را کامل محو کند
val TrackD = Color(0x1A9AA3B8)

data class Accent(val key: String, val a: Color, val b: Color)

val ACCENTS = mapOf(
    "blue" to Accent("blue", Color(0xFF4F8CFF), Color(0xFF6E7BFF)),
    "green" to Accent("green", Color(0xFF2FBF83), Color(0xFF39C9A8)),
    "purple" to Accent("purple", Color(0xFF9B7BFF), Color(0xFFC77BFF)),
    "orange" to Accent("orange", Color(0xFFFF9F43), Color(0xFFFFB86B)),
)

fun accentOf(key: String?): Accent = ACCENTS[key] ?: ACCENTS.getValue("blue")

/** رنگ نوار پیشرفت بودجه — مثل budgetBarColor وب (نسخه تم‌آگاه) */
fun JibetColors.budgetBarColor(pct: Float): Color = when {
    pct < 70f -> income
    pct < 100f -> if (isDark) Color(0xFFFFBE4D) else Color(0xFFF5A524)
    else -> expense
}
