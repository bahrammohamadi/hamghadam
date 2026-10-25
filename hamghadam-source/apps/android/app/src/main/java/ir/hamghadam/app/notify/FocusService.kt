package ir.hamghadam.app.notify

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.hamghadam.app.MainActivity
import ir.hamghadam.app.R
import ir.hamghadam.app.domain.PersianText

/**
 * Ongoing notification for a running focus session.
 *
 * Why a foreground service: the session must survive the screen being off and
 * the app being backgrounded. A plain notification would be swiped away and the
 * OS could freeze the timer. The service only holds a countdown; all task state
 * stays in the app.
 *
 * Android 14+ requires a `foregroundServiceType`; the closest truthful fit is
 * `specialUse` (Play policy asks for an explanatory property, added in the
 * manifest). The channel is IMPORTANCE_LOW so the banner never interrupts the
 * user while they are working.
 */
class FocusService : android.app.Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var endsAt = 0L
    private var totalSeconds = 0
    private var label = ""
    private var onBreak = false

    private val ticker = object : Runnable {
        override fun run() {
            val remaining = ((endsAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            push(remaining)
            if (remaining > 0) handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        endsAt = intent?.getLongExtra(EXTRA_ENDS_AT, 0L) ?: 0L
        totalSeconds = intent?.getIntExtra(EXTRA_TOTAL, 0) ?: 0
        label = intent?.getStringExtra(EXTRA_LABEL).orEmpty()
        onBreak = intent?.getBooleanExtra(EXTRA_BREAK, false) ?: false
        runCatching { Notifications.ensureChannels(this) }
        // Android 12+ can refuse a foreground start after the app went to the
        // background; Android 14 throws for a missing service type. Either way
        // the service must stop quietly instead of taking the process down.
        val foreground = runCatching {
            startForeground(Notifications.ID_FOCUS, build(remainingSeconds()))
        }.isSuccess
        if (!foreground) {
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        return START_NOT_STICKY
    }

    private fun remainingSeconds(): Int =
        ((endsAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)

    private fun build(remaining: Int): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val minutes = remaining / 60
        val seconds = remaining % 60
        val clock = PersianText.clock(minutes, seconds)
        val title = when {
            onBreak -> "وقفهٔ کوتاه"
            label.isNotBlank() -> label
            else -> "جلسهٔ تمرکز"
        }
        val percent = if (totalSeconds > 0) {
            ((totalSeconds - remaining) * 100 / totalSeconds).coerceIn(0, 100)
        } else {
            0
        }
        return NotificationCompat.Builder(this, Notifications.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$clock مانده • بدون صدا، بدون مزاحمت")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setProgress(100, percent, false)
            .addAction(0, "پایان", stop)
            .build()
    }

    // The notification is the foreground-service notification; the call is
    // already wrapped because the channel can be blocked by the user.
    @SuppressLint("MissingPermission")
    private fun push(remaining: Int) {
        runCatching {
            NotificationManagerCompat.from(this).notify(Notifications.ID_FOCUS, build(remaining))
        }
        if (remaining <= 0) stopSelfCleanly()
    }

    private fun stopSelfCleanly() {
        handler.removeCallbacks(ticker)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "ir.hamghadam.app.action.FOCUS_STOP"
        private const val EXTRA_ENDS_AT = "ends_at"
        private const val EXTRA_TOTAL = "total"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_BREAK = "break"

        fun start(context: Context, endsAtMillis: Long, totalSeconds: Int, label: String, onBreak: Boolean) {
            val intent = Intent(context, FocusService::class.java).apply {
                putExtra(EXTRA_ENDS_AT, endsAtMillis)
                putExtra(EXTRA_TOTAL, totalSeconds)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_BREAK, onBreak)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, FocusService::class.java)) }
        }
    }
}
