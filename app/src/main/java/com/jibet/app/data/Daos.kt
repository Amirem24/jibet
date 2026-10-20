package com.jibet.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isIncome, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Insert
    suspend fun insert(c: CategoryEntity): Long

    @Update
    suspend fun update(c: CategoryEntity)

    @Delete
    suspend fun delete(c: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllOnce(): List<AccountEntity>

    @Query("SELECT COALESCE(SUM(balance), 0) FROM accounts")
    fun observeTotal(): Flow<Double>

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :id")
    suspend fun addToBalance(id: Long, delta: Double)

    @Insert
    suspend fun insert(a: AccountEntity): Long

    @Update
    suspend fun update(a: AccountEntity)

    @Delete
    suspend fun delete(a: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE isIncome = :isIncome AND timestamp BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long, isIncome: Boolean): Double

    @Query("SELECT categoryId AS categoryId, SUM(amount) AS total FROM transactions WHERE isIncome = :isIncome AND timestamp BETWEEN :from AND :to GROUP BY categoryId")
    suspend fun sumByCategory(from: Long, to: Long, isIncome: Boolean): List<CategorySum>

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Insert
    suspend fun insert(t: TransactionEntity): Long

    @Update
    suspend fun update(t: TransactionEntity)

    @Delete
    suspend fun delete(t: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextRun")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring")
    suspend fun getAllOnce(): List<RecurringEntity>

    @Query("SELECT * FROM recurring WHERE active = 1 AND nextRun <= :now")
    suspend fun dueList(now: Long): List<RecurringEntity>

    @Insert
    suspend fun insert(r: RecurringEntity): Long

    @Update
    suspend fun update(r: RecurringEntity)

    @Delete
    suspend fun delete(r: RecurringEntity)

    @Query("DELETE FROM recurring")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY done, id DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAllOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Insert
    suspend fun insert(g: GoalEntity): Long

    @Update
    suspend fun update(g: GoalEntity)

    @Delete
    suspend fun delete(g: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
