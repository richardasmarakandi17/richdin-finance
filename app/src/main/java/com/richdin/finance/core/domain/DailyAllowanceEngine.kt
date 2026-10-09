package com.richdin.finance.core.domain

import com.richdin.finance.core.model.CowMood
import com.richdin.finance.core.model.DailyAllowanceResult
import com.richdin.finance.core.model.EarlyWarningStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class DailyAllowanceEngine @Inject constructor() {

    /**
     * Calculates the dynamic daily allowance and early warning status.
     *
     * @param today Current date
     * @param nextSalaryDate Date of upcoming salary
     * @param remainingDailyBalance Remaining available balance in the daily pocket
     * @param spentToday Total expenses already recorded for today
     * @param periodStartDate Start date of the current salary period
     * @param totalAllocatedDaily Total initial budget allocated to daily pocket
     */
    fun calculateDailyAllowance(
        today: LocalDate,
        nextSalaryDate: LocalDate,
        remainingDailyBalance: Long,
        spentToday: Long,
        periodStartDate: LocalDate,
        totalAllocatedDaily: Long
    ): DailyAllowanceResult {
        // Remaining days includes today up to the day before nextSalaryDate
        val daysUntilSalary = ChronoUnit.DAYS.between(today, nextSalaryDate).coerceAtLeast(1)
        val totalPeriodDays = ChronoUnit.DAYS.between(periodStartDate, nextSalaryDate).coerceAtLeast(1)
        val daysPassed = ChronoUnit.DAYS.between(periodStartDate, today).coerceAtLeast(0)

        // Today's dynamic allowance = (Remaining daily balance) / (Remaining days)
        val todayAllowance = if (daysUntilSalary > 0) {
            (remainingDailyBalance.toDouble() / daysUntilSalary).roundToLong().coerceAtLeast(0L)
        } else {
            0L
        }

        // Calculate average daily burn rate
        val totalSpentSoFar = (totalAllocatedDaily - remainingDailyBalance).coerceAtLeast(0L)
        val effectiveDaysPassed = if (daysPassed == 0L) 1.0 else daysPassed.toDouble()
        val averageDailyBurn = totalSpentSoFar.toDouble() / effectiveDaysPassed

        // Expected normal daily baseline = total allocated / total period days
        val baselineDaily = totalAllocatedDaily.toDouble() / totalPeriodDays

        // Projected runway in days with current balance
        val projectedRunwayDays = if (averageDailyBurn > 0) {
            remainingDailyBalance.toDouble() / averageDailyBurn
        } else {
            daysUntilSalary.toDouble()
        }

        // Deficit days = How many days will user run out of money before salary date
        val deficitDays = daysUntilSalary - projectedRunwayDays

        // Early warning thresholds:
        // SAFE: Deficit <= 0 or projected runway >= days until salary
        // WARNING: Deficit between 1 and 5 days (runs out 3-5 days before salary)
        // DANGER: Deficit > 5 days or balance is 0 while days remain
        val status: EarlyWarningStatus
        val cowMood: CowMood
        val adviceMessage: String

        if (remainingDailyBalance <= 0 && daysUntilSalary > 0) {
            status = EarlyWarningStatus.DANGER
            cowMood = CowMood.SAD
            adviceMessage = "Mooo! Saldo jatah harianmu sudah habis padahal masih ada $daysUntilSalary hari lagi. Gunakan fitur Pinjam Darurat jika mendesak."
        } else if (deficitDays > 5.0 || (todayAllowance < baselineDaily * 0.5 && daysUntilSalary > 5)) {
            status = EarlyWarningStatus.DANGER
            cowMood = CowMood.SAD
            val reductionNeeded = if (daysUntilSalary > 0) {
                ((averageDailyBurn - todayAllowance) * 0.8).roundToLong().coerceAtLeast(5000L)
            } else 0L
            adviceMessage = "Waduh! Proyeksi saldo akan habis lebih dari 5 hari sebelum gajian. Kurangi pengeluaran ~Rp${formatRupiah(reductionNeeded)}/hari agar tetap aman!"
        } else if (deficitDays in 1.0..5.0 || (todayAllowance < baselineDaily * 0.8)) {
            status = EarlyWarningStatus.WARNING
            cowMood = CowMood.NEUTRAL
            val reductionNeeded = if (daysUntilSalary > 0) {
                ((averageDailyBurn - todayAllowance) * 0.5).roundToLong().coerceAtLeast(2000L)
            } else 0L
            adviceMessage = "Perhatian! Kecepatan belanja agak tinggi. Hemat Rp${formatRupiah(reductionNeeded)}/hari untuk sisa $daysUntilSalary hari ke depan."
        } else {
            status = EarlyWarningStatus.SAFE
            cowMood = CowMood.HAPPY
            adviceMessage = "Hebat! Pengeluaranmu sangat terkendali. Jatah belanja aman hari ini Rp${formatRupiah(todayAllowance)}."
        }

        return DailyAllowanceResult(
            todayAllowance = todayAllowance,
            remainingDays = daysUntilSalary,
            remainingDailyBalance = remainingDailyBalance,
            spentToday = spentToday,
            status = status,
            cowMood = cowMood,
            adviceMessage = adviceMessage,
            daysUntilSalary = daysUntilSalary,
            projectedRunwayDays = projectedRunwayDays
        )
    }

    private fun formatRupiah(amount: Long): String {
        return "%,d".format(amount).replace(',', '.')
    }
}
