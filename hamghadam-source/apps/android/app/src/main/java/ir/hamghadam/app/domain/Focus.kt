package ir.hamghadam.app.domain

/** One finished (or abandoned) focus block. Seconds are wall-clock seconds spent. */
data class FocusRecord(
    val id: String,
    val taskId: String? = null,
    val epochDay: Long,
    val seconds: Int,
    val completed: Boolean = true,
    val startedAtEpochMillis: Long = 0L,
)

/** A single day of focus minutes, used by the weekly chart. */
data class FocusDay(val epochDay: Long, val minutes: Int, val sessions: Int)

object FocusStats {

    fun minutes(seconds: Int): Int = seconds / 60

    fun onDay(records: List<FocusRecord>, epochDay: Long): List<FocusRecord> =
        records.filter { it.epochDay == epochDay }

    fun minutesOnDay(records: List<FocusRecord>, epochDay: Long): Int =
        minutes(onDay(records, epochDay).sumOf { it.seconds.coerceAtLeast(0) })

    fun sessionsOnDay(records: List<FocusRecord>, epochDay: Long): Int =
        onDay(records, epochDay).count { it.completed }

    fun totalMinutes(records: List<FocusRecord>): Int = minutes(records.sumOf { it.seconds.coerceAtLeast(0) })

    /** Minutes for the last [days] days ending at [todayEpochDay], oldest first. */
    fun week(records: List<FocusRecord>, todayEpochDay: Long, days: Int = 7): List<FocusDay> {
        val from = todayEpochDay - (days - 1L)
        val grouped = records
            .filter { it.epochDay in from..todayEpochDay }
            .groupBy { it.epochDay }
        return (from..todayEpochDay).map { day ->
            val entries = grouped[day].orEmpty()
            FocusDay(
                epochDay = day,
                minutes = minutes(entries.sumOf { it.seconds.coerceAtLeast(0) }),
                sessions = entries.count { it.completed },
            )
        }
    }

    /** Best single day inside the window, used to scale the bar chart. */
    fun maxMinutes(days: List<FocusDay>): Int = days.maxOfOrNull { it.minutes } ?: 0

    /** Streak of consecutive days (ending today or yesterday) with at least one minute. */
    fun dayStreak(records: List<FocusRecord>, todayEpochDay: Long): Int {
        val active = records.filter { it.seconds >= 60 }.map { it.epochDay }.toSet()
        var cursor = if (active.contains(todayEpochDay)) todayEpochDay else todayEpochDay - 1
        var streak = 0
        while (active.contains(cursor)) {
            streak += 1
            cursor -= 1
        }
        return streak
    }

    /** Suggested next break, growing slightly after every two finished blocks. */
    fun suggestedBreakMinutes(completedToday: Int, shortBreak: Int, longBreak: Int): Int =
        if (completedToday > 0 && completedToday % 4 == 0) longBreak else shortBreak
}

/** The live timer state kept in the ViewModel so it survives tab switches. */
data class FocusTimerState(
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val running: Boolean = false,
    val onBreak: Boolean = false,
    val taskId: String? = null,
    val taskTitle: String? = null,
    val completedToday: Int = 0,
    val minutesToday: Int = 0,
    val finishedAtEpochMillis: Long = 0L,
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val hasStarted: Boolean get() = remainingSeconds != totalSeconds || running
}
