package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TagsTest {

    @Test
    fun `csv parsing normalises separators, hashes and duplicates`() {
        val parsed = Tags.parseCsv(" #کار، خرید ;  کار ;  ")
        assertEquals(listOf("کار", "خرید"), parsed)
        assertEquals("کار,خرید", Tags.toCsv(parsed))
    }

    @Test
    fun `empty and blank values produce no tags`() {
        assertTrue(Tags.parseCsv(null).isEmpty())
        assertTrue(Tags.parseCsv("   ").isEmpty())
        assertTrue(Tags.parseCsv(",,،;").isEmpty())
        assertNull(Tags.normalizeTag("  #  "))
    }

    @Test
    fun `add and remove keep order and respect the cap`() {
        assertEquals("خرید,کار", Tags.add("خرید", "کار"))
        assertEquals("خرید,کار", Tags.add("خرید,کار", "خرید"))
        assertEquals("کار", Tags.remove("خرید,کار", "خرید"))

        var csv = ""
        for (index in 1..10) csv = Tags.add(csv, "تگ$index")
        assertEquals(Tags.MAX_TAGS, Tags.parseCsv(csv).size)
    }

    @Test
    fun `hashtags are extracted from quick add text`() {
        val (text, tags) = Tags.extractFromQuickAdd("خرید نان #خانه #هفتگی")
        assertEquals("خرید نان", text)
        assertEquals(listOf("خانه", "هفتگی"), tags)
    }

    @Test
    fun `quick add parser keeps tags out of the title`() {
        val result = QuickAddParser.parse("خرید نان #خانه فردا ساعت ۹", JalaliDate(1405, 8, 3))
        assertEquals("خرید نان", result.title)
        assertEquals(listOf("خانه"), result.tags)
        assertEquals(JalaliDate(1405, 8, 4), result.date)
        assertEquals(LocalTime.of(9, 0), result.time)
        assertEquals(false, result.isPlainText)
    }

    @Test
    fun `tag counts are sorted by frequency`() {
        val counts = Tags.counts(listOf("کار,خرید", "کار", "خانه"))
        assertEquals(listOf("کار" to 2, "خانه" to 1, "خرید" to 1), counts)
    }
}
