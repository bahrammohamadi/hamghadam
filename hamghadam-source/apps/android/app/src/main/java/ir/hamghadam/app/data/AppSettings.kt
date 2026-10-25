package ir.hamghadam.app.data

import android.content.Context
import ir.hamghadam.app.domain.CalendarSource
import ir.hamghadam.app.domain.ReminderOptions
import ir.hamghadam.app.ui.theme.AccentColor
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
    /** Which event sources are drawn in the month view. */
    val calendarSources: Set<String> = CalendarSource.defaultEnabled,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val focusGoalMinutes: Int = 50,
    /** OAuth client id typed by the user; empty means Google sync is off. */
    val googleClientId: String = "",
    val googleConnected: Boolean = false,
    /** Accent used by buttons, navigation and links. */
    val accent: AccentColor = AccentColor.VIOLET,
    /** Compact density trims card padding for small screens. */
    val compact: Boolean = false,
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
        refresh()
    }

    fun setPersianDigits(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIGITS, enabled).apply()
        refresh()
    }

    fun setDefaultReminder(minutes: Int?) {
        prefs.edit().putInt(KEY_REMINDER, minutes ?: NO_REMINDER).apply()
        refresh()
    }

    fun setCalendarSource(source: CalendarSource, enabled: Boolean) {
        val current = read().calendarSources.toMutableSet()
        if (enabled) current += source.id else current -= source.id
        // At least one source stays on so the calendar is never inexplicably empty.
        val safe = if (current.isEmpty()) CalendarSource.defaultEnabled else current
        prefs.edit().putStringSet(KEY_SOURCES, safe).apply()
        refresh()
    }

    fun setFocusMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_FOCUS_MINUTES, minutes.coerceIn(5, 120)).apply()
        refresh()
    }

    fun setBreakMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_BREAK_MINUTES, minutes.coerceIn(1, 30)).apply()
        refresh()
    }

    fun setFocusGoalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_FOCUS_GOAL, minutes.coerceIn(10, 600)).apply()
        refresh()
    }

    fun setGoogleClientId(clientId: String) {
        prefs.edit().putString(KEY_GOOGLE_CLIENT, clientId.trim()).apply()
        refresh()
    }

    fun setAccent(accent: AccentColor) {
        prefs.edit().putString(KEY_ACCENT, accent.name).apply()
        refresh()
    }

    fun setCompact(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT, enabled).apply()
        refresh()
    }

    fun setGoogleConnected(connected: Boolean) {
        prefs.edit().putBoolean(KEY_GOOGLE_CONNECTED, connected).apply()
        refresh()
    }

    private fun refresh() {
        _settings.value = read()
    }

    private fun read(): AppSettings {
        val theme = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM
        val reminder = prefs.getInt(KEY_REMINDER, ReminderOptions.AT_TIME)
        val sources = prefs.getStringSet(KEY_SOURCES, CalendarSource.defaultEnabled)
            ?.let { CalendarSource.sanitizeSources(it) }
            ?: CalendarSource.defaultEnabled
        return AppSettings(
            themeMode = theme,
            persianDigits = prefs.getBoolean(KEY_DIGITS, true),
            defaultReminderMinutes = if (reminder == NO_REMINDER) null else reminder,
            calendarSources = sources,
            focusMinutes = prefs.getInt(KEY_FOCUS_MINUTES, 25),
            breakMinutes = prefs.getInt(KEY_BREAK_MINUTES, 5),
            focusGoalMinutes = prefs.getInt(KEY_FOCUS_GOAL, 50),
            googleClientId = prefs.getString(KEY_GOOGLE_CLIENT, "").orEmpty(),
            googleConnected = prefs.getBoolean(KEY_GOOGLE_CONNECTED, false),
            accent = AccentColor.fromName(prefs.getString(KEY_ACCENT, null)),
            compact = prefs.getBoolean(KEY_COMPACT, false),
        )
    }

    companion object {
        private const val PREFS_NAME = "hamghadam.settings"
        private const val KEY_THEME = "theme"
        private const val KEY_DIGITS = "persian_digits"
        private const val KEY_REMINDER = "default_reminder"
        private const val KEY_SOURCES = "calendar_sources"
        private const val KEY_FOCUS_MINUTES = "focus_minutes"
        private const val KEY_BREAK_MINUTES = "break_minutes"
        private const val KEY_FOCUS_GOAL = "focus_goal"
        private const val KEY_GOOGLE_CLIENT = "google_client_id"
        private const val KEY_GOOGLE_CONNECTED = "google_connected"
        private const val KEY_ACCENT = "accent"
        private const val KEY_COMPACT = "compact"
        private const val NO_REMINDER = -1
    }
}
