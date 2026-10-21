package com.jibet.app

import android.app.Application
import android.util.Log
import com.jibet.app.data.JibetDatabase
import com.jibet.app.data.JibetRepository
import com.jibet.app.data.Kind
import com.jibet.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** وضعیت شیت ثبت تراکنش (مشابه zustand نسخه وب) */
object UiBus {
    val addOpen = MutableStateFlow(false)
    val addKind = MutableStateFlow(Kind.EXPENSE)
    val detailTxId = MutableStateFlow<Long?>(null)

    fun openAdd(kind: Kind? = null) {
        // دکمه‌ی مرکزی «+» همیشه «هزینه» را باز می‌کند؛ فراخوانی‌های صریح (اکشن درآمد) نوع خودشان را می‌دهند
        addKind.value = kind ?: Kind.EXPENSE
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

    /** خطاهای پس‌زمینه هرگز پروسه را نمی‌کُشند — فقط لاگ می‌شوند */
    private val scopeErrorHandler = CoroutineExceptionHandler { _, t ->
        Log.e("Jibet", "خطای پس‌زمینه", t)
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + scopeErrorHandler)

    val database: JibetDatabase by lazy { JibetDatabase.create(this) }
    val repository: JibetRepository by lazy {
        JibetRepository(database, appScope).also { repo ->
            // پردازش سررسیدهای تکراری: در شروع + هر ۱۵ دقیقه تا وقتی اپ باز است
            // (قبلاً فقط یک‌بار در شروع اجرا می‌شد و سررسید وسط روز ثبت نمی‌شد)
            appScope.launch {
                while (true) {
                    runCatching { repo.processDueRecurring() }
                        .onFailure { Log.e("Jibet", "خطای پردازش تکراری‌ها", it) }
                    delay(RECURRING_CHECK_INTERVAL_MS)
                }
            }
        }
    }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    private companion object {
        const val RECURRING_CHECK_INTERVAL_MS = 15L * 60 * 1000
    }
}
