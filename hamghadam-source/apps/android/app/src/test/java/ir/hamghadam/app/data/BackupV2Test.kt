package ir.hamghadam.app.data

import ir.hamghadam.app.domain.EventPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupV2Test {

    private val task = TaskEntity(
        id = "t1",
        title = "خرید هفتگی",
        tags = "خانه,خرید",
        dueEpochDay = 20800,
        priority = TaskEntity.PRIORITY_HIGH,
    )

    private val payload = BackupPayload(
        tasks = listOf(task),
        projects = listOf(ProjectEntity(id = "p1", name = "خانه")),
        personalEvents = listOf(
            PersonalEventEntity(
                id = "e1",
                title = "سالگرد",
                epochDay = 20800,
                colorArgb = EventPalette.picks.first(),
                yearlyRepeat = true,
                notes = "یادداشت",
            ),
        ),
        templates = listOf(
            TemplateEntity(
                id = "tpl1",
                name = "صبح",
                title = "بررسی ایمیل",
                notes = "فقط ۱۵ دقیقه",
                priority = TaskEntity.PRIORITY_LOW,
                minuteOfDay = 9 * 60,
                tagCsv = "کار",
            ),
        ),
        focusSessions = listOf(
            FocusSessionEntity(id = "f1", taskId = "t1", epochDay = 20800, seconds = 1500, completed = true),
        ),
    )

    @Test
    fun `v2 round trip keeps tags, events, templates and focus sessions`() {
        val text = BackupCodec.encode(payload)
        assertTrue(text.contains("\"version\":2"))
        assertTrue(text.contains("\"format\":\"hamghadam-backup\""))

        val restored = BackupCodec.decode(text)
        assertEquals("خانه,خرید", restored.tasks.single().tags)
        val event = restored.personalEvents.single()
        assertEquals("سالگرد", event.title)
        assertEquals(20800L, event.epochDay)
        assertTrue(event.yearlyRepeat)
        assertEquals("یادداشت", event.notes)

        val template = restored.templates.single()
        assertEquals("بررسی ایمیل", template.title)
        assertEquals(9 * 60, template.minuteOfDay)
        assertEquals("کار", template.tagCsv)

        val focus = restored.focusSessions.single()
        assertEquals("t1", focus.taskId)
        assertEquals(1500, focus.seconds)
        assertTrue(focus.completed)

        val summary = BackupCodec.summarize(restored)
        assertEquals(1, summary.tasks)
        assertEquals(1, summary.projects)
        assertEquals(1, summary.events)
        assertEquals(1, summary.templates)
        assertEquals(1, summary.focusSessions)
    }

    @Test
    fun `v1 backup without the new arrays still imports`() {
        val v1 = """
            {
              "format": "hamghadam-backup",
              "version": 1,
              "exportedAt": 1700000000000,
              "tasks": [
                {"id": "old", "title": "کار قدیمی", "createdAt": 1, "updatedAt": 2}
              ]
            }
        """.trimIndent()
        val restored = BackupCodec.decode(v1)
        assertEquals(1, restored.tasks.size)
        assertEquals("کار قدیمی", restored.tasks.single().title)
        assertEquals("", restored.tasks.single().tags)
        assertTrue(restored.personalEvents.isEmpty())
        assertTrue(restored.templates.isEmpty())
        assertTrue(restored.focusSessions.isEmpty())
    }

    @Test
    fun `templates without tags decode to an empty csv`() {
        val json = """
            {
              "format": "hamghadam-backup",
              "version": 2,
              "templates": [{"id": "tpl", "name": "x", "title": "y"}],
              "focusSessions": [{"id": "f", "epochDay": 5, "seconds": 60}]
            }
        """.trimIndent()
        val restored = BackupCodec.decode(json)
        assertEquals("", restored.templates.single().tagCsv)
        assertFalse(restored.templates.single().builtin)
        assertEquals(true, restored.focusSessions.single().completed)
    }
}
