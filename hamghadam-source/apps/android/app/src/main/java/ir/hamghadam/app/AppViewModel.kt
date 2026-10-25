package ir.hamghadam.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.HabitEntity
import ir.hamghadam.app.data.HabitLogEntity
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.data.ThemeMode
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.QuickAddParser
import ir.hamghadam.app.domain.RepeatRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Editable copy of a task used by the editor screen. */
data class TaskDraft(
    val id: String? = null,
    val title: String = "",
    val notes: String = "",
    val projectId: String? = null,
    val dueDate: JalaliDate? = null,
    val dueMinuteOfDay: Int? = null,
    val priority: Int = TaskEntity.PRIORITY_NORMAL,
    val repeat: RepeatRule = RepeatRule.NONE,
    val reminderMinutesBefore: Int? = null,
) {
    val isNew: Boolean get() = id == null
}

fun TaskEntity.toDraft(): TaskDraft = TaskDraft(
    id = id,
    title = title,
    notes = notes,
    projectId = projectId,
    dueDate = dueDate,
    dueMinuteOfDay = dueMinuteOfDay,
    priority = priority,
    repeat = repeat,
    reminderMinutesBefore = reminderMinutesBefore,
)

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class TaskDeleted(val id: String, val title: String) : UiEvent
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as HamghadamApplication).container
    private val repository = container.repository
    private val reminders = container.reminders
    private val settingsStore = container.settings

    val tasks: StateFlow<List<TaskEntity>> = repository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habitLogs: StateFlow<List<HabitLogEntity>> = repository.observeHabitLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsStore.settings

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        // Alarms can be lost (reboot, app update, force stop): rebuild them once.
        viewModelScope.launch {
            runCatching { reminders.rescheduleAll(repository.loadTasksWithReminders()) }
        }
    }

    // --------------------------------------------------------------- tasks

    fun quickAdd(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        val today = JalaliDate.today()
        val parsed = QuickAddParser.parse(text, today)
        val title = parsed.title.ifBlank { text }
        val dueDate = parsed.date ?: if (parsed.time != null) today else null
        val minuteOfDay = parsed.time?.let { it.hour * 60 + it.minute }
        val reminder = if (dueDate != null) settingsStore.settings.value.defaultReminderMinutes else null
        viewModelScope.launch {
            val task = repository.createTask(
                title = title,
                dueDate = dueDate,
                dueMinuteOfDay = minuteOfDay,
                priority = parsed.priority,
                reminderMinutesBefore = reminder,
            )
            if (task != null) {
                reminders.schedule(task)
                val summary = PersianCalendar.dueLabel(task.dueDate, task.dueMinuteOfDay, today)
                _events.send(
                    UiEvent.Message(
                        if (summary != null) "«${task.title}» برای $summary ثبت شد" else "«${task.title}» ثبت شد",
                    ),
                )
            }
        }
    }

    fun saveDraft(draft: TaskDraft) {
        val title = draft.title.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val existing = draft.id?.let { repository.findTask(it) }
            if (existing == null) {
                val created = repository.createTask(
                    title = title,
                    notes = draft.notes,
                    projectId = draft.projectId,
                    dueDate = draft.dueDate,
                    dueMinuteOfDay = draft.dueMinuteOfDay,
                    priority = draft.priority,
                    repeat = draft.repeat,
                    reminderMinutesBefore = draft.reminderMinutesBefore.takeIf { draft.dueDate != null },
                )
                if (created != null) {
                    reminders.schedule(created)
                    _events.send(UiEvent.Message("کار تازه ثبت شد"))
                }
            } else {
                val updated = repository.updateTask(
                    existing.copy(
                        title = title,
                        notes = draft.notes,
                        projectId = draft.projectId,
                        dueEpochDay = draft.dueDate?.toEpochDay(),
                        dueMinuteOfDay = draft.dueMinuteOfDay.takeIf { draft.dueDate != null },
                        priority = draft.priority,
                        repeatRule = draft.repeat.storageValue,
                        reminderMinutesBefore = draft.reminderMinutesBefore.takeIf { draft.dueDate != null },
                    ),
                )
                reminders.cancel(updated.id)
                reminders.schedule(updated)
                _events.send(UiEvent.Message("تغییرات ذخیره شد"))
            }
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            reminders.cancel(task.id)
            val followUp = repository.setCompleted(task, !task.isCompleted)
            if (!task.isCompleted) {
                if (followUp != null) {
                    reminders.schedule(followUp)
                    val due = PersianCalendar.dueLabel(followUp.dueDate, followUp.dueMinuteOfDay, JalaliDate.today())
                    _events.send(UiEvent.Message("انجام شد؛ تکرار بعدی برای ${due ?: "بعد"} ساخته شد"))
                }
            } else {
                reminders.schedule(task.copy(completedAtEpochMillis = null))
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            reminders.cancel(task.id)
            repository.deleteTask(task.id)
            _events.send(UiEvent.TaskDeleted(task.id, task.title))
        }
    }

    fun deleteDraft(draft: TaskDraft) {
        val id = draft.id ?: return
        viewModelScope.launch {
            reminders.cancel(id)
            repository.deleteTask(id)
            _events.send(UiEvent.TaskDeleted(id, draft.title))
        }
    }

    fun undoDelete(taskId: String) {
        viewModelScope.launch {
            repository.restoreTask(taskId)
            repository.findTask(taskId)?.let { reminders.schedule(it) }
            _events.send(UiEvent.Message("کار بازگردانده شد"))
        }
    }

    // ------------------------------------------------------------- projects

    fun createProject(name: String) {
        viewModelScope.launch {
            if (repository.createProject(name) != null) {
                _events.send(UiEvent.Message("لیست تازه ساخته شد"))
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            _events.send(UiEvent.Message("لیست حذف شد؛ کارهای آن بدون لیست شدند"))
        }
    }

    // --------------------------------------------------------------- habits

    fun createHabit(name: String) {
        viewModelScope.launch {
            if (repository.createHabit(name) != null) _events.send(UiEvent.Message("عادت تازه ساخته شد"))
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            repository.deleteHabit(id)
            _events.send(UiEvent.Message("عادت حذف شد"))
        }
    }

    fun toggleHabitDay(habitId: String, date: JalaliDate, done: Boolean) {
        viewModelScope.launch { repository.setHabitDone(habitId, date.toEpochDay(), done) }
    }

    // ------------------------------------------------------------- settings

    fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)

    fun setPersianDigits(enabled: Boolean) = settingsStore.setPersianDigits(enabled)

    fun setDefaultReminder(minutes: Int?) = settingsStore.setDefaultReminder(minutes)

    fun rescheduleReminders() {
        viewModelScope.launch {
            runCatching {
                reminders.rescheduleAll(repository.loadTasksWithReminders())
                _events.send(UiEvent.Message("یادآورها دوباره زمان‌بندی شدند"))
            }.onFailure {
                _events.send(UiEvent.Message("زمان‌بندی یادآورها ناموفق بود"))
            }
        }
    }

    // ----------------------------------------------------------------- data

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = repository.exportBackup()
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("نوشتن فایل ممکن نشد")
                }
            }.onSuccess {
                _events.send(UiEvent.Message("فایل پشتیبان ذخیره شد"))
            }.onFailure {
                _events.send(UiEvent.Message("ذخیرهٔ پشتیبان ناموفق بود"))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("خواندن فایل ممکن نشد")
                    repository.importBackup(text)
                }
            }.fold(
                onSuccess = { summary ->
                    runCatching { reminders.rescheduleAll(repository.loadTasksWithReminders()) }
                    "بازیابی انجام شد: ${PersianText.number(summary.tasks)} کار، " +
                        "${PersianText.number(summary.projects)} لیست، ${PersianText.number(summary.habits)} عادت"
                },
                onFailure = { error -> "بازیابی ناموفق بود: ${error.message ?: "فایل معتبر نیست"}" },
            )
            _events.send(UiEvent.Message(message))
        }
    }
}
