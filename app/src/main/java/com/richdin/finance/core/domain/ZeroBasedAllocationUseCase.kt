package com.richdin.finance.core.domain

import com.richdin.finance.core.model.Pocket
import com.richdin.finance.core.model.PocketType
import javax.inject.Inject
import javax.inject.Singleton

data class AllocationPlan(
    val fixedAmount: Long,
    val emergencyAmount: Long,
    val savingsAmount: Long,
    val dailyAmount: Long,
    val totalIncome: Long
) {
    val isBalanced: Boolean
        get() = (fixedAmount + emergencyAmount + savingsAmount + dailyAmount) == totalIncome

    val unallocated: Long
        get() = totalIncome - (fixedAmount + emergencyAmount + savingsAmount + dailyAmount)
}

@Singleton
class ZeroBasedAllocationUseCase @Inject constructor() {

    /**
     * Generates a recommended default allocation template:
     * - Fixed needs: ~50%
     * - Emergency fund: 10%
     * - Savings: 10%
     * - Daily allowance: Remaining 30%
     */
    fun createDefaultAllocationPlan(totalIncome: Long): AllocationPlan {
        if (totalIncome <= 0) {
            return AllocationPlan(0, 0, 0, 0, 0)
        }
        val fixed = (totalIncome * 0.50).toLong()
        val emergency = (totalIncome * 0.10).toLong()
        val savings = (totalIncome * 0.10).toLong()
        val daily = totalIncome - fixed - emergency - savings

        return AllocationPlan(
            fixedAmount = fixed,
            emergencyAmount = emergency,
            savingsAmount = savings,
            dailyAmount = daily,
            totalIncome = totalIncome
        )
    }

    /**
     * Converts an AllocationPlan into the 4 standard Pocket domain objects.
     */
    fun createPocketsFromPlan(periodId: Long, plan: AllocationPlan): List<Pocket> {
        return listOf(
            Pocket(
                salaryPeriodId = periodId,
                name = "Kebutuhan Tetap",
                type = PocketType.FIXED,
                allocatedAmount = plan.fixedAmount
            ),
            Pocket(
                salaryPeriodId = periodId,
                name = "Dana Darurat",
                type = PocketType.EMERGENCY,
                allocatedAmount = plan.emergencyAmount
            ),
            Pocket(
                salaryPeriodId = periodId,
                name = "Tabungan & Impian",
                type = PocketType.SAVINGS,
                allocatedAmount = plan.savingsAmount
            ),
            Pocket(
                salaryPeriodId = periodId,
                name = "Jatah Harian",
                type = PocketType.DAILY,
                allocatedAmount = plan.dailyAmount
            )
        )
    }
}
