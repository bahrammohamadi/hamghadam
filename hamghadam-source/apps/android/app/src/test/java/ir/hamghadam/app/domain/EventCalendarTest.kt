package ir.hamghadam.app.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCalendarTest {

    private fun load(year: Int): EventYear {
        val file = File("src/main/assets/events/$year.json")
        assertTrue("فایل مناسبت‌های $year پیدا نشد: ${file.absolutePath}", file.exists())
        return EventJsonParser.parse(file.readText())
    }

    @Test
    fun `bundled feeds parse with unique ids and valid dates`() {
        for (year in listOf(1404, 1405)) {
            val parsed = load(year)
            assertEquals(year, parsed.year)
            assertTrue("برای $year رویداد کم است", parsed.events.size >= 50)
            assertEquals("شناسهٔ تکراری", parsed.events.size, parsed.events.map { it.id }.distinct().size)
            assertTrue(parsed.events.all { it.date.year == year && it.date.isValid })
            assertTrue(parsed.events.any { it.source == CalendarSource.OFFICIAL })
            assertTrue(parsed.events.any { it.source == CalendarSource.CULTURAL })
            // The official calendar marks 26 holiday days in both years.
            val holidayDays = parsed.events.filter { it.holiday }.map { it.date }.distinct()
            assertEquals("روزهای تعطیل رسمی $year", 26, holidayDays.size)
            assertTrue(parsed.events.all { it.title.isNotBlank() })
            assertTrue(parsed.events.any { it.approximate && it.lunar })
        }
    }

    @Test
    fun `official holidays include the fixed solar dates`() {
        val days1404 = load(1404).events.filter { it.holiday }.map { it.date.month to it.date.day }.toSet()
        assertTrue((1 to 1) in days1404)
        assertTrue((1 to 13) in days1404)
        assertTrue((11 to 22) in days1404)
        assertTrue((12 to 29) in days1404)

        val days1405 = load(1405).events.filter { it.holiday }.map { it.date.month to it.date.day }.toSet()
        assertTrue((1 to 1) in days1405)
        assertTrue((3 to 14) in days1405)
        assertTrue((12 to 9) in days1405)
        assertTrue((12 to 29) in days1405)
    }

    @Test
    fun `parser reads category, colour and flags`() {
        val json = """
            {
              "schema": 1,
              "year": 1405,
              "events": [
                {"id": "a", "month": 1, "day": 1, "title": "آغاز نوروز", "category": "official",
                 "holiday": true, "color": "#C8453C", "description": "تحویل سال", "lunar": false},
                {"id": "b", "month": 5, "day": 10, "title": "رویداد فرهنگی", "category": "cultural",
                 "holiday": false, "color": "#C9872B", "approximate": false, "lunar": false}
              ]
            }
        """.trimIndent()
        val parsed = EventJsonParser.parse(json)
        assertEquals(2, parsed.events.size)
        val first = parsed.events.first()
        assertEquals(CalendarSource.OFFICIAL, first.source)
        assertTrue(first.holiday)
        assertEquals(0xFFC8453C.toInt(), first.colorArgb)
        assertEquals("تحویل سال", first.description)
        assertEquals(CalendarSource.CULTURAL, parsed.events[1].source)
        assertFalse(parsed.events[1].holiday)
    }

    @Test
    fun `invalid dates and missing titles are ignored`() {
        val json = """
            {"year": 1405, "events": [
              {"month": 7, "day": 31, "title": "روز اضافه", "category": "cultural"},
              {"month": 1, "day": 1, "title": "", "category": "official"},
              {"month": 13, "day": 1, "title": "ماه نامعتبر"}
            ]}
        """.trimIndent()
        assertTrue(EventJsonParser.parse(json).events.isEmpty())
    }

    @Test
    fun `colour parsing accepts six and eight hex digits`() {
        assertNull(EventJsonParser.colorFromHex("nope"))
        assertNull(EventJsonParser.colorFromHex("#12345"))
        assertEquals(0xFFC8453C.toInt(), EventJsonParser.colorFromHex("#C8453C"))
        assertEquals(0x80C8453C.toInt(), EventJsonParser.colorFromHex("#80C8453C"))
    }

    @Test
    fun `yearly personal events repeat but leap day does not`() {
        val leap = PersonalEvent(
            id = "leap",
            title = "آخر اسفند",
            date = JalaliDate(1403, 12, 30),
            yearlyRepeat = true,
        )
        assertNotNull(leap.occurrenceIn(1404))
        // 1405 is not a leap year: 30 Esfand does not exist.
        assertNull(leap.occurrenceIn(1405))

        val normal = PersonalEvent(id = "n", title = "سالگرد", date = JalaliDate(1404, 5, 10), yearlyRepeat = true)
        assertEquals(JalaliDate(1407, 5, 10), normal.occurrenceIn(1407))

        val once = PersonalEvent(id = "o", title = "یک‌بار", date = JalaliDate(1404, 5, 10))
        assertNull(once.occurrenceIn(1405))
        assertEquals(JalaliDate(1404, 5, 10), once.occurrenceIn(1404))
    }

    @Test
    fun `projection groups by day and respects active sources`() {
        val year = 1405
        val personal = listOf(
            PersonalEvent(id = "p1", title = "رویداد من", date = JalaliDate(year, 2, 2), colorArgb = 0xFF4E8C6A.toInt()),
        )
        val events = load(year).events + EventProjection.expandPersonal(personal, year)

        val byDay = EventProjection.byDay(events, year, 2)
        val day = JalaliDate(year, 2, 2)
        assertTrue(byDay[day.toEpochDay()].orEmpty().any { it.source == CalendarSource.PERSONAL })

        val enabled = EventProjection.enabled(events, setOf(CalendarSource.OFFICIAL.id))
        assertTrue(enabled.none { it.source == CalendarSource.PERSONAL })
        assertTrue(enabled.all { it.source == CalendarSource.OFFICIAL })

        val dots = EventProjection.dotColors(byDay[day.toEpochDay()].orEmpty())
        assertTrue(dots.isNotEmpty())
        assertTrue(dots.size <= 3)
        assertEquals(dots.size, dots.distinct().size)
    }
}
