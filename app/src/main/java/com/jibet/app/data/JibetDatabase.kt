package com.jibet.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        RecurringEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JibetDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringDao(): RecurringDao
    abstract fun goalDao(): GoalDao
}
