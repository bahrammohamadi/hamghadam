package ir.hamghadam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Encrypted backups (AES-256-GCM + PBKDF2-SHA256).
 *
 * These run on a plain JVM, so the cryptographic envelope is verified without a
 * device: round-trip, wrong password, tampering, and backwards compatibility
 * with the plain-JSON backups written by 0.4.0.
 */
class BackupCipherTest {

    private val json = """{"format":"hamghadam.backup","version":2,"tasks":[{"id":"t1","title":"خرید نان"}]}"""

    @Test
    fun encryptedFileIsNotReadableAsJson() {
        val bytes = BackupCipher.encrypt(json, "رمز-من-۱۲۳", iterations = 2_000)
        assertTrue(BackupCipher.isEncrypted(bytes))
        val asText = String(bytes, Charsets.UTF_8)
        assertFalse(asText.contains("tasks"))
        assertFalse(asText.contains("خرید"))
    }

    @Test
    fun roundTripReturnsTheExactJson() {
        val bytes = BackupCipher.encrypt(json, "12345678", iterations = 2_000)
        assertEquals(json, BackupCipher.decrypt(bytes, "12345678"))
    }

    @Test
    fun unicodeAndLongPayloadsSurvive() {
        val big = json.replace("خرید نان", "خرید نان و پنیر 🧀 برای مهمانی ".repeat(200))
        val bytes = BackupCipher.encrypt(big, "s3cret pass", iterations = 2_000)
        assertEquals(big, BackupCipher.decrypt(bytes, "s3cret pass"))
    }

    @Test
    fun wrongPasswordFailsLoudly() {
        val bytes = BackupCipher.encrypt(json, "correct", iterations = 2_000)
        try {
            BackupCipher.decrypt(bytes, "wrong")
            fail("رمز نادرست باید خطا بدهد")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("رمز نادرست"))
        }
    }

    @Test
    fun tamperedFileFailsTheAuthenticationTag() {
        val bytes = BackupCipher.encrypt(json, "pw123456", iterations = 2_000)
        bytes[bytes.size - 5] = (bytes[bytes.size - 5] + 1).toByte()
        try {
            BackupCipher.decrypt(bytes, "pw123456")
            fail("فایل دست‌کاری‌شده نباید باز شود")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("رمز نادرست"))
        }
    }

    @Test
    fun samePlaintextGivesDifferentCiphertexts() {
        val a = BackupCipher.encrypt(json, "pw", iterations = 1_000)
        val b = BackupCipher.encrypt(json, "pw", iterations = 1_000)
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun plainJsonIsRecognisedAndPassesThroughUnchanged() {
        val plain = json.toByteArray(Charsets.UTF_8)
        assertFalse(BackupCipher.isEncrypted(plain))
        assertEquals(json, BackupCipher.open(plain, password = null))
        // A password on a plain file is harmless: the file is not encrypted.
        assertEquals(json, BackupCipher.open(plain, password = "ignored"))
    }

    @Test
    fun encryptedFileWithoutPasswordAsksForOne() {
        val bytes = BackupCipher.encrypt(json, "pw", iterations = 1_000)
        try {
            BackupCipher.open(bytes, password = null)
            fail("باید رمز بخواهد")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("رمز"))
        }
        assertEquals(json, BackupCipher.open(bytes, "pw"))
    }

    @Test
    fun iterationCountIsStoredInTheFile() {
        val bytes = BackupCipher.encrypt(json, "pw", iterations = 12_345)
        val stored = ((bytes[5].toInt() and 0xFF) shl 24) or
            ((bytes[6].toInt() and 0xFF) shl 16) or
            ((bytes[7].toInt() and 0xFF) shl 8) or
            (bytes[8].toInt() and 0xFF)
        assertEquals(12_345, stored)
        // …and the file still opens, because decryption reads that number back.
        assertEquals(json, BackupCipher.decrypt(bytes, "pw"))
    }

    @Test
    fun emptyPasswordIsRejectedOnEncrypt() {
        try {
            BackupCipher.encrypt(json, "")
            fail("رمز خالی نباید پذیرفته شود")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("رمز"))
        }
    }

    @Test
    fun garbageBytesAreNotMistakenForAnEnvelope() {
        assertFalse(BackupCipher.isEncrypted(ByteArray(0)))
        assertFalse(BackupCipher.isEncrypted("HGB1".toByteArray()))
        assertFalse(BackupCipher.isEncrypted(ByteArray(64) { 0x7A }))
    }
}
