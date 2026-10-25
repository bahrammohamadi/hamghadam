package ir.hamghadam.app

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records the last uncaught exception on the device.
 *
 * Why: this app is offline and has no analytics SDK, so when it crashed the
 * user had no way to send us a stack trace (and "it closes sometimes" is not
 * actionable). The handler writes one small text file in the app's private
 * storage and then delegates to the platform handler, so the system still shows
 * the normal crash dialog and the process still dies — nothing is swallowed.
 *
 * Privacy: the file is local, is never uploaded automatically, and is capped at
 * ~32 KB. The account screen offers an explicit "share" action so the user stays
 * in control of what leaves the phone.
 */
object CrashLog {

    private const val DIR = "diagnostics"
    private const val FILE = "last-crash.txt"
    private const val MAX_CHARS = 32_000

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            // Always continue with the default behaviour: swallowing a crash
            // would leave the UI in an undefined state.
            previous?.uncaughtException(thread, error)
        }
    }

    fun file(context: Context): File = File(File(context.filesDir, DIR), FILE)

    fun exists(context: Context): Boolean = runCatching { file(context).isFile }.getOrDefault(false)

    /** Returns the stored report, or null when there is none / it is unreadable. */
    fun read(context: Context): String? = runCatching {
        file(context).takeIf { it.isFile }?.readText()
    }.getOrNull()?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { writer -> error.printStackTrace(PrintWriter(writer)) }.toString()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "?" }
        val report = buildString {
            appendLine("Hamghadam — گزارش خطا")
            appendLine("time: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            appendLine("app: $version (${context.packageName})")
            appendLine("android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("thread: ${thread.name}")
            appendLine()
            appendLine(trace)
        }.take(MAX_CHARS)

        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        File(dir, FILE).writeText(report)
    }
}
