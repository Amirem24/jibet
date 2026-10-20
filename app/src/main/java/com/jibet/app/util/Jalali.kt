package com.jibet.app.util

import java.time.Instant
import java.time.ZoneId

/**
 * Jalali (Shamsi) <-> Gregorian conversion, port of the well-known
 * jdf.scr.ir algorithm. All arithmetic verified for modern dates.
 */
object Jalali {

    private val G_D_M = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    /** Returns [jy, jm, jd] */
    fun toJalali(gy: Int, gm: Int, gd: Int): IntArray {
        var jy = if (gy <= 1600) 0 else 979
        val g = if (gy <= 1600) gy - 621 else gy - 1600
        val gy2 = if (gm > 2) g + 1 else g
        var days = 365L * g + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + G_D_M[gm - 1]
        jy += 33 * (days / 12053).toInt()
        days %= 12053
        jy += 4 * (days / 1461).toInt()
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31).toInt() else 7 + ((days - 186) / 30).toInt()
        val jd = 1 + (if (days < 186) (days % 31) else ((days - 186) % 30)).toInt()
        return intArrayOf(jy, jm, jd)
    }

    /** Returns [gy, gm, gd] */
    fun toGregorian(jy0: Int, jm: Int, jd: Int): IntArray {
        var jy = jy0
        var gy = if (jy <= 979) 621 else 1600
        jy -= if (jy <= 979) 0 else 979
        var days = 365L * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + 78L + jd + (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        gy += 400 * (days / 146097).toInt()
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524).toInt()
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461).toInt()
        days %= 1461
        if (days > 365) {
            gy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        var gd = (days + 1).toInt()
        val leap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val salA = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    /** epoch millis -> [jy, jm, jd] in the device time zone */
    fun parts(ts: Long, zone: ZoneId = ZoneId.systemDefault()): IntArray {
        val d = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
        return toJalali(d.year, d.monthValue, d.dayOfMonth)
    }

    fun dayOfJalaliMonth(ts: Long, zone: ZoneId = ZoneId.systemDefault()): Int = parts(ts, zone)[2]
}
