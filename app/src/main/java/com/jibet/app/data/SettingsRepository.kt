package com.jibet.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jibet_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
    }

    /** null until loaded from disk; false => show setup wizard */
    val setupDone: Flow<Boolean?> = context.dataStore.data.map { it[Keys.SETUP_DONE] }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val currency: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY] ?: "تومان" }

    suspend fun setSetupDone(v: Boolean) { context.dataStore.edit { it[Keys.SETUP_DONE] = v } }
    suspend fun setTheme(v: String) { context.dataStore.edit { it[Keys.THEME] = v } }
    suspend fun setCurrency(v: String) { context.dataStore.edit { it[Keys.CURRENCY] = v } }
}
