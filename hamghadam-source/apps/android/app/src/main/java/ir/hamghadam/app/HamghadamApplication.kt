package ir.hamghadam.app

import android.app.Application
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.DatabaseCrypto
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
        // Install first: from here on even a startup failure leaves a local
        // stack trace the user can share from the account screen.
        CrashLog.install(this)
        container = AppContainer(this)
        // The encrypted database has been opened by now; drop the plaintext copy.
        runCatching { DatabaseCrypto.cleanupPlaintextBackup(this) }
        runCatching { Notifications.removeLegacyChannels(this) }
        runCatching { Notifications.ensureChannels(this) }
        // Only schedule the optional sync when the user already connected a calendar.
        runCatching {
            val settings = container.settings.settings.value
            if (settings.googleConnected && settings.googleClientId.isNotBlank()) {
                GoogleSyncScheduler.schedule(this)
            }
        }
    }

}
