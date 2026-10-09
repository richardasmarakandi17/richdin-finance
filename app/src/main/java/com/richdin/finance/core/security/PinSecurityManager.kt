package com.richdin.finance.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "encrypted_pin_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for devices with keystore edge cases
            context.getSharedPreferences("richdin_secure_prefs", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT_SECONDS = "key_auto_lock_timeout_seconds"
        private const val DEFAULT_TIMEOUT_SECONDS = 60 // 1 minute
    }

    fun isPinSet(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 6) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin, salt)
        return savedHash == inputHash
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getAutoLockTimeoutSeconds(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)
    }

    fun setAutoLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT_SECONDS, seconds).apply()
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = "$pin:$salt:richdin_salt_pepper"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..16).map { chars.random() }.joinToString("")
    }
}
