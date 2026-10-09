package com.richdin.finance

import com.richdin.finance.core.domain.LoanManagerUseCase
import com.richdin.finance.core.domain.LoanValidationResult
import com.richdin.finance.core.model.*
import com.richdin.finance.core.repository.FinanceRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakeFinanceRepository : FinanceRepository {
    private var loanLimitConfig = LoanLimitConfig(LimitType.PERCENTAGE, 30.0)
    private var totalLoaned = 0L
    private val pocketsMap = mutableMapOf<Long, Pocket>()

    fun setPockets(pockets: List<Pocket>) {
        pockets.forEach { pocketsMap[it.id] = it }
    }

    override fun getActiveSalaryPeriod() = flowOf(null)
    override suspend fun getActiveSalaryPeriodOnce() = null
    override fun getAllSalaryPeriods() = flowOf(emptyList<SalaryPeriod>())
    override suspend fun createSalaryPeriod(period: SalaryPeriod) = 1L
    override suspend fun updateSalaryPeriod(period: SalaryPeriod) {}
    override fun getIncomeSources(periodId: Long) = flowOf(emptyList<IncomeSource>())
    override suspend fun getIncomeSourcesOnce(periodId: Long) = emptyList<IncomeSource>()
    override suspend fun addIncomeSource(income: IncomeSource) = 1L
    override suspend fun deleteIncomeSource(income: IncomeSource) {}
    override fun getPockets(periodId: Long) = flowOf(pocketsMap.values.toList())
    override suspend fun getPocketsOnce(periodId: Long) = pocketsMap.values.toList()
    override fun getPocketByType(periodId: Long, type: PocketType) = flowOf(pocketsMap.values.find { it.type == type })
    override suspend fun getPocketByTypeOnce(periodId: Long, type: PocketType) = pocketsMap.values.find { it.type == type }
    override suspend fun getPocketById(pocketId: Long) = pocketsMap[pocketId]
    override suspend fun savePockets(pockets: List<Pocket>) { setPockets(pockets) }
    override suspend fun updatePocket(pocket: Pocket) { pocketsMap[pocket.id] = pocket }
    override fun getExpenses(periodId: Long) = flowOf(emptyList<Expense>())
    override suspend fun getExpensesOnce(periodId: Long) = emptyList<Expense>()
    override fun getDailySpentSum(date: LocalDate, periodId: Long) = flowOf(0L)
    override suspend fun addExpense(expense: Expense) = 1L
    override suspend fun deleteExpense(expense: Expense) {}
    override suspend fun getAllExpensesForExport() = emptyList<Expense>()
    override fun getLoans(periodId: Long) = flowOf(emptyList<Loan>())
    override fun getActiveUnpaidLoans() = flowOf(emptyList<Loan>())
    override suspend fun getLoansForPeriodOnce(periodId: Long) = emptyList<Loan>()
    override suspend fun getTotalLoanedInPeriod(periodId: Long) = totalLoaned
    override suspend fun addLoan(loan: Loan): Long {
        totalLoaned += loan.amount
        return 1L
    }
    override suspend fun updateLoan(loan: Loan) {}
    override fun getLoanLimitConfig() = flowOf(loanLimitConfig)
    override suspend fun getLoanLimitConfigOnce() = loanLimitConfig
    override suspend fun saveLoanLimitConfig(config: LoanLimitConfig) { loanLimitConfig = config }
    override fun getCategories() = flowOf(emptyList<Category>())
    override suspend fun getCategoriesOnce() = emptyList<Category>()
    override suspend fun addCategory(category: Category) = 1L
    override fun getGoals() = flowOf(emptyList<Goal>())
    override suspend fun saveGoal(goal: Goal) = 1L
    override suspend fun updateGoal(goal: Goal) {}
    override suspend fun deleteGoal(goal: Goal) {}
    override fun getBackupMeta() = flowOf(BackupMeta())
    override suspend fun saveBackupMeta(meta: BackupMeta) {}
}

class LoanManagerUseCaseTest {

    private lateinit var fakeRepo: FakeFinanceRepository
    private lateinit var loanManager: LoanManagerUseCase

    @Before
    fun setup() {
        fakeRepo = FakeFinanceRepository()
        loanManager = LoanManagerUseCase(fakeRepo)
    }

    @Test
    fun testLoanAllowedWithinCap() = runBlocking {
        val emergencyPocket = Pocket(
            id = 2L,
            salaryPeriodId = 1L,
            name = "Dana Darurat",
            type = PocketType.EMERGENCY,
            allocatedAmount = 1_000_000L
        )
        fakeRepo.setPockets(listOf(emergencyPocket))

        // 30% cap of 1,000,000 is 300,000
        val validation = loanManager.validateLoan(
            periodId = 1L,
            sourcePocket = emergencyPocket,
            requestedAmount = 200_000L
        )

        assertTrue(validation is LoanValidationResult.Allowed)
    }

    @Test
    fun testLoanRejectedWhenExceedingCap() = runBlocking {
        val emergencyPocket = Pocket(
            id = 2L,
            salaryPeriodId = 1L,
            name = "Dana Darurat",
            type = PocketType.EMERGENCY,
            allocatedAmount = 1_000_000L
        )
        fakeRepo.setPockets(listOf(emergencyPocket))

        // Requested 500,000 when cap is 300,000 (30%)
        val validation = loanManager.validateLoan(
            periodId = 1L,
            sourcePocket = emergencyPocket,
            requestedAmount = 500_000L
        )

        assertTrue(validation is LoanValidationResult.Rejected)
    }
}
