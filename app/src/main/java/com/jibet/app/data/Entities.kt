package com.jibet.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("sortOrder")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: AccountKind = AccountKind.CASH,
    val initialBalance: Double = 0.0,
    val colorHex: String = "#5B7FFF",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "categories", indices = [Index("kind"), Index("sortOrder")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: Kind = Kind.EXPENSE,
    val colorHex: String = "#8A93A6",
    val icon: String = "more-horizontal",
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("categoryId"), Index("accountId"), Index("date")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val kind: Kind = Kind.EXPENSE,
    val title: String,
    val note: String? = null,
    /** epoch millis */
    val date: Long,
    val accountId: Long,
    val categoryId: Long,
    val recurringId: Long? = null,
)

@Entity(tableName = "budgets", indices = [Index("categoryId"), Index(value = ["jYear", "jMonth"])])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val jYear: Int,
    val jMonth: Int,
    val amount: Double,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    /** رشته شمسی «jy/mm/dd» مثل وب — null یعنی بدون مهلت */
    val deadline: String? = null,
    val colorHex: String = "#5B7FFF",
    val icon: String = "plane",
)

@Entity(tableName = "recurring", indices = [Index("nextDate")])
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val kind: Kind = Kind.EXPENSE,
    val accountId: Long,
    val categoryId: Long,
    val frequency: Frequency = Frequency.MONTHLY,
    /** epoch millis سررسید بعدی */
    val nextDate: Long,
    val isActive: Boolean = true,
)
