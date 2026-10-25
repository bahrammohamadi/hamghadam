package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskQueryTest {

    private data class Item(
        val id: String,
        val title: String,
        val day: Long?,
        val minute: Int?,
        val priority: Int,
        val created: Long,
        val completed: Boolean,
        val notes: String = "",
    )

    private val today = JalaliDate(1405, 8, 3)
    private val todayEpoch = today.toEpochDay()

    private fun item(
        id: String,
        title: String,
        day: JalaliDate? = null,
        minute: Int? = null,
        priority: Int = 1,
        created: Long = 0,
        completed: Boolean = false,
        notes: String = "",
    ) = Item(id, title, day?.toEpochDay(), minute, priority, created, completed, notes)

    private fun bucketsOf(items: List<Item>) = TaskQuery.bucket(
        items = items,
        todayEpochDay = todayEpoch,
        epochDay = { it.day },
        isCompleted = { it.completed },
        minuteOfDay = { it.minute },
        priority = { it.priority },
        createdAt = { it.created },
    )

    @Test
    fun `tasks are grouped by day`() {
        val items = listOf(
            item("late", "عقب‌افتاده", today.plusDays(-1)),
            item("now", "امروز", today),
            item("next", "آینده", today.plusDays(2)),
            item("none", "بدون تاریخ"),
            item("done", "انجام‌شده", today, completed = true),
        )
        val buckets = bucketsOf(items)
        assertEquals(listOf("late"), buckets.overdue.map { it.id })
        assertEquals(listOf("now"), buckets.today.map { it.id })
        assertEquals(listOf("next"), buckets.upcoming.map { it.id })
        assertEquals(listOf("none"), buckets.undated.map { it.id })
        assertEquals(listOf("done"), buckets.completed.map { it.id })
        assertEquals(4, buckets.open.size)
        assertEquals(5, buckets.totalCount)
    }

    @Test
    fun `sorting puts dated first, all day before timed, then priority`() {
        val items = listOf(
            item("noDate", "بدون تاریخ"),
            item("timed", "ساعت دار", today, minute = 15 * 60),
            item("allday", "تمام‌روز", today),
            item("high", "مهم", today, priority = 2),
            item("today2", "فردا", today.plusDays(1)),
        )
        val sorted = TaskQuery.sortForDisplay(items, { it.day }, { it.minute }, { it.priority }, { it.created })
        assertEquals(listOf("high", "allday", "timed", "today2", "noDate"), sorted.map { it.id })
    }

    @Test
    fun `search normalises persian letters and ignores case`() {
        val items = listOf(
            item("1", "جلسه با تیم"),
            item("2", "خرید نان", notes = "از نانوایی"),
            item("3", "Report"),
        )
        assertEquals(listOf("1"), TaskQuery.search(items, "جلسه") { it.title }.map { it.id })
        assertEquals(listOf("2"), TaskQuery.search(items, "نانوایی") { "${it.title} ${it.notes}" }.map { it.id })
        assertEquals(listOf("3"), TaskQuery.search(items, "report") { it.title }.map { it.id })
        assertEquals(3, TaskQuery.search(items, "   ") { it.title }.size)
        // Arabic yeh must match Persian yeh
        assertEquals(listOf("1"), TaskQuery.search(listOf(item("1", "جلسه با تيم")), "تیم") { it.title }.map { it.id })
    }

    @Test
    fun `streak survives the current day`() {
        val days = setOf(10L, 11L, 12L)
        assertEquals(3, TaskQuery.currentStreak(days, todayEpochDay = 12))
        // Today not logged yet: the streak from yesterday still counts.
        assertEquals(3, TaskQuery.currentStreak(days, todayEpochDay = 13))
        assertEquals(0, TaskQuery.currentStreak(days, todayEpochDay = 15))
        assertEquals(0, TaskQuery.currentStreak(emptySet(), todayEpochDay = 12))
    }

    @Test
    fun `best streak is the longest run`() {
        assertEquals(0, TaskQuery.bestStreak(emptySet()))
        assertEquals(1, TaskQuery.bestStreak(setOf(5L)))
        assertEquals(3, TaskQuery.bestStreak(setOf(1L, 2L, 3L, 8L, 9L)))
        assertEquals(4, TaskQuery.bestStreak(setOf(1L, 3L, 4L, 5L, 6L)))
    }

    @Test
    fun `completion percentage`() {
        assertEquals(0, TaskQuery.completionPercent(0, 0))
        assertEquals(50, TaskQuery.completionPercent(1, 2))
        assertEquals(100, TaskQuery.completionPercent(3, 3))
        assertTrue(TaskQuery.completionRatio(1, 4) == 0.25f)
    }
}
