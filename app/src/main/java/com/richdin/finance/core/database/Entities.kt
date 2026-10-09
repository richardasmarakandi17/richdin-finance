package com.richdin.finance.core.database

import androidx.room.*
import com.richdin.finance.core.model.*
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromPocketType(value: PocketType): String = value.name

    @TypeConverter
    fun toPocketType(value: String): PocketType = PocketType.valueOf(value)

    @TypeConverter
    fun fromIncomeType(value: IncomeType): String = value.name

    @TypeConverter
    fun toIncomeType(value: String): IncomeType = IncomeType.valueOf(value)

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus = LoanStatus.valueOf(value)

    @TypeConverter
    fun fromLimitType(value: LimitType): String = value.name

    @TypeConverter
    fun toLimitType(value: String): LimitType = LimitType.valueOf(value)
}

@Entity(tableName = "salary_periods")
data class SalaryPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val nextSalaryDate: LocalDate,
    val totalIncome: Long = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "income_sources",
    foreignKeys = [
        ForeignKey(
            entity = SalaryPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["salaryPeriodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("salaryPeriodId")]
)
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryPeriodId: Long,
    val type: IncomeType,
    val amount: Long,
    val receivedDate: LocalDate,
    val note: String = ""
)

@Entity(
    tableName = "pockets",
    foreignKeys = [
        ForeignKey(
            entity = SalaryPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["salaryPeriodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("salaryPeriodId")]
)
data class PocketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryPeriodId: Long,
    val name: String,
    val type: PocketType,
    val allocatedAmount: Long,
    val spentAmount: Long = 0,
    val borrowedAmount: Long = 0,
    val targetAmount: Long = 0
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = SalaryPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["salaryPeriodId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PocketEntity::class,
            parentColumns = ["id"],
            childColumns = ["pocketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("salaryPeriodId"), Index("pocketId"), Index("categoryId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryPeriodId: Long,
    val pocketId: Long,
    val categoryId: Long,
    val amount: Long,
    val date: LocalDate,
    val note: String = "",
    val receiptUri: String? = null
)

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = SalaryPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["salaryPeriodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("salaryPeriodId")]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryPeriodId: Long,
    val sourcePocketId: Long,
    val targetPocketId: Long,
    val amount: Long,
    val loanDate: LocalDate,
    val reason: String = "",
    val status: LoanStatus = LoanStatus.UNPAID,
    val repaidDate: LocalDate? = null
)

@Entity(tableName = "loan_limit_configs")
data class LoanLimitConfigEntity(
    @PrimaryKey val id: Int = 1,
    val limitType: LimitType = LimitType.PERCENTAGE,
    val limitValue: Double = 30.0,
    val effectiveFromPeriodId: Long = 0
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val isCustom: Boolean = false,
    val colorHex: String = "#4CAF50"
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0,
    val targetDate: LocalDate? = null,
    val pocketType: PocketType = PocketType.SAVINGS
)

@Entity(tableName = "backup_meta")
data class BackupMetaEntity(
    @PrimaryKey val id: Int = 1,
    val lastBackupTimestamp: Long = 0,
    val driveFileId: String? = null,
    val isPendingSync: Boolean = false
)
