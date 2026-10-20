package com.jibet.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToLong
import java.util.Locale

object Fmt {

    private val FA = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** Convert latin digits inside a string to Persian digits. */
    fun fa(s: String): String = buildString {
        for (c in s) append(if (c in '0'..'9') FA[c - '0'] else c)
    }

    fun faNum(n: Long): String = fa(n.toString())

    /** 1250000 -> ۱٬۲۵۰٬۰۰۰ */
    fun money(v: Double): String {
        val n = v.roundToLong()
        return fa(String.format(Locale.US, "%,d", n).replace(',', '٬'))
    }

    /** money + currency suffix */
    fun moneyCurrency(v: Double, currency: String): String = money(v) + " " + currency

    val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val WEEKDAYS = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یکشنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنجشنبه",
        DayOfWeek.FRIDAY to "جمعه"
    )

    private val WEEKDAYS_SHORT = mapOf(
        DayOfWeek.SATURDAY to "ش",
        DayOfWeek.SUNDAY to "ی",
        DayOfWeek.MONDAY to "د",
        DayOfWeek.TUESDAY to "س",
        DayOfWeek.WEDNESDAY to "چ",
        DayOfWeek.THURSDAY to "پ",
        DayOfWeek.FRIDAY to "ج"
    )

    fun weekday(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        WEEKDAYS[Instant.ofEpochMilli(ts).atZone(zone).dayOfWeek] ?: ""

    fun weekdayShort(dow: DayOfWeek): String = WEEKDAYS_SHORT[dow] ?: ""

    /** ۱۴۰۳/۱۱/۳۰ */
    fun date(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val p = Jalali.parts(ts, zone)
        return fa(String.format(Locale.US, "%d/%02d/%02d", p[0], p[1], p[2]))
    }

    /** ۳۰ بهمن ۱۴۰۳ */
    fun dateLong(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val p = Jalali.parts(ts, zone)
        return faNum(p[2].toLong()) + " " + MONTHS[p[1] - 1] + " " + faNum(p[0].toLong())
    }

    /** "امروز" / "دیروز" / null */
    fun relativeDay(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String? {
        val today = LocalDate.now(zone)
        val d = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
        return when (d) {
            today -> "امروز"
            today.minusDays(1) -> "دیروز"
            else -> null
        }
    }

    /** friendly label: امروز / دیروز / شنبه / ۱۲ مهر */
    fun dayLabel(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        relativeDay(ts, zone)?.let { return it }
        val d = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return if (d.isAfter(today.minusDays(7))) {
            weekday(ts, zone)
        } else {
            val p = Jalali.parts(ts, zone)
            faNum(p[2].toLong()) + " " + MONTHS[p[1] - 1]
        }
    }

    /** For material3 DatePicker: local date -> UTC start of that date (what the picker expects) */
    fun tsToDatePicker(ts: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(ts).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** For material3 DatePicker: picked UTC millis -> local timestamp keeping time-of-day of base */
    fun datePickerToTs(pickedUtcMillis: Long, baseTs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val baseTime = Instant.ofEpochMilli(baseTs).atZone(zone).toLocalTime()
        return LocalDateTime.of(pickedDate, baseTime).atZone(zone).toInstant().toEpochMilli()
    }
}

object Ranges {

    private fun zone() = ZoneId.systemDefault()

    private fun startOfDay(d: LocalDate): Long = d.atStartOfDay(zone()).toInstant().toEpochMilli()

    /** inclusive [from, to] epoch millis of today (local) */
    fun today(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val d = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
        return startOfDay(d) to endOfDay(d)
    }

    private fun endOfDay(d: LocalDate): Long = d.plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli() - 1

    /** Jalali week starts on Saturday */
    fun thisWeek(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val d = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
        val start = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        val end = start.plusDays(7)
        return startOfDay(start) to startOfDay(end) - 1
    }

    /** current Jalali month [from, to] */
    fun thisMonth(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val p = Jalali.parts(now)
        val startG = Jalali.toGregorian(p[0], p[1], 1)
        val start = LocalDate.of(startG[0], startG[1], startG[2])
        val nm = if (p[1] == 12) intArrayOf(p[0] + 1, 1, 1) else intArrayOf(p[0], p[1] + 1, 1)
        val nextG = Jalali.toGregorian(nm[0], nm[1], nm[2])
        val end = LocalDate.of(nextG[0], nextG[1], nextG[2])
        return startOfDay(start) to startOfDay(end) - 1
    }

    /** current Jalali year [from, to] */
    fun thisYear(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val p = Jalali.parts(now)
        val startG = Jalali.toGregorian(p[0], 1, 1)
        val start = LocalDate.of(startG[0], startG[1], startG[2])
        val nextG = Jalali.toGregorian(p[0] + 1, 1, 1)
        val end = LocalDate.of(nextG[0], nextG[1], nextG[2])
        return startOfDay(start) to startOfDay(end) - 1
    }

    /** last N days including today: list of local dates, oldest first */
    fun lastNDates(n: Int, now: Long = System.currentTimeMillis()): List<LocalDate> {
        val today = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
        return (n - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }
}
