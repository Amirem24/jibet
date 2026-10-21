package com.jibet.app

import com.jibet.app.util.Fmt
import org.junit.Assert.assertEquals
import org.junit.Test

/** تست فرمت‌دهی فارسی — رگرسیون کرش parseNumber روی ارقام یونیکد ناشناخته */
class FmtTest {

    /* ---------- parseNumber ---------- */

    @Test
    fun `latin digits parse`() {
        assertEquals(123.0, Fmt.parseNumber("123"), 0.0)
        assertEquals(1000.0, Fmt.parseNumber("1,000"), 0.0)
        assertEquals(12.0, Fmt.parseNumber("0012"), 0.0)
    }

    @Test
    fun `persian digits parse`() {
        assertEquals(123.0, Fmt.parseNumber("۱۲۳"), 0.0)
        assertEquals(100000.0, Fmt.parseNumber("۱۰۰٬۰۰۰"), 0.0)
        assertEquals(1500000.0, Fmt.parseNumber("۱,۵۰۰,۰۰۰"), 0.0)
    }

    @Test
    fun `arabic-indic digits parse`() {
        assertEquals(123.0, Fmt.parseNumber("١٢٣"), 0.0)
    }

    @Test
    fun `unknown unicode digits are ignored instead of crashing`() {
        // دِواَناغاری ۱۲۳ — قبلاً NumberFormatException می‌داد (کرش واقعی)
        assertEquals(0.0, Fmt.parseNumber("१२३"), 0.0)
        assertEquals(0.0, Fmt.parseNumber("१५"), 0.0)
        // ترکیب رقم ناشناخته با رقم معتبر — فقط معتبرها شمرده می‌شوند
        assertEquals(1.0, Fmt.parseNumber("१1५"), 0.0)
        // ترکیب فارسی و لاتین
        assertEquals(1200.0, Fmt.parseNumber("۱۲00"), 0.0)
    }

    @Test
    fun `garbage input never crashes`() {
        assertEquals(0.0, Fmt.parseNumber(""), 0.0)
        assertEquals(0.0, Fmt.parseNumber("abc"), 0.0)
        assertEquals(0.0, Fmt.parseNumber("—"), 0.0)
        assertEquals(0.0, Fmt.parseNumber("   "), 0.0)
    }

    /* ---------- money / faDigits / percent ---------- */

    @Test
    fun `money formats with separators and persian digits`() {
        assertEquals("۰", Fmt.money(0.0))
        assertEquals("۱,۲۳۴", Fmt.money(1234.0))
        assertEquals("۳۲,۴۸۰,۰۰۰", Fmt.money(32480000.0))
        assertEquals("−۵", Fmt.money(-5.0))
    }

    @Test
    fun `faDigits maps only ascii digits`() {
        assertEquals("۱۲۳", Fmt.faDigits("123"))
        assertEquals("۱۴۰۴/۰۷/۲۹", Fmt.faDigits("1404/07/29"))
        // ارقام فارسی دست‌نخورده می‌مانند (بدون کرش)
        assertEquals("۱۲۳", Fmt.faDigits("۱۲۳"))
    }

    @Test
    fun `percent rounds`() {
        assertEquals("۶۴٪", Fmt.percent(64.2))
        assertEquals("۰٪", Fmt.percent(0.0))
    }
}
