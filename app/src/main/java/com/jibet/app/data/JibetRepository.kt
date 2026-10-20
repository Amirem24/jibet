package com.jibet.app.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class JibetRepository(private val db: JibetDatabase) {
    private val txDao = db.transactionDao()
    private val catDao = db.categoryDao()
    private val accDao = db.accountDao()
    private val recDao = db.recurringDao()
    private val goalDao = db.goalDao()

    private val zone: ZoneId = ZoneId.systemDefault()

    // ---------- Flows ----------
    val categories: Flow<List<CategoryEntity>> = catDao.observeAll()
    val accounts: Flow<List<AccountEntity>> = accDao.observeAll()
    val transactions: Flow<List<TransactionEntity>> = txDao.observeAll()
    val recurring: Flow<List<RecurringEntity>> = recDao.observeAll()
    val goals: Flow<List<GoalEntity>> = goalDao.observeAll()
    val totalBalance: Flow<Double> = accDao.observeTotal()

    // ---------- Categories ----------
    suspend fun addCategory(name: String, emoji: String, colorHex: String, isIncome: Boolean, budget: Double = 0.0): Long =
        catDao.insert(CategoryEntity(name = name, emoji = emoji, colorHex = colorHex, isIncome = isIncome, budget = budget))

    suspend fun updateCategory(c: CategoryEntity) = catDao.update(c)
    suspend fun deleteCategory(c: CategoryEntity) = catDao.delete(c)

    // ---------- Accounts ----------
    suspend fun addAccount(name: String, emoji: String, colorHex: String, balance: Double): Long =
        accDao.insert(AccountEntity(name = name, emoji = emoji, colorHex = colorHex, balance = balance))

    suspend fun updateAccount(a: AccountEntity) = accDao.update(a)
    suspend fun deleteAccount(a: AccountEntity) {
        txDao.deleteByAccount(a.id)
        accDao.delete(a)
    }

    // ---------- Transactions ----------
    suspend fun getTransaction(id: Long): TransactionEntity? = txDao.getById(id)

    suspend fun addTransaction(
        amount: Double,
        isIncome: Boolean,
        categoryId: Long,
        accountId: Long,
        note: String,
        timestamp: Long,
        recurringId: Long? = null
    ) {
        txDao.insert(
            TransactionEntity(
                amount = amount, isIncome = isIncome, categoryId = categoryId,
                accountId = accountId, note = note, timestamp = timestamp, recurringId = recurringId
            )
        )
        accDao.addToBalance(accountId, if (isIncome) amount else -amount)
    }

    suspend fun updateTransaction(tx: TransactionEntity) {
        val old = txDao.getById(tx.id) ?: return
        accDao.addToBalance(old.accountId, if (old.isIncome) -old.amount else old.amount)
        accDao.addToBalance(tx.accountId, if (tx.isIncome) tx.amount else -tx.amount)
        txDao.update(tx)
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        accDao.addToBalance(tx.accountId, if (tx.isIncome) -tx.amount else tx.amount)
        txDao.delete(tx)
    }

    suspend fun sumsBetween(from: Long, to: Long): Pair<Double, Double> {
        val income = txDao.sumBetween(from, to, true)
        val expense = txDao.sumBetween(from, to, false)
        return income to expense
    }

    suspend fun categorySums(from: Long, to: Long, isIncome: Boolean): List<CategorySum> =
        txDao.sumByCategory(from, to, isIncome)

    // ---------- Recurring ----------
    suspend fun addRecurring(
        title: String,
        amount: Double,
        isIncome: Boolean,
        categoryId: Long,
        accountId: Long,
        freq: RecurFreq,
        firstRun: Long
    ): Long = recDao.insert(
        RecurringEntity(
            title = title, amount = amount, isIncome = isIncome,
            categoryId = categoryId, accountId = accountId,
            freq = freq.name, nextRun = firstRun
        )
    )

    suspend fun updateRecurring(r: RecurringEntity) = recDao.update(r)
    suspend fun deleteRecurring(r: RecurringEntity) = recDao.delete(r)
    suspend fun toggleRecurring(r: RecurringEntity, active: Boolean) = recDao.update(r.copy(active = active))

    /** On app start: create transactions for every due recurring item, then advance nextRun. */
    suspend fun processDueRecurring(now: Long = System.currentTimeMillis()) {
        val due = recDao.dueList(now)
        for (r in due) {
            addTransaction(r.amount, r.isIncome, r.categoryId, r.accountId, r.title, r.nextRun, r.id)
            var next = r.nextRun
            val freq = try { RecurFreq.valueOf(r.freq) } catch (e: Exception) { RecurFreq.MONTHLY }
            var guard = 0
            while (next <= now && guard < 1000) {
                next = when (freq) {
                    RecurFreq.DAILY -> plusDays(next, 1)
                    RecurFreq.WEEKLY -> plusDays(next, 7)
                    RecurFreq.MONTHLY -> plusMonths(next, 1)
                }
                guard++
            }
            recDao.update(r.copy(nextRun = next))
        }
    }

    private fun plusDays(ts: Long, days: Long): Long =
        Instant.ofEpochMilli(ts).atZone(zone).plusDays(days).toInstant().toEpochMilli()

    private fun plusMonths(ts: Long, months: Long): Long =
        Instant.ofEpochMilli(ts).atZone(zone).plusMonths(months).toInstant().toEpochMilli()

    // ---------- Goals ----------
    suspend fun addGoal(title: String, target: Double, emoji: String, colorHex: String, deadline: Long?): Long =
        goalDao.insert(GoalEntity(title = title, target = target, emoji = emoji, colorHex = colorHex, deadline = deadline))

    suspend fun updateGoal(g: GoalEntity) = goalDao.update(g)
    suspend fun deleteGoal(g: GoalEntity) = goalDao.delete(g)

    suspend fun contributeGoal(id: Long, amount: Double) {
        val g = goalDao.getById(id) ?: return
        val newSaved = (g.saved + amount).coerceAtLeast(0.0)
        goalDao.update(g.copy(saved = newSaved, done = newSaved >= g.target))
    }

    // ---------- Backup / Restore ----------
    suspend fun snapshot(): BackupData = BackupData(
        categories = catDao.getAllOnce(),
        accounts = accDao.getAllOnce(),
        transactions = txDao.getAllOnce(),
        recurring = recDao.getAllOnce(),
        goals = goalDao.getAllOnce()
    )

    suspend fun clearAll() {
        txDao.deleteAll()
        recDao.deleteAll()
        goalDao.deleteAll()
        catDao.deleteAll()
        accDao.deleteAll()
    }

    suspend fun restore(b: BackupData) {
        clearAll()
        b.categories.forEach { catDao.insert(it) }
        b.accounts.forEach { accDao.insert(it) }
        b.transactions.forEach { txDao.insert(it) }
        b.recurring.forEach { recDao.insert(it) }
        b.goals.forEach { goalDao.insert(it) }
    }
}
