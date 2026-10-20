package com.jibet.app.data

import androidx.room.*
import kotlinx.serialization.Serializable

enum class RecurFreq { DAILY, WEEKLY, MONTHLY }

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val isIncome: Boolean,
    val isDefault: Boolean = false,
    val budget: Double = 0.0
)

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val balance: Double = 0.0
)

@Serializable
@Entity(
    tableName = "transactions",
    indices = [Index("categoryId"), Index("accountId"), Index("timestamp")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val isIncome: Boolean,
    val categoryId: Long,
    val accountId: Long,
    val note: String = "",
    val timestamp: Long,
    val recurringId: Long? = null
)

@Serializable
@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val categoryId: Long,
    val accountId: Long,
    val freq: String,
    val nextRun: Long,
    val active: Boolean = true
)

@Serializable
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val target: Double,
    val saved: Double = 0.0,
    val emoji: String = "🎯",
    val colorHex: String = "#2A9D8F",
    val deadline: Long? = null,
    val done: Boolean = false
)

data class CategorySum(val categoryId: Long, val total: Double)

@Serializable
data class BackupData(
    val version: Int = 1,
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList()
)
