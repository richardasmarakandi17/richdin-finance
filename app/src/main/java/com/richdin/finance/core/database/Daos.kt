package com.richdin.finance.core.database

import androidx.room.*
import com.richdin.finance.core.model.LoanStatus
import com.richdin.finance.core.model.PocketType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SalaryPeriodDao {
    @Query("SELECT * FROM salary_periods WHERE isActive = 1 LIMIT 1")
    fun getActivePeriod(): Flow<SalaryPeriodEntity?>

    @Query("SELECT * FROM salary_periods WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePeriodOnce(): SalaryPeriodEntity?

    @Query("SELECT * FROM salary_periods ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<SalaryPeriodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(period: SalaryPeriodEntity): Long

    @Update
    suspend fun update(period: SalaryPeriodEntity)

    @Query("UPDATE salary_periods SET isActive = 0 WHERE id != :activeId")
    suspend fun deactivateOtherPeriods(activeId: Long)
}

@Dao
interface IncomeSourceDao {
    @Query("SELECT * FROM income_sources WHERE salaryPeriodId = :periodId ORDER BY receivedDate DESC")
    fun getIncomeSources(periodId: Long): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE salaryPeriodId = :periodId")
    suspend fun getIncomeSourcesOnce(periodId: Long): List<IncomeSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(income: IncomeSourceEntity): Long

    @Delete
    suspend fun delete(income: IncomeSourceEntity)
}

@Dao
interface PocketDao {
    @Query("SELECT * FROM pockets WHERE salaryPeriodId = :periodId")
    fun getPockets(periodId: Long): Flow<List<PocketEntity>>

    @Query("SELECT * FROM pockets WHERE salaryPeriodId = :periodId")
    suspend fun getPocketsOnce(periodId: Long): List<PocketEntity>

    @Query("SELECT * FROM pockets WHERE salaryPeriodId = :periodId AND type = :type LIMIT 1")
    fun getPocketByType(periodId: Long, type: PocketType): Flow<PocketEntity?>

    @Query("SELECT * FROM pockets WHERE salaryPeriodId = :periodId AND type = :type LIMIT 1")
    suspend fun getPocketByTypeOnce(periodId: Long, type: PocketType): PocketEntity?

    @Query("SELECT * FROM pockets WHERE id = :pocketId")
    suspend fun getPocketById(pocketId: Long): PocketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pockets: List<PocketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pocket: PocketEntity): Long

    @Update
    suspend fun update(pocket: PocketEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE salaryPeriodId = :periodId ORDER BY date DESC, id DESC")
    fun getExpenses(periodId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE salaryPeriodId = :periodId ORDER BY date DESC, id DESC")
    suspend fun getExpensesOnce(periodId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE date = :date")
    fun getExpensesByDate(date: LocalDate): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date = :date AND salaryPeriodId = :periodId")
    fun getDailySpentSum(date: LocalDate, periodId: Long): Flow<Long?>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesForExport(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE salaryPeriodId = :periodId ORDER BY loanDate DESC")
    fun getLoans(periodId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = 'UNPAID' ORDER BY loanDate DESC")
    fun getActiveUnpaidLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE salaryPeriodId = :periodId")
    suspend fun getLoansForPeriodOnce(periodId: Long): List<LoanEntity>

    @Query("SELECT SUM(amount) FROM loans WHERE salaryPeriodId = :periodId")
    suspend fun getTotalLoanedInPeriod(periodId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)
}

@Dao
interface LoanLimitConfigDao {
    @Query("SELECT * FROM loan_limit_configs WHERE id = 1")
    fun getConfig(): Flow<LoanLimitConfigEntity?>

    @Query("SELECT * FROM loan_limit_configs WHERE id = 1")
    suspend fun getConfigOnce(): LoanLimitConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: LoanLimitConfigEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun getGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)
}

@Dao
interface BackupMetaDao {
    @Query("SELECT * FROM backup_meta WHERE id = 1")
    fun getBackupMeta(): Flow<BackupMetaEntity?>

    @Query("SELECT * FROM backup_meta WHERE id = 1")
    suspend fun getBackupMetaOnce(): BackupMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBackupMeta(meta: BackupMetaEntity)
}
