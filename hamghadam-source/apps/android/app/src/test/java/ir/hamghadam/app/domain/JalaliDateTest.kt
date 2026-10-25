package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Reference vectors were generated with `jdatetime` / `convertdate` (Python) and
 * cross-checked against `jalaali-js` semantics. The full 1850..2150 range was
 * compared day by day during development; the tests below keep a representative
 * slice so CI stays fast while still guarding every boundary class (Nowruz,
 * month 6/7 changeover, leap Esfand, far future dates).
 */
class JalaliDateTest {

    private val vectors: List<Pair<String, JalaliDate>> = listOf(
        "1971-03-21" to JalaliDate(1350, 1, 1),
        "1971-09-22" to JalaliDate(1350, 6, 31),
        "1971-09-23" to JalaliDate(1350, 7, 1),
        "1972-03-19" to JalaliDate(1350, 12, 29),
        "1981-03-21" to JalaliDate(1360, 1, 1),
        "1981-09-22" to JalaliDate(1360, 6, 31),
        "1981-09-23" to JalaliDate(1360, 7, 1),
        "1982-03-20" to JalaliDate(1360, 12, 29),
        "1991-03-21" to JalaliDate(1370, 1, 1),
        "1991-09-22" to JalaliDate(1370, 6, 31),
        "1991-09-23" to JalaliDate(1370, 7, 1),
        "1992-03-19" to JalaliDate(1370, 12, 29),
        "2000-03-20" to JalaliDate(1379, 1, 1),
        "2000-09-21" to JalaliDate(1379, 6, 31),
        "2000-09-22" to JalaliDate(1379, 7, 1),
        "2001-03-19" to JalaliDate(1379, 12, 29),
        "2001-03-21" to JalaliDate(1380, 1, 1),
        "2001-09-22" to JalaliDate(1380, 6, 31),
        "2001-09-23" to JalaliDate(1380, 7, 1),
        "2002-03-20" to JalaliDate(1380, 12, 29),
        "2011-03-21" to JalaliDate(1390, 1, 1),
        "2011-09-22" to JalaliDate(1390, 6, 31),
        "2011-09-23" to JalaliDate(1390, 7, 1),
        "2012-03-19" to JalaliDate(1390, 12, 29),
        "2016-03-20" to JalaliDate(1395, 1, 1),
        "2016-09-21" to JalaliDate(1395, 6, 31),
        "2016-09-22" to JalaliDate(1395, 7, 1),
        "2017-03-19" to JalaliDate(1395, 12, 29),
        "2020-03-20" to JalaliDate(1399, 1, 1),
        "2020-09-21" to JalaliDate(1399, 6, 31),
        "2020-09-22" to JalaliDate(1399, 7, 1),
        "2021-03-19" to JalaliDate(1399, 12, 29),
        "2021-03-21" to JalaliDate(1400, 1, 1),
        "2021-09-22" to JalaliDate(1400, 6, 31),
        "2021-09-23" to JalaliDate(1400, 7, 1),
        "2022-03-20" to JalaliDate(1400, 12, 29),
        "2024-03-20" to JalaliDate(1403, 1, 1),
        "2024-09-21" to JalaliDate(1403, 6, 31),
        "2024-09-22" to JalaliDate(1403, 7, 1),
        "2025-03-19" to JalaliDate(1403, 12, 29),
        "2025-03-21" to JalaliDate(1404, 1, 1),
        "2025-09-22" to JalaliDate(1404, 6, 31),
        "2025-09-23" to JalaliDate(1404, 7, 1),
        "2026-03-20" to JalaliDate(1404, 12, 29),
        "2026-03-21" to JalaliDate(1405, 1, 1),
        "2026-09-22" to JalaliDate(1405, 6, 31),
        "2026-09-23" to JalaliDate(1405, 7, 1),
        "2027-03-20" to JalaliDate(1405, 12, 29),
        "2031-03-21" to JalaliDate(1410, 1, 1),
        "2031-09-22" to JalaliDate(1410, 6, 31),
        "2031-09-23" to JalaliDate(1410, 7, 1),
        "2032-03-19" to JalaliDate(1410, 12, 29),
        "2041-03-20" to JalaliDate(1420, 1, 1),
        "2041-09-21" to JalaliDate(1420, 6, 31),
        "2041-09-22" to JalaliDate(1420, 7, 1),
        "2042-03-19" to JalaliDate(1420, 12, 29),
        "2061-03-20" to JalaliDate(1440, 1, 1),
        "2061-09-21" to JalaliDate(1440, 6, 31),
        "2061-09-22" to JalaliDate(1440, 7, 1),
        "2062-03-19" to JalaliDate(1440, 12, 29),
        "1900-03-21" to JalaliDate(1279, 1, 1),
        "1979-02-11" to JalaliDate(1357, 11, 22),
        "1999-12-31" to JalaliDate(1378, 10, 10),
        "2000-02-29" to JalaliDate(1378, 12, 10),
        "2021-03-20" to JalaliDate(1399, 12, 30),
        "2024-03-19" to JalaliDate(1402, 12, 29),
        "2026-10-25" to JalaliDate(1405, 8, 3),
        "2038-01-19" to JalaliDate(1416, 10, 30),
        "2099-12-31" to JalaliDate(1478, 10, 11),
    )

    @Test
    fun `gregorian to jalali matches reference vectors`() {
        for ((iso, expected) in vectors) {
            assertEquals("Gregorian $iso", expected, JalaliDate.fromGregorian(LocalDate.parse(iso)))
        }
    }

    @Test
    fun `jalali to gregorian matches reference vectors`() {
        for ((iso, jalali) in vectors) {
            assertEquals("Jalali $jalali", LocalDate.parse(iso), jalali.toGregorian())
        }
    }

    @Test
    fun `round trip every day for eighty years`() {
        var epochDay = LocalDate.of(1930, 1, 1).toEpochDay()
        val end = LocalDate.of(2010, 12, 31).toEpochDay()
        while (epochDay <= end) {
            val jalali = JalaliDate.fromEpochDay(epochDay)
            assertEquals("epoch day $epochDay -> $jalali", epochDay, jalali.toEpochDay())
            assertTrue("$jalali must be a valid date", jalali.isValid)
            epochDay += 1
        }
    }

    @Test
    fun `leap years follow the 33 year cycle`() {
        val leapYears = listOf(
            1350, 1354, 1358, 1362, 1366, 1370, 1375, 1379, 1383, 1387,
            1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420, 1424, 1428,
            1432, 1436, 1441, 1445, 1449,
        )
        for (year in 1350..1450) {
            assertEquals("leap($year)", year in leapYears, JalaliDate.isLeapYear(year))
        }
    }

    @Test
    fun `month lengths are 31 then 30 then 29 or 30`() {
        for (month in 1..6) assertEquals(31, JalaliDate.monthLength(1404, month))
        for (month in 7..11) assertEquals(30, JalaliDate.monthLength(1404, month))
        assertEquals(29, JalaliDate.monthLength(1404, 12))
        assertEquals(30, JalaliDate.monthLength(1403, 12))
        assertEquals(30, JalaliDate.monthLength(1408, 12))
    }

    @Test
    fun `invalid day in month is reported`() {
        assertFalse(JalaliDate(1404, 12, 30).isValid)
        assertTrue(JalaliDate(1403, 12, 30).isValid)
        assertFalse(JalaliDate(1404, 7, 31).isValid)
    }

    @Test
    fun `adding months clamps to the last day`() {
        assertEquals(JalaliDate(1404, 7, 30), JalaliDate(1404, 6, 31).plusMonths(1))
        assertEquals(JalaliDate(1404, 8, 30), JalaliDate(1404, 6, 31).plusMonths(2))
        assertEquals(JalaliDate(1403, 12, 30), JalaliDate(1403, 11, 30).plusMonths(1))
        assertEquals(JalaliDate(1404, 12, 29), JalaliDate(1404, 11, 30).plusMonths(1))
        assertEquals(JalaliDate(1406, 1, 15), JalaliDate(1405, 11, 15).plusMonths(2))
        assertEquals(JalaliDate(1404, 12, 29), JalaliDate(1405, 1, 29).plusMonths(-1))
    }

    @Test
    fun `adding years clamps esfand`() {
        assertEquals(JalaliDate(1404, 12, 29), JalaliDate(1403, 12, 30).plusYears(1))
        assertEquals(JalaliDate(1408, 12, 30), JalaliDate(1403, 12, 30).plusYears(5))
    }

    @Test
    fun `adding days crosses month and year boundaries`() {
        assertEquals(JalaliDate(1405, 1, 1), JalaliDate(1404, 12, 29).plusDays(1))
        assertEquals(JalaliDate(1404, 7, 1), JalaliDate(1404, 6, 31).plusDays(1))
        assertEquals(JalaliDate(1404, 6, 31), JalaliDate(1404, 7, 1).plusDays(-1))
    }

    @Test
    fun `weekday matches java time`() {
        for ((iso, jalali) in vectors) {
            assertEquals(LocalDate.parse(iso).dayOfWeek, jalali.dayOfWeek())
        }
    }

    @Test
    fun `parse slash accepts common formats`() {
        assertEquals(JalaliDate(1404, 7, 12), JalaliDate.parseSlash("1404/07/12"))
        assertEquals(JalaliDate(1404, 7, 12), JalaliDate.parseSlash("1404-7-12"))
        assertEquals(JalaliDate(1404, 7, 12), JalaliDate.parseSlash(" 1404.7.12 "))
        assertNull(JalaliDate.parseSlash("1404/13/01"))
        assertNull(JalaliDate.parseSlash("1404/12/31"))
        assertNull(JalaliDate.parseSlash("hello"))
        assertNull(JalaliDate.parseSlash("1404/07"))
    }

    @Test
    fun `comparison sorts chronologically`() {
        val sorted = listOf(
            JalaliDate(1405, 1, 1),
            JalaliDate(1404, 12, 29),
            JalaliDate(1404, 12, 28),
            JalaliDate(1405, 1, 2),
        ).sorted()
        assertEquals(
            listOf(1404, 1404, 1405, 1405).zip(listOf(12, 12, 1, 1)).zip(listOf(28, 29, 1, 2)).map { (ym, d) ->
                JalaliDate(ym.first, ym.second, d)
            },
            sorted,
        )
    }
}
