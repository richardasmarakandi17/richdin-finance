package com.richdin.finance.core.domain

import com.richdin.finance.core.repository.FinanceRepository
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportCsvUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    suspend fun exportExpensesToCsv(outputDir: File): File {
        val expenses = repository.getAllExpensesForExport()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val sb = StringBuilder()
        sb.append("ID,Tanggal,Kategori,Nominal (Rp),Catatan,FotoStruk\n")

        for (exp in expenses) {
            val noteCleaned = exp.note.replace(",", " ").replace("\n", " ")
            val receipt = exp.receiptUri ?: ""
            sb.append("${exp.id},${exp.date.format(dateFormatter)},${exp.categoryName},${exp.amount},$noteCleaned,$receipt\n")
        }

        val filename = "RichdinFinance_Export_${System.currentTimeMillis()}.csv"
        val csvFile = File(outputDir, filename)
        csvFile.writeText(sb.toString())
        return csvFile
    }
}
