package com.jibet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jibet.app.R

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/** تایپوگرافی با فونت وزیرمتن — اندازه‌ها مطابق وب */
val JibetTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Vazir),
        displayMedium = base.displayMedium.copy(fontFamily = Vazir),
        displaySmall = base.displaySmall.copy(fontFamily = Vazir),
        headlineLarge = base.headlineLarge.copy(fontFamily = Vazir),
        headlineMedium = base.headlineMedium.copy(fontFamily = Vazir),
        headlineSmall = base.headlineSmall.copy(fontFamily = Vazir),
        titleLarge = base.titleLarge.copy(fontFamily = Vazir),
        titleMedium = base.titleMedium.copy(fontFamily = Vazir),
        titleSmall = base.titleSmall.copy(fontFamily = Vazir),
        bodyLarge = base.bodyLarge.copy(fontFamily = Vazir),
        bodyMedium = base.bodyMedium.copy(fontFamily = Vazir),
        bodySmall = base.bodySmall.copy(fontFamily = Vazir),
        labelLarge = base.labelLarge.copy(fontFamily = Vazir),
        labelMedium = base.labelMedium.copy(fontFamily = Vazir),
        labelSmall = base.labelSmall.copy(fontFamily = Vazir),
    )
}

/** استایل‌های پرکاربرد */
object T {
    /** 24px/800 — تیتر هدر خانه */
    val h1 = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp, color = Ink)
    /** 19px/800 — تیتر هدر صفحات */
    val header = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Ink)
    /** 34px/900 — عدد موجودی */
    val balance = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 34.sp, color = CardBg)
    /** 26px/900 */
    val num26 = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp, color = Ink)
    /** 22px/800 */
    val num22 = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink)
    /** 14px/700 — تیتر ردیف */
    val rowTitle = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
    /** 13px/700 */
    val label = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkSoft)
    /** 12px/500 */
    val caption = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSoft)
    /** 15px/700 دکمه اصلی */
    val button = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}
