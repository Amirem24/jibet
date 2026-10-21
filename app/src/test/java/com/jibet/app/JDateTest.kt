package com.jibet.app

import com.jibet.app.util.JDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

/** تست تقویم شمسی — رگرسیون کرش تاریخی (ArrayIndexOutOfBounds) و صحت الگوریتم jalaali-js */
class JDateTest {

    @Before
    fun setUp() {
        // قطعیت تست‌های وابسته به منطقه‌ی زمانی
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
    }

    /* ---------- تاریخ‌های مرجع ---------- */

    @Test
    fun `known dates convert correctly`() {
        // نمونه‌ی رسمی README جالالی-js
        assertEquals(JDate.JDateParts(1395, 1, 23), JDate.toJalali(LocalDate.of(2016, 4, 11)))
        assertEquals(LocalDate.of(2016, 4, 11), JDate.toGregorian(1395, 1, 23))

        // تاریخ کرش کاربر — ۲۰۲۵-۱۰-۲۱ باید ۲۹ مهر ۱۴۰۴ باشد
        assertEquals(JDate.JDateParts(1404, 7, 29), JDate.toJalali(LocalDate.of(2025, 10, 21)))

        // نوروزها
        assertEquals(JDate.JDateParts(1404, 1, 1), JDate.toJalali(LocalDate.of(2025, 3, 21)))
        assertEquals(JDate.JDateParts(1403, 1, 1), JDate.toJalali(LocalDate.of(2024, 3, 20)))
        assertEquals(JDate.JDateParts(1400, 1, 1), JDate.toJalali(LocalDate.of(2021, 3, 21)))

        // آخرین روز سال کبیسه ۱۴۰۳
        assertEquals(JDate.JDateParts(1403, 12, 30), JDate.toJalali(LocalDate.of(2025, 3, 20)))
    }

    @Test
    fun `leap years and month lengths`() {
        // سال‌های کبیسه‌ی واقعی در بازه: ۱۳۹۱، ۱۳۹۵، ۱۳۹۹، ۱۴۰۳، ۱۴۰۸
        assertTrue(JDate.isJalaliLeap(1391))
        assertTrue(JDate.isJalaliLeap(1395))
        assertTrue(JDate.isJalaliLeap(1399))
        assertTrue(JDate.isJalaliLeap(1403))
        assertTrue(JDate.isJalaliLeap(1408))
        assertFalse(JDate.isJalaliLeap(1393))
        assertFalse(JDate.isJalaliLeap(1400))
        assertFalse(JDate.isJalaliLeap(1401))
        assertFalse(JDate.isJalaliLeap(1402))
        assertFalse(JDate.isJalaliLeap(1404))

        assertEquals(31, JDate.jalaliMonthLength(1404, 1))
        assertEquals(30, JDate.jalaliMonthLength(1404, 7))
        assertEquals(29, JDate.jalaliMonthLength(1404, 12))
        assertEquals(30, JDate.jalaliMonthLength(1403, 12))
    }

    /* ---------- رفت‌وبرگشت کامل ۱۹۰۰ تا ۲۱۰۰ ---------- */

    @Test
    fun `round-trip every day 1900 to 2100`() {
        var d = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2100, 12, 31)
        var count = 0
        while (!d.isAfter(end)) {
            val j = JDate.toJalali(d)
            // ۱) برگشت به میلادی باید دقیقاً همان روز باشد
            assertEquals("round-trip $d", d, JDate.toGregorian(j.jy, j.jm, j.jd))
            // ۲) اجزای شمسی باید در محدوده‌ی معتبر باشند (کهنه‌ترین رگرسیون کرش)
            assertTrue("jm $j", j.jm in 1..12)
            assertTrue("jd $j", j.jd in 1..JDate.jalaliMonthLength(j.jy, j.jm))
            // ۳) فرمت هرگز نباید crash کند
            JDate.formatJalaliDate(d, JDate.Style.LONG)
            JDate.formatJalaliDate(d, JDate.Style.SHORT)
            JDate.formatJalaliDate(d, JDate.Style.NUMERIC)
            d = d.plusDays(1)
            count++
        }
        assertTrue("expected ~73k days, got $count", count > 70_000)
    }

    /* ---------- روز هفته ---------- */

    @Test
    fun `persian weekday starts on Saturday`() {
        // ۲۰۲۵-۱۰-۲۵ = شنبه → ۰
        assertEquals(0, JDate.persianWeekday(LocalDate.of(2025, 10, 25)))
        // یکشنبه → ۱
        assertEquals(1, JDate.persianWeekday(LocalDate.of(2025, 10, 26)))
        // سه‌شنبه → ۳
        assertEquals(3, JDate.persianWeekday(LocalDate.of(2025, 10, 21)))
        // جمعه → ۶
        assertEquals(6, JDate.persianWeekday(LocalDate.of(2025, 10, 24)))
    }

    @Test
    fun `persian week range starts Saturday and spans 7 days`() {
        val (start, end) = JDate.persianWeekRange(LocalDate.of(2025, 10, 21)) // سه‌شنبه
        val startDate = JDate.localDate(start)
        val endDate = JDate.localDate(end)
        assertEquals(0, JDate.persianWeekday(startDate)) // شروع = شنبه
        assertEquals(6, JDate.persianWeekday(endDate))   // پایان = جمعه
        assertEquals(6L, endDate.toEpochDay() - startDate.toEpochDay())
    }

    /* ---------- جابه‌جایی ماه شمسی ---------- */

    @Test
    fun `add jalali months wraps correctly`() {
        assertEquals(JDate.JDateParts(1405, 1, 1), JDate.addJalaliMonths(1404, 12, 1))
        assertEquals(JDate.JDateParts(1403, 12, 1), JDate.addJalaliMonths(1404, 1, -1))
        assertEquals(JDate.JDateParts(1406, 3, 1), JDate.addJalaliMonths(1404, 3, 24))
        // ۱۰ − ۲۶ = −۱۶ ← −۴ (۱۴۰۳) ← ۸ (۱۴۰۲)
        assertEquals(JDate.JDateParts(1402, 8, 1), JDate.addJalaliMonths(1404, 10, -26))
    }

    /* ---------- قالب‌ها و محافظت ---------- */

    @Test
    fun `format styles are stable`() {
        val d = LocalDate.of(2025, 10, 21)
        assertEquals("1404/07/29", JDate.formatJalaliDate(d, JDate.Style.NUMERIC))
        assertEquals("29 مهر", JDate.formatJalaliDate(d, JDate.Style.SHORT))
        assertEquals("29 مهر 1404", JDate.formatJalaliDate(d, JDate.Style.LONG))
    }

    @Test
    fun `month name is guarded against bad index`() {
        assertEquals("فروردین", JDate.monthName(1))
        assertEquals("اسفند", JDate.monthName(12))
        // ورودی نامعتبر — هرگز crash نکند
        assertEquals("فروردین", JDate.monthName(0))
        assertEquals("اسفند", JDate.monthName(80))
        assertEquals("فروردین", JDate.monthName(-5))
    }

    @Test
    fun `month range boundaries match the month`() {
        val (start, end, j) = JDate.jalaliMonthRange(LocalDate.of(2025, 10, 21))
        assertEquals(JDate.JDateParts(1404, 7, 1), JDate.toJalali(JDate.localDate(start)))
        assertEquals(JDate.JDateParts(1404, 7, 30), JDate.toJalali(JDate.localDate(end)))
        assertEquals(j, JDate.toJalali(LocalDate.of(2025, 10, 21)))
    }
}
