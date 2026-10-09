package com.richdin.finance.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLockManager @Inject constructor(
    private val pinSecurityManager: PinSecurityManager
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun onAppMovedToBackground() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppMovedToForeground() {
        if (!pinSecurityManager.isPinSet()) {
            // First time setup - no lock needed until PIN configured
            _isUnlocked.value = true
            return
        }

        val timeoutMillis = pinSecurityManager.getAutoLockTimeoutSeconds() * 1000L
        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        if (lastBackgroundTimestamp > 0 && elapsed > timeoutMillis) {
            _isUnlocked.value = false
        }
    }
}

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Buka Kunci Richdin Finance",
        subtitle: String = "Gunakan sensor sidik jari atau wajah Anda",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autentikasi gagal, silakan coba lagi")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Gunakan PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
