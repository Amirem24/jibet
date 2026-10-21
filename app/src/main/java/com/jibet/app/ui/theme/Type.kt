package com.jibet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jibet.app.R

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
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
