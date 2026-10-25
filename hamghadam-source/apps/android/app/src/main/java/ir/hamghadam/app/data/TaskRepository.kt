package ir.hamghadam.app.data

import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.RepeatRule
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()

    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    fun observeHabits(): Flow<List<HabitEntity>> = habitDao.observeHabits()

    fun observeHabitLogs(): Flow<List<HabitLogEntity>> = habitDao.observeLogs()

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
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp,
        )
        taskDao.upsert(task)
        return task
    }

    suspend fun updateTask(task: TaskEntity): TaskEntity {
        val updated = task.copy(updatedAtEpochMillis = now())
        taskDao.upsert(updated)
        return updated
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
        )
        projectDao.upsert(project)
        return project
    }

    suspend fun renameProject(project: ProjectEntity, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        projectDao.upsert(project.copy(name = clean))
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

    // --------------------------------------------------------------- backup

    suspend fun exportBackup(): String = BackupCodec.encode(
        BackupPayload(
            tasks = taskDao.loadAll(),
            projects = projectDao.loadAll(),
            habits = habitDao.loadHabits(),
            habitLogs = habitDao.loadLogs(),
            exportedAtEpochMillis = now(),
        ),
    )

    /** Restores a backup over the current data and returns what was written. */
    suspend fun importBackup(text: String): ImportSummary {
        val payload = BackupCodec.decode(text)
        taskDao.deleteAll()
        projectDao.deleteAll()
        habitDao.deleteAllHabits()
        habitDao.deleteAllLogs()
        taskDao.upsertAll(payload.tasks)
        projectDao.upsertAll(payload.projects)
        habitDao.upsertHabits(payload.habits)
        habitDao.insertLogs(payload.habitLogs)
        return BackupCodec.summarize(payload)
    }
}
