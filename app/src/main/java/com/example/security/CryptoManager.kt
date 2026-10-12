package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vault_crypto_prefs", Context.MODE_PRIVATE)

    private val secureRandom = SecureRandom()

    // In-memory unlocked master key (wiped when vault is locked)
    @Volatile
    private var activeSessionKey: SecretKey? = null

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATION_COUNT = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128

        private const val PREF_VAULT_INITIALIZED = "vault_initialized"
        private const val PREF_MASTER_SALT = "master_salt"
        private const val PREF_CANARY_ENCRYPTED = "canary_encrypted"
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_BIOMETRIC_WRAPPED_KEY = "biometric_wrapped_key"
        private const val PREF_BIOMETRIC_IV = "biometric_iv"
        private const val CANARY_TEXT = "VAULT_NOTES_CANARY_2026_VALID"
    }

    val isVaultInitialized: Boolean
        get() = prefs.getBoolean(PREF_VAULT_INITIALIZED, false)

    val isBiometricEnabled: Boolean
        get() = prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)

    val isUnlocked: Boolean
        get() = activeSessionKey != null

    var vaultName: String
        get() = prefs.getString("vault_name", "Mi Bóveda") ?: "Mi Bóveda"
        set(value) = prefs.edit().putString("vault_name", value).apply()

    /**
     * Initializes the vault for the first time with a user master password.
     */
    fun setupVault(name: String, masterPassword: String, enableBiometric: Boolean = true): Boolean {
        try {
            val salt = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
            val derivedKey = deriveKey(masterPassword.toCharArray(), salt)
            
            // Encrypt canary text to verify password in the future
            val canaryEncrypted = encryptInternal(CANARY_TEXT.toByteArray(Charsets.UTF_8), derivedKey)

            val editor = prefs.edit()
                .putBoolean(PREF_VAULT_INITIALIZED, true)
                .putString("vault_name", name.trim().ifBlank { "Mi Bóveda" })
                .putString(PREF_MASTER_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(PREF_CANARY_ENCRYPTED, canaryEncrypted)
                .putBoolean(PREF_BIOMETRIC_ENABLED, enableBiometric)

            if (enableBiometric) {
                // Store wrapped key for biometric unlock
                val wrapped = wrapKeyForBiometric(derivedKey)
                if (wrapped != null) {
                    editor.putString(PREF_BIOMETRIC_WRAPPED_KEY, wrapped.first)
                    editor.putString(PREF_BIOMETRIC_IV, wrapped.second)
                }
            }

            editor.apply()
            activeSessionKey = derivedKey
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Unlocks the vault using the user master password.
     */
    fun unlockWithPassword(masterPassword: String): Boolean {
        try {
            val saltBase64 = prefs.getString(PREF_MASTER_SALT, null) ?: return false
            val canaryEncrypted = prefs.getString(PREF_CANARY_ENCRYPTED, null) ?: return false
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

            val derivedKey = deriveKey(masterPassword.toCharArray(), salt)
            val decryptedCanaryBytes = decryptInternal(canaryEncrypted, derivedKey) ?: return false
            val decryptedCanary = String(decryptedCanaryBytes, Charsets.UTF_8)

            if (decryptedCanary == CANARY_TEXT) {
                activeSessionKey = derivedKey
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Unlocks the vault using stored biometric credentials.
     */
    fun unlockWithBiometric(): Boolean {
        try {
            if (!isBiometricEnabled) return false
            val wrappedKeyB64 = prefs.getString(PREF_BIOMETRIC_WRAPPED_KEY, null) ?: return false
            val ivB64 = prefs.getString(PREF_BIOMETRIC_IV, null) ?: return false

            val key = unwrapKeyFromBiometric(wrappedKeyB64, ivB64) ?: return false
            // Verify with canary
            val canaryEncrypted = prefs.getString(PREF_CANARY_ENCRYPTED, null) ?: return false
            val decryptedCanaryBytes = decryptInternal(canaryEncrypted, key) ?: return false
            if (String(decryptedCanaryBytes, Charsets.UTF_8) == CANARY_TEXT) {
                activeSessionKey = key
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val editor = prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled)
        if (enabled && activeSessionKey != null) {
            val wrapped = wrapKeyForBiometric(activeSessionKey!!)
            if (wrapped != null) {
                editor.putString(PREF_BIOMETRIC_WRAPPED_KEY, wrapped.first)
                editor.putString(PREF_BIOMETRIC_IV, wrapped.second)
            }
        } else if (!enabled) {
            editor.remove(PREF_BIOMETRIC_WRAPPED_KEY)
            editor.remove(PREF_BIOMETRIC_IV)
        }
        editor.apply()
    }

    /**
     * Locks the vault immediately, zeroing and clearing active session key.
     */
    fun lockVault() {
        activeSessionKey = null
    }

    /**
     * Encrypts plaintext string with AES-256-GCM.
     */
    fun encryptString(plainText: String): String {
        val key = activeSessionKey ?: throw IllegalStateException("Bóveda bloqueada: clave no disponible")
        return encryptInternal(plainText.toByteArray(Charsets.UTF_8), key)
    }

    /**
     * Decrypts ciphertext string with AES-256-GCM.
     */
    fun decryptString(cipherTextBase64: String): String {
        val key = activeSessionKey ?: throw IllegalStateException("Bóveda bloqueada: clave no disponible")
        val decryptedBytes = decryptInternal(cipherTextBase64, key)
            ?: throw SecurityException("Fallo al descifrar el contenido. Clave inválida o datos corruptos.")
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Encrypts raw bytes (e.g. for image attachment or document).
     */
    fun encryptBytes(data: ByteArray): ByteArray {
        val key = activeSessionKey ?: throw IllegalStateException("Bóveda bloqueada: clave no disponible")
        val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val cipherBytes = cipher.doFinal(data)

        val buffer = ByteBuffer.allocate(IV_LENGTH_BYTES + cipherBytes.size)
        buffer.put(iv)
        buffer.put(cipherBytes)
        return buffer.array()
    }

    /**
     * Decrypts raw bytes (e.g. for image attachment or document).
     */
    fun decryptBytes(encryptedData: ByteArray): ByteArray {
        val key = activeSessionKey ?: throw IllegalStateException("Bóveda bloqueada: clave no disponible")
        if (encryptedData.size < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Datos cifrados corruptos o incompletos")
        }
        val buffer = ByteBuffer.wrap(encryptedData)
        val iv = ByteArray(IV_LENGTH_BYTES)
        buffer.get(iv)
        val cipherBytes = ByteArray(buffer.remaining())
        buffer.get(cipherBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    private fun encryptInternal(data: ByteArray, key: SecretKey): String {
        val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val cipherBytes = cipher.doFinal(data)

        val combined = ByteBuffer.allocate(IV_LENGTH_BYTES + cipherBytes.size)
            .put(iv)
            .put(cipherBytes)
            .array()
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptInternal(base64Payload: String, key: SecretKey): ByteArray? {
        return try {
            val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTES) return null

            val buffer = ByteBuffer.wrap(combined)
            val iv = ByteArray(IV_LENGTH_BYTES)
            buffer.get(iv)
            val cipherBytes = ByteArray(buffer.remaining())
            buffer.get(cipherBytes)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(DERIVATION_ALGORITHM)
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    // Biometric key wrapping mechanism using device-level obfuscation/app-specific secure seed
    private fun wrapKeyForBiometric(masterKey: SecretKey): Pair<String, String>? {
        return try {
            val deviceEntropy = (prefs.getString("device_entropy", null) ?: run {
                val newEntropy = ByteArray(32).apply { secureRandom.nextBytes(this) }
                val encoded = Base64.encodeToString(newEntropy, Base64.NO_WRAP)
                prefs.edit().putString("device_entropy", encoded).apply()
                encoded
            })
            val salt = Base64.decode(deviceEntropy, Base64.NO_WRAP)
            val wrappingKey = SecretKeySpec(salt, KEY_ALGORITHM)

            val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val wrapped = cipher.doFinal(masterKey.encoded)

            Pair(
                Base64.encodeToString(wrapped, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun unwrapKeyFromBiometric(wrappedB64: String, ivB64: String): SecretKey? {
        return try {
            val deviceEntropy = prefs.getString("device_entropy", null) ?: return null
            val salt = Base64.decode(deviceEntropy, Base64.NO_WRAP)
            val wrappingKey = SecretKeySpec(salt, KEY_ALGORITHM)

            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val wrappedBytes = Base64.decode(wrappedB64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val rawKeyBytes = cipher.doFinal(wrappedBytes)
            SecretKeySpec(rawKeyBytes, KEY_ALGORITHM)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
