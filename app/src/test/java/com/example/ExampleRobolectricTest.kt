package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LogitNotes", appName)
    }

    @Test
    fun `test AES-256-GCM encryption roundtrip`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cryptoManager = CryptoManager(context)
        val initialized = cryptoManager.setupVault("Mi Bóveda", "MasterPassword2026!", enableBiometric = false)
        assertTrue(initialized)
        assertTrue(cryptoManager.isUnlocked)

        val originalText = "# Secreto de la Bóveda\nEste contenido está cifrado con AES-256-GCM."
        val encrypted = cryptoManager.encryptString(originalText)
        assertTrue(encrypted.isNotBlank())
        assertTrue(encrypted != originalText)

        val decrypted = cryptoManager.decryptString(encrypted)
        assertEquals(originalText, decrypted)
    }
}
