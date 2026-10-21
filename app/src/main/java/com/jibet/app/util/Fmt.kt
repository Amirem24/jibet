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

    /** تبدیل رشته‌ی فارسی/لاتین عدد به Double — همه‌ی جداکننده‌ها حذف می‌شوند */
    fun parseNumber(input: String): Double {
        val cleaned = input
            .map { c ->
                when {
                    c in '۰'..'۹' -> ('0' + (c - '۰'))
                    c in '٠'..'٩' -> ('0' + (c - '٠'))
                    else -> c
                }
            }
            .filter { it.isDigit() }
            .joinToString("")
        return if (cleaned.isEmpty()) 0.0 else cleaned.toDouble()
    }
}
