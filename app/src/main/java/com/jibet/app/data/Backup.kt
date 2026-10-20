package com.jibet.app.data

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/* فرمت فایل پشتیبان — دقیقاً مطابق /api/backup نسخه وب (سازگار دوسویه) */

@Serializable
data class BackupAccount(
    val name: String,
    val kind: String,
    val initialBalance: Double,
    val color: String,
    val icon: String,
    val isDefault: Boolean = false,
    val order: Int = 0,
    val _uid: String,
)

@Serializable
data class BackupCategory(
    val name: String,
    val kind: String,
    val color: String,
    val icon: String,
    val isDefault: Boolean = false,
    val order: Int = 0,
    val _uid: String,
)

@Serializable
data class BackupTx(
    val amount: Double,
    val kind: String,
    val title: String,
    val note: String? = null,
    val date: String,
    val accountUid: String,
    val categoryUid: String,
    val createdAt: String? = null,
)

@Serializable
data class BackupBudget(
    val categoryUid: String,
    val amount: Double,
    val jYear: Int,
    val jMonth: Int,
)

@Serializable
data class BackupGoal(
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val deadline: String? = null,
    val color: String,
    val icon: String,
)

@Serializable
data class BackupRecurring(
    val title: String,
    val amount: Double,
    val kind: String,
    val accountUid: String,
    val categoryUid: String,
    val frequency: String,
    val nextDate: String,
    val isActive: Boolean = true,
)

@Serializable
data class BackupPayload(
    val app: String = "jibet",
    val version: Int = 1,
    val exportedAt: String? = null,
    val settings: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val accounts: List<BackupAccount> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val transactions: List<BackupTx> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val goals: List<BackupGoal> = emptyList(),
    val recurrings: List<BackupRecurring> = emptyList(),
)

private val backupJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/** ساخت فایل پشتیبان JSON — سازگار با نسخه وب */
suspend fun JibetRepository.exportBackupJson(): String {
    val accounts = dao.allAccounts()
    val categories = dao.allCategories()
    val txs = dao.allTransactions()
    val budgets = dao.allBudgets()
    val goals = dao.allGoals()
    val recurrings = dao.allRecurrings()

    val payload = BackupPayload(
        app = "jibet",
        version = 1,
        exportedAt = Instant.now().toString(),
        accounts = accounts.map {
            BackupAccount(
                name = it.name, kind = it.kind.name.lowercase(), initialBalance = it.initialBalance,
                color = it.colorHex, icon = it.icon, isDefault = it.isDefault,
                order = it.sortOrder, _uid = it.id.toString(),
            )
        },
        categories = categories.map {
            BackupCategory(
                name = it.name, kind = it.kind.name.lowercase(), color = it.colorHex,
                icon = it.icon, isDefault = it.isDefault, order = it.sortOrder, _uid = it.id.toString(),
            )
        },
        transactions = txs.map {
            BackupTx(
                amount = it.amount, kind = it.kind.name.lowercase(), title = it.title, note = it.note,
                date = Instant.ofEpochMilli(it.date).toString(),
                accountUid = it.accountId.toString(), categoryUid = it.categoryId.toString(),
            )
        },
        budgets = budgets.map {
            BackupBudget(
                categoryUid = it.categoryId.toString(), amount = it.amount,
                jYear = it.jYear, jMonth = it.jMonth,
            )
        },
        goals = goals.map {
            BackupGoal(
                title = it.title, targetAmount = it.targetAmount, savedAmount = it.savedAmount,
                deadline = it.deadline, color = it.colorHex, icon = it.icon,
            )
        },
        recurrings = recurrings.map {
            BackupRecurring(
                title = it.title, amount = it.amount, kind = it.kind.name.lowercase(),
                accountUid = it.accountId.toString(), categoryUid = it.categoryId.toString(),
                frequency = it.frequency.name.lowercase(), nextDate = Instant.ofEpochMilli(it.nextDate).toString(),
                isActive = it.isActive,
            )
        },
    )
    return backupJson.encodeToString(payload)
}

/** بازیابی کامل از فایل پشتیبان — جایگزینی همه‌ی داده‌ها */
suspend fun JibetRepository.restoreBackup(json: String) {
    val payload = try {
        backupJson.decodeFromString<BackupPayload>(json)
    } catch (_: SerializationException) {
        throw IllegalArgumentException("فایل پشتیبان معتبر نیست")
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("فایل پشتیبان معتبر نیست")
    }
    if (payload.app != "jibet") throw IllegalArgumentException("فایل پشتیبان معتبر نیست")

    db.withTransactionCompat {
        dao.deleteAllTx()
        dao.deleteAllBudgets()
        dao.deleteAllRecurring()
        dao.deleteAllGoals()
        dao.deleteAllCategories()
        dao.deleteAllAccounts()

        val accId = HashMap<String, Long>()
        val catId = HashMap<String, Long>()

        for (a in payload.accounts) {
            val id = dao.insertAccount(
                AccountEntity(
                    name = a.name,
                    kind = AccountKind.from(a.kind.uppercase()),
                    initialBalance = a.initialBalance,
                    colorHex = a.color,
                    icon = a.icon,
                    isDefault = a.isDefault,
                    sortOrder = a.order,
                )
            )
            accId[a._uid] = id
        }
        for (c in payload.categories) {
            val id = dao.insertCategory(
                CategoryEntity(
                    name = c.name,
                    kind = Kind.from(c.kind.uppercase()),
                    colorHex = c.color,
                    icon = c.icon,
                    isDefault = c.isDefault,
                    sortOrder = c.order,
                )
            )
            catId[c._uid] = id
        }
        for (t in payload.transactions) {
            val accountId = accId[t.accountUid] ?: continue
            val categoryId = catId[t.categoryUid] ?: continue
            val date = runCatching { Instant.parse(t.date).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
            dao.insertTx(
                TransactionEntity(
                    amount = t.amount, kind = Kind.from(t.kind.uppercase()), title = t.title,
                    note = t.note, date = date, accountId = accountId, categoryId = categoryId,
                )
            )
        }
        for (b in payload.budgets) {
            val categoryId = catId[b.categoryUid] ?: continue
            dao.insertBudget(
                BudgetEntity(categoryId = categoryId, amount = b.amount, jYear = b.jYear, jMonth = b.jMonth)
            )
        }
        for (g in payload.goals) {
            dao.insertGoal(
                GoalEntity(
                    title = g.title, targetAmount = g.targetAmount, savedAmount = g.savedAmount,
                    deadline = g.deadline, colorHex = g.color, icon = g.icon,
                )
            )
        }
        for (r in payload.recurrings) {
            val accountId = accId[r.accountUid] ?: continue
            val categoryId = catId[r.categoryUid] ?: continue
            val next = runCatching { Instant.parse(r.nextDate).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
            dao.insertRecurring(
                RecurringEntity(
                    title = r.title, amount = r.amount, kind = Kind.from(r.kind.uppercase()),
                    accountId = accountId, categoryId = categoryId,
                    frequency = Frequency.from(r.frequency.uppercase()),
                    nextDate = next, isActive = r.isActive,
                )
            )
        }
    }
}

/** خروجی CSV با BOM — دقیقاً مثل /api/export وب */
suspend fun JibetRepository.exportCsv(): String {
    val accounts = dao.allAccounts().associateBy { it.id }
    val categories = dao.allCategories().associateBy { it.id }
    val txs = dao.allTransactions()

    fun cell(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"${v.replace("\"", "\"\"")}\"" else v

    val header = listOf("تاریخ", "نوع", "عنوان", "دسته‌بندی", "حساب", "مبلغ (تومان)", "یادداشت")
    val rows = txs.map { t ->
        listOf(
            com.jibet.app.util.JDate.formatJalaliDate(t.date, com.jibet.app.util.JDate.Style.NUMERIC),
            if (t.kind == Kind.INCOME) "درآمد" else "هزینه",
            t.title,
            categories[t.categoryId]?.name ?: "",
            accounts[t.accountId]?.name ?: "",
            t.amount.toLong().toString(),
            t.note ?: "",
        ).joinToString(",") { cell(it) }
    }
    return "\uFEFF" + (listOf(header.joinToString(",")) + rows).joinToString("\r\n")
}

private suspend fun JibetDatabase.withTransactionCompat(block: suspend () -> Unit) {
    withTransaction { block() }
}
