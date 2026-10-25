package ir.hamghadam.app.domain

/** Tasks grouped for the "today" screen. Every list keeps the display order. */
data class TaskBuckets<T>(
    val overdue: List<T>,
    val today: List<T>,
    val upcoming: List<T>,
    val undated: List<T>,
    val completed: List<T>,
) {
    val open: List<T> get() = overdue + today + upcoming + undated
    val totalCount: Int get() = open.size + completed.size
}

/**
 * Pure list operations used by the UI. They take accessor lambdas instead of a
 * concrete entity so they can be unit tested without Android or Room.
 */
object TaskQuery {

    fun <T> sortForDisplay(
        items: List<T>,
        epochDay: (T) -> Long?,
        minuteOfDay: (T) -> Int?,
        priority: (T) -> Int,
        createdAt: (T) -> Long,
    ): List<T> = items.sortedWith(
        compareBy<T> { epochDay(it) ?: Long.MAX_VALUE }
            .thenBy { minuteOfDay(it) ?: -1 }
            .thenByDescending { priority(it) }
            .thenByDescending { createdAt(it) },
    )

    fun <T> bucket(
        items: List<T>,
        todayEpochDay: Long,
        epochDay: (T) -> Long?,
        isCompleted: (T) -> Boolean,
        minuteOfDay: (T) -> Int? = { null },
        priority: (T) -> Int = { 1 },
        createdAt: (T) -> Long = { 0L },
    ): TaskBuckets<T> {
        val (done, open) = items.partition { isCompleted(it) }
        val sortedOpen = sortForDisplay(open, epochDay, minuteOfDay, priority, createdAt)
        val (dated, undated) = sortedOpen.partition { epochDay(it) != null }
        val (past, rest) = dated.partition { (epochDay(it) ?: 0L) < todayEpochDay }
        val (today, future) = rest.partition { epochDay(it) == todayEpochDay }
        return TaskBuckets(
            overdue = past,
            today = today,
            upcoming = future,
            undated = undated,
            completed = done.sortedByDescending { createdAt(it) },
        )
    }

    /** All query tokens must appear in the text; Persian letter variants are ignored. */
    fun <T> search(items: List<T>, query: String, text: (T) -> String): List<T> {
        val tokens = PersianText.normalizeForMatching(query).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return items
        return items.filter { item ->
            val haystack = PersianText.normalizeForMatching(text(item))
            tokens.all { haystack.contains(it) }
        }
    }

    /**
     * Consecutive days ending today (or yesterday, so a streak survives until the
     * end of the current day without being marked as broken).
     */
    fun currentStreak(loggedDays: Set<Long>, todayEpochDay: Long): Int {
        var cursor = if (loggedDays.contains(todayEpochDay)) todayEpochDay else todayEpochDay - 1
        var streak = 0
        while (loggedDays.contains(cursor)) {
            streak += 1
            cursor -= 1
        }
        return streak
    }

    /** Longest run of consecutive days in [loggedDays]; used for habit statistics. */
    fun bestStreak(loggedDays: Set<Long>): Int {
        if (loggedDays.isEmpty()) return 0
        val sorted = loggedDays.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current += 1
            } else {
                current = 1
            }
            if (current > best) best = current
        }
        return best
    }

    fun completionRatio(done: Int, total: Int): Float = if (total <= 0) 0f else done.toFloat() / total.toFloat()

    /** Percentage rounded for display, clamped to 0..100. */
    fun completionPercent(done: Int, total: Int): Int =
        if (total <= 0) 0 else ((done * 100f) / total).toInt().coerceIn(0, 100)
}
