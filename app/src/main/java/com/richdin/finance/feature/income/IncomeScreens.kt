package com.richdin.finance.feature.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richdin.finance.core.domain.AllocationPlan
import com.richdin.finance.core.domain.ZeroBasedAllocationUseCase
import com.richdin.finance.core.model.*
import com.richdin.finance.core.repository.FinanceRepository
import com.richdin.finance.core.ui.components.FarmCard
import com.richdin.finance.core.ui.components.formatCurrency
import com.richdin.finance.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val allocationUseCase: ZeroBasedAllocationUseCase
) : ViewModel() {

    private val _totalSalary = MutableStateFlow("7000000")
    val totalSalary: StateFlow<String> = _totalSalary.asStateFlow()

    private val _incomeType = MutableStateFlow(IncomeType.MAIN_SALARY)
    val incomeType: StateFlow<IncomeType> = _incomeType.asStateFlow()

    private val _fixedAmount = MutableStateFlow(3500000L)
    val fixedAmount: StateFlow<Long> = _fixedAmount.asStateFlow()

    private val _emergencyAmount = MutableStateFlow(700000L)
    val emergencyAmount: StateFlow<Long> = _emergencyAmount.asStateFlow()

    private val _savingsAmount = MutableStateFlow(700000L)
    val savingsAmount: StateFlow<Long> = _savingsAmount.asStateFlow()

    private val _dailyAmount = MutableStateFlow(2100000L)
    val dailyAmount: StateFlow<Long> = _dailyAmount.asStateFlow()

    fun onSalaryChange(amountStr: String) {
        _totalSalary.value = amountStr
        val total = amountStr.toLongOrNull() ?: 0L
        applyDefaultTemplate(total)
    }

    fun applyDefaultTemplate(total: Long) {
        val plan = allocationUseCase.createDefaultAllocationPlan(total)
        _fixedAmount.value = plan.fixedAmount
        _emergencyAmount.value = plan.emergencyAmount
        _savingsAmount.value = plan.savingsAmount
        _dailyAmount.value = plan.dailyAmount
    }

    fun updatePocketAllocations(fixed: Long, emergency: Long, savings: Long, daily: Long) {
        _fixedAmount.value = fixed
        _emergencyAmount.value = emergency
        _savingsAmount.value = savings
        _dailyAmount.value = daily
    }

    fun saveIncomeAndAllocation(onSuccess: () -> Unit) {
        val total = _totalSalary.value.toLongOrNull() ?: 0L
        if (total <= 0) return

        val plan = AllocationPlan(
            fixedAmount = _fixedAmount.value,
            emergencyAmount = _emergencyAmount.value,
            savingsAmount = _savingsAmount.value,
            dailyAmount = _dailyAmount.value,
            totalIncome = total
        )

        if (!plan.isBalanced) return

        viewModelScope.launch {
            val today = LocalDate.now()
            // Next salary default: Next month tanggal 2
            val nextSalaryDate = if (today.dayOfMonth < 2) {
                today.withDayOfMonth(2)
            } else {
                today.plusMonths(1).withDayOfMonth(2)
            }

            val period = SalaryPeriod(
                startDate = today,
                nextSalaryDate = nextSalaryDate,
                totalIncome = total,
                isActive = true
            )

            val periodId = repository.createSalaryPeriod(period)

            // Save Income Source
            repository.addIncomeSource(
                IncomeSource(
                    salaryPeriodId = periodId,
                    type = _incomeType.value,
                    amount = total,
                    receivedDate = today,
                    note = "Gaji periode ${today.month.name}"
                )
            )

            // Save Pockets
            val pockets = allocationUseCase.createPocketsFromPlan(periodId, plan)
            repository.savePockets(pockets)

            onSuccess()
        }
    }
}

@Composable
fun IncomeAllocationScreen(
    onNavigateBack: () -> Unit,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val totalSalaryStr by viewModel.totalSalary.collectAsState()
    val fixed by viewModel.fixedAmount.collectAsState()
    val emergency by viewModel.emergencyAmount.collectAsState()
    val savings by viewModel.savingsAmount.collectAsState()
    val daily by viewModel.dailyAmount.collectAsState()

    val total = totalSalaryStr.toLongOrNull() ?: 0L
    val allocatedSum = fixed + emergency + savings + daily
    val unallocated = total - allocatedSum

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
                    text = "Catat Gaji & Alokasi Zero-Based",
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
            // 1. Income Input Card
            item {
                FarmCard {
                    Text(
                        text = "Nominal Gaji / Pemasukan",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = totalSalaryStr,
                        onValueChange = viewModel::onSalaryChange,
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Periode gajian default: Tanggal 2",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        TextButton(onClick = { viewModel.applyDefaultTemplate(total) }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset 50/10/10/30")
                        }
                    }
                }
            }

            // 2. Zero-Based Balance Status
            item {
                FarmCard(
                    backgroundColor = if (unallocated == 0L) FarmGreenContainer else FarmBarnRedContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (unallocated == 0L) "Alokasi Seimbang (Zero-Based) ✨" else "Belum Seimbang",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (unallocated == 0L) FarmGreenText else FarmBarnRedText
                                )
                            )
                            Text(
                                text = if (unallocated == 0L) "100% pendapatan teralokasi ke 4 kantong" else "Sisa belum dialokasikan: ${formatCurrency(unallocated)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (unallocated == 0L) FarmGreenText else FarmBarnRedText
                                )
                            )
                        }
                    }
                }
            }

            // 3. Four Pockets Sliders / Allocators
            item {
                Text(
                    text = "Rincian 4 Kantong",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                PocketAllocationItem(
                    title = "🏠 Kebutuhan Tetap (Kos, Listrik, Internet)",
                    amount = fixed,
                    total = total,
                    onAmountChange = { newAmount ->
                        viewModel.updatePocketAllocations(newAmount, emergency, savings, daily)
                    }
                )
            }

            item {
                PocketAllocationItem(
                    title = "🛡️ Dana Darurat (Terkunci)",
                    amount = emergency,
                    total = total,
                    onAmountChange = { newAmount ->
                        viewModel.updatePocketAllocations(fixed, newAmount, savings, daily)
                    }
                )
            }

            item {
                PocketAllocationItem(
                    title = "🎯 Tabungan / Goals (Terkunci)",
                    amount = savings,
                    total = total,
                    onAmountChange = { newAmount ->
                        viewModel.updatePocketAllocations(fixed, emergency, newAmount, daily)
                    }
                )
            }

            item {
                PocketAllocationItem(
                    title = "🌾 Jatah Harian (Untuk Makan & Transport)",
                    amount = daily,
                    total = total,
                    onAmountChange = { newAmount ->
                        viewModel.updatePocketAllocations(fixed, emergency, savings, newAmount)
                    }
                )
            }

            // 4. Submit Button
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.saveIncomeAndAllocation(onSuccess = onNavigateBack) },
                    enabled = unallocated == 0L && total > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FarmGreenPrimary,
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Konfirmasi & Mulai Jatah Harian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PocketAllocationItem(
    title: String,
    amount: Long,
    total: Long,
    onAmountChange: (Long) -> Unit
) {
    FarmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(
                formatCurrency(amount),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenPrimary
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val percent = if (total > 0) ((amount.toDouble() / total) * 100).toInt() else 0
        Text(
            text = "$percent% dari total pendapatan",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
