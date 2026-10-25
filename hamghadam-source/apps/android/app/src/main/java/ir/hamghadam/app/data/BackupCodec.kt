package ir.hamghadam.app.data

import ir.hamghadam.app.domain.SimpleJson

/** Everything the app stores locally, ready to be written to a backup file. */
data class BackupPayload(
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val personalEvents: List<PersonalEventEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
)

data class ImportSummary(
    val tasks: Int,
    val projects: Int,
    val habits: Int,
    val habitLogs: Int,
    val events: Int = 0,
    val templates: Int = 0,
    val focusSessions: Int = 0,
    val exportedAtEpochMillis: Long?,
)

/**
 * Versioned JSON backup format. It is deliberately readable: a user can open the
 * file, and a future server importer can reuse the same field names.
 *
 * v1 covered tasks/projects/habits; v2 adds tags, personal events, user
 * templates and focus sessions. Reading a v1 file still works.
 */
object BackupCodec {

    const val FORMAT = "hamghadam-backup"
    const val VERSION = 2

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
                    "tags" to task.tags,
                    "completedAt" to task.completedAtEpochMillis,
                    "createdAt" to task.createdAtEpochMillis,
                    "updatedAt" to task.updatedAtEpochMillis,
                    "sortOrder" to task.sortOrder,
                    "subtasks" to task.subtasks,
                    "starred" to task.starred,
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
            "personalEvents" to payload.personalEvents.map { event ->
                mapOf(
                    "id" to event.id,
                    "title" to event.title,
                    "epochDay" to event.epochDay,
                    "colorArgb" to event.colorArgb,
                    "yearlyRepeat" to event.yearlyRepeat,
                    "notes" to event.notes,
                    "createdAt" to event.createdAtEpochMillis,
                    "updatedAt" to event.updatedAtEpochMillis,
                )
            },
            "templates" to payload.templates.map { template ->
                mapOf(
                    "id" to template.id,
                    "name" to template.name,
                    "title" to template.title,
                    "notes" to template.notes,
                    "priority" to template.priority,
                    "minuteOfDay" to template.minuteOfDay,
                    "tags" to template.tagCsv,
                    "projectId" to template.projectId,
                    "builtin" to template.builtin,
                    "createdAt" to template.createdAtEpochMillis,
                )
            },
            "focusSessions" to payload.focusSessions.map { session ->
                mapOf(
                    "id" to session.id,
                    "taskId" to session.taskId,
                    "epochDay" to session.epochDay,
                    "seconds" to session.seconds,
                    "completed" to session.completed,
                    "startedAt" to session.startedAtEpochMillis,
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
                tags = entry.string("tags").orEmpty(),
                completedAtEpochMillis = entry.long("completedAt"),
                createdAtEpochMillis = entry.long("createdAt") ?: 0L,
                updatedAtEpochMillis = entry.long("updatedAt") ?: 0L,
                sortOrder = entry.long("sortOrder") ?: 0L,
                // Backups from 0.6 and older simply have no such keys.
                subtasks = entry.string("subtasks").orEmpty(),
                starred = entry["starred"] as? Boolean ?: false,
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

        val personalEvents = (root["personalEvents"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = entry.string("id") ?: return@mapNotNull null
            val title = entry.string("title") ?: return@mapNotNull null
            val epochDay = entry.long("epochDay") ?: return@mapNotNull null
            PersonalEventEntity(
                id = id,
                title = title,
                epochDay = epochDay,
                colorArgb = (entry["colorArgb"] as? Number)?.toInt() ?: 0xFF6C5CE7.toInt(),
                yearlyRepeat = entry["yearlyRepeat"] as? Boolean ?: false,
                notes = entry.string("notes").orEmpty(),
                createdAtEpochMillis = entry.long("createdAt") ?: 0L,
                updatedAtEpochMillis = entry.long("updatedAt") ?: 0L,
            )
        }.filter { it.title.isNotBlank() }

        val templates = (root["templates"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = entry.string("id") ?: return@mapNotNull null
            val name = entry.string("name") ?: return@mapNotNull null
            val title = entry.string("title") ?: return@mapNotNull null
            TemplateEntity(
                id = id,
                name = name,
                title = title,
                notes = entry.string("notes").orEmpty(),
                priority = (entry["priority"] as? Number)?.toInt() ?: TaskEntity.PRIORITY_NORMAL,
                minuteOfDay = (entry["minuteOfDay"] as? Number)?.toInt(),
                tagCsv = entry.string("tags").orEmpty(),
                projectId = entry.string("projectId"),
                builtin = entry["builtin"] as? Boolean ?: false,
                createdAtEpochMillis = entry.long("createdAt") ?: 0L,
            )
        }.filter { it.name.isNotBlank() && it.title.isNotBlank() }

        val focusSessions = (root["focusSessions"] as? List<*>).orEmpty().mapNotNull { raw ->
            val entry = raw as? Map<*, *> ?: return@mapNotNull null
            val id = entry.string("id") ?: return@mapNotNull null
            val epochDay = entry.long("epochDay") ?: return@mapNotNull null
            val seconds = (entry["seconds"] as? Number)?.toInt() ?: return@mapNotNull null
            FocusSessionEntity(
                id = id,
                taskId = entry.string("taskId"),
                epochDay = epochDay,
                seconds = seconds,
                completed = entry["completed"] as? Boolean ?: true,
                startedAtEpochMillis = entry.long("startedAt") ?: 0L,
            )
        }

        return BackupPayload(
            tasks = tasks,
            projects = projects,
            habits = habits,
            habitLogs = habitLogs,
            personalEvents = personalEvents,
            templates = templates,
            focusSessions = focusSessions,
            exportedAtEpochMillis = (root["exportedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    /** Number of records in a payload, used for the confirmation message. */
    fun summarize(payload: BackupPayload): ImportSummary = ImportSummary(
        tasks = payload.tasks.size,
        projects = payload.projects.size,
        habits = payload.habits.size,
        habitLogs = payload.habitLogs.size,
        events = payload.personalEvents.size,
        templates = payload.templates.size,
        focusSessions = payload.focusSessions.size,
        exportedAtEpochMillis = payload.exportedAtEpochMillis.takeIf { it > 0 },
    )

    private fun Map<*, *>.string(key: String): String? = this[key] as? String

    private fun Map<*, *>.long(key: String): Long? = (this[key] as? Number)?.toLong()
}
