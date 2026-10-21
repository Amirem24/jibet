package com.jibet.app.util

import kotlin.math.abs
import kotlin.math.roundToLong

/** ابزارهای فرمت‌دهی فارسی: اعداد، پول، درصدها — قرارداد یکسان با نسخه وب */
object Fmt {

    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    /** 1234 → «۱٬۲۳۴» با ارقام فارسی (جداکننده «,» لاتین مثل وب)
     *  فقط ارقام ASCII نگاشت می‌شوند — ارقام فارسی/عربی دست‌نخورده می‌مانند تا
     *  ورودی دو-بار-فرمت‌شده هرگز StringIndexOutOfBounds نسازد. */
    fun faDigits(input: Any): String = input.toString().map { c ->
        if (c in '0'..'9') FA_DIGITS[c - '0'] else c
    }.joinToString("")

    fun faDigits(input: Long): String = faDigits(input.toString())
    fun faDigits(input: Int): String = faDigits(input.toString())

    /** 32480000.0 → «۳۲,۴۸۰,۰۰۰» (منفی با علامت −) */
    fun money(amount: Double): String {
        val n = abs(amount).roundToLong()
        val s = n.toString()
            .replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
        return (if (amount < 0) "−" else "") + faDigits(s)
    }

    fun money(amount: Long): String = money(amount.toDouble())

    /** 64.2 → «۶۴٪» */
    fun percent(p: Double): String = "${faDigits(Math.round(p).toInt())}٪"

    /** تبدیل رشته‌ی فارسی/لاتین عدد به Double — هیچ‌وقت استثنا نمی‌دهد.
     *  قواعد جداکننده‌ها:
     *  • «,» و «٬» و «،» و فاصله = جداکننده هزارگان → همیشه حذف می‌شوند
     *  • «٫» (اعشار فارسی) = اعشار
     *  • «.» اگر همه‌ی گروه‌های بعد از هر نقطه دقیقاً ۳ رقمی باشند → هزارگان («۱.۵۰۰» = ۱۵۰۰)
     *    در غیر این صورت نخستین نقطه اعشار است («۱۲.۵» = ۱۲٫۵)
     *  • هر رقم یونیکد دیگر (مثلاً دِواَناغاری) نادیده گرفته می‌شود. */
    fun parseNumber(input: String): Double {
        // ۱) فقط نویسه‌های مجاز نگه داشته می‌شوند؛ ارقام فارسی/عربی به لاتین تبدیل می‌شوند
        val t = buildString {
            for (c in input) when {
                c in '۰'..'۹' -> append('0' + (c - '۰'))
                c in '٠'..'٩' -> append('0' + (c - '٠'))
                c in '0'..'9' -> append(c)
                c == '.' || c == '٫' || c == ',' || c == '٬' || c == '،' -> append(c)
            }
        }
        // ۲) جداکننده‌های هزارگان صریح («,» و «٬» و «،») همیشه حذف می‌شوند
        val noThousands = t.filter { it != ',' && it != '٬' && it != '،' }
        if (noThousands.isEmpty()) return 0.0

        val cleaned = if ('٫' in noThousands) {
            // «٫» اعشار است؛ هر «.» باقی‌مانده هزارگان است
            var seenDecimal = false
            buildString {
                for (c in noThousands) when {
                    c == '.' -> {}
                    c == '٫' -> if (!seenDecimal) { seenDecimal = true; append('.') }
                    else -> append(c)
                }
            }
        } else {
            val groups = noThousands.split('.')
            when {
                groups.size == 1 -> noThousands
                // «1.500» / «1.500.000» — همه‌ی گروه‌های بعد از نقطه ۳ رقمی‌اند → هزارگان
                groups.drop(1).all { it.length == 3 } -> noThousands.replace(".", "")
                // «12.5» — نخستین نقطه اعشار است تا مقادیر ذخیره‌شده سالم رفت‌وبرگشت شوند
                else -> {
                    var seenDecimal = false
                    buildString {
                        for (c in noThousands) when {
                            c == '.' -> if (!seenDecimal) { seenDecimal = true; append('.') }
                            else -> append(c)
                        }
                    }
                }
            }
        }
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /** فقط ارقام ورودی (برای جستجوی مبلغ) — ارقام فارسی/عربی به لاتین تبدیل می‌شوند */
    fun digitsOnly(input: String): String = buildString {
        for (c in input) when {
            c in '۰'..'۹' -> append('0' + (c - '۰'))
            c in '٠'..'٩' -> append('0' + (c - '٠'))
            c in '0'..'9' -> append(c)
        }
    }
}
