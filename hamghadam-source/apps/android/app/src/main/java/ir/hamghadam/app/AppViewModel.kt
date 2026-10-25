package ir.hamghadam.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.EventCatalog
import ir.hamghadam.app.data.ExternalEventEntity
import ir.hamghadam.app.data.FocusSessionEntity
import ir.hamghadam.app.data.HabitEntity
import ir.hamghadam.app.data.HabitLogEntity
import ir.hamghadam.app.data.LocalProfile
import ir.hamghadam.app.data.PersonalEventEntity
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.DEFAULT_PROFILE_ID
import ir.hamghadam.app.data.isPinValid
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.data.TemplateEntity
import ir.hamghadam.app.data.ThemeMode
import ir.hamghadam.app.domain.CalendarSource
import ir.hamghadam.app.domain.EventPalette
import ir.hamghadam.app.domain.FocusRecord
import ir.hamghadam.app.domain.FocusStats
import ir.hamghadam.app.domain.FocusTimerState
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.QuickAddParser
import ir.hamghadam.app.domain.RepeatRule
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.ui.theme.AccentColor
import ir.hamghadam.app.google.GoogleCalendarApi
import ir.hamghadam.app.google.GoogleOAuth
import ir.hamghadam.app.google.GoogleSyncScheduler
import ir.hamghadam.app.google.GoogleTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    val tags: String = "",
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
    tags = tags,
)

/** Editable copy of a personal calendar event. */
data class EventDraft(
    val id: String? = null,
    val title: String = "",
    val date: JalaliDate = JalaliDate.today(),
    val colorArgb: Int = EventPalette.personalDefault,
    val yearlyRepeat: Boolean = false,
    val notes: String = "",
) {
    val isNew: Boolean get() = id == null
}

fun PersonalEventEntity.toDraft(): EventDraft {
    val date = JalaliDate.fromEpochDay(epochDay)
    return EventDraft(
        id = id,
        title = title,
        date = date,
        colorArgb = colorArgb,
        yearlyRepeat = yearlyRepeat,
        notes = notes,
    )
}

/** Status shown on the optional Google Calendar screen. */
data class GoogleUiState(
    val connected: Boolean = false,
    val clientId: String = "",
    val lastSyncMillis: Long = 0L,
    val eventCount: Int = 0,
    val busy: Boolean = false,
    val statusMessage: String = "متصل نیست — اپ کاملاً آفلاین کار می‌کند",
    val authUrl: String? = null,
    val error: String = "",
)

/** Lock screen state: whether a PIN exists, whether the app is locked right now. */
data class LockUiState(
    val enabled: Boolean = false,
    val locked: Boolean = false,
    val blockedSeconds: Long = 0L,
    val failures: Int = 0,
    val error: String? = null,
    val justUnlocked: Boolean = false,
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
    private val tokens: GoogleTokenStore = container.googleTokens

    private val profilesStore = container.profiles
    private val security = container.security

    val eventCatalog: EventCatalog = container.events

    val profiles: StateFlow<List<LocalProfile>> = profilesStore.profiles

    val activeProfileId: StateFlow<String> = profilesStore.activeId

    val tasks: StateFlow<List<TaskEntity>> = repository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habitLogs: StateFlow<List<HabitLogEntity>> = repository.observeHabitLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val personalEvents: StateFlow<List<PersonalEventEntity>> = repository.observePersonalEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val googleEvents: StateFlow<List<ExternalEventEntity>> = repository.observeExternalEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = repository.observeFocusSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<TemplateEntity>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsStore.settings

    private val _focusTimer = MutableStateFlow(FocusTimerState())
    val focusTimer: StateFlow<FocusTimerState> = _focusTimer.asStateFlow()

    private val _google = MutableStateFlow(
        GoogleUiState(
            connected = tokens.isConnected(),
            clientId = settingsStore.settings.value.googleClientId,
            lastSyncMillis = tokens.lastSyncMillis(),
        ),
    )
    val google: StateFlow<GoogleUiState> = _google.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    private val _lock = MutableStateFlow(
        LockUiState(
            enabled = security.isPinSet,
            locked = security.isPinSet,
            blockedSeconds = security.blockedForSeconds(),
        ),
    )
    val lock: StateFlow<LockUiState> = _lock.asStateFlow()

    val secureScreen: Boolean get() = security.secureScreen

    val lockTimeoutMinutes: Int get() = security.lockAfterMinutes

    private var pausedAtMillis = 0L

    private var focusJob: Job? = null
    private var focusEndsAtMillis: Long = 0L
    private var focusStartedAtMillis: Long = 0L
    private var focusStartIfNeeded: Long = 0L

    init {
        // Alarms can be lost (reboot, app update, force stop): rebuild them once.
        viewModelScope.launch {
            runCatching { reminders.rescheduleAll(repository.loadTasksWithReminders()) }
            runCatching { repository.ensureTemplatesSeeded() }
            refreshFocusToday()
            refreshGoogleStatus()
            val focusSeconds = settingsStore.settings.value.focusMinutes * 60
            _focusTimer.update { it.copy(totalSeconds = focusSeconds, remainingSeconds = focusSeconds) }
        }
    }

    // --------------------------------------------------------------- tasks

    fun quickAdd(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        val today = JalaliDate.today()
        val (withoutProject, projectId) = extractProjectHint(text)
        val parsed = QuickAddParser.parse(withoutProject, today)
        val title = parsed.title.ifBlank { withoutProject.trim() }
        if (title.isBlank()) return
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
                projectId = projectId,
                tags = Tags.toCsv(parsed.tags),
            )
            if (task != null) {
                reminders.schedule(task)
                val summary = PersianCalendar.dueLabel(task.dueDate, task.dueMinuteOfDay, today)
                val tagPart = if (parsed.tags.isEmpty()) "" else " • " + parsed.tags.joinToString(" ") { Tags.display(it) }
                _events.send(
                    UiEvent.Message(
                        if (summary != null) "«${task.title}» برای $summary ثبت شد$tagPart" else "«${task.title}» ثبت شد$tagPart",
                    ),
                )
            }
        }
    }

    /** `+نام لیست` in quick add picks an existing project without leaving the text. */
    private fun extractProjectHint(text: String): Pair<String, String?> {
        val match = Regex("(?:^|\\s)\\+([^\\s#]+)").find(text) ?: return text to null
        val rawName = match.groupValues[1].trim()
        val project = projects.value.firstOrNull {
            PersianText.normalizeForMatching(it.name) == PersianText.normalizeForMatching(rawName)
        }
        if (project == null) return text to null
        val cleaned = text.removeRange(match.range).trim()
        return cleaned to project.id
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
                    tags = draft.tags,
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
                        tags = Tags.toCsv(Tags.parseCsv(draft.tags)),
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

    // ------------------------------------------------------------ projects

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

    fun setProjectColor(project: ProjectEntity, colorArgb: Int) {
        viewModelScope.launch { repository.updateProjectColor(project, colorArgb) }
    }

    // -------------------------------------------------------------- habits

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

    // -------------------------------------------------------------- events

    fun saveEvent(draft: EventDraft) {
        if (draft.title.isBlank()) return
        viewModelScope.launch {
            val saved = repository.savePersonalEvent(
                id = draft.id,
                title = draft.title,
                date = draft.date,
                colorArgb = draft.colorArgb,
                yearlyRepeat = draft.yearlyRepeat,
                notes = draft.notes,
            )
            if (saved != null) {
                _events.send(UiEvent.Message(if (draft.isNew) "رویداد شخصی اضافه شد" else "رویداد ویرایش شد"))
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deletePersonalEvent(id)
            _events.send(UiEvent.Message("رویداد حذف شد"))
        }
    }

    fun toggleCalendarSource(source: CalendarSource, enabled: Boolean) {
        settingsStore.setCalendarSource(source, enabled)
    }

    // ----------------------------------------------------------- templates

    fun applyTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            val created = repository.createTask(
                title = template.title,
                notes = template.notes,
                projectId = template.projectId,
                dueDate = JalaliDate.today(),
                dueMinuteOfDay = template.minuteOfDay,
                priority = template.priority,
                reminderMinutesBefore = settingsStore.settings.value.defaultReminderMinutes,
                tags = template.tagCsv,
            )
            if (created != null) {
                reminders.schedule(created)
                _events.send(UiEvent.Message("«${template.title}» از تمپلیت ساخته شد"))
            }
        }
    }

    fun createTemplate(name: String, title: String, notes: String, priority: Int, tags: String, minuteOfDay: Int?) {
        viewModelScope.launch {
            if (repository.createTemplate(name, title, notes, priority, minuteOfDay, tags) != null) {
                _events.send(UiEvent.Message("تمپلیت ذخیره شد"))
            }
        }
    }

    fun saveDraftAsTemplate(draft: TaskDraft) {
        val title = draft.title.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            repository.createTemplate(
                name = title,
                title = title,
                notes = draft.notes,
                priority = draft.priority,
                minuteOfDay = draft.dueMinuteOfDay,
                tags = draft.tags,
                projectId = draft.projectId,
            )
            _events.send(UiEvent.Message("به تمپلیت‌ها اضافه شد"))
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            _events.send(UiEvent.Message("تمپلیت حذف شد"))
        }
    }

    // --------------------------------------------------------------- focus

    fun startFocus(minutes: Int = settingsStore.settings.value.focusMinutes, task: TaskEntity? = null) {
        val totalSeconds = (minutes.coerceIn(1, 180)) * 60
        focusStartedAtMillis = System.currentTimeMillis()
        focusEndsAtMillis = focusStartedAtMillis + totalSeconds * 1000L
        _focusTimer.value = _focusTimer.value.copy(
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            running = true,
            onBreak = false,
            taskId = task?.id ?: _focusTimer.value.taskId,
            taskTitle = task?.title ?: _focusTimer.value.taskTitle,
            finishedAtEpochMillis = 0L,
        )
        launchTimer()
    }

    fun startBreak() {
        val totalSeconds = settingsStore.settings.value.breakMinutes.coerceIn(1, 30) * 60
        focusStartIfNeeded = System.currentTimeMillis()
        focusStartedAtMillis = focusStartIfNeeded
        focusEndsAtMillis = focusStartIfNeeded + totalSeconds * 1000L
        _focusTimer.value = _focusTimer.value.copy(
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            running = true,
            onBreak = true,
            finishedAtEpochMillis = 0L,
        )
        launchTimer()
    }

    private fun launchTimer() {
        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            while (isActive) {
                val remaining = ((focusEndsAtMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                _focusTimer.update { it.copy(remainingSeconds = remaining) }
                if (remaining <= 0) {
                    finishTimer()
                    break
                }
                delay(500)
            }
        }
    }

    private suspend fun finishTimer() {
        val state = _focusTimer.value
        _focusTimer.update {
            it.copy(running = false, remainingSeconds = 0, finishedAtEpochMillis = System.currentTimeMillis())
        }
        if (state.onBreak) {
            _focusTimer.update { it.copy(onBreak = false) }
            _events.send(UiEvent.Message("استراحت تمام شد؛ آمادهٔ تمرکز بعدی هستی"))
        } else {
            recordFocus(state.totalSeconds, completed = true)
            _events.send(UiEvent.Message("جلسهٔ تمرکز تمام شد؛ چند دقیقه استراحت کن"))
        }
    }

    fun pauseFocus() {
        if (!_focusTimer.value.running) return
        val remaining = ((focusEndsAtMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        focusJob?.cancel()
        _focusTimer.update { it.copy(running = false, remainingSeconds = remaining) }
    }

    fun resumeFocus() {
        val state = _focusTimer.value
        if (state.running || state.remainingSeconds <= 0) return
        focusEndsAtMillis = System.currentTimeMillis() + state.remainingSeconds * 1000L
        _focusTimer.update { it.copy(running = true) }
        launchTimer()
    }

    fun stopFocus() {
        val state = _focusTimer.value
        focusJob?.cancel()
        val elapsedSeconds = (state.totalSeconds - state.remainingSeconds).coerceAtLeast(0)
        if (!state.onBreak && elapsedSeconds >= 60) {
            recordFocus(elapsedSeconds, completed = false)
        }
        val focusSeconds = settingsStore.settings.value.focusMinutes * 60
        _focusTimer.value = FocusTimerState(
            totalSeconds = focusSeconds,
            remainingSeconds = focusSeconds,
            completedToday = state.completedToday,
            minutesToday = state.minutesToday,
            taskId = state.taskId,
            taskTitle = state.taskTitle,
        )
    }

    private fun recordFocus(seconds: Int, completed: Boolean) {
        viewModelScope.launch {
            val state = _focusTimer.value
            val record = FocusRecord(
                id = UUID.randomUUID().toString(),
                taskId = state.taskId,
                epochDay = repository.epochDayOf(System.currentTimeMillis()),
                seconds = seconds,
                completed = completed,
                startedAtEpochMillis = focusStartedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
            repository.recordFocus(record)
            refreshFocusToday()
        }
    }

    private fun refreshFocusToday() {
        viewModelScope.launch {
            val today = repository.epochDayOf(System.currentTimeMillis())
            val (sessions, minutes) = repository.focusDay(today)
            _focusTimer.update { it.copy(completedToday = sessions, minutesToday = minutes) }
        }
    }

    fun setFocusMinutes(minutes: Int) {
        settingsStore.setFocusMinutes(minutes)
        if (!_focusTimer.value.running && !_focusTimer.value.onBreak) {
            val seconds = settingsStore.settings.value.focusMinutes * 60
            _focusTimer.update { it.copy(totalSeconds = seconds, remainingSeconds = seconds, running = false) }
        }
    }

    fun setBreakMinutes(minutes: Int) = settingsStore.setBreakMinutes(minutes)

    fun setFocusGoalMinutes(minutes: Int) = settingsStore.setFocusGoalMinutes(minutes)

    fun clearFocusTask() {
        _focusTimer.update { it.copy(taskId = null, taskTitle = null) }
    }

    fun selectFocusTask(task: TaskEntity) {
        _focusTimer.update { it.copy(taskId = task.id, taskTitle = task.title) }
    }

    // --------------------------------------------------------------- google

    fun setGoogleClientId(clientId: String) {
        settingsStore.setGoogleClientId(clientId)
        _google.update { it.copy(clientId = clientId.trim()) }
    }

    fun consumeGoogleAuthUrl() {
        _google.update { it.copy(authUrl = null) }
    }

    /**
     * Starts the PKCE + loopback flow. The browser opens [GoogleUiState.authUrl];
     * the tiny loopback server in [GoogleOAuth] receives the redirect.
     */
    fun connectGoogle() {
        val clientId = settingsStore.settings.value.googleClientId.trim()
        if (clientId.isBlank()) {
            _google.update { it.copy(error = "شناسهٔ کلاینت OAuth را وارد کن", statusMessage = "برای اتصال، Client ID لازم است") }
            return
        }
        if (_google.value.busy) return
        _google.update { it.copy(busy = true, error = "", statusMessage = "در حال باز کردن صفحهٔ ورود گوگل…") }
        viewModelScope.launch {
            val port = withContext(Dispatchers.IO) { GoogleOAuth.freePort() }
            val verifier = GoogleOAuth.codeVerifier()
            val state = UUID.randomUUID().toString()
            val redirect = GoogleOAuth.redirectUri(port)
            val url = GoogleOAuth.authorizeUrl(clientId, redirect, GoogleOAuth.codeChallenge(verifier), state)
            _google.update { it.copy(authUrl = url, statusMessage = "منتظر تأیید در مرورگر…") }

            val code = GoogleOAuth.awaitCode(port, state, timeoutMillis = 300_000)
            if (code == null) {
                _google.update {
                    it.copy(busy = false, error = "ورود کامل نشد یا زمان تمام شد", statusMessage = "متصل نیست — اپ آفلاین کار می‌کند")
                }
                return@launch
            }
            _google.update { it.copy(statusMessage = "در حال دریافت دسترسی…") }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val token = GoogleOAuth.exchangeCode(clientId, code, verifier, redirect)
                    tokens.save(token)
                    token
                }
            }
            result.fold(
                onSuccess = {
                    settingsStore.setGoogleConnected(true)
                    GoogleSyncScheduler.schedule(getApplication<Application>())
                    _google.update { it.copy(busy = false, connected = true, error = "") }
                    syncGoogleNow()
                },
                onFailure = { error ->
                    _google.update {
                        it.copy(
                            busy = false,
                            connected = tokens.isConnected(),
                            error = error.message ?: "تبادل توکن ناموفق بود",
                            statusMessage = "اتصال برقرار نشد",
                        )
                    }
                },
            )
        }
    }

    fun disconnectGoogle() {
        viewModelScope.launch {
            tokens.clear()
            settingsStore.setGoogleConnected(false)
            GoogleSyncScheduler.cancel(getApplication<Application>())
            runCatching { repository.clearExternalEvents() }
            _google.update {
                it.copy(
                    connected = false,
                    busy = false,
                    eventCount = 0,
                    lastSyncMillis = 0L,
                    statusMessage = "اتصال قطع شد؛ دادهٔ محلی دست‌نخورده است",
                    error = "",
                )
            }
            _events.send(UiEvent.Message("تقویم گوگل قطع شد"))
        }
    }

    fun syncGoogleNow() {
        val clientId = settingsStore.settings.value.googleClientId.trim()
        if (clientId.isBlank() || !tokens.isConnected()) {
            _google.update { it.copy(error = "اول حساب گوگل را وصل کن", statusMessage = "متصل نیست") }
            return
        }
        _google.update { it.copy(busy = true, error = "", statusMessage = "در حال همگام‌سازی…") }
        viewModelScope.launch {
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    val token = tokens.validAccessToken(clientId) ?: throw IllegalStateException("دسترسی منقضی شده؛ دوباره وصل شو")
                    GoogleCalendarApi.fetchEvents(token)
                }
            }
            outcome.fold(
                onSuccess = { events ->
                    repository.replaceExternalEvents(events)
                    tokens.setLastSync(System.currentTimeMillis())
                    _google.update {
                        it.copy(
                            busy = false,
                            connected = true,
                            lastSyncMillis = tokens.lastSyncMillis(),
                            eventCount = events.size,
                            statusMessage = "همگام شد • ${PersianText.number(events.size)} رویداد در حافظهٔ محلی",
                            error = "",
                        )
                    }
                    _events.send(UiEvent.Message("تقویم گوگل همگام شد"))
                },
                onFailure = { error ->
                    _google.update {
                        it.copy(
                            busy = false,
                            lastSyncMillis = tokens.lastSyncMillis(),
                            statusMessage = "همگام‌سازی ناموفق؛ رویدادهای ذخیره‌شده نمایش داده می‌شوند",
                            error = error.message ?: "خطای شبکه",
                        )
                    }
                },
            )
        }
    }

    private fun refreshGoogleStatus() {
        viewModelScope.launch {
            val count = runCatching { repository.externalEventsAsCalendar().size }.getOrDefault(0)
            _google.update {
                it.copy(
                    connected = tokens.isConnected(),
                    clientId = settingsStore.settings.value.googleClientId,
                    lastSyncMillis = tokens.lastSyncMillis(),
                    eventCount = count,
                    statusMessage = when {
                        tokens.lastSyncMillis() > 0 -> "آخرین همگام‌سازی ذخیره شده است"
                        tokens.isConnected() -> "متصل؛ هنوز همگام نشده"
                        else -> "متصل نیست — اپ کاملاً آفلاین کار می‌کند"
                    },
                )
            }
        }
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
                    runCatching { repository.ensureTemplatesSeeded() }
                    refreshFocusToday()
                    "بازیابی انجام شد: ${PersianText.number(summary.tasks)} کار، " +
                        "${PersianText.number(summary.projects)} لیست، ${PersianText.number(summary.habits)} عادت، " +
                        "${PersianText.number(summary.events)} رویداد و ${PersianText.number(summary.focusSessions)} جلسهٔ تمرکز"
                },
                onFailure = { error -> "بازیابی ناموفق بود: ${error.message ?: "فایل معتبر نیست"}" },
            )
            _events.send(UiEvent.Message(message))
        }
    }

    // ------------------------------------------------------------- accounts

    fun createProfile(name: String, emoji: String, colorArgb: Int) {
        val profile = profilesStore.create(name, emoji, colorArgb)
        switchProfile(profile.id)
        viewModelScope.launch { _events.send(UiEvent.Message("حساب «${profile.name}» ساخته شد")) }
    }

    fun updateProfile(profile: LocalProfile, name: String, emoji: String, colorArgb: Int) {
        profilesStore.update(profile.id, name, emoji, colorArgb)
        viewModelScope.launch { _events.send(UiEvent.Message("حساب به‌روز شد")) }
    }

    fun switchProfile(id: String) {
        if (id == profilesStore.activeId.value) return
        stopFocus()
        profilesStore.setActive(id)
        refreshFocusToday()
        val name = profilesStore.active.name
        viewModelScope.launch { _events.send(UiEvent.Message("حساب فعال: $name")) }
    }

    fun deleteProfile(profile: LocalProfile) {
        if (profile.id == DEFAULT_PROFILE_ID) return
        viewModelScope.launch {
            runCatching { repository.deleteProfileData(profile.id) }
            profilesStore.remove(profile.id)
            refreshFocusToday()
            _events.send(UiEvent.Message("حساب و داده‌هایش حذف شد"))
        }
    }

    suspend fun profileHasData(profile: LocalProfile): Boolean =
        runCatching { !repository.profileIsEmpty(profile.id) }.getOrDefault(false)

    // ------------------------------------------------------------- security

    fun enableLock(pin: String): Boolean {
        if (!isPinValid(pin)) {
            _lock.update { it.copy(error = "PIN باید ۴ تا ۸ رقم باشد") }
            return false
        }
        security.setPin(pin)
        security.resetFailures()
        _lock.value = LockUiState(enabled = true, locked = false)
        viewModelScope.launch { _events.send(UiEvent.Message("قفل ورود فعال شد")) }
        return true
    }

    fun changeLockPin(pin: String): Boolean = enableLock(pin)

    fun disableLock(pin: String): Boolean {
        if (!security.verify(pin)) {
            _lock.update { it.copy(error = "PIN درست نیست") }
            return false
        }
        security.clearPin()
        _lock.value = LockUiState(enabled = false, locked = false)
        viewModelScope.launch { _events.send(UiEvent.Message("قفل ورود خاموش شد")) }
        return true
    }

    fun unlock(pin: String) {
        val blocked = security.blockedForSeconds()
        if (blocked > 0) {
            _lock.update { it.copy(blockedSeconds = blocked, error = "کمی صبر کن") }
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { security.verify(pin) }
            if (ok) {
                security.resetFailures()
                _lock.value = LockUiState(enabled = true, locked = false, justUnlocked = true)
            } else {
                val failures = withContext(Dispatchers.Default) { security.registerFailure() }
                val wait = security.blockedForSeconds()
                _lock.update {
                    it.copy(
                        locked = true,
                        failures = failures,
                        blockedSeconds = wait,
                        error = if (failures >= 3) "چند بار اشتباه؛ ${wait} ثانیه صبر کن" else "PIN درست نیست",
                    )
                }
            }
        }
    }

    fun lockNow() {
        if (!security.isPinSet) return
        _lock.value = LockUiState(enabled = true, locked = true)
    }

    fun setLockAfterMinutes(minutes: Int) = security.setLockAfterMinutes(minutes)

    fun setSecureScreen(enabled: Boolean) = security.setSecureScreen(enabled)

    /** Called from `MainActivity.onStop`; records when the UI left the foreground. */
    fun onAppPaused() {
        pausedAtMillis = System.currentTimeMillis()
    }

    /** Called from `MainActivity.onStart`; locks again after the chosen timeout. */
    fun onAppResumed() {
        if (!security.isPinSet) return
        if (pausedAtMillis == 0L) return
        val timeout = security.lockAfterMinutes
        val elapsedMinutes = (System.currentTimeMillis() - pausedAtMillis) / 60_000
        if (timeout == 0 || elapsedMinutes >= timeout) {
            _lock.value = LockUiState(enabled = true, locked = true)
        }
        pausedAtMillis = 0L
    }

    // ----------------------------------------------------------- appearance

    fun setAccent(accent: AccentColor) = settingsStore.setAccent(accent)

    fun setCompact(enabled: Boolean) = settingsStore.setCompact(enabled)

    // ---------------------------------------------------------- task extras

    fun duplicateTask(task: TaskEntity) {
        viewModelScope.launch {
            val copy = repository.duplicateTask(task)
            if (copy != null) {
                reminders.schedule(copy)
                _events.send(UiEvent.Message("یک نسخهٔ تازه ساخته شد"))
            }
        }
    }

    fun moveTask(task: TaskEntity, projectId: String?) {
        viewModelScope.launch {
            repository.updateTask(task.copy(projectId = projectId))
            _events.send(UiEvent.Message(if (projectId == null) "از لیست خارج شد" else "به لیست منتقل شد"))
        }
    }

    fun setTaskPriority(task: TaskEntity, priority: Int) {
        viewModelScope.launch {
            repository.updateTask(task.copy(priority = priority))
            _events.send(UiEvent.Message("اهمیت به‌روز شد"))
        }
    }
}
