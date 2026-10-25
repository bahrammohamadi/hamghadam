package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepeatRuleTest {

    @Test
    fun `daily weekly monthly next dates`() {
        val start = JalaliDate(1404, 6, 31)
        assertEquals(JalaliDate(1404, 7, 1), RepeatRule.DAILY.nextDate(start))
        assertEquals(JalaliDate(1404, 7, 7), RepeatRule.WEEKLY.nextDate(start))
        assertEquals(JalaliDate(1404, 7, 30), RepeatRule.MONTHLY.nextDate(start))
        assertNull(RepeatRule.NONE.nextDate(start))
    }

    @Test
    fun `monthly repeat clamps to the shorter month`() {
        assertEquals(JalaliDate(1404, 7, 30), JalaliDate(1404, 6, 31).plusMonths(1))
        assertEquals(JalaliDate(1404, 8, 30), JalaliDate(1404, 7, 30).plusMonths(1))
        assertEquals(JalaliDate(1404, 12, 29), JalaliDate(1404, 11, 30).plusMonths(1))
        assertEquals(JalaliDate(1405, 1, 29), JalaliDate(1404, 12, 29).plusMonths(1))
    }

    @Test
    fun `storage values round trip`() {
        for (rule in RepeatRule.entries) {
            assertEquals(rule, RepeatRule.fromStorage(rule.storageValue))
        }
        assertEquals(RepeatRule.NONE, RepeatRule.fromStorage(null))
        assertEquals(RepeatRule.NONE, RepeatRule.fromStorage("garbage"))
    }

    @Test
    fun `reminder labels`() {
        assertEquals("بدون یادآور", ReminderOptions.label(null))
        assertEquals("در زمان موعد", ReminderOptions.label(ReminderOptions.AT_TIME))
        assertEquals("یک ساعت قبل", ReminderOptions.label(ReminderOptions.ONE_HOUR))
        assertEquals("یک روز قبل", ReminderOptions.label(ReminderOptions.ONE_DAY))
        assertEquals("۱۵ دقیقه قبل", ReminderOptions.label(ReminderOptions.FIFTEEN_MINUTES))
    }
}
