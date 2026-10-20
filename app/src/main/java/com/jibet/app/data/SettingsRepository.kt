package com.jibet.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jibet_settings")

/** وضعیت بوت برنامه —Splash فقط تا نخستین تابش DataStore نمایش داده می‌شود */
sealed class BootState {
    data object Loading : BootState()
    data object Fresh : BootState()
    data object Done : BootState()
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val ACCENT = stringPreferencesKey("accent")
    }

    /** null هرگز منتشر نمی‌شود؛ کلید نبود = false (آموزش اولیه لازم) */
    val bootState: Flow<BootState> = context.dataStore.data.map { p ->
        if (p[Keys.ONBOARDED] == true) BootState.Done else BootState.Fresh
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }

    /** آبی/سبز/بنفش/نارنجی — پیش‌فرض blue */
    val accent: Flow<String> = context.dataStore.data.map { it[Keys.ACCENT] ?: "blue" }

    suspend fun setAccent(key: String) {
        context.dataStore.edit { it[Keys.ACCENT] = key }
    }
}
