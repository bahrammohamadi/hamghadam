package ir.hamghadam.app.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Password-protected backup envelope.
 *
 * File layout (big-endian where multi-byte):
 * ```
 * 0..3    magic "HGB1"
 * 4       format version (1)
 * 5..8    PBKDF2 iteration count
 * 9..24   salt (16 bytes)
 * 25..36  GCM IV (12 bytes)
 * 37..    AES-256/GCM ciphertext with a 128-bit tag
 * ```
 *
 * Why AES-GCM and not plain AES: GCM is authenticated, so a corrupted or
 * tampered file fails loudly instead of importing garbage. The iteration count
 * is stored *in the file* so backups stay readable if the default is raised
 * later (old files use their own count).
 *
 * The same class is used for both "encrypted" and "verify" paths, and it has no
 * Android dependencies, so the tests for it run on the plain JVM.
 */
object BackupCipher {

    private const val MAGIC = "HGB1"
    const val VERSION = 1
    const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + IV_BYTES

    /** Cheap magic check so a plain JSON backup is still accepted. */
    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < HEADER_BYTES) return false
        return String(bytes, 0, 4, Charsets.US_ASCII) == MAGIC
    }

    fun encrypt(
        plainText: String,
        password: String,
        iterations: Int = DEFAULT_ITERATIONS,
    ): ByteArray {
        require(password.isNotEmpty()) { "رمز خالی است" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = PinHasher.derive(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val body = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val out = ByteArray(HEADER_BYTES + body.size)
        MAGIC.toByteArray(Charsets.US_ASCII).copyInto(out, 0)
        out[4] = VERSION.toByte()
        out[5] = (iterations ushr 24).toByte()
        out[6] = (iterations ushr 16).toByte()
        out[7] = (iterations ushr 8).toByte()
        out[8] = iterations.toByte()
        salt.copyInto(out, 9)
        iv.copyInto(out, 9 + SALT_BYTES)
        body.copyInto(out, HEADER_BYTES)
        return out
    }

    /**
     * @throws IllegalArgumentException when the password is wrong, the file is
     * damaged, or the magic/version is unknown.
     */
    fun decrypt(bytes: ByteArray, password: String): String {
        if (!isEncrypted(bytes)) throw IllegalArgumentException("این فایل پشتیبان رمزگذاری‌شده نیست")
        val version = bytes[4].toInt()
        if (version != VERSION) throw IllegalArgumentException("نسخهٔ فایل رمزگذاری‌شده پشتیبانی نمی‌شود")
        val iterations = ((bytes[5].toInt() and 0xFF) shl 24) or
            ((bytes[6].toInt() and 0xFF) shl 16) or
            ((bytes[7].toInt() and 0xFF) shl 8) or
            (bytes[8].toInt() and 0xFF)
        if (iterations !in 1_000..5_000_000) throw IllegalArgumentException("فایل پشتیبان معتبر نیست")
        val salt = bytes.copyOfRange(9, 9 + SALT_BYTES)
        val iv = bytes.copyOfRange(9 + SALT_BYTES, 9 + SALT_BYTES + IV_BYTES)
        val body = bytes.copyOfRange(HEADER_BYTES, bytes.size)

        val key = PinHasher.derive(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val plain = try {
            cipher.doFinal(body)
        } catch (e: Exception) {
            throw IllegalArgumentException("رمز نادرست است یا فایل آسیب دیده", e)
        }
        return String(plain, Charsets.UTF_8)
    }

    /** Reads a backup file regardless of format, with an optional password. */
    fun open(bytes: ByteArray, password: String?): String {
        if (!isEncrypted(bytes)) return String(bytes, Charsets.UTF_8)
        if (password.isNullOrBlank()) {
            throw IllegalArgumentException("این فایل با رمز محافظت شده؛ رمز را وارد کن")
        }
        return decrypt(bytes, password)
    }
}
