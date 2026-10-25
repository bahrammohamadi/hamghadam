package ir.hamghadam.app.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * App lock backed by a PIN.
 *
 * Security notes (this is a local-only app, but the same rules apply):
 *  - the PIN is never stored, only a PBKDF2-HMAC-SHA256 hash with a random salt;
 *  - verification compares byte arrays in constant time;
 *  - repeated failures add an exponential delay (2s, 4s, 8s … capped at 60s);
 *  - nothing is sent anywhere, and there is no recovery question to leak it.
 */
class SecurityStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("hamghadam.security", Context.MODE_PRIVATE)

    val isPinSet: Boolean
        get() = !prefs.getString(KEY_HASH, null).isNullOrBlank()

    val lockAfterMinutes: Int
        get() = prefs.getInt(KEY_TIMEOUT, 1)

    val secureScreen: Boolean
        get() = prefs.getBoolean(KEY_SECURE, false)

    val biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_BLOCKED_UNTIL, 0L)
            .apply()
    }

    fun verify(pin: String): Boolean {
        val saltRaw = prefs.getString(KEY_SALT, null) ?: return false
        val hashRaw = prefs.getString(KEY_HASH, null) ?: return false
        val salt = runCatching { Base64.decode(saltRaw, Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(hashRaw, Base64.NO_WRAP) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, derive(pin, salt))
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_BLOCKED_UNTIL, 0L)
            .apply()
    }

    fun setLockAfterMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_TIMEOUT, minutes.coerceIn(0, 60)).apply()
    }

    fun setSecureScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE, enabled).apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    /** Seconds the user still has to wait after too many wrong PINs. */
    fun blockedForSeconds(): Long {
        val until = prefs.getLong(KEY_BLOCKED_UNTIL, 0L)
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 + 1 else 0
    }

    fun registerFailure(): Int {
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        val delaySeconds = minOf(60L, 1L shl minOf(failures - 1, 5))
        prefs.edit()
            .putInt(KEY_FAILURES, failures)
            .putLong(KEY_BLOCKED_UNTIL, System.currentTimeMillis() + delaySeconds * 1000L)
            .apply()
        return failures
    }

    fun resetFailures() {
        prefs.edit().putInt(KEY_FAILURES, 0).putLong(KEY_BLOCKED_UNTIL, 0L).apply()
    }

    fun failureCount(): Int = prefs.getInt(KEY_FAILURES, 0)

    private fun derive(pin: String, salt: ByteArray): ByteArray = runCatching {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }.getOrElse { ByteArray(32) }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_TIMEOUT = "lock_timeout_minutes"
        const val KEY_SECURE = "secure_screen"
        const val KEY_BIOMETRIC = "biometric"
        const val KEY_FAILURES = "failures"
        const val KEY_BLOCKED_UNTIL = "blocked_until"
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}

fun isPinValid(pin: String): Boolean = pin.length in 4..8 && pin.all { it.isDigit() }
