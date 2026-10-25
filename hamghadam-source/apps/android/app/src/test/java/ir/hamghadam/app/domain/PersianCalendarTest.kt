package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class PersianCalendarTest {

    private val today = JalaliDate(1405, 8, 3) // Sunday

    @Test
    fun `weekday names use persian week`() {
        assertEquals("شنبه", PersianCalendar.weekdayName(DayOfWeek.SATURDAY))
        assertEquals("یک‌شنبه", PersianCalendar.weekdayName(DayOfWeek.SUNDAY))
        assertEquals("دوشنبه", PersianCalendar.weekdayName(DayOfWeek.MONDAY))
        assertEquals("جمعه", PersianCalendar.weekdayName(DayOfWeek.FRIDAY))
        assertEquals(DayOfWeek.SATURDAY, PersianCalendar.weekOrder().first())
        assertEquals(DayOfWeek.FRIDAY, PersianCalendar.weekOrder().last())
    }

    @Test
    fun `week starts on saturday`() {
        // 1405/08/03 is Sunday, so the week starts one day earlier.
        assertEquals(JalaliDate(1405, 8, 2), PersianCalendar.weekStart(today))
        val week = PersianCalendar.weekDays(today)
        assertEquals(7, week.size)
        assertEquals(DayOfWeek.SATURDAY, week.first().dayOfWeek())
        assertEquals(DayOfWeek.FRIDAY, week.last().dayOfWeek())
    }

    @Test
    fun `month grid starts on saturday and fills whole weeks`() {
        // 1405/08/01 is a Friday, so the first row has six empty cells.
        val grid = PersianCalendar.monthGrid(JalaliDate(1405, 8, 1))
        assertEquals(DayOfWeek.FRIDAY, grid.first()[6]!!.dayOfWeek())
        assertEquals(1, grid.first()[6]!!.day)
        assertNull(grid.first()[0])
        assertEquals(30, grid.flatten().count { it != null })
        assertEquals(0, grid.size % 1)
        for (week in grid) assertEquals(7, week.size)

        val firstMondayMonth = PersianCalendar.monthGrid(JalaliDate(1405, 1, 1))
        assertEquals(JalaliDate(1405, 1, 1), firstMondayMonth.first()[0])
    }

    @Test
    fun `long date renders persian digits`() {
        assertEquals("یک‌شنبه ۳ آبان ۱۴۰۵", PersianCalendar.longDate(today))
        assertEquals("یک‌شنبه 3 آبان 1405", PersianCalendar.longDate(today, persianDigits = false))
        assertEquals("آبان ۱۴۰۵", PersianCalendar.monthTitle(today))
    }

    @Test
    fun `relative labels`() {
        assertEquals("امروز", PersianCalendar.relativeDate(today, today))
        assertEquals("فردا", PersianCalendar.relativeDate(today.plusDays(1), today))
        assertEquals("دیروز", PersianCalendar.relativeDate(today.plusDays(-1), today))
        assertEquals("۱۰ آبان", PersianCalendar.relativeDate(today.plusDays(7), today))
        assertEquals("۳ آبان ۱۴۰۶", PersianCalendar.relativeDate(today.plusYears(1), today))
    }

    @Test
    fun `due label combines day and clock`() {
        assertEquals("امروز ۰۹:۳۰", PersianCalendar.dueLabel(today, 9 * 60 + 30, today))
        assertEquals("فردا", PersianCalendar.dueLabel(today.plusDays(1), null, today))
        assertNull(PersianCalendar.dueLabel(null, 600, today))
    }

    @Test
    fun `greeting follows the hour of day`() {
        assertEquals("صبح بخیر", PersianCalendar.greeting(8))
        assertEquals("ظهر بخیر", PersianCalendar.greeting(13))
        assertEquals("عصر بخیر", PersianCalendar.greeting(18))
        assertEquals("شب بخیر", PersianCalendar.greeting(23))
    }
}
