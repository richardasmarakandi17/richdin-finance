package com.richdin.finance.feature.pockets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richdin.finance.core.model.Goal
import com.richdin.finance.core.model.Pocket
import com.richdin.finance.core.model.PocketType
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

@HiltViewModel
class PocketsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _pockets = MutableStateFlow<List<Pocket>>(emptyList())
    val pockets: StateFlow<List<Pocket>> = _pockets.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getActiveSalaryPeriod().collect { period ->
                if (period != null) {
                    repository.getPockets(period.id).collect { list ->
                        _pockets.value = list
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getGoals().collect { list ->
                _goals.value = list
            }
        }
    }

    fun addGoal(name: String, targetAmount: Long) {
        viewModelScope.launch {
            val goal = Goal(name = name, targetAmount = targetAmount, currentAmount = 0L)
            repository.saveGoal(goal)
        }
    }
}

@Composable
fun PocketsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PocketsViewModel = hiltViewModel()
) {
    val pockets by viewModel.pockets.collectAsState()
    val goals by viewModel.goals.collectAsState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var newGoalName by remember { mutableStateOf("") }
    var newGoalTarget by remember { mutableStateOf("") }

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
                    text = "Kantong Keuangan & Goals 🎯",
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
            item {
                Text(
                    text = "Status 4 Kantong Utama",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(pockets) { pocket ->
                PocketDetailCard(pocket = pocket)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Tabungan & Impian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showAddGoalDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Goal")
                    }
                }
            }

            if (goals.isEmpty()) {
                item {
                    FarmCard {
                        Text(
                            text = "Belum ada goal impian. Buat target seperti beli laptop atau dana darurat 6 bulan!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(goals) { goal ->
                    GoalCard(goal = goal)
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Tambah Target Impian Baru 🎯", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newGoalName,
                        onValueChange = { newGoalName = it },
                        label = { Text("Nama Target (mis: Beli Laptop)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGoalTarget,
                        onValueChange = { newGoalTarget = it },
                        label = { Text("Target Nominal (Rp)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = newGoalTarget.toLongOrNull() ?: 0L
                        if (newGoalName.isNotEmpty() && target > 0) {
                            viewModel.addGoal(newGoalName, target)
                            showAddGoalDialog = false
                            newGoalName = ""
                            newGoalTarget = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Simpan Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PocketDetailCard(pocket: Pocket) {
    FarmCard {
        val (icon, title, desc) = when (pocket.type) {
            PocketType.FIXED -> Triple("🏠", "Kebutuhan Tetap", "Kos, cicilan, listrik & tagihan rutin")
            PocketType.EMERGENCY -> Triple("🛡️", "Dana Darurat", "Cadangan untuk kondisi mendesak/sakit")
            PocketType.SAVINGS -> Triple("🎯", "Tabungan & Goals", "Alokasi masa depan & impian")
            PocketType.DAILY -> Triple("🌾", "Jatah Harian", "Makan, transport & pengeluaran harian")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Alokasi Awal", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text(formatCurrency(pocket.allocatedAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            if (pocket.borrowedAmount > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dipinjam Keluar", style = MaterialTheme.typography.bodySmall.copy(color = FarmBarnRedText))
                    Text("- ${formatCurrency(pocket.borrowedAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = FarmBarnRedText))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Saldo Tersedia", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text(formatCurrency(pocket.availableBalance), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = FarmGreenPrimary))
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal) {
    FarmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(goal.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(
                formatCurrency(goal.targetAmount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val progress = if (goal.targetAmount > 0) {
            (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
        } else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = FarmGreenPrimary,
            trackColor = FarmGreenContainer
        )
    }
}
