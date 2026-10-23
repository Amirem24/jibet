package com.jibet.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class JibetDatabase : RoomDatabase() {
    abstract fun dao(): JibetDao

    companion object {

        /** ۳→۴: ستون لنگر روز ماه برای جلوگیری از دریفت ماهانه/سالانه‌ی تکراری‌ها */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recurring ADD COLUMN anchorDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** ۴→۵: ترمیم داده‌های خراب‌شده با باگ آنبوردینگ (kind حساس به حروف کوچک/بزرگ بود):
         *  دسته‌های درآمدِ پریست که EXPENSE ذخیره شده‌اند و حساب‌های پریست که همه CASH شده‌اند،
         *  بر اساس نام دقیق پریست اصلاح می‌شوند. دسته/حساب سفارشی کاربر دست نمی‌خورد
         *  مگر نامش دقیقاً نام یکی از پریست‌های سیستمی باشد. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE categories SET kind = 'INCOME' WHERE kind = 'EXPENSE' AND name IN " +
                        "('حقوق', 'فروش', 'هدیه', 'سرمایه‌گذاری', 'سایر درآمد')"
                )
                db.execSQL("UPDATE accounts SET kind = 'BANK' WHERE kind = 'CASH' AND name = 'کارت بانکی'")
                db.execSQL("UPDATE accounts SET kind = 'WALLET' WHERE kind = 'CASH' AND name = 'کیف پول الکترونیکی'")
                db.execSQL("UPDATE accounts SET kind = 'SAVINGS' WHERE kind = 'CASH' AND name = 'پس‌انداز'")
            }
        }

        fun create(context: Context): JibetDatabase =
            Room.databaseBuilder(context, JibetDatabase::class.java, "jibet.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                // تنها مسیر عمداً destructive: نسخه‌ی ۱ (نخستین APK باکرش) اسکیمایی
                // کاملاً متفاوت داشت و هرگز به‌صورت سالم توزیع نشد.
                // هر ارتقای دیگر «باید» مهاجرت صریح بگیرد وگرنه Room خطا می‌دهد —
                // یعنی داده‌ی کاربر هرگز بی‌سروصدا پاک نمی‌شود.
                .fallbackToDestructiveMigrationFrom(1)
                .build()
    }
}
