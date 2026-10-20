package com.jibet.app.util

object Defaults {

    /** soft palette for categories / accounts / goals */
    val palette = listOf(
        "#2A9D8F", "#E9C46A", "#E76F51", "#84A98C", "#B5838D",
        "#6D6875", "#CB997E", "#7FB685", "#DDA15E", "#8E7DBE"
    )

    val emojiPicker = listOf(
        "🍔", "🍕", "🚌", "✈️", "🛍️", "🧾", "🎬", "🎮", "💊", "🏠",
        "🎁", "☕", "📚", "🐾", "💇", "⛽", "📱", "🏋️", "💼", "💻",
        "📈", "🏦", "👛", "💵", "💳", "🐖", "🎯", "🚀", "🎓", "🩺",
        "🌱", "⚽", "👶", "👗", "🚗", "🛠️"
    )

    data class CatDef(val name: String, val emoji: String, val colorHex: String)

    val expenseCategories = listOf(
        CatDef("خوراک", "🍔", "#E76F51"),
        CatDef("حمل‌ونقل", "🚌", "#DDA15E"),
        CatDef("خرید", "🛍️", "#B5838D"),
        CatDef("قبض‌ها", "🧾", "#6D6875"),
        CatDef("سرگرمی", "🎬", "#8E7DBE"),
        CatDef("سلامت", "💊", "#84A98C"),
        CatDef("خانه", "🏠", "#CB997E"),
        CatDef("هدیه", "🎁", "#E9C46A")
    )

    val incomeCategories = listOf(
        CatDef("حقوق", "💼", "#2A9D8F"),
        CatDef("فریلنس", "💻", "#7FB685"),
        CatDef("سود سرمایه", "📈", "#E9C46A")
    )

    data class AccountDef(val name: String, val emoji: String, val colorHex: String)

    val accountSuggestions = listOf(
        AccountDef("نقدی", "💵", "#84A98C"),
        AccountDef("کارت بانکی", "💳", "#2A9D8F"),
        AccountDef("کیف پول", "👛", "#E9C46A")
    )

    val goalEmojis = listOf("🎯", "✈️", "🏠", "🚗", "🎓", "💍", "📱", "🐖", "🏖️", "💻")

    fun freqLabel(freq: String): String = when (freq) {
        "DAILY" -> "روزانه"
        "WEEKLY" -> "هفتگی"
        else -> "ماهانه"
    }
}
