package com.richdin.finance

import com.richdin.finance.core.domain.ZeroBasedAllocationUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZeroBasedAllocationUseCaseTest {

    private lateinit var useCase: ZeroBasedAllocationUseCase

    @Before
    fun setup() {
        useCase = ZeroBasedAllocationUseCase()
    }

    @Test
    fun testDefaultAllocationPlanTemplate() {
        val totalIncome = 10_000_000L
        val plan = useCase.createDefaultAllocationPlan(totalIncome)

        assertEquals(5_000_000L, plan.fixedAmount) // 50%
        assertEquals(1_000_000L, plan.emergencyAmount) // 10%
        assertEquals(1_000_000L, plan.savingsAmount) // 10%
        assertEquals(3_000_000L, plan.dailyAmount) // 30%
        assertTrue(plan.isBalanced)
        assertEquals(0L, plan.unallocated)
    }

    @Test
    fun testCreatePocketsFromPlan() {
        val plan = useCase.createDefaultAllocationPlan(5_000_000L)
        val pockets = useCase.createPocketsFromPlan(periodId = 1L, plan = plan)

        assertEquals(4, pockets.size)
        assertEquals(2_500_000L, pockets[0].allocatedAmount)
        assertEquals(500_000L, pockets[1].allocatedAmount)
        assertEquals(500_000L, pockets[2].allocatedAmount)
        assertEquals(1_500_000L, pockets[3].allocatedAmount)
    }
}
