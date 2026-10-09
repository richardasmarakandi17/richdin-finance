package com.richdin.finance.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richdin.finance.core.model.Expense
import com.richdin.finance.core.repository.FinanceRepository
import com.richdin.finance.core.ui.components.FarmCard
import com.richdin.finance.core.ui.components.formatCurrency
import com.richdin.finance.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySummary(
    val categoryName: String,
    val totalAmount: Long,
    val percentage: Int
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _categorySummaries = MutableStateFlow<List<CategorySummary>>(emptyList())
    val categorySummaries: StateFlow<List<CategorySummary>> = _categorySummaries.asStateFlow()

    private val _totalSpent = MutableStateFlow(0L)
    val totalSpent: StateFlow<Long> = _totalSpent.asStateFlow()

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            repository.getActiveSalaryPeriod().collect { period ->
                if (period != null) {
                    repository.getExpenses(period.id).collect { list ->
                        _expenses.value = list
                        val total = list.sumOf { it.amount }
                        _totalSpent.value = total

                        val group = list.groupBy { it.categoryName }
                        val summaries = group.map { (catName, items) ->
                            val catTotal = items.sumOf { it.amount }
                            val pct = if (total > 0) ((catTotal.toDouble() / total) * 100).toInt() else 0
                            CategorySummary(catName, catTotal, pct)
                        }.sortedByDescending { it.totalAmount }

                        _categorySummaries.value = summaries
                    }
                }
            }
        }
    }
}

@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val summaries by viewModel.categorySummaries.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                }
                Text(
                    text = "Laporan & Evaluasi Tren 📊",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Total Spent Summary Card
            item {
                FarmCard(backgroundColor = FarmGreenContainer) {
                    Text(
                        text = "Total Pengeluaran Periode Ini",
                        style = MaterialTheme.typography.labelLarge.copy(color = FarmGreenText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatCurrency(totalSpent),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = FarmGreenPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total transaksi dicatat: ${expenses.size} kali",
                        style = MaterialTheme.typography.bodySmall.copy(color = FarmGreenText)
                    )
                }
            }

            // 2. Category Breakdown Section
            item {
                Text(
                    text = "Distribusi Per Kategori",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (summaries.isEmpty()) {
                item {
                    FarmCard {
                        Text(
                            text = "Belum ada transaksi pengeluaran pada periode ini.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(summaries) { summary ->
                    FarmCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(summary.categoryName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "${formatCurrency(summary.totalAmount)} (${summary.percentage}%)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (summary.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = FarmGreenPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
