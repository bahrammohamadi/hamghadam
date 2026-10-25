package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusStatsTest {

    private fun record(day: Long, minutes: Int, completed: Boolean = true, id: String = "s-$day-$minutes") =
        FocusRecord(id = id, epochDay = day, seconds = minutes * 60, completed = completed)

    @Test
    fun `minutes and sessions are grouped per day`() {
        val records = listOf(
            record(day = 100, minutes = 25),
            record(day = 100, minutes = 15, completed = false, id = "partial"),
            record(day = 99, minutes = 50),
        )
        assertEquals(40, FocusStats.minutesOnDay(records, 100))
        assertEquals(1, FocusStats.sessionsOnDay(records, 100))
        assertEquals(90, FocusStats.totalMinutes(records))
        assertEquals(0, FocusStats.minutesOnDay(records, 98))
    }

    @Test
    fun `week series is oldest first and has a fixed length`() {
        val records = listOf(record(day = 10, minutes = 25), record(day = 8, minutes = 50))
        val week = FocusStats.week(records, todayEpochDay = 10, days = 7)
        assertEquals(7, week.size)
        assertEquals(4L, week.first().epochDay)
        assertEquals(10L, week.last().epochDay)
        assertEquals(25, week.last().minutes)
        assertEquals(50, week[4].minutes)
        assertEquals(50, FocusStats.maxMinutes(week))
    }

    @Test
    fun `streak counts consecutive days and tolerates an empty today`() {
        val records = listOf(
            record(day = 10, minutes = 25),
            record(day = 9, minutes = 25),
            record(day = 8, minutes = 25),
            record(day = 5, minutes = 25),
        )
        assertEquals(3, FocusStats.dayStreak(records, todayEpochDay = 10))
        assertEquals(3, FocusStats.dayStreak(records, todayEpochDay = 11))
        assertEquals(2, FocusStats.dayStreak(records, todayEpochDay = 9))
        assertEquals(0, FocusStats.dayStreak(emptyList(), todayEpochDay = 10))
    }

    @Test
    fun `very short sessions do not start a streak`() {
        val records = listOf(FocusRecord(id = "s", epochDay = 10, seconds = 30))
        assertEquals(0, FocusStats.dayStreak(records, todayEpochDay = 10))
    }

    @Test
    fun `timer progress is clamped`() {
        assertEquals(0.0, FocusTimerState(totalSeconds = 1500, remainingSeconds = 1500).progress.toDouble(), 0.001)
        assertEquals(0.5, FocusTimerState(totalSeconds = 1500, remainingSeconds = 750).progress.toDouble(), 0.001)
        assertEquals(1.0, FocusTimerState(totalSeconds = 1500, remainingSeconds = -10).progress.toDouble(), 0.001)
        assertEquals(0.0, FocusTimerState(totalSeconds = 0, remainingSeconds = 0).progress.toDouble(), 0.001)
    }

    @Test
    fun `suggested break grows after four sessions`() {
        assertEquals(5, FocusStats.suggestedBreakMinutes(completedToday = 1, shortBreak = 5, longBreak = 15))
        assertEquals(15, FocusStats.suggestedBreakMinutes(completedToday = 4, shortBreak = 5, longBreak = 15))
        assertEquals(5, FocusStats.suggestedBreakMinutes(completedToday = 5, shortBreak = 5, longBreak = 15))
    }
}
