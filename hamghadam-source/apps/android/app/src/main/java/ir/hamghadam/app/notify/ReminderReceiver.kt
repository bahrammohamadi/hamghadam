package ir.hamghadam.app.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
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
        // Receivers run on the main thread and their exceptions kill the whole
        // process with "app keeps stopping"; a failing reminder is never worth that.
        runCatching { handle(context, intent) }
            .onFailure { android.util.Log.e("ReminderReceiver", "reminder handling failed", it) }
    }

    private fun handle(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        when (intent.action) {
            ACTION_DONE -> completeTask(context, taskId)
            ACTION_SNOOZE -> snooze(context, taskId, intent)
            else -> showNotification(
                context = context,
                taskId = taskId,
                title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty(),
                dueLabel = intent.getStringExtra(EXTRA_DUE_LABEL).orEmpty(),
            )
        }
    }

    // POST_NOTIFICATIONS is checked by `hasNotificationPermission()` above and
    // every notify() call is wrapped in runCatching; lint cannot see either.
    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, taskId: String, title: String, dueLabel: String) {
        if (!hasNotificationPermission(context)) return
        Notifications.ensureChannels(context)

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
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_DUE_LABEL, dueLabel)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { "یادآور کار" })
            .setContentText(
                if (dueLabel.isBlank()) "وقت انجام این کار رسیده" else "هم‌قدم • $dueLabel",
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(Notifications.GROUP_TASKS)
            .setAutoCancel(true)
            // Title of a task can be private, so hide it on the lock screen.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openPendingIntent)
            .addAction(0, "انجام شد", donePendingIntent)
            .addAction(0, "۱۰ دقیقه بعد", snoozePendingIntent)
            .build()

        val manager = NotificationManagerCompat.from(context)
        runCatching {
            manager.notify(taskId.hashCode(), notification)
            manager.notify(Notifications.ID_GROUP_SUMMARY, groupSummary(context))
        }
    }

    /**
     * One-line summary so several reminders collapse into a single stack instead
     * of flooding the shade. It is silent and auto-cancels itself.
     */
    private fun groupSummary(context: Context): android.app.Notification =
        NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("یادآورهای هم‌قدم")
            .setContentText("کارهای سررسیده را ببینید")
            .setGroup(Notifications.GROUP_TASKS)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

    @SuppressLint("MissingPermission")
    private fun snooze(context: Context, taskId: String, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(taskId.hashCode())

        // Re-post the same notification ten minutes from now with AlarmManager,
        // so snoozing survives the process being killed.
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        val alarm = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_DUE_LABEL, "۱۰ دقیقه بعد")
        }
        val pending = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 3,
            alarm,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        // Short confirmation so the tap has visible feedback.
        runCatching {
            manager.notify(
                Notifications.ID_SNOOZE_CONFIRM,
                NotificationCompat.Builder(context, Notifications.CHANNEL_DIGEST)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("۱۰ دقیقه بعد یادآوری می‌شود")
                    .setContentText(title.ifBlank { "کار" })
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .setTimeoutAfter(4000)
                    .build(),
            )
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
            } catch (t: Throwable) {
                android.util.Log.e("ReminderReceiver", "completing task failed", t)
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

    companion object {
        const val ACTION_REMIND = "ir.hamghadam.app.action.REMIND"
        const val ACTION_DONE = "ir.hamghadam.app.action.DONE"
        const val ACTION_SNOOZE = "ir.hamghadam.app.action.SNOOZE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_DUE_LABEL = "task_due_label"
        const val CHANNEL_ID = Notifications.CHANNEL_REMINDERS
        const val SNOOZE_MINUTES = 10

        /** Human readable summary used by the notification body. */
        fun dueSummary(date: JalaliDate?, minuteOfDay: Int?): String {
            if (date == null) return ""
            val day = PersianCalendar.relativeDate(date, JalaliDate.today())
            if (minuteOfDay == null) return day
            return "$day ${PersianText.clock(minuteOfDay / 60, minuteOfDay % 60)}"
        }
    }
}
