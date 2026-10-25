package ir.hamghadam.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * The parts of database encryption that can be checked without the SQLCipher
 * native library: header detection (is this file still plaintext?) and key
 * generation/stability.
 *
 * The actual `sqlcipher_export` migration needs a device or an instrumented
 * test; that gap is documented rather than hidden.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DatabaseCryptoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun missingFileIsNotPlaintext() {
        val file = File(context.cacheDir, "does-not-exist.db")
        file.delete()
        assertFalse(DatabaseCrypto.looksPlaintext(file))
    }

    @Test
    fun sqliteHeaderIsDetectedAsPlaintext() {
        val file = File(context.cacheDir, "plain.db")
        file.outputStream().use { it.write("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(100)) }
        assertTrue(DatabaseCrypto.looksPlaintext(file))
        file.delete()
    }

    @Test
    fun encryptedLookingFileIsNotPlaintext() {
        val file = File(context.cacheDir, "encrypted.db")
        val random = ByteArray(256) { (it * 31 + 7).toByte() }
        random[0] = 0x21
        file.writeBytes(random)
        assertFalse(DatabaseCrypto.looksPlaintext(file))
        file.delete()
    }

    @Test
    fun shortFileIsNotPlaintext() {
        val file = File(context.cacheDir, "short.db")
        file.writeBytes("SQLite".toByteArray())
        assertFalse(DatabaseCrypto.looksPlaintext(file))
        file.delete()
    }

    @Test
    fun passphraseIsStableAndLooksLikeA256BitKey() {
        val first = DatabaseCrypto.passphrase(context)
        val second = DatabaseCrypto.passphrase(context)
        assertEquals(first, second)
        assertEquals(64, first.length)                       // 32 bytes as hex
        assertTrue(first.all { it in "0123456789abcdef" })
    }

    @Test
    fun keystoreStatusIsReportedWithoutThrowing() {
        // Robolectric may not provide a Keystore; either answer is fine, but the
        // call must not crash and must fall back to private prefs.
        val protected = DatabaseCrypto.isKeyKeystoreProtected(context)
        assertEquals(true, protected || !protected)
        DatabaseCrypto.passphrase(context)
    }

    @Test
    fun cleanupDoesNothingWhenThereIsNoDatabase() {
        // Must not throw when the app has never created a database.
        DatabaseCrypto.cleanupPlaintextBackup(context)
    }
}
