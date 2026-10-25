package ir.hamghadam.app

import android.app.Application
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.EventCatalog
import ir.hamghadam.app.data.ProfileStore
import ir.hamghadam.app.data.SecurityStore
import ir.hamghadam.app.data.SettingsStore
import ir.hamghadam.app.data.TaskRepository
import ir.hamghadam.app.google.GoogleSyncScheduler
import ir.hamghadam.app.google.GoogleTokenStore
import ir.hamghadam.app.notify.Notifications
import ir.hamghadam.app.notify.ReminderScheduler

/** Manual dependency container: small app, no DI framework needed yet. */
class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.get(application)
    val profiles = ProfileStore(application)
    val security = SecurityStore(application)
    val repository = TaskRepository(
        taskDao = database.taskDao(),
        projectDao = database.projectDao(),
        habitDao = database.habitDao(),
        eventDao = database.personalEventDao(),
        externalEventDao = database.externalEventDao(),
        focusDao = database.focusSessionDao(),
        templateDao = database.templateDao(),
        profiles = profiles,
    )
    val settings = SettingsStore(application)
    val reminders = ReminderScheduler(application)
    val events = EventCatalog(application)
    val googleTokens = GoogleTokenStore(application)
}

class HamghadamApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.removeLegacyChannels(this)
        Notifications.ensureChannels(this)
        // Only schedule the optional sync when the user already connected a calendar.
        val settings = container.settings.settings.value
        if (settings.googleConnected && settings.googleClientId.isNotBlank()) {
            runCatching { GoogleSyncScheduler.schedule(this) }
        }
    }

}
