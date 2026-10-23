package com.jibet.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "jibet_settings")

/** وضعیت بوت برنامه —Splash فقط تا نخستین تابش DataStore نمایش داده می‌شود */
sealed class BootState {
    data object Loading : BootState()
    data object Fresh : BootState()
    data object Done : BootState()
}

/** حالت نمایش برنامه — پیش‌فرض: هم‌راستا با دستگاه */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(k: String?): ThemeMode = entries.firstOrNull { it.key == k } ?: SYSTEM
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val ACCENT = stringPreferencesKey("accent")
        val THEME = stringPreferencesKey("theme")
    }

    // FIX (debug): فایل خراب DataStore قبلاً Flow را با CorruptionException می‌ترکاند و اپ کرش می‌کرد؛
    // حالا روی خطای IO مثل «تنظیمات خالی» رفتار می‌شود تا اپ بالا بیاید
    private val safeData: Flow<androidx.datastore.preferences.core.Preferences> =
        context.dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    /** null هرگز منتشر نمی‌شود؛ کلید نبود = false (آموزش اولیه لازم) */
    val bootState: Flow<BootState> = safeData.map { p ->
        if (p[Keys.ONBOARDED] == true) BootState.Done else BootState.Fresh
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }

    /** آبی/سبز/بنفش/نارنجی — پیش‌فرض blue */
    val accent: Flow<String> = safeData.map { it[Keys.ACCENT] ?: "blue" }

    suspend fun setAccent(key: String) {
        context.dataStore.edit { it[Keys.ACCENT] = key }
    }

    /** حالت روشن/تاریک/سیستمی */
    val themeMode: Flow<ThemeMode> = safeData.map { p -> ThemeMode.fromKey(p[Keys.THEME]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.key }
    }
}
