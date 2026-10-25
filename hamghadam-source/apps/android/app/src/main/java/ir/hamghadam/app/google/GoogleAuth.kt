package ir.hamghadam.app.google

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import ir.hamghadam.app.domain.SimpleJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Optional Google Calendar connection.
 *
 * Design rules:
 *  - the app stays fully usable offline; this layer only fills a local cache;
 *  - the OAuth client id is provided by the user in settings (no secret ships
 *    inside the APK);
 *  - PKCE + loopback redirect is used, which works with a "Desktop app" OAuth
 *    client and does not depend on the signing certificate of a debug build.
 */
object GoogleOAuth {

    const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    const val SCOPE = "https://www.googleapis.com/auth/calendar.readonly"
    const val CALLBACK_PATH = "/oauth2callback"

    data class Token(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAtMillis: Long,
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() > expiresAtMillis - 60_000L
    }

    fun redirectUri(port: Int): String = "http://127.0.0.1:$port$CALLBACK_PATH"

    fun codeVerifier(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(64).also { random.nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun authorizeUrl(clientId: String, redirectUri: String, challenge: String, state: String): String {
        fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
        return buildString {
            append(AUTH_ENDPOINT)
            append("?client_id=").append(enc(clientId))
            append("&redirect_uri=").append(enc(redirectUri))
            append("&response_type=code")
            append("&scope=").append(enc(SCOPE))
            append("&code_challenge=").append(enc(challenge))
            append("&code_challenge_method=S256")
            append("&access_type=offline")
            append("&prompt=consent")
            append("&state=").append(enc(state))
        }
    }

    fun parseQueryParameter(query: String?, key: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split('&').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
        }.firstOrNull { (name, _) -> name == key }?.let { (_, value) ->
            runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
        }
    }

    /** Picks a free loopback port; Google accepts any port for installed apps. */
    fun freePort(): Int {
        for (port in 52000..52999) {
            val free = runCatching { ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).use { true } }
                .getOrDefault(false)
            if (free) return port
        }
        return 52000
    }

    /**
     * Starts a tiny loopback server, waits for the browser redirect and returns
     * the authorization code. Runs off the main thread and is safe to cancel.
     */
    suspend fun awaitCode(port: Int, expectedState: String, timeoutMillis: Int = 300_000): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).use { server ->
                    server.soTimeout = timeoutMillis
                    server.accept().use { socket ->
                        socket.soTimeout = 10_000
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                        val requestLine = reader.readLine().orEmpty()
                        val path = requestLine.split(' ').getOrNull(1).orEmpty()
                        val query = path.substringAfter('?', "")
                        val code = parseQueryParameter(query, "code")
                        val state = parseQueryParameter(query, "state")
                        val error = parseQueryParameter(query, "error")
                        val body = buildString {
                            append("<!doctype html><html lang=\"fa\" dir=\"rtl\"><meta charset=\"utf-8\">")
                            append("<title>هم‌قدم</title>")
                            append("<body style=\"font-family:sans-serif;background:#FBF6EE;color:#2A211B;padding:32px\">")
                            append("<h2>هم‌قدم</h2>")
                            if (error != null) {
                                append("<p>اتصال به گوگل انجام نشد: ").append(escapeHtml(error)).append("</p>")
                            } else if (code != null) {
                                append("<p>اتصال برقرار شد. می‌توانی به اپ برگردی؛ همگام‌سازی شروع می‌شود.</p>")
                            } else {
                                append("<p>پاسخ نامعتبر بود.</p>")
                            }
                            append("<p style=\"color:#6F6259;font-size:13px\">این صفحه فقط برای تکمیل ورود باز شده است.</p>")
                            append("</body></html>")
                        }
                        val bytes = body.toByteArray(Charsets.UTF_8)
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        socket.getOutputStream().apply {
                            write(header.toByteArray(Charsets.US_ASCII))
                            write(bytes)
                            flush()
                        }
                        if (state != expectedState) null else code
                    }
                }
            }.getOrNull()
        }

    fun exchangeCode(
        clientId: String,
        code: String,
        verifier: String,
        redirectUri: String,
    ): Token = postToken(
        mapOf(
            "client_id" to clientId,
            "code" to code,
            "code_verifier" to verifier,
            "grant_type" to "authorization_code",
            "redirect_uri" to redirectUri,
        ),
    )

    fun refresh(clientId: String, refreshToken: String): Token =
        postToken(
            mapOf(
                "client_id" to clientId,
                "refresh_token" to refreshToken,
                "grant_type" to "refresh_token",
            ),
        ).let { refreshed ->
            // Google only returns the refresh token on the first exchange.
            refreshed.copy(refreshToken = refreshed.refreshToken ?: refreshToken)
        }

    private fun postToken(fields: Map<String, String>): Token {
        val body = fields.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val connection = (URL(TOKEN_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = SimpleJson.parse(text) as? Map<*, *>
                ?: throw IllegalStateException("پاسخ گوگل خوانده نشد")
            val access = json["access_token"] as? String
                ?: throw IllegalStateException((json["error_description"] as? String) ?: "توکن دریافت نشد")
            val refresh = json["refresh_token"] as? String
            val expiresIn = (json["expires_in"] as? Number)?.toLong() ?: 3600L
            return Token(
                accessToken = access,
                refreshToken = refresh,
                expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun escapeHtml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

/** Encrypted token storage with a graceful plain-preferences fallback. */
class GoogleTokenStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        context.getSharedPreferences("$FILE_NAME.plain", Context.MODE_PRIVATE)
    }

    /**
     * EncryptedSharedPreferences throws (not returns null) when the Keystore
     * key was invalidated. This store is read while the ViewModel is built, so
     * an unguarded read used to close the app before the first frame; every
     * access is now best-effort and falls back to "not connected".
     */
    private fun <T> read(fallback: T, block: SharedPreferences.() -> T): T =
        runCatching { prefs.block() }.getOrDefault(fallback)

    fun isConnected(): Boolean = !read<String?>(null) { getString(KEY_ACCESS, null) }.isNullOrBlank()

    fun accessToken(): String? = read<String?>(null) { getString(KEY_ACCESS, null) }

    fun refreshToken(): String? = read<String?>(null) { getString(KEY_REFRESH, null) }

    fun expiresAtMillis(): Long = read(0L) { getLong(KEY_EXPIRES, 0L) }

    fun save(token: GoogleOAuth.Token) {
        runCatching {
            prefs.edit()
                .putString(KEY_ACCESS, token.accessToken)
                .putString(KEY_REFRESH, token.refreshToken ?: refreshToken())
                .putLong(KEY_EXPIRES, token.expiresAtMillis)
                .apply()
        }
    }

    fun clear() {
        runCatching { prefs.edit().clear().apply() }
    }

    fun lastSyncMillis(): Long = read(0L) { getLong(KEY_LAST_SYNC, 0L) }

    fun setLastSync(millis: Long) {
        runCatching { prefs.edit().putLong(KEY_LAST_SYNC, millis).apply() }
    }

    fun accountLabel(): String = read("") { getString(KEY_ACCOUNT, "") }.orEmpty()

    fun setAccountLabel(label: String) {
        runCatching { prefs.edit().putString(KEY_ACCOUNT, label).apply() }
    }

    /**
     * Returns a usable access token, refreshing it when it is about to expire.
     * Any network problem simply returns null; the caller keeps local data.
     */
    fun validAccessToken(clientId: String): String? {
        val access = accessToken() ?: return null
        if (System.currentTimeMillis() < expiresAtMillis() - 60_000L) return access
        val refresh = refreshToken() ?: return null
        if (clientId.isBlank()) return null
        return runCatching {
            val refreshed = GoogleOAuth.refresh(clientId, refresh)
            save(refreshed)
            refreshed.accessToken
        }.getOrNull()
    }

    private companion object {
        const val FILE_NAME = "hamghadam.google"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_ACCOUNT = "account"
    }
}
