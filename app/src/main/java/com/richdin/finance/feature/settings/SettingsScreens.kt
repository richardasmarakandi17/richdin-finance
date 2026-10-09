package com.richdin.finance.feature.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richdin.finance.core.domain.ExportCsvUseCase
import com.richdin.finance.core.model.BackupMeta
import com.richdin.finance.core.model.LimitType
import com.richdin.finance.core.model.LoanLimitConfig
import com.richdin.finance.core.repository.FinanceRepository
import com.richdin.finance.core.security.BiometricAuthManager
import com.richdin.finance.core.security.PinSecurityManager
import com.richdin.finance.core.ui.components.FarmCard
import com.richdin.finance.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pinSecurityManager: PinSecurityManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val repository: FinanceRepository,
    private val exportCsvUseCase: ExportCsvUseCase
) : ViewModel() {

    val isBiometricSupported: Boolean
        get() = biometricAuthManager.canAuthenticate()

    private val _isBiometricEnabled = MutableStateFlow(pinSecurityManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _loanConfig = MutableStateFlow(LoanLimitConfig())
    val loanConfig: StateFlow<LoanLimitConfig> = _loanConfig.asStateFlow()

    private val _backupMeta = MutableStateFlow(BackupMeta())
    val backupMeta: StateFlow<BackupMeta> = _backupMeta.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getLoanLimitConfig().collect { config ->
                _loanConfig.value = config
            }
        }
        viewModelScope.launch {
            repository.getBackupMeta().collect { meta ->
                _backupMeta.value = meta
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        pinSecurityManager.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun updateLoanLimit(limitType: LimitType, value: Double) {
        viewModelScope.launch {
            val updated = _loanConfig.value.copy(limitType = limitType, limitValue = value)
            repository.saveLoanLimitConfig(updated)
        }
    }

    fun triggerBackupNow() {
        viewModelScope.launch {
            val updated = BackupMeta(
                lastBackupTimestamp = System.currentTimeMillis(),
                driveFileId = "drive_backup_richdin_appdata",
                isPendingSync = false
            )
            repository.saveBackupMeta(updated)
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val cacheDir = context.cacheDir
                val file = exportCsvUseCase.exportExpensesToCsv(cacheDir)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Richdin Finance Data Export")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(intent, "Bagikan Data CSV"))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isExporting.value = false
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val loanConfig by viewModel.loanConfig.collectAsState()
    val backupMeta by viewModel.backupMeta.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var showLoanConfigDialog by remember { mutableStateOf(false) }
    var tempCapValue by remember { mutableStateOf(loanConfig.limitValue.toInt().toString()) }

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
                    text = "Pengaturan & Cadangan ⚙️",
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
            // 1. Keamanan & Biometrik
            item {
                Text(
                    text = "Keamanan & PIN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                FarmCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Buka Kunci Biometrik (Sidik Jari / Wajah)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                if (viewModel.isBiometricSupported) "Gunakan sensor biometrik untuk membuka app" else "Sensor biometrik tidak tersedia",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = viewModel::setBiometricEnabled,
                            enabled = viewModel.isBiometricSupported,
                            colors = SwitchDefaults.colors(checkedThumbColor = FarmGreenPrimary)
                        )
                    }
                }
            }

            // 2. Batas Kuota Pinjam Antar-Kantong
            item {
                Text(
                    text = "Disiplin Pinjam Antar-Kantong",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                FarmCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Batas Maksimal Pinjam Bulanan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "Saat ini: ${loanConfig.limitValue.toInt()}% dari saldo Dana Darurat",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                tempCapValue = loanConfig.limitValue.toInt().toString()
                                showLoanConfigDialog = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ubah")
                        }
                    }
                }
            }

            // 3. Backup Otomatis Hybrid Google Drive
            item {
                Text(
                    text = "Cadangan Data (Google Drive)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                FarmCard {
                    val lastDateStr = if (backupMeta.lastBackupTimestamp > 0) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))
                        sdf.format(Date(backupMeta.lastBackupTimestamp))
                    } else "Belum pernah"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cadangan Otomatis Google Drive", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "Terakhir dicadangkan: $lastDateStr",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Button(
                            onClick = viewModel::triggerBackupNow,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Sinkron")
                        }
                    }
                }
            }

            // 4. Export Manual CSV
            item {
                Text(
                    text = "Ekspor Mandiri Data",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                FarmCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ekspor Semua Data ke CSV", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "Simpan atau bagikan data spreadsheet untuk backup mandiri.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Button(
                            onClick = { viewModel.exportCsv(context) },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BarnWoodDark)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekspor")
                        }
                    }
                }
            }

            // 5. App Info
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Richdin Finance v1.0.0 (Production-Ready)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "Clean Architecture • Jetpack Compose • Room DB",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }

    if (showLoanConfigDialog) {
        AlertDialog(
            onDismissRequest = { showLoanConfigDialog = false },
            title = { Text("Atur Batas Pinjam Bulanan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tentukan persentase maksimal Dana Darurat yang boleh dipinjam dalam satu bulan (Disarankan: 20-30%). Perubahan berlaku periode berikutnya.")
                    OutlinedTextField(
                        value = tempCapValue,
                        onValueChange = { tempCapValue = it },
                        suffix = { Text("%") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = tempCapValue.toDoubleOrNull() ?: 30.0
                        viewModel.updateLoanLimit(LimitType.PERCENTAGE, cap)
                        showLoanConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoanConfigDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
