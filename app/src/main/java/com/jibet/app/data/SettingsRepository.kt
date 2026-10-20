package com.jibet.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private val Context.dataStore by preferencesDataStore(name = "jibet_settings")

/** Distinguishes "still reading disk" from "never set up" — they are NOT the same. */
sealed interface SetupState {
    data object Loading : SetupState
    data object Fresh : SetupState
    data object Done : SetupState
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
    }

    /**
     * Emits Loading immediately, then always emits exactly one real state
     * (Fresh when the key is absent, Done when setup finished before).
     * DataStore always emits at least emptyPreferences(), so Fresh is guaranteed.
     */
    val setupState: Flow<SetupState> = context.dataStore.data
        .map { if (it[Keys.SETUP_DONE] == true) SetupState.Done else SetupState.Fresh }
        .onStart { emit(SetupState.Loading) }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val currency: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY] ?: "تومان" }

    suspend fun setSetupDone(v: Boolean) { context.dataStore.edit { it[Keys.SETUP_DONE] = v } }
    suspend fun setTheme(v: String) { context.dataStore.edit { it[Keys.THEME] = v } }
    suspend fun setCurrency(v: String) { context.dataStore.edit { it[Keys.CURRENCY] = v } }
}
