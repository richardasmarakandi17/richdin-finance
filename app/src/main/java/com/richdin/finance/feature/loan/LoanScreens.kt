package com.richdin.finance.feature.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.richdin.finance.core.domain.LoanManagerUseCase
import com.richdin.finance.core.domain.LoanValidationResult
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
import javax.inject.Inject

data class LoanUiState(
    val activePeriod: SalaryPeriod? = null,
    val emergencyPocket: Pocket? = null,
    val savingsPocket: Pocket? = null,
    val dailyPocket: Pocket? = null,
    val totalQuota: Long = 0L,
    val remainingQuota: Long = 0L,
    val activeLoans: List<Loan> = emptyList(),
    val errorDialogMessage: String? = null,
    val showConfirmDialog: Boolean = false
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val loanManager: LoanManagerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanUiState())
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    private val _requestedAmount = MutableStateFlow("")
    val requestedAmount: StateFlow<String> = _requestedAmount.asStateFlow()

    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()

    private val _selectedPocketType = MutableStateFlow(PocketType.EMERGENCY)
    val selectedPocketType: StateFlow<PocketType> = _selectedPocketType.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getActiveSalaryPeriod().collect { period ->
                if (period != null) {
                    repository.getPockets(period.id).collect { pockets ->
                        val emergency = pockets.find { it.type == PocketType.EMERGENCY }
                        val savings = pockets.find { it.type == PocketType.SAVINGS }
                        val daily = pockets.find { it.type == PocketType.DAILY }

                        val sourcePocket = if (_selectedPocketType.value == PocketType.EMERGENCY) emergency else savings
                        val quota = if (sourcePocket != null) {
                            loanManager.getMonthlyBorrowingQuota(period.id, sourcePocket)
                        } else 0L

                        val remaining = if (sourcePocket != null) {
                            loanManager.getRemainingBorrowingQuota(period.id, sourcePocket)
                        } else 0L

                        repository.getLoans(period.id).collect { loans ->
                            _uiState.value = _uiState.value.copy(
                                activePeriod = period,
                                emergencyPocket = emergency,
                                savingsPocket = savings,
                                dailyPocket = daily,
                                totalQuota = quota,
                                remainingQuota = remaining,
                                activeLoans = loans
                            )
                        }
                    }
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        _requestedAmount.value = value
    }

    fun onReasonChange(value: String) {
        _reason.value = value
    }

    fun onSelectPocketType(type: PocketType) {
        _selectedPocketType.value = type
    }

    fun onDismissError() {
        _uiState.value = _uiState.value.copy(errorDialogMessage = null)
    }

    fun onRequestConfirm() {
        val amount = _requestedAmount.value.toLongOrNull() ?: 0L
        val period = _uiState.value.activePeriod ?: return
        val sourcePocket = if (_selectedPocketType.value == PocketType.EMERGENCY) {
            _uiState.value.emergencyPocket
        } else {
            _uiState.value.savingsPocket
        } ?: return

        viewModelScope.launch {
            val validation = loanManager.validateLoan(period.id, sourcePocket, amount)
            if (validation is LoanValidationResult.Rejected) {
                _uiState.value = _uiState.value.copy(errorDialogMessage = validation.reason)
            } else {
                _uiState.value = _uiState.value.copy(showConfirmDialog = true)
            }
        }
    }

    fun onDismissConfirm() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    fun executeBorrowing(onSuccess: () -> Unit) {
        val amount = _requestedAmount.value.toLongOrNull() ?: 0L
        val period = _uiState.value.activePeriod ?: return
        val dailyPocket = _uiState.value.dailyPocket ?: return
        val sourcePocket = if (_selectedPocketType.value == PocketType.EMERGENCY) {
            _uiState.value.emergencyPocket
        } else {
            _uiState.value.savingsPocket
        } ?: return

        viewModelScope.launch {
            val result = loanManager.executeLoan(
                periodId = period.id,
                sourcePocketId = sourcePocket.id,
                targetPocketId = dailyPocket.id,
                amount = amount,
                reason = _reason.value.ifEmpty { "Keperluan Darurat" }
            )

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(showConfirmDialog = false)
                _requestedAmount.value = ""
                _reason.value = ""
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    showConfirmDialog = false,
                    errorDialogMessage = result.exceptionOrNull()?.message ?: "Gagal memproses pinjaman"
                )
            }
        }
    }

    fun repayLoan(loan: Loan) {
        viewModelScope.launch {
            loanManager.repayLoan(loan)
        }
    }
}

@Composable
fun EmergencyLoanScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val requestedAmount by viewModel.requestedAmount.collectAsState()
    val reason by viewModel.reason.collectAsState()
    val selectedPocketType by viewModel.selectedPocketType.collectAsState()

    val parsedAmount = requestedAmount.toLongOrNull() ?: 0L

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
                    text = "Pinjam Antar-Kantong 🛡️",
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
            // 1. Quota Cap Indicator Card
            item {
                FarmCard(
                    backgroundColor = FarmStrawContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Batas Kuota Pinjam Bulan Ini",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FarmStrawText
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sisa Kuota: ${formatCurrency(state.remainingQuota)} / ${formatCurrency(state.totalQuota)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FarmStrawText
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (state.totalQuota > 0) {
                        (state.remainingQuota.toFloat() / state.totalQuota.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = FarmStrawWarning,
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // 2. Source Pocket Picker
            item {
                FarmCard {
                    Text(
                        text = "Pilih Kantong Asal Pinjaman",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedPocketType == PocketType.EMERGENCY,
                            onClick = { viewModel.onSelectPocketType(PocketType.EMERGENCY) },
                            label = {
                                Text("Dana Darurat (${formatCurrency(state.emergencyPocket?.availableBalance ?: 0L)})")
                            }
                        )
                        FilterChip(
                            selected = selectedPocketType == PocketType.SAVINGS,
                            onClick = { viewModel.onSelectPocketType(PocketType.SAVINGS) },
                            label = {
                                Text("Tabungan (${formatCurrency(state.savingsPocket?.availableBalance ?: 0L)})")
                            }
                        )
                    }
                }
            }

            // 3. Amount & Reason Inputs
            item {
                FarmCard {
                    Text(
                        text = "Nominal yang Ingin Dipinjam",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = requestedAmount,
                        onValueChange = viewModel::onAmountChange,
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = viewModel::onReasonChange,
                        placeholder = { Text("Alasan darurat (mis: Biaya berobat / ban bocor)...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // 4. Request Button
            item {
                Button(
                    onClick = viewModel::onRequestConfirm,
                    enabled = parsedAmount > 0 && parsedAmount <= state.remainingQuota,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FarmStrawText,
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajukan Pinjaman Darurat", fontWeight = FontWeight.Bold)
                }
            }

            // 5. Loan History & Repay Section
            item {
                Text(
                    text = "Riwayat Pinjaman Periode Ini",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (state.activeLoans.isEmpty()) {
                item {
                    FarmCard {
                        Text(
                            text = "Tidak ada pinjaman aktif. Keuanganmu sangat sehat!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(state.activeLoans) { loan ->
                    LoanItemCard(
                        loan = loan,
                        onRepay = { viewModel.repayLoan(loan) }
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissConfirm,
            title = {
                Text("Konfirmasi Pinjam Dana ⚠️", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Anda akan meminjam ${formatCurrency(parsedAmount)} dari ${
                        if (selectedPocketType == PocketType.EMERGENCY) "Dana Darurat" else "Tabungan"
                    } ke Jatah Harian.\n\nPinjaman ini wajib dikembalikan pada periode gajian berikutnya agar target masa depan Anda tetap aman!"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.executeBorrowing(onSuccess = onNavigateBack) },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmBarnRed)
                ) {
                    Text("Ya, Pinjam Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissConfirm) {
                    Text("Batal")
                }
            }
        )
    }

    // Error / Quota Rejected Dialog
    if (state.errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissError,
            title = {
                Text("Pinjaman Ditolak ❌", fontWeight = FontWeight.Bold, color = FarmBarnRed)
            },
            text = {
                Text(state.errorDialogMessage ?: "")
            },
            confirmButton = {
                Button(onClick = viewModel::onDismissError) {
                    Text("Mengerti")
                }
            }
        )
    }
}

@Composable
fun LoanItemCard(
    loan: Loan,
    onRepay: () -> Unit
) {
    FarmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatCurrency(loan.amount)} (${loan.reason})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Dipinjam dari: ${loan.sourcePocketName} • ${loan.loanDate}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (loan.status == LoanStatus.UNPAID) "🔴 Belum Lunas" else "🟢 Sudah Lunas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (loan.status == LoanStatus.UNPAID) FarmBarnRedText else FarmGreenText
                    )
                )
            }

            if (loan.status == LoanStatus.UNPAID) {
                Button(
                    onClick = onRepay,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Lunasi", fontSize = 12.sp)
                }
            }
        }
    }
}
