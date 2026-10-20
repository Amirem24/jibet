package com.jibet.app

import android.app.Application
import com.jibet.app.data.JibetDatabase
import com.jibet.app.data.JibetRepository
import com.jibet.app.data.Kind
import com.jibet.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** وضعیت شیت ثبت تراکنش (مشابه zustand نسخه وب) */
object UiBus {
    val addOpen = MutableStateFlow(false)
    val addKind = MutableStateFlow(Kind.EXPENSE)
    val detailTxId = MutableStateFlow<Long?>(null)

    fun openAdd(kind: Kind? = null) {
        if (kind != null) addKind.value = kind
        addOpen.value = true
    }

    fun closeAdd() {
        addOpen.value = false
    }

    fun openDetail(id: Long) {
        detailTxId.value = id
    }

    fun closeDetail() {
        detailTxId.value = null
    }
}

class JibetApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: JibetDatabase by lazy { JibetDatabase.create(this) }
    val repository: JibetRepository by lazy {
        JibetRepository(database, appScope).also { repo ->
            // پردازش سررسیدهای تکراری در شروع برنامه
            appScope.launch { repo.processDueRecurring() }
        }
    }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
