package com.richdin.finance.core.repository

import com.richdin.finance.core.database.*
import com.richdin.finance.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface FinanceRepository {
    fun getActiveSalaryPeriod(): Flow<SalaryPeriod?>
    suspend fun getActiveSalaryPeriodOnce(): SalaryPeriod?
    fun getAllSalaryPeriods(): Flow<List<SalaryPeriod>>
    suspend fun createSalaryPeriod(period: SalaryPeriod): Long
    suspend fun updateSalaryPeriod(period: SalaryPeriod)

    fun getIncomeSources(periodId: Long): Flow<List<IncomeSource>>
    suspend fun getIncomeSourcesOnce(periodId: Long): List<IncomeSource>
    suspend fun addIncomeSource(income: IncomeSource): Long
    suspend fun deleteIncomeSource(income: IncomeSource)

    fun getPockets(periodId: Long): Flow<List<Pocket>>
    suspend fun getPocketsOnce(periodId: Long): List<Pocket>
    fun getPocketByType(periodId: Long, type: PocketType): Flow<Pocket?>
    suspend fun getPocketByTypeOnce(periodId: Long, type: PocketType): Pocket?
    suspend fun getPocketById(pocketId: Long): Pocket?
    suspend fun savePockets(pockets: List<Pocket>)
    suspend fun updatePocket(pocket: Pocket)

    fun getExpenses(periodId: Long): Flow<List<Expense>>
    suspend fun getExpensesOnce(periodId: Long): List<Expense>
    fun getDailySpentSum(date: LocalDate, periodId: Long): Flow<Long>
    suspend fun addExpense(expense: Expense): Long
    suspend fun deleteExpense(expense: Expense)
    suspend fun getAllExpensesForExport(): List<Expense>

    fun getLoans(periodId: Long): Flow<List<Loan>>
    fun getActiveUnpaidLoans(): Flow<List<Loan>>
    suspend fun getLoansForPeriodOnce(periodId: Long): List<Loan>
    suspend fun getTotalLoanedInPeriod(periodId: Long): Long
    suspend fun addLoan(loan: Loan): Long
    suspend fun updateLoan(loan: Loan)

    fun getLoanLimitConfig(): Flow<LoanLimitConfig>
    suspend fun getLoanLimitConfigOnce(): LoanLimitConfig
    suspend fun saveLoanLimitConfig(config: LoanLimitConfig)

    fun getCategories(): Flow<List<Category>>
    suspend fun getCategoriesOnce(): List<Category>
    suspend fun addCategory(category: Category): Long

    fun getGoals(): Flow<List<Goal>>
    suspend fun saveGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)

    fun getBackupMeta(): Flow<BackupMeta>
    suspend fun saveBackupMeta(meta: BackupMeta)
}

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val db: RichdinDatabase
) : FinanceRepository {

    override fun getActiveSalaryPeriod(): Flow<SalaryPeriod?> {
        return db.salaryPeriodDao().getActivePeriod().map { it?.toDomain() }
    }

    override suspend fun getActiveSalaryPeriodOnce(): SalaryPeriod? {
        return db.salaryPeriodDao().getActivePeriodOnce()?.toDomain()
    }

    override fun getAllSalaryPeriods(): Flow<List<SalaryPeriod>> {
        return db.salaryPeriodDao().getAllPeriods().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createSalaryPeriod(period: SalaryPeriod): Long {
        val id = db.salaryPeriodDao().insert(period.toEntity())
        db.salaryPeriodDao().deactivateOtherPeriods(id)
        return id
    }

    override suspend fun updateSalaryPeriod(period: SalaryPeriod) {
        db.salaryPeriodDao().update(period.toEntity())
    }

    override fun getIncomeSources(periodId: Long): Flow<List<IncomeSource>> {
        return db.incomeSourceDao().getIncomeSources(periodId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIncomeSourcesOnce(periodId: Long): List<IncomeSource> {
        return db.incomeSourceDao().getIncomeSourcesOnce(periodId).map { it.toDomain() }
    }

    override suspend fun addIncomeSource(income: IncomeSource): Long {
        return db.incomeSourceDao().insert(income.toEntity())
    }

    override suspend fun deleteIncomeSource(income: IncomeSource) {
        db.incomeSourceDao().delete(income.toEntity())
    }

    override fun getPockets(periodId: Long): Flow<List<Pocket>> {
        return db.pocketDao().getPockets(periodId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPocketsOnce(periodId: Long): List<Pocket> {
        return db.pocketDao().getPocketsOnce(periodId).map { it.toDomain() }
    }

    override fun getPocketByType(periodId: Long, type: PocketType): Flow<Pocket?> {
        return db.pocketDao().getPocketByType(periodId, type).map { it?.toDomain() }
    }

    override suspend fun getPocketByTypeOnce(periodId: Long, type: PocketType): Pocket? {
        return db.pocketDao().getPocketByTypeOnce(periodId, type)?.toDomain()
    }

    override suspend fun getPocketById(pocketId: Long): Pocket? {
        return db.pocketDao().getPocketById(pocketId)?.toDomain()
    }

    override suspend fun savePockets(pockets: List<Pocket>) {
        db.pocketDao().insertAll(pockets.map { it.toEntity() })
    }

    override suspend fun updatePocket(pocket: Pocket) {
        db.pocketDao().update(pocket.toEntity())
    }

    override fun getExpenses(periodId: Long): Flow<List<Expense>> {
        return db.expenseDao().getExpenses(periodId).map { list ->
            val categories = db.categoryDao().getCategoriesOnce().associateBy { it.id }
            list.map { entity ->
                val cat = categories[entity.categoryId]
                entity.toDomain(cat?.name ?: "Lain-lain", cat?.iconName ?: "cart")
            }
        }
    }

    override suspend fun getExpensesOnce(periodId: Long): List<Expense> {
        val categories = db.categoryDao().getCategoriesOnce().associateBy { it.id }
        return db.expenseDao().getExpensesOnce(periodId).map { entity ->
            val cat = categories[entity.categoryId]
            entity.toDomain(cat?.name ?: "Lain-lain", cat?.iconName ?: "cart")
        }
    }

    override fun getDailySpentSum(date: LocalDate, periodId: Long): Flow<Long> {
        return db.expenseDao().getDailySpentSum(date, periodId).map { it ?: 0L }
    }

    override suspend fun addExpense(expense: Expense): Long {
        val id = db.expenseDao().insert(expense.toEntity())
        // Also update pocket spentAmount
        val pocket = db.pocketDao().getPocketById(expense.pocketId)
        if (pocket != null) {
            db.pocketDao().update(pocket.copy(spentAmount = pocket.spentAmount + expense.amount))
        }
        return id
    }

    override suspend fun deleteExpense(expense: Expense) {
        db.expenseDao().delete(expense.toEntity())
        val pocket = db.pocketDao().getPocketById(expense.pocketId)
        if (pocket != null) {
            db.pocketDao().update(pocket.copy(spentAmount = (pocket.spentAmount - expense.amount).coerceAtLeast(0)))
        }
    }

    override suspend fun getAllExpensesForExport(): List<Expense> {
        val categories = db.categoryDao().getCategoriesOnce().associateBy { it.id }
        return db.expenseDao().getAllExpensesForExport().map { entity ->
            val cat = categories[entity.categoryId]
            entity.toDomain(cat?.name ?: "Lain-lain", cat?.iconName ?: "cart")
        }
    }

    override fun getLoans(periodId: Long): Flow<List<Loan>> {
        return db.loanDao().getLoans(periodId).map { list ->
            list.map { loanEntity ->
                val sourcePocket = db.pocketDao().getPocketById(loanEntity.sourcePocketId)
                loanEntity.toDomain(sourcePocket?.name ?: "Kantong Asal")
            }
        }
    }

    override fun getActiveUnpaidLoans(): Flow<List<Loan>> {
        return db.loanDao().getActiveUnpaidLoans().map { list ->
            list.map { loanEntity ->
                val sourcePocket = db.pocketDao().getPocketById(loanEntity.sourcePocketId)
                loanEntity.toDomain(sourcePocket?.name ?: "Kantong Asal")
            }
        }
    }

    override suspend fun getLoansForPeriodOnce(periodId: Long): List<Loan> {
        return db.loanDao().getLoansForPeriodOnce(periodId).map { it.toDomain("") }
    }

    override suspend fun getTotalLoanedInPeriod(periodId: Long): Long {
        return db.loanDao().getTotalLoanedInPeriod(periodId) ?: 0L
    }

    override suspend fun addLoan(loan: Loan): Long {
        val id = db.loanDao().insert(loan.toEntity())
        // Adjust source pocket borrowedAmount and target pocket allocatedAmount
        val sourcePocket = db.pocketDao().getPocketById(loan.sourcePocketId)
        if (sourcePocket != null) {
            db.pocketDao().update(sourcePocket.copy(borrowedAmount = sourcePocket.borrowedAmount + loan.amount))
        }
        val targetPocket = db.pocketDao().getPocketById(loan.targetPocketId)
        if (targetPocket != null) {
            db.pocketDao().update(targetPocket.copy(allocatedAmount = targetPocket.allocatedAmount + loan.amount))
        }
        return id
    }

    override suspend fun updateLoan(loan: Loan) {
        db.loanDao().update(loan.toEntity())
    }

    override fun getLoanLimitConfig(): Flow<LoanLimitConfig> {
        return db.loanLimitConfigDao().getConfig().map { it?.toDomain() ?: LoanLimitConfig() }
    }

    override suspend fun getLoanLimitConfigOnce(): LoanLimitConfig {
        return db.loanLimitConfigDao().getConfigOnce()?.toDomain() ?: LoanLimitConfig()
    }

    override suspend fun saveLoanLimitConfig(config: LoanLimitConfig) {
        db.loanLimitConfigDao().insertOrUpdate(config.toEntity())
    }

    override fun getCategories(): Flow<List<Category>> {
        return db.categoryDao().getCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoriesOnce(): List<Category> {
        return db.categoryDao().getCategoriesOnce().map { it.toDomain() }
    }

    override suspend fun addCategory(category: Category): Long {
        return db.categoryDao().insert(category.toEntity())
    }

    override fun getGoals(): Flow<List<Goal>> {
        return db.goalDao().getGoals().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveGoal(goal: Goal): Long {
        return db.goalDao().insert(goal.toEntity())
    }

    override suspend fun updateGoal(goal: Goal) {
        db.goalDao().update(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: Goal) {
        db.goalDao().delete(goal.toEntity())
    }

    override fun getBackupMeta(): Flow<BackupMeta> {
        return db.backupMetaDao().getBackupMeta().map {
            it?.let { meta -> BackupMeta(meta.lastBackupTimestamp, meta.driveFileId, meta.isPendingSync) }
                ?: BackupMeta()
        }
    }

    override suspend fun saveBackupMeta(meta: BackupMeta) {
        db.backupMetaDao().saveBackupMeta(
            BackupMetaEntity(1, meta.lastBackupTimestamp, meta.driveFileId, meta.isPendingSync)
        )
    }

    // Mapping extensions
    private fun SalaryPeriodEntity.toDomain() = SalaryPeriod(id, startDate, nextSalaryDate, totalIncome, isActive)
    private fun SalaryPeriod.toEntity() = SalaryPeriodEntity(id, startDate, nextSalaryDate, totalIncome, isActive)

    private fun IncomeSourceEntity.toDomain() = IncomeSource(id, salaryPeriodId, type, amount, receivedDate, note)
    private fun IncomeSource.toEntity() = IncomeSourceEntity(id, salaryPeriodId, type, amount, receivedDate, note)

    private fun PocketEntity.toDomain() = Pocket(id, salaryPeriodId, name, type, allocatedAmount, spentAmount, borrowedAmount, targetAmount)
    private fun Pocket.toEntity() = PocketEntity(id, salaryPeriodId, name, type, allocatedAmount, spentAmount, borrowedAmount, targetAmount)

    private fun ExpenseEntity.toDomain(catName: String, catIcon: String) =
        Expense(id, salaryPeriodId, pocketId, categoryId, catName, catIcon, amount, date, note, receiptUri)
    private fun Expense.toEntity() = ExpenseEntity(id, salaryPeriodId, pocketId, categoryId, amount, date, note, receiptUri)

    private fun LoanEntity.toDomain(sourceName: String) =
        Loan(id, salaryPeriodId, sourcePocketId, sourceName, targetPocketId, amount, loanDate, reason, status, repaidDate)
    private fun Loan.toEntity() = LoanEntity(id, salaryPeriodId, sourcePocketId, targetPocketId, amount, loanDate, reason, status, repaidDate)

    private fun LoanLimitConfigEntity.toDomain() = LoanLimitConfig(limitType, limitValue, effectiveFromPeriodId)
    private fun LoanLimitConfig.toEntity() = LoanLimitConfigEntity(1, limitType, limitValue, effectiveFromPeriodId)

    private fun CategoryEntity.toDomain() = Category(id, name, iconName, isCustom, colorHex)
    private fun Category.toEntity() = CategoryEntity(id, name, iconName, isCustom, colorHex)

    private fun GoalEntity.toDomain() = Goal(id, name, targetAmount, currentAmount, targetDate, pocketType)
    private fun Goal.toEntity() = GoalEntity(id, name, targetAmount, currentAmount, targetDate, pocketType)
}
