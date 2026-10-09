package com.richdin.finance.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SalaryPeriodEntity::class,
        IncomeSourceEntity::class,
        PocketEntity::class,
        ExpenseEntity::class,
        LoanEntity::class,
        LoanLimitConfigEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        BackupMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RichdinDatabase : RoomDatabase() {
    abstract fun salaryPeriodDao(): SalaryPeriodDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun pocketDao(): PocketDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun loanDao(): LoanDao
    abstract fun loanLimitConfigDao(): LoanLimitConfigDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun backupMetaDao(): BackupMetaDao

    companion object {
        private const val DATABASE_NAME = "richdin_finance.db"

        fun buildDatabase(context: Context): RichdinDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RichdinDatabase::class.java,
                DATABASE_NAME
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed initial categories & default config in background
                    CoroutineScope(Dispatchers.IO).launch {
                        val instance = buildDatabase(context)
                        seedInitialData(instance)
                    }
                }
            }).build()
        }

        private suspend fun seedInitialData(database: RichdinDatabase) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Makan & Minum", iconName = "restaurant", colorHex = "#FF7043"),
                CategoryEntity(name = "Bensin & Bahan Bakar", iconName = "local_gas_station", colorHex = "#FFA726"),
                CategoryEntity(name = "Parkir & Tol", iconName = "local_parking", colorHex = "#7E57C2"),
                CategoryEntity(name = "Transportasi Umum", iconName = "directions_bus", colorHex = "#42A5F5"),
                CategoryEntity(name = "Belanja Harian", iconName = "shopping_cart", colorHex = "#66BB6A"),
                CategoryEntity(name = "Tagihan & Listrik", iconName = "receipt_long", colorHex = "#26A69A"),
                CategoryEntity(name = "Kesehatan & Obat", iconName = "medical_services", colorHex = "#EF5350"),
                CategoryEntity(name = "Hiburan & Rekreasi", iconName = "sports_esports", colorHex = "#AB47BC"),
                CategoryEntity(name = "Lain-lain", iconName = "category", colorHex = "#78909C")
            )
            database.categoryDao().insertAll(defaultCategories)
            database.loanLimitConfigDao().insertOrUpdate(
                LoanLimitConfigEntity(id = 1, limitValue = 30.0)
            )
        }
    }
}
