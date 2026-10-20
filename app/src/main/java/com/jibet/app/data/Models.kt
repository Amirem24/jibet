package com.jibet.app.data

/** نوع تراکنش */
enum class Kind { EXPENSE, INCOME;

    companion object {
        fun from(s: String?): Kind = if (s == "INCOME") INCOME else EXPENSE
    }
}

/** نوع حساب */
enum class AccountKind { CASH, BANK, WALLET, SAVINGS, OTHER;

    companion object {
        fun from(s: String?): AccountKind = entries.firstOrNull { it.name == s } ?: CASH
    }
}

/** تناوب تراکنش تکراری */
enum class Frequency { DAILY, WEEKLY, MONTHLY, YEARLY;

    companion object {
        fun from(s: String?): Frequency = entries.firstOrNull { it.name == s } ?: MONTHLY
    }
}
