package ir.hamghadam.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.hamghadam.app.MainActivity
import ir.hamghadam.app.R
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.ProfileStore
import ir.hamghadam.app.data.SettingsStore
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * «امروز» widget — a 4×2 glance view of the active account.
 *
 * Widgets run in the launcher process and must never touch Room on the main
 * thread, so every refresh works on a background scope and finishes through the
 * broadcast's `goAsync()` token.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetUpdater.refreshToday(appContext, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    /** Tapping the add button opens the app on the tasks tab. */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_ROUTE, MainActivity.ROUTE_QUICK_ADD)
                },
            )
        }
    }

    companion object {
        const val ACTION_ADD = "ir.hamghadam.app.widget.ADD"

        fun addPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            7801,
            Intent(context, TodayWidgetProvider::class.java).apply { action = ACTION_ADD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/**
 * «تمرکز» widget — 2×2, today's focus minutes against the daily goal plus a
 * one-tap 25-minute session.
 */
class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetUpdater.refreshFocus(appContext, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_START -> {
                val launch = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_ROUTE, MainActivity.ROUTE_START_FOCUS)
                }
                context.startActivity(launch)
            }
        }
    }

    companion object {
        const val ACTION_START = "ir.hamghadam.app.widget.START_FOCUS"

        fun startPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            7802,
            Intent(context, FocusWidgetProvider::class.java).apply { action = ACTION_START },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Shared refresh logic so widgets and the app agree on the numbers. */
object WidgetUpdater {

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val todayIds = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
        val focusIds = manager.getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java))
        if (todayIds.isEmpty() && focusIds.isEmpty()) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (todayIds.isNotEmpty()) refreshToday(context, manager, todayIds)
            if (focusIds.isNotEmpty()) refreshFocus(context, manager, focusIds)
        }
    }

    suspend fun refreshToday(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val digits = SettingsStore(context).settings.value.persianDigits
        val profileId = ProfileStore(context).activeId
        val today = JalaliDate.today()
        val database = AppDatabase.get(context)
        val tasks = runCatching { database.taskDao().loadAll() }.getOrDefault(emptyList())
        val mine = tasks.filter { it.profileId == profileId }
        val open = mine.filter { !it.isCompleted }
        val dueToday = open.filter { it.dueEpochDay != null && it.dueEpochDay == today.toEpochDay() }
        val next = (
            dueToday.sortedBy { it.dueMinuteOfDay ?: 24 * 60 } +
                open.filter { it.dueEpochDay != null && it.dueEpochDay!! > today.toEpochDay() }
                    .sortedWith(compareBy({ it.dueEpochDay }, { it.dueMinuteOfDay ?: 24 * 60 }))
            ).take(3)

        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setTextViewText(
            R.id.widget_date_pill,
            PersianCalendar.weekdayAndDay(today, digits),
        )
        views.setTextViewText(
            R.id.widget_open_count,
            if (open.isEmpty()) {
                "همه‌چیز انجام شده"
            } else {
                "${PersianText.number(open.size, digits)} کار باز • ${PersianText.number(dueToday.size, digits)} امروز"
            },
        )
        views.setViewVisibility(R.id.widget_task_1, android.view.View.VISIBLE)
        views.setTextViewText(
            R.id.widget_task_1,
            next.firstOrNull()?.let { taskLabel(it, digits) } ?: "کاری برای امروز نیست 🌿",
        )
        listOf(2, 3).forEachIndexed { index, order ->
            val id = if (order == 2) R.id.widget_task_2 else R.id.widget_task_3
            val task = next.getOrNull(index + 1)
            views.setViewVisibility(id, if (task == null) android.view.View.GONE else android.view.View.VISIBLE)
            if (task != null) views.setTextViewText(id, taskLabel(task, digits))
        }

        val openIntent = PendingIntent.getActivity(
            context,
            7803,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_today_root, openIntent)
        views.setOnClickPendingIntent(R.id.widget_add, TodayWidgetProvider.addPendingIntent(context))
        for (id in ids) manager.updateAppWidget(id, views)
    }

    suspend fun refreshFocus(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val settings = SettingsStore(context).settings.value
        val digits = settings.persianDigits
        val profileId = ProfileStore(context).activeId
        val today = JalaliDate.today().toEpochDay()
        val database = AppDatabase.get(context)
        val sessions = runCatching { database.focusSessionDao().loadAll() }.getOrDefault(emptyList())
        val minutes = sessions
            .filter { it.profileId == profileId && it.epochDay == today && it.completed }
            .sumOf { it.seconds } / 60

        val views = RemoteViews(context.packageName, R.layout.widget_focus)
        views.setTextViewText(R.id.widget_focus_minutes, "${PersianText.number(minutes, digits)} دقیقه")
        views.setTextViewText(
            R.id.widget_focus_goal,
            if (minutes >= settings.focusGoalMinutes) {
                "هدف روزانه انجام شد ✓"
            } else {
                "هدف: ${PersianText.number(settings.focusGoalMinutes, digits)} دقیقه"
            },
        )
        views.setOnClickPendingIntent(R.id.widget_focus_root, openIntent(context, 7804))
        views.setOnClickPendingIntent(R.id.widget_focus_start, FocusWidgetProvider.startPendingIntent(context))
        for (id in ids) manager.updateAppWidget(id, views)
    }

    private fun openIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun taskLabel(task: ir.hamghadam.app.data.TaskEntity, digits: Boolean): String {
        val time = task.dueMinuteOfDay?.let { PersianText.clock(it / 60, it % 60) }
        val day = task.dueDate
        return buildString {
            if (time != null) append("$time  ")
            append(task.title)
            if (day != null && day.toEpochDay() > JalaliDate.today().toEpochDay()) {
                append("  •  ")
                append(PersianCalendar.shortDate(day, digits))
            }
        }
    }
}
