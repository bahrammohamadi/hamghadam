package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checklist storage (introduced in 0.7.0) — the format is persisted, so it is pinned by tests. */
class SubtaskTest {

    @Test
    fun `encode and decode keep order, titles and state`() {
        val items = listOf(
            Subtask(id = "a", title = "خرید نان", done = true),
            Subtask(id = "b", title = "زنگ به مادر", done = false),
        )
        val restored = SubtaskCodec.decode(SubtaskCodec.encode(items))
        assertEquals(items, restored)
    }

    @Test
    fun `empty checklist is stored as an empty string`() {
        assertEquals("", SubtaskCodec.encode(emptyList()))
        assertTrue(SubtaskCodec.decode("").isEmpty())
        assertTrue(SubtaskCodec.decode(null).isEmpty())
    }

    @Test
    fun `corrupt json never throws`() {
        assertTrue(SubtaskCodec.decode("{{{ not json").isEmpty())
        assertTrue(SubtaskCodec.decode("""{"title":"x"}""").isEmpty())
    }

    @Test
    fun `items without an id get one and duplicates are dropped`() {
        val decoded = SubtaskCodec.decode(
            """[{"title":"الف"},{"title":"ب"},{"title":"الف","done":true}]""",
        )
        assertEquals(3, decoded.size)
        assertEquals(3, decoded.map { it.id }.toSet().size)
    }

    @Test
    fun `blank titles are skipped and the list is capped`() {
        val raw = (1..80).joinToString(",", "[", "]") { """{"title":"قدم $it"}""" }
        assertEquals(Subtask.MAX_ITEMS, SubtaskCodec.decode(raw).size)
        assertTrue(SubtaskCodec.decode("""[{"title":"   "},{"title":"ok"}]""").size == 1)
    }

    @Test
    fun `progress counts only done items`() {
        val items = listOf(
            Subtask("1", "a", true),
            Subtask("2", "b", false),
            Subtask("3", "c", true),
        )
        assertEquals(2 to 3, SubtaskCodec.progress(items))
        assertEquals("", SubtaskCodec.progress(emptyList()).label(true))
        assertEquals("۲ از ۳", SubtaskCodec.progress(items).label(true))
        assertEquals("2 از 3", SubtaskCodec.progress(items).label(false))
    }

    @Test
    fun `create trims and rejects blanks`() {
        val created = Subtask.create("  خرید   نان  ")
        assertNotNull(created)
        assertEquals("خرید نان", created!!.title)
        assertFalse(created.done)
        assertNull(Subtask.create("   "))
        assertEquals(Subtask.MAX_TITLE, Subtask.create("x".repeat(400))!!.title.length)
    }
}
