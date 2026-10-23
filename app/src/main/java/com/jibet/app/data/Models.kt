package com.jibet.app.data

/** نوع تراکنش */
enum class Kind { EXPENSE, INCOME;

    companion object {
        // FIX (debug): قبلاً فقط "INCOME" دقیق شناخته می‌شد و هر مقدار دیگری (مثل "income"
        // نسخه وب) بی‌صدا EXPENSE می‌شد؛ حالا مثل بقیه‌ی enumها case-insensitive است
        fun from(s: String?): Kind = entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: EXPENSE
    }
}

/** نوع حساب */
enum class AccountKind { CASH, BANK, WALLET, SAVINGS, OTHER;

    companion object {
        // FIX (debug): case-insensitive مثل Kind — پریست‌های آنبوردینگ kind را کوچک می‌دهند
        // («bank») و قبلاً همه‌ی حساب‌ها بی‌صدا CASH می‌شدند
        fun from(s: String?): AccountKind = entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: CASH
    }
}

/** تناوب تراکنش تکراری */
enum class Frequency { DAILY, WEEKLY, MONTHLY, YEARLY;

    companion object {
        fun from(s: String?): Frequency = entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: MONTHLY
    }
}
