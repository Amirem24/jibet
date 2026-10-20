package com.jibet.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * تبدیل تاریخ میلادی ↔ شمسی — پورت دقیق الگوریتم jalaali-js (همان الگوریتم نسخه وب).
 * تمام مقادیر بر اساس تاریخ محلی دستگاه.
 */
object JDate {

    data class JDateParts(val jy: Int, val jm: Int, val jd: Int)

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097,
        2192, 2262, 2324, 2394, 2456, 3178,
    )

    private fun div(a: Int, b: Int): Int {
        var q = a / b
        if ((a % b != 0) && ((a < 0) != (b < 0))) q--
        return q
    }

    private fun mod(a: Int, b: Int): Int = a - div(a, b) * b

    private class JalCalResult(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCalResult {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jm = 0
        var jump = 0
        for (i in 1 until bl) {
            jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCalResult(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Long {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4).toLong() +
            div(153 * mod(gm + 9, 12) + 2, 5).toLong() + gd - 34840408L
        d -= div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4).toLong() + 752L
        return d
    }

    private fun d2g(jdn: Long): IntArray {
        var j = 4 * jdn + 139361631L
        j += div(div((4 * jdn + 183187720).toInt(), 146097) * 3, 4).toLong() * 4 - 3908L
        val i = div(mod(j.toInt(), 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j.toInt(), 1461) - 100100 + div(8 - gm, 6)
        return intArrayOf(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31L - div(jm, 7) * (jm - 7) + jd - 1L
    }

    private fun d2j(jdn: Long): JDateParts {
        val gy = d2g(jdn)[0]
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.march)
        var k = (jdn - jdn1f).toInt()
        if (k in 0..185) {
            return JDateParts(jy, 1 + div(k, 31), mod(k, 31) + 1)
        } else if (k >= 0) {
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JDateParts(jy, 7 + div(k, 30), mod(k, 30) + 1)
    }

    /** تاریخ میلادی → شمسی */
    fun toJalali(date: LocalDate): JDateParts = d2j(g2d(date.year, date.monthValue, date.dayOfMonth))

    /** epoch millis → شمسی (زمان محلی دستگاه) */
    fun toJalali(millis: Long, zone: ZoneId = ZoneId.systemDefault()): JDateParts =
        toJalali(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

    /** تاریخ شمسی → میلادی */
    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        val g = d2g(j2d(jy, jm, jd))
        return LocalDate.of(g[0], g[1], g[2])
    }

    fun isJalaliLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    /** تعداد روزهای یک ماه شمسی */
    fun jalaliMonthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isJalaliLeap(jy)) 30 else 29
    }

    val J_MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** شنبه تا جمعه */
    val J_WEEKDAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    /** ایندکس روز هفته با شنبه = ۰ */
    fun persianWeekday(date: LocalDate): Int = (date.dayOfWeek.value % 7)

    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun endOfDay(date: LocalDate): Long =
        LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, 23, 59, 59)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 999

    /** بازه‌ی ماه شمسی شامل تاریخ داده‌شده (میلی‌ثانیه) */
    fun jalaliMonthRange(date: LocalDate): Triple<Long, Long, JDateParts> {
        val j = toJalali(date)
        val start = startOfDay(toGregorian(j.jy, j.jm, 1))
        val end = endOfDay(toGregorian(j.jy, j.jm, jalaliMonthLength(j.jy, j.jm)))
        return Triple(start, end, j)
    }

    /** بازه‌ی سال شمسی شامل تاریخ داده‌شده (میلی‌ثانیه) */
    fun jalaliYearRange(date: LocalDate): Triple<Long, Long, JDateParts> {
        val j = toJalali(date)
        val start = startOfDay(toGregorian(j.jy, 1, 1))
        val end = endOfDay(toGregorian(j.jy, 12, jalaliMonthLength(j.jy, 12)))
        return Triple(start, end, j)
    }

    /** بازه‌ی هفته (شنبه تا جمعه) شامل تاریخ داده‌شده (میلی‌ثانیه) */
    fun persianWeekRange(date: LocalDate): Pair<Long, Long> {
        val wd = persianWeekday(date)
        val start = date.minusDays(wd.toLong())
        val end = start.plusDays(6)
        return Pair(startOfDay(start), endOfDay(end))
    }

    fun addJalaliMonths(jy: Int, jm: Int, delta: Int): JDateParts {
        var m = jm + delta
        var y = jy
        while (m > 12) { m -= 12; y += 1 }
        while (m < 1) { m += 12; y -= 1 }
        return JDateParts(y, m, 1)
    }

    enum class Style { NUMERIC, LONG, SHORT }

    fun formatJalaliDate(date: LocalDate, style: Style = Style.NUMERIC): String {
        val j = toJalali(date)
        return formatParts(j, style)
    }

    fun formatJalaliDate(millis: Long, style: Style = Style.NUMERIC): String =
        formatJalaliDate(
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate(),
            style,
        )

    private fun formatParts(j: JDateParts, style: Style): String = when (style) {
        Style.LONG -> "${j.jd} ${J_MONTHS[j.jm - 1]} ${j.jy}"
        Style.SHORT -> "${j.jd} ${J_MONTHS[j.jm - 1]}"
        Style.NUMERIC ->
            "${j.jy}/${j.jm.toString().padStart(2, '0')}/${j.jd.toString().padStart(2, '0')}"
    }

    /** برچسب گروه‌بندی لیست تراکنش‌ها: امروز / دیروز / X مهر */
    fun dayGroupLabel(millis: Long, today: LocalDate = LocalDate.now()): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val diff = today.toEpochDay() - date.toEpochDay()
        if (diff == 0L) return "امروز"
        if (diff == 1L) return "دیروز"
        val j = toJalali(date)
        val t = toJalali(today)
        return if (j.jm == t.jm && j.jy == t.jy) "${j.jd} ${J_MONTHS[j.jm - 1]}"
        else formatJalaliDate(date, Style.SHORT)
    }

    /** شماره‌ی روز شمسی از epoch millis */
    fun jalaliDayOf(millis: Long): Int = toJalali(millis).jd

    fun localDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun millis(date: LocalDate): Long = startOfDay(date)

    /** ساعت:دقیقه از epoch millis (مثلاً ۱۴:۰۵) */
    fun timeLabel(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return String.format("%02d:%02d", t.hour, t.minute)
    }
}
