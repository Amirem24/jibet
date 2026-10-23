package com.jibet.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** حساب با موجودی محاسبه‌شده (موجودی اولیه + تراکنش‌ها) */
data class AccountRow(
    val id: Long,
    val name: String,
    val kind: AccountKind,
    val initialBalance: Double,
    val balance: Double,
    val colorHex: String,
    val icon: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)

/** تراکنش با دسته و حساب پیوست‌شده */
data class TxRow(
    val id: Long,
    val amount: Double,
    val kind: Kind,
    val title: String,
    val note: String?,
    val date: Long,
    val accountId: Long,
    val categoryId: Long,
    val recurringId: Long?,
    val catName: String?,
    val catColor: String?,
    val catIcon: String?,
    val accName: String?,
    val accColor: String?,
    val accIcon: String?,
)

/** تراکنش تکراری با دسته و حساب */
data class RecurringRow(
    val id: Long,
    val title: String,
    val amount: Double,
    val kind: Kind,
    val accountId: Long,
    val categoryId: Long,
    val frequency: Frequency,
    val nextDate: Long,
    val isActive: Boolean,
    val anchorDay: Int = 0,
    val catName: String?,
    val catColor: String?,
    val catIcon: String?,
    val accName: String?,
    val accColor: String?,
    val accIcon: String?,
)

/** بودجه با مصرف‌شده و دسته */
data class BudgetRow(
    val id: Long,
    val categoryId: Long,
    val jYear: Int,
    val jMonth: Int,
    val amount: Double,
    val spent: Double,
    val catName: String?,
    val catColor: String?,
    val catIcon: String?,
)

/** جفت جمع درآمد/هزینه */
data class MoneyPair(val income: Double, val expense: Double)

/** خلاصه صفحه تراکنش‌ها */
data class TxTotals(val total: Int, val income: Double, val expense: Double)

@Dao
interface JibetDao {

    /* ---------- حساب‌ها ---------- */

    @Query(
        """
        SELECT a.id, a.name, a.kind, a.initialBalance,
            a.initialBalance + COALESCE(SUM(CASE WHEN t.kind = 'INCOME' THEN t.amount ELSE -t.amount END), 0) AS balance,
            a.colorHex, a.icon, a.isDefault, a.sortOrder
        FROM accounts a LEFT JOIN transactions t ON t.accountId = a.id
        GROUP BY a.id
        ORDER BY a.sortOrder ASC, a.id ASC
        """
    )
    fun watchAccounts(): Flow<List<AccountRow>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun firstAccount(): AccountEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countAccountTx(accountId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(a: AccountEntity): Long

    @Update
    suspend fun updateAccount(a: AccountEntity)

    @Delete
    suspend fun deleteAccount(a: AccountEntity)

    @Query("SELECT COALESCE(SUM(initialBalance), 0) + COALESCE((SELECT COALESCE(SUM(CASE WHEN kind = 'INCOME' THEN amount ELSE -amount END), 0) FROM transactions), 0) FROM accounts")
    fun watchTotalBalance(): Flow<Double>

    /* ---------- دسته‌ها ---------- */

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun watchCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countCategoryTx(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring WHERE accountId = :accountId")
    suspend fun countAccountRecurring(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring WHERE categoryId = :categoryId")
    suspend fun countCategoryRecurring(categoryId: Long): Int

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetsForCategory(categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(c: CategoryEntity): Long

    @Update
    suspend fun updateCategory(c: CategoryEntity)

    @Delete
    suspend fun deleteCategory(c: CategoryEntity)

    /* ---------- تراکنش‌ها ---------- */

    private companion object {
        const val TX_SELECT = """
            SELECT t.id, t.amount, t.kind, t.title, t.note, t.date, t.accountId, t.categoryId, t.recurringId,
                c.name AS catName, c.colorHex AS catColor, c.icon AS catIcon,
                a.name AS accName, a.colorHex AS accColor, a.icon AS accIcon
            FROM transactions t
            LEFT JOIN categories c ON c.id = t.categoryId
            LEFT JOIN accounts a ON a.id = t.accountId
        """
        const val TX_FILTER = """
            (:q = '' OR t.title LIKE '%' || :q || '%' ESCAPE '\' OR t.note LIKE '%' || :q || '%' ESCAPE '\' OR c.name LIKE '%' || :q || '%' ESCAPE '\')
            AND (:qNum = '' OR CAST(CAST(t.amount AS INTEGER) AS TEXT) LIKE '%' || :qNum || '%')
            AND (:kindIsAll OR t.kind = :kind)
            AND (:accountId = 0 OR t.accountId = :accountId)
            AND (:categoryId = 0 OR t.categoryId = :categoryId)
            AND (:from = 0 OR t.date >= :from)
            AND (:to = 0 OR t.date <= :to)
        """
    }

    @Query(
        TX_SELECT + " WHERE " + TX_FILTER + " ORDER BY t.date DESC, t.id DESC LIMIT :limit OFFSET :offset"
    )
    fun watchTxPage(
        q: String,
        qNum: String,
        kindIsAll: Boolean,
        kind: Kind,
        accountId: Long,
        categoryId: Long,
        from: Long,
        to: Long,
        limit: Int,
        offset: Int,
    ): Flow<List<TxRow>>

    @Query(
        """
        SELECT COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN t.kind = 'INCOME' THEN t.amount END), 0) AS income,
            COALESCE(SUM(CASE WHEN t.kind = 'EXPENSE' THEN t.amount END), 0) AS expense
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE """ + TX_FILTER
    )
    fun watchTxTotals(
        q: String,
        qNum: String,
        kindIsAll: Boolean,
        kind: Kind,
        accountId: Long,
        categoryId: Long,
        from: Long,
        to: Long,
    ): Flow<TxTotals>

    @Query(
        TX_SELECT + " WHERE t.date >= :from AND t.date <= :to ORDER BY t.date ASC"
    )
    fun watchTxInRange(from: Long, to: Long): Flow<List<TxRow>>

    @Query(TX_SELECT + " WHERE t.id = :id")
    fun watchTx(id: Long): Flow<TxRow?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTx(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTx(t: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxs(t: List<TransactionEntity>)

    @Update
    suspend fun updateTx(t: TransactionEntity)

    @Delete
    suspend fun deleteTx(t: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTx()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTx(): Int

    @Query("SELECT * FROM transactions ORDER BY date ASC, id ASC")
    suspend fun allTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM budgets")
    suspend fun allBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM goals ORDER BY id ASC")
    suspend fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM recurring ORDER BY nextDate ASC")
    suspend fun allRecurrings(): List<RecurringEntity>

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    /* ---------- بودجه‌ها ---------- */

    @Query(
        """
        SELECT b.id, b.categoryId, b.jYear, b.jMonth, b.amount,
            COALESCE((SELECT SUM(t.amount) FROM transactions t
                WHERE t.categoryId = b.categoryId AND t.kind = 'EXPENSE' AND t.date BETWEEN :start AND :end), 0) AS spent,
            c.name AS catName, c.colorHex AS catColor, c.icon AS catIcon
        FROM budgets b LEFT JOIN categories c ON c.id = b.categoryId
        WHERE b.jYear = :jy AND b.jMonth = :jm
        ORDER BY b.id ASC
        """
    )
    fun watchBudgets(jy: Int, jm: Int, start: Long, end: Long): Flow<List<BudgetRow>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND jYear = :jy AND jMonth = :jm LIMIT 1")
    suspend fun findBudget(categoryId: Long, jy: Int, jm: Int): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(b: BudgetEntity): Long

    @Delete
    suspend fun deleteBudget(b: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    /* ---------- هدف‌ها ---------- */

    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun watchGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoal(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(g: GoalEntity): Long

    @Update
    suspend fun updateGoal(g: GoalEntity)

    @Delete
    suspend fun deleteGoal(g: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun countGoals(): Int

    /* ---------- تکراری‌ها ---------- */

    @Query(
        """
        SELECT r.id, r.title, r.amount, r.kind, r.accountId, r.categoryId, r.frequency, r.nextDate, r.isActive, r.anchorDay,
            c.name AS catName, c.colorHex AS catColor, c.icon AS catIcon,
            a.name AS accName, a.colorHex AS accColor, a.icon AS accIcon
        FROM recurring r
        LEFT JOIN categories c ON c.id = r.categoryId
        LEFT JOIN accounts a ON a.id = r.accountId
        ORDER BY r.nextDate ASC
        """
    )
    fun watchRecurrings(): Flow<List<RecurringRow>>

    @Query("SELECT * FROM recurring WHERE isActive = 1")
    suspend fun activeRecurrings(): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(r: RecurringEntity): Long

    @Update
    suspend fun updateRecurring(r: RecurringEntity)

    @Delete
    suspend fun deleteRecurring(r: RecurringEntity)

    @Query("DELETE FROM recurring")
    suspend fun deleteAllRecurring()

    @Query("SELECT COUNT(*) FROM recurring")
    suspend fun countRecurringAll(): Int

    /* ---------- خلاصه‌ها ---------- */

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN kind = 'INCOME' THEN amount END), 0) AS income,
            COALESCE(SUM(CASE WHEN kind = 'EXPENSE' THEN amount END), 0) AS expense
        FROM transactions WHERE date >= :from AND date <= :to
        """
    )
    fun watchMoneyInRange(from: Long, to: Long): Flow<MoneyPair>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC")
    suspend fun allAccounts(): List<AccountEntity>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun allCategories(): List<CategoryEntity>
}


