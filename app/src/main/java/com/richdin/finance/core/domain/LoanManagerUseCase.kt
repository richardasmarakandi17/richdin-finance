package com.richdin.finance.core.domain

import com.richdin.finance.core.model.*
import com.richdin.finance.core.repository.FinanceRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoanValidationResult {
    data class Allowed(val remainingQuotaAfter: Long) : LoanValidationResult()
    data class Rejected(val reason: String, val maxAllowed: Long, val currentQuotaRemaining: Long) : LoanValidationResult()
}

@Singleton
class LoanManagerUseCase @Inject constructor(
    private val repository: FinanceRepository
) {

    /**
     * Calculates the borrowing quota limit for the current active salary period.
     */
    suspend fun getMonthlyBorrowingQuota(periodId: Long, sourcePocket: Pocket): Long {
        val config = repository.getLoanLimitConfigOnce()
        return when (config.limitType) {
            LimitType.FIXED_AMOUNT -> config.limitValue.toLong()
            LimitType.PERCENTAGE -> ((sourcePocket.allocatedAmount * (config.limitValue / 100.0)).toLong())
        }.coerceAtLeast(0L)
    }

    /**
     * Calculates the remaining available quota for borrowing this month.
     */
    suspend fun getRemainingBorrowingQuota(periodId: Long, sourcePocket: Pocket): Long {
        val totalCap = getMonthlyBorrowingQuota(periodId, sourcePocket)
        val alreadyBorrowed = repository.getTotalLoanedInPeriod(periodId)
        return (totalCap - alreadyBorrowed).coerceAtLeast(0L)
    }

    /**
     * Validates whether a loan can be processed or should be strictly rejected.
     */
    suspend fun validateLoan(
        periodId: Long,
        sourcePocket: Pocket,
        requestedAmount: Long
    ): LoanValidationResult {
        if (requestedAmount <= 0) {
            return LoanValidationResult.Rejected("Nominal pinjaman harus lebih dari 0", 0, 0)
        }

        if (requestedAmount > sourcePocket.availableBalance) {
            return LoanValidationResult.Rejected(
                reason = "Saldo kantong ${sourcePocket.name} tidak mencukupi (Tersedia: Rp${formatRupiah(sourcePocket.availableBalance)}).",
                maxAllowed = sourcePocket.availableBalance,
                currentQuotaRemaining = sourcePocket.availableBalance
            )
        }

        val remainingQuota = getRemainingBorrowingQuota(periodId, sourcePocket)
        if (requestedAmount > remainingQuota) {
            return LoanValidationResult.Rejected(
                reason = "Pinjaman melebihi batas kuota bulanan! Sisa kuota pinjam bulan ini adalah Rp${formatRupiah(remainingQuota)}.",
                maxAllowed = remainingQuota,
                currentQuotaRemaining = remainingQuota
            )
        }

        return LoanValidationResult.Allowed(remainingQuotaAfter = remainingQuota - requestedAmount)
    }

    /**
     * Executes the emergency loan after validation.
     */
    suspend fun executeLoan(
        periodId: Long,
        sourcePocketId: Long,
        targetPocketId: Long,
        amount: Long,
        reason: String
    ): Result<Long> {
        val sourcePocket = repository.getPocketById(sourcePocketId)
            ?: return Result.failure(IllegalArgumentException("Kantong asal tidak ditemukan"))

        val validation = validateLoan(periodId, sourcePocket, amount)
        if (validation is LoanValidationResult.Rejected) {
            return Result.failure(IllegalStateException(validation.reason))
        }

        val loan = Loan(
            salaryPeriodId = periodId,
            sourcePocketId = sourcePocketId,
            targetPocketId = targetPocketId,
            amount = amount,
            loanDate = LocalDate.now(),
            reason = reason,
            status = LoanStatus.UNPAID
        )

        val loanId = repository.addLoan(loan)
        return Result.success(loanId)
    }

    /**
     * Repays an unpaid loan (restores borrowed balance).
     */
    suspend fun repayLoan(loan: Loan): Result<Unit> {
        if (loan.status == LoanStatus.PAID) {
            return Result.success(Unit)
        }

        // Restore source pocket borrowed amount
        val sourcePocket = repository.getPocketById(loan.sourcePocketId)
        if (sourcePocket != null) {
            repository.updatePocket(
                sourcePocket.copy(borrowedAmount = (sourcePocket.borrowedAmount - loan.amount).coerceAtLeast(0))
            )
        }

        // Update loan entity status
        repository.updateLoan(
            loan.copy(
                status = LoanStatus.PAID,
                repaidDate = LocalDate.now()
            )
        )

        return Result.success(Unit)
    }

    private fun formatRupiah(amount: Long): String {
        return "%,d".format(amount).replace(',', '.')
    }
}
