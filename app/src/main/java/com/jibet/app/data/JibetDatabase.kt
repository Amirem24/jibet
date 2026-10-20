package com.jibet.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun kindToString(k: Kind): String = k.name
    @TypeConverter fun stringToKind(s: String): Kind = Kind.from(s)
    @TypeConverter fun accountKindToString(k: AccountKind): String = k.name
    @TypeConverter fun stringToAccountKind(s: String): AccountKind = AccountKind.from(s)
    @TypeConverter fun frequencyToString(f: Frequency): String = f.name
    @TypeConverter fun stringToFrequency(s: String): Frequency = Frequency.from(s)
}

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        RecurringEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class JibetDatabase : RoomDatabase() {
    abstract fun dao(): JibetDao

    companion object {
        fun create(context: Context): JibetDatabase =
            Room.databaseBuilder(context, JibetDatabase::class.java, "jibet.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
