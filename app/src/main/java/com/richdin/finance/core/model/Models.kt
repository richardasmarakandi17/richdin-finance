package com.richdin.finance.core.model

import java.time.LocalDate

enum class PocketType {
    FIXED,       // Kebutuhan Tetap / Wajib
    EMERGENCY,   // Dana Darurat
    SAVINGS,     // Tabungan / Goals
    DAILY        // Jatah Harian
}

enum class IncomeType {
    MAIN_SALARY, // Gaji Utama
    COMMISSION,  // Komisi
    GIFT,        // Hadiah
    BONUS,       // Bonus
    OTHER        // Lainnya
}

enum class LoanStatus {
    UNPAID,      // Belum Lunas
    PAID         // Lunas
}

enum class LimitType {
    PERCENTAGE,   // Persentase dari saldo kantong asal (misal 20-30%)
    FIXED_AMOUNT // Nominal tetap (misal Rp 1.000.000)
}

enum class EarlyWarningStatus {
    SAFE,    // 🟢 Hijau (Aman): belanja on track
    WARNING, // 🟡 Kuning (Waspada): proyeksi saldo habis H-3 s/d H-5 gajian
    DANGER   // 🔴 Merah (Bahaya): proyeksi saldo habis > 5 hari sebelum gajian
}

enum class CowMood {
    HAPPY,   // Senyum ceria (Aman)
    NEUTRAL, // Ekspresi datar/waspada (Waspada)
    SAD      // Menangis (Bahaya)
}

data class DailyAllowanceResult(
    val todayAllowance: Long,
    val remainingDays: Long,
    val remainingDailyBalance: Long,
    val spentToday: Long,
    val status: EarlyWarningStatus,
    val cowMood: CowMood,
    val adviceMessage: String,
    val daysUntilSalary: Long,
    val projectedRunwayDays: Double
)

data class SalaryPeriod(
    val id: Long = 0,
    val startDate: LocalDate,
    val nextSalaryDate: LocalDate,
    val totalIncome: Long = 0,
    val isActive: Boolean = true
)

data class IncomeSource(
    val id: Long = 0,
    val salaryPeriodId: Long,
    val type: IncomeType,
    val amount: Long,
    val receivedDate: LocalDate,
    val note: String = ""
)

data class Pocket(
    val id: Long = 0,
    val salaryPeriodId: Long,
    val name: String,
    val type: PocketType,
    val allocatedAmount: Long,
    val spentAmount: Long = 0,
    val borrowedAmount: Long = 0, // Pinjaman keluar dari kantong ini
    val targetAmount: Long = 0
) {
    val availableBalance: Long
        get() = (allocatedAmount - spentAmount - borrowedAmount).coerceAtLeast(0)
}

data class Expense(
    val id: Long = 0,
    val salaryPeriodId: Long,
    val pocketId: Long,
    val categoryId: Long,
    val categoryName: String = "",
    val categoryIcon: String = "cart",
    val amount: Long,
    val date: LocalDate,
    val note: String = "",
    val receiptUri: String? = null
)

data class Loan(
    val id: Long = 0,
    val salaryPeriodId: Long,
    val sourcePocketId: Long,
    val sourcePocketName: String = "",
    val targetPocketId: Long,
    val amount: Long,
    val loanDate: LocalDate,
    val reason: String = "",
    val status: LoanStatus = LoanStatus.UNPAID,
    val repaidDate: LocalDate? = null
)

data class LoanLimitConfig(
    val limitType: LimitType = LimitType.PERCENTAGE,
    val limitValue: Double = 30.0, // Default 30% dari dana darurat
    val effectiveFromPeriodId: Long = 0
)

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val isCustom: Boolean = false,
    val colorHex: String = "#4CAF50"
)

data class Goal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0,
    val targetDate: LocalDate? = null,
    val pocketType: PocketType = PocketType.SAVINGS
)

data class BackupMeta(
    val lastBackupTimestamp: Long = 0,
    val driveFileId: String? = null,
    val isPendingSync: Boolean = false
)
