package ir.hamghadam.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.hamghadam.app.MainActivity
import ir.hamghadam.app.R
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.TaskRepository
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Posts the reminder notification and handles its "done" action without opening
 * the app. Long work runs in `goAsync()` so the receiver is not killed early.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        when (intent.action) {
            ACTION_DONE -> completeTask(context, taskId)
            else -> showNotification(
                context = context,
                taskId = taskId,
                title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty(),
                dueLabel = intent.getStringExtra(EXTRA_DUE_LABEL).orEmpty(),
            )
        }
    }

    private fun showNotification(context: Context, taskId: String, title: String, dueLabel: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { "یادآور کار" })
            .setContentText(
                if (dueLabel.isBlank()) "وقت انجام این کار رسیده" else "هم‌قدم • $dueLabel",
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "انجام شد", donePendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        }
    }

    private fun completeTask(context: Context, taskId: String) {
        val pending = goAsync()
        val database = AppDatabase.get(context)
        val repository = TaskRepository(database)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val task = repository.findTask(taskId)
                if (task != null) repository.setCompleted(task, true)
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
            } finally {
                pending.finish()
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "یادآور کارها", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "یادآور کارهای دارای تاریخ و ساعت"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_REMIND = "ir.hamghadam.app.action.REMIND"
        const val ACTION_DONE = "ir.hamghadam.app.action.DONE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_DUE_LABEL = "task_due_label"
        const val CHANNEL_ID = "task_reminders"

        /** Human readable summary used by the notification body. */
        fun dueSummary(date: JalaliDate?, minuteOfDay: Int?): String {
            if (date == null) return ""
            val day = PersianCalendar.relativeDate(date, JalaliDate.today())
            if (minuteOfDay == null) return day
            return "$day ${PersianText.clock(minuteOfDay / 60, minuteOfDay % 60)}"
        }
    }
}
