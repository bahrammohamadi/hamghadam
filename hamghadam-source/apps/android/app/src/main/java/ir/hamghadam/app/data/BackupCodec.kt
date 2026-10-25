package ir.hamghadam.app.data

import ir.hamghadam.app.domain.SimpleJson

/** Everything the app stores locally, ready to be written to a backup file. */
data class BackupPayload(
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
)

data class ImportSummary(
    val tasks: Int,
    val projects: Int,
    val habits: Int,
    val habitLogs: Int,
    val exportedAtEpochMillis: Long?,
)

/**
 * Versioned JSON backup format. It is deliberately readable: a user can open the
 * file, and a future server importer can reuse the same field names.
 */
object BackupCodec {

    const val FORMAT = "hamghadam-backup"
    const val VERSION = 1

    fun encode(payload: BackupPayload): String = SimpleJson.encode(
        mapOf(
            "format" to FORMAT,
            "version" to VERSION,
            "exportedAt" to payload.exportedAtEpochMillis,
            "projects" to payload.projects.map { project ->
                mapOf(
                    "id" to project.id,
                    "name" to project.name,
                    "colorArgb" to project.colorArgb,
                    "createdAt" to project.createdAtEpochMillis,
                    "archived" to project.archived,
                )
            },
            "tasks" to payload.tasks.map { task ->
                mapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "notes" to task.notes,
                    "projectId" to task.projectId,
                    "dueEpochDay" to task.dueEpochDay,
                    "dueMinuteOfDay" to task.dueMinuteOfDay,
                    "priority" to task.priority,
                    "repeat" to task.repeatRule,
                    "reminderMinutesBefore" to task.reminderMinutesBefore,
                    "completedAt" to task.completedAtEpochMillis,
                    "createdAt" to task.createdAtEpochMillis,
                    "updatedAt" to task.updatedAtEpochMillis,
                    "sortOrder" to task.sortOrder,
                )
            },
            "habits" to payload.habits.map { habit ->
                mapOf(
                    "id" to habit.id,
                    "name" to habit.name,
                    "createdAt" to habit.createdAtEpochMillis,
                    "archived" to habit.archived,
                    "sortOrder" to habit.sortOrder,
                )
            },
            "habitLogs" to payload.habitLogs.map { log ->
                mapOf(
                    "habitId" to log.habitId,
                    "epochDay" to log.epochDay,
                    "doneAt" to log.doneAtEpochMillis,
                )
            },
        ),
    )

    fun decode(text: String): BackupPayload {
        val root = SimpleJson.parse(text) as? Map<*, *>
            ?: throw IllegalArgumentException("فایل پشتیبان معتبر نیست")
        val format = root["format"] as? String
        if (format != FORMAT) throw IllegalArgumentException("این فایل، پشتیبان هم‌قدم نیست")
        val version = (root["version"] as? Number)?.toInt() ?: 0
        if (version > VERSION) throw IllegalArgumentException("نسخهٔ فایل پشتیبان جدیدتر است")

        val projects = (root["projects"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = entry.string("id") ?: return@mapNotNull null
            ProjectEntity(
                id = id,
                name = entry.string("name").orEmpty(),
                colorArgb = (entry["colorArgb"] as? Number)?.toInt() ?: 0xFF237A69.toInt(),
                createdAtEpochMillis = entry.long("createdAt") ?: 0L,
                archived = entry["archived"] as? Boolean ?: false,
            )
        }

        val tasks = (root["tasks"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = entry.string("id") ?: return@mapNotNull null
            TaskEntity(
                id = id,
                title = entry.string("title").orEmpty(),
                notes = entry.string("notes").orEmpty(),
                projectId = entry.string("projectId"),
                dueEpochDay = entry.long("dueEpochDay"),
                dueMinuteOfDay = (entry["dueMinuteOfDay"] as? Number)?.toInt(),
                priority = (entry["priority"] as? Number)?.toInt() ?: TaskEntity.PRIORITY_NORMAL,
                repeatRule = entry.string("repeat") ?: "NONE",
                reminderMinutesBefore = (entry["reminderMinutesBefore"] as? Number)?.toInt(),
                completedAtEpochMillis = entry.long("completedAt"),
                createdAtEpochMillis = entry.long("createdAt") ?: 0L,
                updatedAtEpochMillis = entry.long("updatedAt") ?: 0L,
                sortOrder = entry.long("sortOrder") ?: 0L,
            )
        }.filter { it.title.isNotBlank() }

        val habits = (root["habits"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = entry.string("id") ?: return@mapNotNull null
            HabitEntity(
                id = id,
                name = entry.string("name").orEmpty(),
                createdAtEpochMillis = entry.long("createdAt") ?: 0L,
                archived = entry["archived"] as? Boolean ?: false,
                sortOrder = entry.long("sortOrder") ?: 0L,
            )
        }.filter { it.name.isNotBlank() }

        val habitLogs = (root["habitLogs"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val habitId = entry.string("habitId") ?: return@mapNotNull null
            val epochDay = entry.long("epochDay") ?: return@mapNotNull null
            HabitLogEntity(
                habitId = habitId,
                epochDay = epochDay,
                doneAtEpochMillis = entry.long("doneAt") ?: 0L,
            )
        }

        return BackupPayload(
            tasks = tasks,
            projects = projects,
            habits = habits,
            habitLogs = habitLogs,
            exportedAtEpochMillis = (root["exportedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    /** Number of records in a payload, used for the confirmation message. */
    fun summarize(payload: BackupPayload): ImportSummary = ImportSummary(
        tasks = payload.tasks.size,
        projects = payload.projects.size,
        habits = payload.habits.size,
        habitLogs = payload.habitLogs.size,
        exportedAtEpochMillis = payload.exportedAtEpochMillis.takeIf { it > 0 },
    )

    private fun Map<*, *>.string(key: String): String? = this[key] as? String

    private fun Map<*, *>.long(key: String): Long? = (this[key] as? Number)?.toLong()
}
