package com.richdin.finance.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richdin.finance.core.model.CowMood
import com.richdin.finance.core.security.BiometricAuthManager
import com.richdin.finance.core.security.PinSecurityManager
import com.richdin.finance.core.security.SessionLockManager
import com.richdin.finance.core.ui.components.NumberKeypad
import com.richdin.finance.core.ui.components.SapiMascot
import com.richdin.finance.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinSecurityManager: PinSecurityManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val sessionLockManager: SessionLockManager
) : ViewModel() {

    val isPinConfigured: Boolean
        get() = pinSecurityManager.isPinSet()

    val isBiometricAvailable: Boolean
        get() = biometricAuthManager.canAuthenticate() && pinSecurityManager.isBiometricEnabled()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _confirmPin = MutableStateFlow("")
    val confirmPin: StateFlow<String> = _confirmPin.asStateFlow()

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onPinDigit(digit: String) {
        _errorMessage.value = null
        if (!isPinConfigured) {
            // Setup Mode
            if (!_isConfirming.value) {
                if (_enteredPin.value.length < 6) {
                    _enteredPin.value += digit
                    if (_enteredPin.value.length == 6) {
                        _isConfirming.value = true
                    }
                }
            } else {
                if (_confirmPin.value.length < 6) {
                    _confirmPin.value += digit
                    if (_confirmPin.value.length == 6) {
                        finalizePinSetup()
                    }
                }
            }
        } else {
            // Unlock Mode
            if (_enteredPin.value.length < 6) {
                _enteredPin.value += digit
                if (_enteredPin.value.length == 6) {
                    verifyAndUnlock()
                }
            }
        }
    }

    fun onBackspace() {
        _errorMessage.value = null
        if (!isPinConfigured) {
            if (_isConfirming.value) {
                if (_confirmPin.value.isNotEmpty()) {
                    _confirmPin.value = _confirmPin.value.dropLast(1)
                } else {
                    _isConfirming.value = false
                }
            } else {
                if (_enteredPin.value.isNotEmpty()) {
                    _enteredPin.value = _enteredPin.value.dropLast(1)
                }
            }
        } else {
            if (_enteredPin.value.isNotEmpty()) {
                _enteredPin.value = _enteredPin.value.dropLast(1)
            }
        }
    }

    private fun verifyAndUnlock() {
        val isValid = pinSecurityManager.verifyPin(_enteredPin.value)
        if (isValid) {
            sessionLockManager.unlock()
        } else {
            _errorMessage.value = "PIN salah, silakan coba lagi"
            _enteredPin.value = ""
        }
    }

    private fun finalizePinSetup() {
        if (_enteredPin.value == _confirmPin.value) {
            pinSecurityManager.setPin(_enteredPin.value)
            sessionLockManager.unlock()
        } else {
            _errorMessage.value = "PIN konfirmasi tidak cocok!"
            _confirmPin.value = ""
            _isConfirming.value = false
            _enteredPin.value = ""
        }
    }

    fun triggerBiometric(activity: FragmentActivity) {
        if (!isBiometricAvailable) return
        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                sessionLockManager.unlock()
            },
            onError = { err ->
                _errorMessage.value = err
            }
        )
    }
}

@Composable
fun AuthLockScreen(
    onUnlocked: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val enteredPin by viewModel.enteredPin.collectAsState()
    val confirmPin by viewModel.confirmPin.collectAsState()
    val isConfirming by viewModel.isConfirming.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val isConfigured = viewModel.isPinConfigured
    val activePin = if (isConfirming) confirmPin else enteredPin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            SapiMascot(mood = CowMood.HAPPY, size = 96.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (!isConfigured) {
                    if (isConfirming) "Konfirmasi PIN 6-Digit" else "Buat PIN Keamanan"
                } else {
                    "Masukkan PIN Anda"
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (!isConfigured) {
                    "Lindungi data keuangan Anda dengan PIN 6-digit yang aman."
                } else {
                    "Aplikasi terkunci untuk menjaga privasi keuangan Anda."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < activePin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) FarmGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FarmBarnRed, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Custom Keypad
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (item in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (item.isEmpty()) Color.Transparent
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable(enabled = item.isNotEmpty()) {
                                    when (item) {
                                        "DEL" -> viewModel.onBackspace()
                                        "BIO" -> {
                                            if (viewModel.isBiometricAvailable && context is FragmentActivity) {
                                                viewModel.triggerBiometric(context)
                                            }
                                        }
                                        else -> viewModel.onPinDigit(item)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                "BIO" -> {
                                    if (viewModel.isBiometricAvailable) {
                                        Icon(
                                            Icons.Default.Fingerprint,
                                            contentDescription = "Biometric",
                                            tint = FarmGreenPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                "DEL" -> {
                                    Text(
                                        text = "⌫",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                                else -> {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
