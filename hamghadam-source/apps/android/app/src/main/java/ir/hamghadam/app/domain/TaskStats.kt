package ir.hamghadam.app.domain

/** Completed vs. due count for one day, used by the weekly overview chart. */
data class DayProgress(val epochDay: Long, val done: Int, val total: Int) {
    val percent: Int get() = if (total <= 0) 0 else ((done * 100f) / total).toInt().coerceIn(0, 100)
}

/** Aggregations shown on the "آمار" screen and in the overview cards. */
object TaskStats {

    fun <T> week(
        items: List<T>,
        todayEpochDay: Long,
        epochDay: (T) -> Long?,
        completedEpochDay: (T) -> Long?,
        days: Int = 7,
    ): List<DayProgress> {
        val from = todayEpochDay - (days - 1L)
        val dueByDay = items.mapNotNull { task -> epochDay(task)?.takeIf { it in from..todayEpochDay } }
            .groupingBy { it }
            .eachCount()
        val doneByDay = items.mapNotNull { task -> completedEpochDay(task)?.takeIf { it in from..todayEpochDay } }
            .groupingBy { it }
            .eachCount()
        return (from..todayEpochDay).map { day ->
            DayProgress(epochDay = day, done = doneByDay[day] ?: 0, total = dueByDay[day] ?: 0)
        }
    }

    fun <T> completionPercent(items: List<T>, isCompleted: (T) -> Boolean): Int =
        if (items.isEmpty()) 0 else ((items.count(isCompleted) * 100f) / items.size).toInt().coerceIn(0, 100)

    /** Top projects by open task count; [nameOf] maps a project id to a label. */
    fun <T> openPerProject(
        items: List<T>,
        isCompleted: (T) -> Boolean,
        projectId: (T) -> String?,
        nameOf: (String) -> String?,
    ): List<Pair<String, Int>> = items
        .filterNot(isCompleted)
        .mapNotNull { projectId(it) }
        .filter { nameOf(it) != null }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { nameOf(it.key) })
        .map { (id, count) -> (nameOf(id) ?: "") to count }

    /** Completion of a single day, clamped to 0..100. */
    fun dayPercent(done: Int, total: Int): Int =
        if (total <= 0) 0 else ((done * 100f) / total).toInt().coerceIn(0, 100)
}
