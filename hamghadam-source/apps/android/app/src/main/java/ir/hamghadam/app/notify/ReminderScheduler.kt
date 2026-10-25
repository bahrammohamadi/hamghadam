package ir.hamghadam.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules local reminders with AlarmManager.
 *
 * Timed tasks fire exactly at the requested minute when the OS allows exact
 * alarms; otherwise the alarm is set inexact instead of being dropped. All-day
 * tasks (no time) remind at 09:00 local time.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    /**
     * Never throws: an alarm that cannot be scheduled is logged and the task
     * still works when the app is opened. Before the audit a `SecurityException`
     * from an OEM alarm policy could close the app right after saving a task.
     */
    fun schedule(task: TaskEntity) {
        runCatching { scheduleOrThrow(task) }
            .onFailure { Log.w(TAG, "reminder not scheduled for ${task.id}", it) }
    }

    private fun scheduleOrThrow(task: TaskEntity) {
        cancel(task.id)
        val dueDate = task.dueDate ?: return
        val offset = task.reminderMinutesBefore ?: return
        if (task.isCompleted || task.deletedAtEpochMillis != null) return
        val minuteOfDay = task.dueMinuteOfDay ?: DEFAULT_ALL_DAY_MINUTE
        val triggerAt = triggerMillis(dueDate.toGregorian(), minuteOfDay, offset) ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val manager = alarmManager ?: return
        val pendingIntent = buildPendingIntent(task.id, task.title, dueLabel(task))
        val exact = runCatching {
            if (canUseExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.isSuccess
        if (!exact) {
            // Last resort (e.g. exact alarms revoked between check and call).
            runCatching { manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent) }
        }
    }

    fun cancel(taskId: String) {
        runCatching { alarmManager?.cancel(buildPendingIntent(taskId, "", "")) }
    }

    fun rescheduleAll(tasks: List<TaskEntity>) {
        for (task in tasks) schedule(task)
    }

    private fun triggerMillis(dueDay: LocalDate, minuteOfDay: Int, offsetMinutes: Int): Long? = runCatching {
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        dueDay.atTime(time)
            .atZone(ZoneId.systemDefault())
            .minusMinutes(offsetMinutes.toLong())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    private fun canUseExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { alarmManager?.canScheduleExactAlarms() == true }.getOrDefault(false)
        } else {
            true
        }

    private fun dueLabel(task: TaskEntity): String =
        PersianCalendar.dueLabel(task.dueDate, task.dueMinuteOfDay, JalaliDate.today(), persianDigits = true).orEmpty()

    private fun buildPendingIntent(taskId: String, title: String, dueLabel: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_DUE_LABEL, dueLabel)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "ReminderScheduler"
        const val DEFAULT_ALL_DAY_MINUTE = 9 * 60
    }
}
