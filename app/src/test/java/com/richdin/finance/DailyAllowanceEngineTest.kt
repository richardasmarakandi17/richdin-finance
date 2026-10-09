package com.richdin.finance

import com.richdin.finance.core.domain.DailyAllowanceEngine
import com.richdin.finance.core.model.CowMood
import com.richdin.finance.core.model.EarlyWarningStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DailyAllowanceEngineTest {

    private lateinit var engine: DailyAllowanceEngine

    @Before
    fun setup() {
        engine = DailyAllowanceEngine()
    }

    @Test
    fun testNormalDailyAllowanceCalculation() {
        val today = LocalDate.of(2026, 9, 2)
        val nextSalaryDate = LocalDate.of(2026, 10, 2) // 30 days
        val remainingDailyBalance = 3_000_000L
        val totalAllocated = 3_000_000L

        val result = engine.calculateDailyAllowance(
            today = today,
            nextSalaryDate = nextSalaryDate,
            remainingDailyBalance = remainingDailyBalance,
            spentToday = 0L,
            periodStartDate = today,
            totalAllocatedDaily = totalAllocated
        )

        assertEquals(30L, result.remainingDays)
        assertEquals(100_000L, result.todayAllowance)
        assertEquals(EarlyWarningStatus.SAFE, result.status)
        assertEquals(CowMood.HAPPY, result.cowMood)
    }

    @Test
    fun testEarlyWarningWhenBurnRateIsHigh() {
        val periodStart = LocalDate.of(2026, 9, 2)
        val today = LocalDate.of(2026, 9, 12) // 10 days passed, 20 days remaining
        val nextSalaryDate = LocalDate.of(2026, 10, 2)
        val totalAllocated = 3_000_000L
        // User spent 2.5 million in 10 days! Only 500k left for 20 days.
        val remainingDailyBalance = 500_000L

        val result = engine.calculateDailyAllowance(
            today = today,
            nextSalaryDate = nextSalaryDate,
            remainingDailyBalance = remainingDailyBalance,
            spentToday = 50_000L,
            periodStartDate = periodStart,
            totalAllocatedDaily = totalAllocated
        )

        assertEquals(20L, result.remainingDays)
        assertEquals(25_000L, result.todayAllowance)
        assertEquals(EarlyWarningStatus.DANGER, result.status)
        assertEquals(CowMood.SAD, result.cowMood)
        assertTrue(result.adviceMessage.contains("Waduh") || result.adviceMessage.contains("Kurangi"))
    }

    @Test
    fun testDepletedBalanceWithDaysRemaining() {
        val periodStart = LocalDate.of(2026, 9, 2)
        val today = LocalDate.of(2026, 9, 20)
        val nextSalaryDate = LocalDate.of(2026, 10, 2)
        val totalAllocated = 3_000_000L
        val remainingDailyBalance = 0L

        val result = engine.calculateDailyAllowance(
            today = today,
            nextSalaryDate = nextSalaryDate,
            remainingDailyBalance = remainingDailyBalance,
            spentToday = 0L,
            periodStartDate = periodStart,
            totalAllocatedDaily = totalAllocated
        )

        assertEquals(EarlyWarningStatus.DANGER, result.status)
        assertEquals(CowMood.SAD, result.cowMood)
        assertEquals(0L, result.todayAllowance)
    }
}
