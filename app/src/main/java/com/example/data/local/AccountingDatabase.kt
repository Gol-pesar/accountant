package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        PersonEntity::class,
        BankAccountEntity::class,
        TransactionEntity::class,
        PendingSmsEntity::class,
        SavingsGoalEntity::class,
        CustomBankEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AccountingDatabase : RoomDatabase() {
    abstract fun accountingDao(): AccountingDao

    companion object {
        @Volatile
        private var INSTANCE: AccountingDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_banks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `code` TEXT NOT NULL,
                        `persianName` TEXT NOT NULL,
                        `shortName` TEXT NOT NULL,
                        `primaryColorHex` INTEGER NOT NULL,
                        `cardPrefix` TEXT NOT NULL,
                        `smsKeyword` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_banks_code` ON `custom_banks` (`code`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `bank_accounts` ADD COLUMN `shebaNumber` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            // Expense categories
            CategoryEntity(name = "خوراک و رستوران", isIncome = false, iconKey = "restaurant", colorHex = 0xFFEA580C, monthlyBudgetToman = 0L),
            CategoryEntity(name = "حمل‌ونقل", isIncome = false, iconKey = "directions_car", colorHex = 0xFF0284C7, monthlyBudgetToman = 0L),
            CategoryEntity(name = "خرید و پوشاک", isIncome = false, iconKey = "shopping_bag", colorHex = 0xFF9333EA, monthlyBudgetToman = 0L),
            CategoryEntity(name = "قبوض و اینترنت", isIncome = false, iconKey = "bolt", colorHex = 0xFFD97706, monthlyBudgetToman = 0L),
            CategoryEntity(name = "مسکن و اجاره", isIncome = false, iconKey = "home", colorHex = 0xFF0D9488, monthlyBudgetToman = 0L),
            CategoryEntity(name = "درمان و سلامت", isIncome = false, iconKey = "medical", colorHex = 0xFFDC2626, monthlyBudgetToman = 0L),
            CategoryEntity(name = "تفریح و سفر", isIncome = false, iconKey = "flight", colorHex = 0xFFEC4899, monthlyBudgetToman = 0L),
            CategoryEntity(name = "آموزش و کتاب", isIncome = false, iconKey = "school", colorHex = 0xFF4F46E5, monthlyBudgetToman = 0L),
            // Income categories
            CategoryEntity(name = "حقوق و دستمزد", isIncome = true, iconKey = "work", colorHex = 0xFF059669, monthlyBudgetToman = 0L),
            CategoryEntity(name = "کسب‌وکار و پروژه", isIncome = true, iconKey = "business", colorHex = 0xFF0B4F4A, monthlyBudgetToman = 0L),
            CategoryEntity(name = "سرمایه‌گذاری", isIncome = true, iconKey = "trending_up", colorHex = 0xFF16A34A, monthlyBudgetToman = 0L),
            CategoryEntity(name = "درآمد متفرقه", isIncome = true, iconKey = "card_giftcard", colorHex = 0xFF0891B2, monthlyBudgetToman = 0L)
        )

        fun getInstance(context: Context): AccountingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AccountingDatabase {
            lateinit var db: AccountingDatabase
            db = Room.databaseBuilder(
                appContext,
                AccountingDatabase::class.java,
                "hesabdar_man.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(database: SupportSQLiteDatabase) {
                        super.onCreate(database)
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            val dao = db.accountingDao()
                            if (dao.getCategoriesOnce().isEmpty()) {
                                dao.insertCategories(DEFAULT_CATEGORIES)
                            }
                        }
                    }
                })
                .build()
            return db
        }
    }
}
