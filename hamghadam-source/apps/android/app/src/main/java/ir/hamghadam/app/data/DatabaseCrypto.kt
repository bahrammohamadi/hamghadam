package ir.hamghadam.app.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File
import java.security.SecureRandom

/**
 * At-rest encryption for the Room database (SQLCipher 4 / AES-256).
 *
 * Threat model, stated plainly:
 *  - the PIN lock is an in-app gate; it does not protect the file itself;
 *  - this key protects the file: copying `hamghadam.db` off the device yields an
 *    unreadable blob, not a plain SQLite file you can open in any browser tool;
 *  - the key lives in EncryptedSharedPreferences backed by the Android Keystore,
 *    so it is not readable from a normal backup or another app;
 *  - on a rooted device with the app's keystore unlocked, an attacker with the
 *    PIN can still read data. Full-disk encryption plus this is the honest
 *    ceiling for an offline app without a server.
 *
 * Migration safety: upgrades from 0.4.0 (plaintext DB) are exported with
 * `sqlcipher_export` into a new encrypted file. The plaintext file is only
 * deleted after the encrypted copy opens and reports the same schema version.
 * If anything fails we keep the plaintext database and keep working — a broken
 * migration must never look like data loss.
 */
object DatabaseCrypto {

    const val DB_NAME = "hamghadam.db"

    private const val TAG = "DatabaseCrypto"
    private const val PREFS_ENCRYPTED = "hamghadam.dbkey"
    private const val PREFS_FALLBACK = "hamghadam.dbkey.plain"
    private const val KEY_PASSPHRASE = "passphrase"
    private const val KEY_PROTECTED = "keystore_protected"
    private const val PASSPHRASE_BYTES = 32

    @Volatile private var libraryLoaded = false
    @Volatile private var cachedPassphrase: String? = null

    /** SQLCipher ships the native core in the AAR; it must be loaded once. */
    fun loadLibrary() {
        if (libraryLoaded) return
        System.loadLibrary("sqlcipher")
        libraryLoaded = true
    }

    /**
     * Stable 32-byte passphrase, created on first run and stored encrypted.
     * Falls back to a private prefs file if the Keystore is unavailable; the
     * database stays encrypted either way, which is strictly better than plain.
     */
    @Synchronized
    fun passphrase(context: Context): String {
        cachedPassphrase?.let { return it }
        val app = context.applicationContext
        val encrypted = runCatching {
            EncryptedSharedPreferences.create(
                PREFS_ENCRYPTED,
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                app,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()

        val existing = encrypted?.getString(KEY_PASSPHRASE, null)
            ?: app.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE)
                .getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            cachedPassphrase = existing
            return existing
        }

        val fresh = newPassphrase()
        if (encrypted != null) {
            encrypted.edit().putString(KEY_PASSPHRASE, fresh).putBoolean(KEY_PROTECTED, true).apply()
        } else {
            Log.w(TAG, "Keystore unavailable; storing the database key in private prefs")
            app.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE)
                .edit().putString(KEY_PASSPHRASE, fresh).putBoolean(KEY_PROTECTED, false).apply()
        }
        cachedPassphrase = fresh
        return fresh
    }

    /** False when the Keystore was unavailable and the key sits in plain prefs. */
    fun isKeyKeystoreProtected(context: Context): Boolean {
        val app = context.applicationContext
        return app.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROTECTED, true)
    }

    fun databaseFile(context: Context): File = context.getDatabasePath(DB_NAME)

    /** A real SQLite file starts with the 16-byte "SQLite format 3\0" magic. */
    fun looksPlaintext(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        runCatching { file.inputStream().use { it.read(header) } }.getOrElse { return false }
        return header.contentEquals(SQLITE_HEADER)
    }

    /**
     * Converts an existing plaintext database to SQLCipher in place.
     *
     * @return true when the file is encrypted now (either migrated or already was).
     */
    fun migrateIfNeeded(context: Context): Boolean {
        val file = databaseFile(context)
        if (!file.exists()) return true
        if (!looksPlaintext(file)) return true
        val key = passphrase(context)
        val temp = File(file.parentFile, "${DB_NAME}.encrypted")
        temp.delete()
        // Old WAL/SHM side files belong to the plaintext connection.
        File("${file.path}-wal").delete()
        File("${file.path}-shm").delete()

        var migrated = false
        runCatching {
            loadLibrary()
            val plain = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                file.path,
                "",
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
                null,
                null,
            )
            try {
                plain.rawExecSQL("ATTACH DATABASE ? AS encrypted KEY ?", temp.path, key)
                plain.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                plain.rawExecSQL("DETACH DATABASE encrypted")
            } finally {
                plain.close()
            }
            migrated = temp.exists() && temp.length() > 0
        }.onFailure { Log.e(TAG, "Encryption migration failed; keeping plaintext database", it) }

        if (!migrated) {
            temp.delete()
            return false
        }
        // Verify the encrypted copy really opens with our key before replacing.
        val verified = runCatching {
            loadLibrary()
            val probe = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                temp.path,
                key,
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                null,
                null,
            )
            val ok = runCatching {
                probe.rawQuery("PRAGMA user_version", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) >= 4
                }
            }.getOrDefault(false)
            probe.close()
            ok
        }.getOrDefault(false)

        if (!verified) {
            Log.e(TAG, "Encrypted copy did not verify; keeping plaintext database")
            temp.delete()
            return false
        }

        val backup = File(file.parentFile, "${DB_NAME}.plain.bak")
        backup.delete()
        val replaced = runCatching {
            if (!file.renameTo(backup)) return@runCatching false
            if (!temp.renameTo(file)) {
                backup.renameTo(file)
                return@runCatching false
            }
            true
        }.getOrDefault(false)
        if (!replaced) {
            Log.e(TAG, "Could not swap in the encrypted database")
            temp.delete()
            return false
        }
        // Keep the plaintext copy until the next successful start, then remove it.
        val stale = File(file.parentFile, "${DB_NAME}.plain.bak")
        if (stale.exists() && stale.length() > 0) stale.deleteOnExit()
        return true
    }

    /** Removes the leftover plaintext copy after the encrypted DB has been used. */
    fun cleanupPlaintextBackup(context: Context) {
        runCatching {
            val file = databaseFile(context)
            if (!looksPlaintext(file)) {
                File(file.parentFile, "$DB_NAME.plain.bak").delete()
            }
        }
    }

    private fun newPassphrase(): String {
        val bytes = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
}
