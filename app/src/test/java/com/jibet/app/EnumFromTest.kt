package com.jibet.app

import com.jibet.app.data.AccountKind
import com.jibet.app.data.Frequency
import com.jibet.app.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

/** رگرسیون باگ آنبوردینگ: پریست‌ها kind را با حروف کوچک می‌دهند («income»/«bank»)
 *  و from()های حساس به بزرگی حروف همه را بی‌صدا EXPENSE/CASH می‌کردند */
class EnumFromTest {

    @Test
    fun `kind from is case-insensitive`() {
        assertEquals(Kind.INCOME, Kind.from("income"))
        assertEquals(Kind.INCOME, Kind.from("INCOME"))
        assertEquals(Kind.EXPENSE, Kind.from("expense"))
        assertEquals(Kind.EXPENSE, Kind.from(null))
        assertEquals(Kind.EXPENSE, Kind.from("garbage"))
    }

    @Test
    fun `account kind from is case-insensitive`() {
        assertEquals(AccountKind.BANK, AccountKind.from("bank"))
        assertEquals(AccountKind.WALLET, AccountKind.from("wallet"))
        assertEquals(AccountKind.SAVINGS, AccountKind.from("savings"))
        assertEquals(AccountKind.CASH, AccountKind.from("cash"))
        assertEquals(AccountKind.CASH, AccountKind.from(null))
    }

    @Test
    fun `frequency from is case-insensitive`() {
        assertEquals(Frequency.WEEKLY, Frequency.from("weekly"))
        assertEquals(Frequency.YEARLY, Frequency.from("YEARLY"))
        assertEquals(Frequency.MONTHLY, Frequency.from(null))
    }
}
