package ir.hamghadam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val task = TaskEntity(
        id = "t1",
        title = "جلسه با \"تیم\" محصول",
        notes = "خط اول\nخط دوم با \\ بک‌اسلش و «گیومه»",
        projectId = "p1",
        dueEpochDay = 20500,
        dueMinuteOfDay = 9 * 60 + 30,
        priority = TaskEntity.PRIORITY_HIGH,
        repeatRule = "WEEKLY",
        reminderMinutesBefore = 15,
        createdAtEpochMillis = 1_700_000_000_000,
        updatedAtEpochMillis = 1_700_000_100_000,
        subtasks = """[{"id":"a","title":"آماده کردن فایل","done":true}]""",
        starred = true,
    )

    private val payload = BackupPayload(
        tasks = listOf(task),
        projects = listOf(ProjectEntity(id = "p1", name = "کار", createdAtEpochMillis = 1_700_000_000_000)),
        habits = listOf(HabitEntity(id = "h1", name = "ورزش صبحگاهی", createdAtEpochMillis = 1_700_000_000_000)),
        habitLogs = listOf(HabitLogEntity(habitId = "h1", epochDay = 20490, doneAtEpochMillis = 1_700_000_000_000)),
        exportedAtEpochMillis = 1_700_000_200_000,
    )

    @Test
    fun `round trip keeps every field`() {
        val restored = BackupCodec.decode(BackupCodec.encode(payload))
        assertEquals(1, restored.tasks.size)
        val copy = restored.tasks.first()
        assertEquals(task.id, copy.id)
        assertEquals(task.title, copy.title)
        assertEquals(task.notes, copy.notes)
        assertEquals(task.projectId, copy.projectId)
        assertEquals(task.dueEpochDay, copy.dueEpochDay)
        assertEquals(task.dueMinuteOfDay, copy.dueMinuteOfDay)
        assertEquals(task.priority, copy.priority)
        assertEquals(task.repeatRule, copy.repeatRule)
        assertEquals(task.reminderMinutesBefore, copy.reminderMinutesBefore)
        assertEquals(task.createdAtEpochMillis, copy.createdAtEpochMillis)
        assertEquals(task.subtasks, copy.subtasks)
        assertEquals(task.starred, copy.starred)
        assertNull(copy.completedAtEpochMillis)
        assertEquals("کار", restored.projects.first().name)
        assertEquals("ورزش صبحگاهی", restored.habits.first().name)
        assertEquals(20490L, restored.habitLogs.first().epochDay)
        assertEquals(1_700_000_200_000, restored.exportedAtEpochMillis)
    }

    @Test
    fun `summary counts records`() {
        val summary = BackupCodec.summarize(payload)
        assertEquals(1, summary.tasks)
        assertEquals(1, summary.projects)
        assertEquals(1, summary.habits)
        assertEquals(1, summary.habitLogs)
    }

    @Test
    fun `foreign file is rejected`() {
        val error = runCatching { BackupCodec.decode("""{"format":"something-else","version":1}""") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `newer format version is rejected`() {
        val error = runCatching {
            BackupCodec.decode("""{"format":"hamghadam-backup","version":99,"tasks":[]}""")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `broken json fails with a clear error`() {
        assertTrue(runCatching { BackupCodec.decode("{oops") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { BackupCodec.decode("") }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `empty backup decodes to empty lists`() {
        val restored = BackupCodec.decode("""{"format":"hamghadam-backup","version":1}""")
        assertTrue(restored.tasks.isEmpty())
        assertTrue(restored.projects.isEmpty())
        assertTrue(restored.habits.isEmpty())
        assertTrue(restored.habitLogs.isEmpty())
    }
}

class BackupCodecV2CompatibilityTest {

    private val v2 = """
        {
          "format": "hamghadam-backup",
          "version": 2,
          "exportedAt": 1700000000000,
          "tasks": [
            {"id":"t1","title":"کار قدیمی","notes":"","priority":1,"tags":"","createdAt":1,"updatedAt":2}
          ]
        }
    """.trimIndent()

    @Test
    fun `backup from 0-6 without checklist fields still imports`() {
        val restored = BackupCodec.decode(v2)
        assertEquals(1, restored.tasks.size)
        val task = restored.tasks.first()
        assertEquals("کار قدیمی", task.title)
        assertEquals("", task.subtasks)
        assertEquals(false, task.starred)
        assertEquals(emptyList<ir.hamghadam.app.domain.Subtask>(), task.subtaskList)
    }
}
