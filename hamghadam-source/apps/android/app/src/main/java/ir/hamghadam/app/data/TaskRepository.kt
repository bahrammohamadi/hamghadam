package ir.hamghadam.app.data

import ir.hamghadam.app.domain.CalendarEvent
import ir.hamghadam.app.domain.FocusRecord
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.RepeatRule
import ir.hamghadam.app.domain.Tags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Built-in task templates ("تمپلیت") available on a fresh install. */
object TemplatePresets {

    val defaults: List<TemplateEntity> = listOf(
        TemplateEntity(
            id = "preset-deep-work",
            name = "تمرکز عمیق",
            title = "یک بلوک تمرکز ۲۵ دقیقه‌ای",
            notes = "گوشی را کنار بگذار و فقط روی همین کار بمان.",
            priority = TaskEntity.PRIORITY_HIGH,
            minuteOfDay = 9 * 60,
            tagCsv = "تمرکز",
            builtin = true,
            createdAtEpochMillis = 1_700_000_000_000L,
        ),
        TemplateEntity(
            id = "preset-daily-review",
            name = "مرور روزانه",
            title = "مرور برنامهٔ فردا",
            notes = "سه کار مهم فردا را انتخاب کن.",
            priority = TaskEntity.PRIORITY_NORMAL,
            minuteOfDay = 21 * 60,
            tagCsv = "مرور",
            builtin = true,
            createdAtEpochMillis = 1_700_000_001_000L,
        ),
        TemplateEntity(
            id = "preset-study",
            name = "مطالعه",
            title = "مطالعهٔ ۳۰ دقیقه‌ای",
            priority = TaskEntity.PRIORITY_NORMAL,
            minuteOfDay = 20 * 60,
            tagCsv = "مطالعه",
            builtin = true,
            createdAtEpochMillis = 1_700_000_002_000L,
        ),
        TemplateEntity(
            id = "preset-workout",
            name = "ورزش صبحگاهی",
            title = "ورزش صبحگاهی (۲۰ دقیقه)",
            priority = TaskEntity.PRIORITY_NORMAL,
            minuteOfDay = 7 * 60,
            tagCsv = "سلامتی",
            builtin = true,
            createdAtEpochMillis = 1_700_000_003_000L,
        ),
        TemplateEntity(
            id = "preset-weekly-shopping",
            name = "خرید هفتگی",
            title = "خرید هفتگی خانه",
            priority = TaskEntity.PRIORITY_LOW,
            tagCsv = "خانه",
            builtin = true,
            createdAtEpochMillis = 1_700_000_004_000L,
        ),
    )
}

/**
 * Single entry point for reading and changing local data.
 *
 * All writes stamp `updatedAtEpochMillis` and keep tombstones instead of hard
 * deletes so the UI can offer undo (and a future sync worker can replay them).
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val habitDao: HabitDao,
    private val eventDao: PersonalEventDao,
    private val externalEventDao: ExternalEventDao,
    private val focusDao: FocusSessionDao,
    private val templateDao: TemplateDao,
    /** When present, every read/write is scoped to the active local account. */
    private val profiles: ProfileStore? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    /** Convenience constructor used by receivers that only have the database. */
    constructor(database: AppDatabase) : this(
        taskDao = database.taskDao(),
        projectDao = database.projectDao(),
        habitDao = database.habitDao(),
        eventDao = database.personalEventDao(),
        externalEventDao = database.externalEventDao(),
        focusDao = database.focusSessionDao(),
        templateDao = database.templateDao(),
    )

    /** The active account id ("" is the implicit first account). */
    val activeProfileId: String get() = profiles?.activeId?.value ?: DEFAULT_PROFILE_ID

    /**
     * Re-emits whenever the active account changes, so switching accounts swaps
     * the whole UI without restarting the process.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> scoped(source: Flow<List<T>>, profileOf: (T) -> String): Flow<List<T>> {
        // A database/decryption failure must degrade to an empty list, not kill
        // the collector (which would close the app from inside Compose).
        val safe = source.catch { emit(emptyList()) }
        val store = profiles ?: return safe
        return store.activeId.flatMapLatest { id -> safe.map { rows -> rows.filter { profileOf(it) == id } } }
    }

    fun observeTasks(): Flow<List<TaskEntity>> = scoped(taskDao.observeAll()) { it.profileId }

    fun observeProjects(): Flow<List<ProjectEntity>> = scoped(projectDao.observeAll()) { it.profileId }

    fun observeHabits(): Flow<List<HabitEntity>> = scoped(habitDao.observeHabits()) { it.profileId }

    fun observeHabitLogs(): Flow<List<HabitLogEntity>> =
        habitDao.observeLogs().catch { emit(emptyList()) }

    fun observePersonalEvents(): Flow<List<PersonalEventEntity>> = scoped(eventDao.observeAll()) { it.profileId }

    fun observeExternalEvents(): Flow<List<ExternalEventEntity>> =
        externalEventDao.observeAll().catch { emit(emptyList()) }

    fun observeFocusSessions(): Flow<List<FocusSessionEntity>> = scoped(focusDao.observeAll()) { it.profileId }

    fun observeTemplates(): Flow<List<TemplateEntity>> = scoped(templateDao.observeAll()) { it.profileId }

    suspend fun findTask(id: String): TaskEntity? = taskDao.findById(id)

    suspend fun createTask(
        title: String,
        notes: String = "",
        projectId: String? = null,
        dueDate: JalaliDate? = null,
        dueMinuteOfDay: Int? = null,
        priority: Int = TaskEntity.PRIORITY_NORMAL,
        repeat: RepeatRule = RepeatRule.NONE,
        reminderMinutesBefore: Int? = null,
        tags: String = "",
        subtasks: List<ir.hamghadam.app.domain.Subtask> = emptyList(),
        starred: Boolean = false,
    ): TaskEntity? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        val timestamp = now()
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            notes = notes.trim(),
            projectId = projectId,
            dueEpochDay = dueDate?.toEpochDay(),
            dueMinuteOfDay = dueMinuteOfDay?.takeIf { dueDate != null },
            priority = priority,
            repeatRule = repeat.storageValue,
            reminderMinutesBefore = reminderMinutesBefore?.takeIf { dueDate != null },
            tags = Tags.toCsv(Tags.parseCsv(tags)),
            subtasks = ir.hamghadam.app.domain.SubtaskCodec.encode(subtasks),
            starred = starred,
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp,
            profileId = activeProfileId,
        )
        taskDao.upsert(task)
        return task
    }

    suspend fun updateTask(task: TaskEntity): TaskEntity {
        val updated = task.copy(updatedAtEpochMillis = now())
        taskDao.upsert(updated)
        return updated
    }

    /** Star / un-star a task («ستاره‌دار») without touching anything else. */
    suspend fun setStarred(task: TaskEntity, starred: Boolean): TaskEntity =
        updateTask(task.copy(starred = starred))

    /**
     * Toggles one checklist line and returns the saved task (or null when the
     * line no longer exists). Completion of a task is **not** inferred from the
     * checklist: the PRD keeps the two independent.
     */
    suspend fun toggleSubtask(task: TaskEntity, subtaskId: String): TaskEntity? {
        val items = task.subtaskList
        if (items.none { it.id == subtaskId }) return null
        val updated = items.map { if (it.id == subtaskId) it.copy(done = !it.done) else it }
        return updateTask(task.copy(subtasks = ir.hamghadam.app.domain.SubtaskCodec.encode(updated)))
    }

    suspend fun deleteTask(id: String) = taskDao.softDelete(id, now())

    suspend fun restoreTask(id: String) = taskDao.restore(id, now())

    suspend fun loadTasksWithReminders(): List<TaskEntity> = taskDao.loadWithReminders()

    /**
     * Marks a task done. Repeating tasks produce their next occurrence so the
     * series keeps going; the returned task is that new occurrence (if any).
     */
    suspend fun setCompleted(task: TaskEntity, completed: Boolean): TaskEntity? {
        val timestamp = now()
        taskDao.upsert(
            task.copy(
                completedAtEpochMillis = if (completed) timestamp else null,
                updatedAtEpochMillis = timestamp,
            ),
        )
        if (!completed) return null
        val currentDate = task.dueDate ?: return null
        val nextDate = task.repeat.nextDate(currentDate) ?: return null
        val followUp = task.copy(
            id = UUID.randomUUID().toString(),
            dueEpochDay = nextDate.toEpochDay(),
            completedAtEpochMillis = null,
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp,
            deletedAtEpochMillis = null,
        )
        taskDao.upsert(followUp)
        return followUp
    }

    // ---------------------------------------------------------------- lists

    suspend fun createProject(name: String, colorArgb: Int = 0xFF237A69.toInt()): ProjectEntity? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = clean,
            colorArgb = colorArgb,
            createdAtEpochMillis = now(),
            profileId = activeProfileId,
        )
        projectDao.upsert(project)
        return project
    }

    suspend fun renameProject(project: ProjectEntity, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        projectDao.upsert(project.copy(name = clean))
    }

    suspend fun updateProjectColor(project: ProjectEntity, colorArgb: Int) {
        projectDao.upsert(project.copy(colorArgb = colorArgb))
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
        taskDao.clearProject(id, now())
    }

    // --------------------------------------------------------------- habits

    suspend fun createHabit(name: String): HabitEntity? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            name = clean,
            createdAtEpochMillis = now(),
            profileId = activeProfileId,
        )
        habitDao.upsertHabit(habit)
        return habit
    }

    suspend fun deleteHabit(id: String) {
        habitDao.deleteHabit(id)
        habitDao.deleteLogsForHabit(id)
    }

    suspend fun setHabitDone(habitId: String, epochDay: Long, done: Boolean) {
        if (done) {
            habitDao.insertLog(HabitLogEntity(habitId = habitId, epochDay = epochDay, doneAtEpochMillis = now()))
        } else {
            habitDao.deleteLog(habitId, epochDay)
        }
    }

    // --------------------------------------------------------------- events

    suspend fun savePersonalEvent(
        id: String?,
        title: String,
        date: JalaliDate,
        colorArgb: Int,
        yearlyRepeat: Boolean,
        notes: String,
    ): PersonalEventEntity? {
        val clean = title.trim()
        if (clean.isEmpty()) return null
        val timestamp = now()
        val existing = id?.let { target -> eventDao.loadAll().firstOrNull { it.id == target } }
        val entity = PersonalEventEntity(
            id = existing?.id ?: id ?: UUID.randomUUID().toString(),
            title = clean,
            epochDay = date.toEpochDay(),
            colorArgb = colorArgb,
            yearlyRepeat = yearlyRepeat,
            notes = notes.trim(),
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: timestamp,
            updatedAtEpochMillis = timestamp,
            profileId = existing?.profileId ?: activeProfileId,
        )
        eventDao.upsert(entity)
        return entity
    }

    suspend fun deletePersonalEvent(id: String) = eventDao.deleteById(id)

    suspend fun loadPersonalEvents(): List<PersonalEventEntity> =
        eventDao.loadAll().ownedBy(activeProfileId) { row -> row.profileId }

    // -------------------------------------------------------------- focus

    suspend fun recordFocus(record: FocusRecord): FocusSessionEntity {
        val entity = FocusSessionEntity(
            id = record.id,
            taskId = record.taskId,
            epochDay = record.epochDay,
            seconds = record.seconds,
            completed = record.completed,
            startedAtEpochMillis = record.startedAtEpochMillis,
            profileId = activeProfileId,
        )
        focusDao.upsert(entity)
        return entity
    }

    suspend fun loadFocusSessions(): List<FocusSessionEntity> =
        focusDao.loadAll().ownedBy(activeProfileId) { row -> row.profileId }

    suspend fun focusDay(epochDay: Long): Pair<Int, Int> {
        val sessions = focusDao.loadAll().filter { it.epochDay == epochDay && it.profileId == activeProfileId }
        return sessions.count { it.completed } to (sessions.sumOf { it.seconds } / 60)
    }

    // ---------------------------------------------------------- templates

    suspend fun ensureTemplatesSeeded() {
        if (templateDao.count() == 0) templateDao.upsertAll(TemplatePresets.defaults)
    }

    suspend fun createTemplate(
        name: String,
        title: String,
        notes: String = "",
        priority: Int = TaskEntity.PRIORITY_NORMAL,
        minuteOfDay: Int? = null,
        tags: String = "",
        projectId: String? = null,
    ): TemplateEntity? {
        val cleanName = name.trim()
        val cleanTitle = title.trim()
        if (cleanName.isEmpty() || cleanTitle.isEmpty()) return null
        val template = TemplateEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            title = cleanTitle,
            notes = notes.trim(),
            priority = priority,
            minuteOfDay = minuteOfDay,
            tagCsv = Tags.toCsv(Tags.parseCsv(tags)),
            projectId = projectId,
            builtin = false,
            createdAtEpochMillis = now(),
            profileId = activeProfileId,
        )
        templateDao.upsertAll(listOf(template))
        return template
    }

    suspend fun deleteTemplate(id: String) = templateDao.deleteById(id)

    // ------------------------------------------------------- google cache

    suspend fun replaceExternalEvents(events: List<ExternalEventEntity>) {
        externalEventDao.deleteAll()
        if (events.isNotEmpty()) externalEventDao.upsertAll(events)
    }

    suspend fun clearExternalEvents() = externalEventDao.deleteAll()

    suspend fun externalEventsAsCalendar(): List<CalendarEvent> =
        externalEventDao.loadAll().mapNotNull { it.toEvent() }

    // --------------------------------------------------------------- backup

    suspend fun exportBackup(): String = BackupCodec.encode(
        BackupPayload(
            tasks = taskDao.loadAll().ownedBy(activeProfileId) { row -> row.profileId },
            projects = projectDao.loadAll().ownedBy(activeProfileId) { row -> row.profileId },
            habits = habitDao.loadHabits().ownedBy(activeProfileId) { row -> row.profileId },
            habitLogs = habitDao.loadLogs(),
            personalEvents = eventDao.loadAll().ownedBy(activeProfileId) { row -> row.profileId },
            templates = templateDao.loadAll().filterNot { it.builtin }.ownedBy(activeProfileId) { row -> row.profileId },
            focusSessions = focusDao.loadAll().ownedBy(activeProfileId) { row -> row.profileId },
            exportedAtEpochMillis = now(),
        ),
    )

    /** Restores a backup over the current data and returns what was written. */
    suspend fun importBackup(text: String): ImportSummary {
        val decoded = BackupCodec.decode(text)
        // Everything restored belongs to the account the user is currently in.
        val owner = activeProfileId
        val payload = decoded.copy(
            tasks = decoded.tasks.map { it.copy(profileId = owner) },
            projects = decoded.projects.map { it.copy(profileId = owner) },
            habits = decoded.habits.map { it.copy(profileId = owner) },
            personalEvents = decoded.personalEvents.map { it.copy(profileId = owner) },
            templates = decoded.templates.map { it.copy(profileId = owner) },
            focusSessions = decoded.focusSessions.map { it.copy(profileId = owner) },
        )
        // Only the active account is replaced. Deleting every row (the old
        // behaviour) silently wiped all other local accounts during a restore
        // (audit C-15).
        taskDao.deleteByProfile(owner)
        projectDao.deleteByProfile(owner)
        habitDao.deleteHabitsByProfile(owner)
        habitDao.deleteOrphanLogs()
        eventDao.deleteByProfile(owner)
        focusDao.deleteByProfile(owner)
        // Built-in templates stay; only this account's user templates are replaced.
        templateDao.loadAll()
            .filter { !it.builtin && it.profileId == owner }
            .forEach { templateDao.deleteById(it.id) }
        taskDao.upsertAll(payload.tasks)
        projectDao.upsertAll(payload.projects)
        habitDao.upsertHabits(payload.habits)
        // A crafted backup must not attach check-ins to habits of another
        // account: keep only logs whose habit is part of this payload.
        val restoredHabitIds = payload.habits.mapTo(mutableSetOf()) { it.id }
        habitDao.insertLogs(payload.habitLogs.filter { it.habitId in restoredHabitIds })
        payload.personalEvents.forEach { eventDao.upsert(it) }
        if (payload.templates.isNotEmpty()) templateDao.upsertAll(payload.templates)
        payload.focusSessions.forEach { focusDao.upsert(it) }
        return BackupCodec.summarize(payload)
    }

    /** Hint for the backup screen: how much is stored right now. */
    suspend fun counts(): Map<String, Int> {
        val owner = activeProfileId
        val focus = focusDao.loadAll().filter { it.profileId == owner }
        return mapOf(
            "tasks" to taskDao.loadAll().count { it.profileId == owner },
            "projects" to projectDao.loadAll().count { it.profileId == owner },
            "habits" to habitDao.loadHabits().count { it.profileId == owner },
            "events" to eventDao.loadAll().count { it.profileId == owner },
            "focus" to focus.count { it.completed },
            "templates" to templateDao.loadAll().count { !it.builtin && it.profileId == owner },
        )
    }

    /** Local Jalali day for an instant, used when logging focus blocks. */
    fun epochDayOf(millis: Long): Long =
        JalaliDate.fromGregorian(
            java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate(),
        ).toEpochDay()

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    // -------------------------------------------------------------- accounts

    /** Copies a task (title + schedule + tags) as a new open task today. */
    suspend fun duplicateTask(task: TaskEntity): TaskEntity? {
        val timestamp = now()
        val copy = task.copy(
            id = UUID.randomUUID().toString(),
            title = task.title,
            completedAtEpochMillis = null,
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp,
            deletedAtEpochMillis = null,
            profileId = activeProfileId,
            // Fresh checklist ids so the two tasks can be checked independently.
            subtasks = ir.hamghadam.app.domain.SubtaskCodec.encode(
                task.subtaskList.map { it.copy(id = java.util.UUID.randomUUID().toString(), done = false) },
            ),
        )
        taskDao.upsert(copy)
        return copy
    }

    /** Hard-deletes every row owned by a local account (called when it is removed). */
    suspend fun deleteProfileData(profileId: String) {
        taskDao.deleteByProfile(profileId)
        projectDao.deleteByProfile(profileId)
        habitDao.deleteHabitsByProfile(profileId)
        habitDao.deleteOrphanLogs()
        eventDao.deleteByProfile(profileId)
        focusDao.deleteByProfile(profileId)
        templateDao.deleteByProfile(profileId)
    }

    /** True when the account holds at least one row; used by the delete warning. */
    suspend fun profileIsEmpty(profileId: String): Boolean =
        taskDao.loadAll().none { it.profileId == profileId } &&
            projectDao.loadAll().none { it.profileId == profileId } &&
            habitDao.loadHabits().none { it.profileId == profileId } &&
            eventDao.loadAll().none { it.profileId == profileId } &&
            focusDao.loadAll().none { it.profileId == profileId } &&
            templateDao.loadAll().none { it.profileId == profileId }
}
