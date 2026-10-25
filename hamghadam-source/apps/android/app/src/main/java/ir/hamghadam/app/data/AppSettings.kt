package ir.hamghadam.app.data

import android.content.Context
import ir.hamghadam.app.domain.ReminderOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("سیستم"),
    LIGHT("روشن"),
    DARK("تیره"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val persianDigits: Boolean = true,
    /** Reminder applied to new tasks that have a date; null means no reminder. */
    val defaultReminderMinutes: Int? = ReminderOptions.AT_TIME,
)

/**
 * Small settings store backed by SharedPreferences and exposed as a StateFlow so
 * Compose reacts immediately when a switch changes.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.value = read()
    }

    fun setPersianDigits(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIGITS, enabled).apply()
        _settings.value = read()
    }

    fun setDefaultReminder(minutes: Int?) {
        prefs.edit().putInt(KEY_REMINDER, minutes ?: NO_REMINDER).apply()
        _settings.value = read()
    }

    private fun read(): AppSettings {
        val theme = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM
        val reminder = prefs.getInt(KEY_REMINDER, ReminderOptions.AT_TIME)
        return AppSettings(
            themeMode = theme,
            persianDigits = prefs.getBoolean(KEY_DIGITS, true),
            defaultReminderMinutes = if (reminder == NO_REMINDER) null else reminder,
        )
    }

    private companion object {
        const val PREFS_NAME = "hamghadam.settings"
        const val KEY_THEME = "theme"
        const val KEY_DIGITS = "persian_digits"
        const val KEY_REMINDER = "default_reminder"
        const val NO_REMINDER = -1
    }
}
