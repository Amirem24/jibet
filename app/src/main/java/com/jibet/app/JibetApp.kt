package com.jibet.app

import android.app.Application
import androidx.room.Room
import com.jibet.app.data.JibetDatabase
import com.jibet.app.data.JibetRepository
import com.jibet.app.data.SettingsRepository

class JibetApp : Application() {

    val database: JibetDatabase by lazy {
        Room.databaseBuilder(this, JibetDatabase::class.java, "jibet.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: JibetRepository by lazy { JibetRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
