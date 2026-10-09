package com.richdin.finance.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richdin.finance.core.model.Category
import com.richdin.finance.core.model.Expense
import com.richdin.finance.core.model.PocketType
import com.richdin.finance.core.repository.FinanceRepository
import com.richdin.finance.core.ui.components.NumberKeypad
import com.richdin.finance.core.ui.components.formatCurrency
import com.richdin.finance.core.ui.theme.*
import com.richdin.finance.core.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _amountInput = MutableStateFlow("0")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect { list ->
                _categories.value = list
                if (_selectedCategory.value == null && list.isNotEmpty()) {
                    _selectedCategory.value = list.first()
                }
            }
        }
    }

    fun onSelectCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun onAmountChange(value: String) {
        _amountInput.value = if (value.isEmpty()) "0" else value
    }

    fun onNoteChange(text: String) {
        _note.value = text
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val amount = _amountInput.value.toLongOrNull() ?: 0L
        if (amount <= 0) return

        val category = _selectedCategory.value ?: return

        viewModelScope.launch {
            val period = repository.getActiveSalaryPeriodOnce() ?: return@launch
            val dailyPocket = repository.getPocketByTypeOnce(period.id, PocketType.DAILY) ?: return@launch

            val expense = Expense(
                salaryPeriodId = period.id,
                pocketId = dailyPocket.id,
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.iconName,
                amount = amount,
                date = LocalDate.now(),
                note = _note.value
            )

            repository.addExpense(expense)
            _isSaved.value = true
            onSuccess()
        }
    }
}

@Composable
fun QuickExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val amountInput by viewModel.amountInput.collectAsState()
    val note by viewModel.note.collectAsState()

    val parsedAmount = amountInput.toLongOrNull() ?: 0L

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
                    text = "Catat Pengeluaran Cepat",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))

                // Display Amount
                Text(
                    text = formatCurrency(parsedAmount),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenPrimary,
                        fontSize = 38.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector Chips
                Text(
                    text = "Pilih Kategori",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = category.id == selectedCategory?.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) FarmGreenPrimary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.onSelectCategory(category) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = { Text("Catatan singkat (mis: Nasi Padang + Es Teh)...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FarmGreenPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Keypad & Save Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NumberKeypad(
                    currentValue = amountInput,
                    onValueChange = viewModel::onAmountChange
                )

                Button(
                    onClick = { viewModel.saveExpense(onSuccess = onNavigateBack) },
                    enabled = parsedAmount > 0,
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
                        text = "Simpan Pengeluaran",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
