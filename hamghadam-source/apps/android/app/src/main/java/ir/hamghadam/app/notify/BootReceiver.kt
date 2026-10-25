package ir.hamghadam.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarms do not survive a reboot, so pending reminders are re-scheduled here. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val database = AppDatabase.get(context)
        val repository = TaskRepository(database.taskDao(), database.projectDao(), database.habitDao())
        val scheduler = ReminderScheduler(context.applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                scheduler.rescheduleAll(repository.loadTasksWithReminders())
            } finally {
                pending.finish()
            }
        }
    }
}
