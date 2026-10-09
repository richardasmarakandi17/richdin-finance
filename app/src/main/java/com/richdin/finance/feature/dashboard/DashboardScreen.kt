package com.richdin.finance.feature.dashboard

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
import com.richdin.finance.core.domain.DailyAllowanceEngine
import com.richdin.finance.core.domain.LoanManagerUseCase
import com.richdin.finance.core.model.*
import com.richdin.finance.core.repository.FinanceRepository
import com.richdin.finance.core.ui.components.FarmCard
import com.richdin.finance.core.ui.components.SapiMascotCard
import com.richdin.finance.core.ui.components.formatCurrency
import com.richdin.finance.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val activePeriod: SalaryPeriod? = null,
    val allowanceResult: DailyAllowanceResult? = null,
    val pockets: List<Pocket> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val activeLoans: List<Loan> = emptyList(),
    val remainingBorrowingQuota: Long = 0L
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val allowanceEngine: DailyAllowanceEngine,
    private val loanManager: LoanManagerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            repository.getActiveSalaryPeriod().collect { period ->
                if (period == null) {
                    _uiState.value = DashboardUiState(isLoading = false, activePeriod = null)
                } else {
                    combine(
                        repository.getPockets(period.id),
                        repository.getExpenses(period.id),
                        repository.getActiveUnpaidLoans()
                    ) { pockets, expenses, unpaidLoans ->
                        val dailyPocket = pockets.find { it.type == PocketType.DAILY }
                        val emergencyPocket = pockets.find { it.type == PocketType.EMERGENCY }
                        val today = LocalDate.now()

                        val allowance = if (dailyPocket != null) {
                            val todayExpenses = expenses.filter { it.date == today }.sumOf { it.amount }
                            allowanceEngine.calculateDailyAllowance(
                                today = today,
                                nextSalaryDate = period.nextSalaryDate,
                                remainingDailyBalance = dailyPocket.availableBalance,
                                spentToday = todayExpenses,
                                periodStartDate = period.startDate,
                                totalAllocatedDaily = dailyPocket.allocatedAmount
                            )
                        } else null

                        val quota = if (emergencyPocket != null) {
                            loanManager.getRemainingBorrowingQuota(period.id, emergencyPocket)
                        } else 0L

                        DashboardUiState(
                            isLoading = false,
                            activePeriod = period,
                            allowanceResult = allowance,
                            pockets = pockets,
                            recentExpenses = expenses.take(5),
                            activeLoans = unpaidLoans,
                            remainingBorrowingQuota = quota
                        )
                    }.collect { state ->
                        _uiState.value = state
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigateToIncome: () -> Unit,
    onNavigateToQuickExpense: () -> Unit,
    onNavigateToLoan: () -> Unit,
    onNavigateToPockets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Richdin Finance 🐮",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary
                        )
                    )
                    Text(
                        text = "Kelola Jatah Harian & Zero-Based Budget",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.activePeriod != null) {
                FloatingActionButton(
                    onClick = onNavigateToQuickExpense,
                    containerColor = FarmGreenPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Cepat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FarmGreenPrimary)
            }
        } else if (state.activePeriod == null) {
            // Setup Initial Period prompt
            EmptyPeriodPrompt(
                onSetupPeriod = onNavigateToIncome,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            val allowance = state.allowanceResult
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Card: Dynamic Daily Allowance Engine
                item {
                    DailyAllowanceHeroCard(
                        allowanceResult = allowance,
                        onQuickExpense = onNavigateToQuickExpense
                    )
                }

                // 2. Sapi Mascot Reactive Status Card
                if (allowance != null) {
                    item {
                        SapiMascotCard(
                            mood = allowance.cowMood,
                            status = allowance.status,
                            speechBubbleText = allowance.adviceMessage
                        )
                    }
                }

                // 3. Active Unpaid Loans Alert
                if (state.activeLoans.isNotEmpty()) {
                    item {
                        ActiveLoanWarningCard(
                            loans = state.activeLoans,
                            onManageLoan = onNavigateToLoan
                        )
                    }
                }

                // 4. Quick Action Buttons Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = "Tambah Gaji / Pemasukan",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = FarmGreenPrimary,
                            bgColor = FarmGreenContainer,
                            onClick = onNavigateToIncome,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            title = "Pinjam Darurat",
                            icon = Icons.Default.Emergency,
                            color = FarmStrawText,
                            bgColor = FarmStrawContainer,
                            onClick = onNavigateToLoan,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            title = "Kelola Kantong",
                            icon = Icons.Default.PieChart,
                            color = BarnWoodDark,
                            bgColor = FarmCardSurfaceVariant,
                            onClick = onNavigateToPockets,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. Pockets Summary Preview
                item {
                    PocketsSummarySection(
                        pockets = state.pockets,
                        onViewAll = onNavigateToPockets
                    )
                }

                // 6. Today's Recent Expenses
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pengeluaran Terbaru",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (state.recentExpenses.isEmpty()) {
                    item {
                        FarmCard {
                            Text(
                                text = "Belum ada pengeluaran dicatat hari ini. Ketuk tombol 'Catat Cepat' setelah berbelanja!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    items(state.recentExpenses) { expense ->
                        ExpenseListItem(expense = expense)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
fun DailyAllowanceHeroCard(
    allowanceResult: DailyAllowanceResult?,
    onQuickExpense: () -> Unit
) {
    val allowance = allowanceResult?.todayAllowance ?: 0L
    val daysLeft = allowanceResult?.remainingDays ?: 0L
    val dailyBalance = allowanceResult?.remainingDailyBalance ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JATAH BELANJA HARI INI",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.2.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Sisa $daysLeft Hari",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatCurrency(allowance),
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sisa Saldo Kantong Harian",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Text(
                        text = formatCurrency(dailyBalance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Terpakai Hari Ini",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Text(
                        text = formatCurrency(allowanceResult?.spentToday ?: 0L),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            ),
            maxLines = 2
        )
    }
}

@Composable
fun ActiveLoanWarningCard(
    loans: List<Loan>,
    onManageLoan: () -> Unit
) {
    val totalUnpaid = loans.sumOf { it.amount }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onManageLoan),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FarmBarnRedContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = FarmBarnRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pinjaman Darurat Aktif: ${loans.size} Pinjaman",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmBarnRedText
                    )
                )
                Text(
                    text = "Total belum lunas: ${formatCurrency(totalUnpaid)}. Ketuk untuk melunasi.",
                    style = MaterialTheme.typography.bodySmall.copy(color = FarmBarnRedText)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = FarmBarnRedText
            )
        }
    }
}

@Composable
fun PocketsSummarySection(
    pockets: List<Pocket>,
    onViewAll: () -> Unit
) {
    FarmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alokasi Kantong (Zero-Based)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Lihat Semua",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = FarmGreenPrimary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        for (pocket in pockets) {
            val typeTitle = when (pocket.type) {
                PocketType.FIXED -> "🏠 Kebutuhan Tetap"
                PocketType.EMERGENCY -> "🛡️ Dana Darurat"
                PocketType.SAVINGS -> "🎯 Tabungan & Goals"
                PocketType.DAILY -> "🌾 Jatah Harian"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(typeTitle, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatCurrency(pocket.availableBalance),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ExpenseListItem(expense: Expense) {
    FarmCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FarmGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = FarmGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = expense.categoryName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (expense.note.isNotEmpty()) {
                        Text(
                            text = expense.note,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Text(
                text = "- ${formatCurrency(expense.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FarmBarnRed
                )
            )
        }
    }
}

@Composable
fun EmptyPeriodPrompt(
    onSetupPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SapiMascotCard(
            mood = CowMood.HAPPY,
            status = EarlyWarningStatus.SAFE,
            speechBubbleText = "Halo! Selamat datang di Richdin Finance. Yuk catat gaji dan buat alokasi kantong zero-based pertamamu!"
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSetupPeriod,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Catat Gaji & Mulai Periode Baru", fontWeight = FontWeight.Bold)
        }
    }
}
