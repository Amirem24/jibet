package com.jibet.app.util

/** پیش‌فرض‌های جیبت — قرارداد یکسان با lib/presets.ts نسخه وب */

data class PresetAccount(val key: String, val name: String, val kind: String, val icon: String, val color: String)

val PRESET_ACCOUNTS = listOf(
    PresetAccount("cash", "نقد", "cash", "banknote", "#34C77B"),
    PresetAccount("bank", "کارت بانکی", "bank", "credit-card", "#5B7FFF"),
    PresetAccount("wallet", "کیف پول الکترونیکی", "wallet", "wallet", "#F5A524"),
    PresetAccount("savings", "پس‌انداز", "savings", "piggy-bank", "#F07BB4"),
    PresetAccount("other", "سایر", "other", "more-horizontal", "#8A93A6"),
)

data class PresetCategory(val key: String, val name: String, val kind: String, val icon: String, val color: String)

val PRESET_CATEGORIES = listOf(
    PresetCategory("food", "خوراک و نوشیدنی", "expense", "utensils", "#34C77B"),
    PresetCategory("transport", "حمل و نقل", "expense", "car", "#5B7FFF"),
    PresetCategory("housing", "مسکن", "expense", "home", "#F5A524"),
    PresetCategory("fun", "تفریح و سرگرمی", "expense", "gamepad", "#B07BFF"),
    PresetCategory("education", "آموزش", "expense", "book", "#23C4D8"),
    PresetCategory("shopping", "خرید", "expense", "shopping-bag", "#F07BB4"),
    PresetCategory("bills", "قبض‌ها", "expense", "receipt", "#F26D6D"),
    PresetCategory("health", "سلامت", "expense", "heart", "#FF8FA3"),
    PresetCategory("clothing", "پوشاک", "expense", "shirt", "#8B8BF5"),
    PresetCategory("salary", "حقوق", "income", "briefcase", "#34C77B"),
    PresetCategory("sale", "فروش", "income", "tag", "#23C4D8"),
    PresetCategory("gift", "هدیه", "income", "gift", "#F07BB4"),
    PresetCategory("invest", "سرمایه‌گذاری", "income", "trending-up", "#5B7FFF"),
    PresetCategory("income-other", "سایر درآمد", "income", "more-horizontal", "#8A93A6"),
)

/** ست پیشنهادی در آنبوردینگ */
val SUGGESTED_EXPENSE_KEYS = listOf("food", "transport", "housing", "fun", "education", "bills")
val SUGGESTED_INCOME_KEYS = listOf("salary", "income-other")

val CATEGORY_COLORS = listOf(
    "#34C77B", "#5B7FFF", "#F5A524", "#B07BFF", "#23C4D8", "#F07BB4",
    "#F26D6D", "#FF8FA3", "#8B8BF5", "#4ECDC4", "#FFB020", "#8A93A6",
)

val ACCOUNT_ICONS = listOf("banknote", "credit-card", "wallet", "piggy-bank", "more-horizontal")

val CATEGORY_ICONS = listOf(
    "utensils", "car", "home", "gamepad", "book", "shopping-bag", "receipt", "heart", "shirt",
    "briefcase", "tag", "gift", "trending-up", "wallet", "coffee", "zap", "wifi", "bus",
    "dumbbell", "music", "plane", "laptop", "graduation", "phone", "popcorn", "droplets",
    "clapperboard", "baby", "ring", "more-horizontal",
)

val GOAL_ICONS = listOf("plane", "laptop", "car", "home", "gift", "graduation", "ring", "phone")
val GOAL_COLORS = listOf("#34C77B", "#5B7FFF", "#B07BFF", "#F5A524", "#F07BB4", "#23C4D8")

data class AccentPreset(val key: String, val name: String, val a: String, val b: String)

val ACCENT_PRESETS = listOf(
    AccentPreset("blue", "آبی روشن", "#4F8CFF", "#6E7BFF"),
    AccentPreset("green", "سبز نعنایی", "#2FBF83", "#39C9A8"),
    AccentPreset("purple", "بنفش ملایم", "#9B7BFF", "#C77BFF"),
    AccentPreset("orange", "نارنجی گرم", "#FF9F43", "#FFB86B"),
)

val FREQUENCY_LABELS = mapOf(
    "DAILY" to "روزانه",
    "WEEKLY" to "هفتگی",
    "MONTHLY" to "ماهانه",
    "YEARLY" to "سالانه",
)

val KIND_LABELS = mapOf(
    "EXPENSE" to "هزینه",
    "INCOME" to "درآمد",
)

val ACCOUNT_KIND_LABELS = mapOf(
    "CASH" to "نقد",
    "BANK" to "بانکی",
    "WALLET" to "کیف پول",
    "SAVINGS" to "پس‌انداز",
    "OTHER" to "سایر",
)
