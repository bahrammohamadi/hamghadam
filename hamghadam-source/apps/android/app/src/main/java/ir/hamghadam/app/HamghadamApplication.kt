package ir.hamghadam.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.SettingsStore
import ir.hamghadam.app.data.TaskRepository
import ir.hamghadam.app.notify.ReminderReceiver
import ir.hamghadam.app.notify.ReminderScheduler

/** Manual dependency container: small app, no DI framework needed yet. */
class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.get(application)
    val repository = TaskRepository(database.taskDao(), database.projectDao(), database.habitDao())
    val settings = SettingsStore(application)
    val reminders = ReminderScheduler(application)
}

class HamghadamApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID,
            "یادآور کارها",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "یادآور کارهای دارای تاریخ و ساعت"
        }
        manager.createNotificationChannel(channel)
    }
}
