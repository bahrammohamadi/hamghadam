package ir.hamghadam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hamghadam.app.AppViewModel
import ir.hamghadam.app.EventDraft
import ir.hamghadam.app.TaskDraft
import ir.hamghadam.app.UiEvent
import ir.hamghadam.app.toDraft
import ir.hamghadam.app.data.ThemeMode
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.ui.theme.HamghadamTheme

enum class AppTab(val label: String) {
    HOME("خانه"),
    TASKS("کارها"),
    CALENDAR("تقویم"),
    FOCUS("تمرکز"),
    MORE("بیشتر"),
    ;

    val icon: ImageVector
        get() = when (this) {
            HOME -> Icons.Filled.Home
            TASKS -> Icons.AutoMirrored.Filled.List
            CALENDAR -> Icons.Filled.DateRange
            FOCUS -> Icons.Filled.PlayArrow
            MORE -> Icons.Filled.Menu
        }
}

private object MoreRoute {
    const val HABITS = "habits"
    const val STATS = "stats"
    const val TEMPLATES = "templates"
    const val CALENDAR = "calendar-settings"
    const val GOOGLE = "google"
    const val SETTINGS = "settings"
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val habitLogs by viewModel.habitLogs.collectAsStateWithLifecycle()
    val personalEvents by viewModel.personalEvents.collectAsStateWithLifecycle()
    val googleEvents by viewModel.googleEvents.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val focusTimer by viewModel.focusTimer.collectAsStateWithLifecycle()
    val googleState by viewModel.google.collectAsStateWithLifecycle()

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HamghadamTheme(darkTheme = darkTheme) {
            var tabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
            var moreRoute by rememberSaveable { mutableStateOf<String?>(null) }
            var activeDraft by remember { mutableStateOf<TaskDraft?>(null) }
            val snackbarHostState = remember { SnackbarHostState() }
            val currentTab = AppTab.entries.firstOrNull { it.name == tabName } ?: AppTab.HOME
            val today = JalaliDate.today()
            val knownTags = remember(tasks) { Tags.counts(tasks.map { it.tags }).map { it.first } }

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.TaskDeleted -> {
                            val result = snackbarHostState.showSnackbar(
                                message = "«${event.title}» حذف شد",
                                actionLabel = "واگرد",
                                withDismissAction = false,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.id)
                        }
                        is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                    }
                }
            }

            BackHandler(enabled = activeDraft != null) { activeDraft = null }
            BackHandler(enabled = activeDraft == null && moreRoute != null) { moreRoute = null }

            val draft = activeDraft
            when {
                draft != null -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .systemBarsPadding()
                        .imePadding(),
                ) {
                    TaskEditorScreen(
                        initial = draft,
                        projects = projects,
                        settings = settings,
                        knownTags = knownTags,
                        onSave = { saved ->
                            viewModel.saveDraft(saved)
                            activeDraft = null
                        },
                        onCancel = { activeDraft = null },
                        onDelete = { toDelete ->
                            viewModel.deleteDraft(toDelete)
                            activeDraft = null
                        },
                        onSaveAsTemplate = { saved -> viewModel.saveDraftAsTemplate(saved) },
                    )
                }

                moreRoute != null -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .systemBarsPadding(),
                ) {
                    when (moreRoute) {
                        MoreRoute.HABITS -> HabitsScreen(
                            habits = habits,
                            logs = habitLogs,
                            settings = settings,
                            onToggleDay = { habitId, date, done ->
                                viewModel.toggleHabitDay(habitId, date, done)
                            },
                            onCreate = viewModel::createHabit,
                            onDelete = viewModel::deleteHabit,
                            onBack = { moreRoute = null },
                        )

                        MoreRoute.STATS -> StatsScreen(
                            tasks = tasks,
                            projects = projects,
                            habits = habits,
                            habitLogs = habitLogs,
                            focusSessions = focusSessions,
                            settings = settings,
                            onBack = { moreRoute = null },
                        )

                        MoreRoute.TEMPLATES -> TemplatesScreen(
                            templates = templates,
                            settings = settings,
                            onBack = { moreRoute = null },
                            onUse = viewModel::applyTemplate,
                            onCreate = { name, title, notes, priority, tags, minute ->
                                viewModel.createTemplate(name, title, notes, priority, tags, minute)
                            },
                            onDelete = viewModel::deleteTemplate,
                        )

                        MoreRoute.CALENDAR -> CalendarSettingsScreen(
                            settings = settings,
                            catalog = viewModel.eventCatalog,
                            personalEvents = personalEvents,
                            googleEventCount = googleEvents.size,
                            onBack = { moreRoute = null },
                            onToggleSource = { source, enabled ->
                                viewModel.toggleCalendarSource(source, enabled)
                            },
                            onDeleteEvent = viewModel::deleteEvent,
                            onOpenGoogle = { moreRoute = MoreRoute.GOOGLE },
                        )

                        MoreRoute.GOOGLE -> GoogleCalendarScreen(
                            state = googleState,
                            settings = settings,
                            events = googleEvents,
                            onBack = { moreRoute = null },
                            onClientIdChange = viewModel::setGoogleClientId,
                            onConnect = viewModel::connectGoogle,
                            onDisconnect = viewModel::disconnectGoogle,
                            onSync = viewModel::syncGoogleNow,
                            onAuthUrlOpened = viewModel::consumeGoogleAuthUrl,
                        )

                        MoreRoute.SETTINGS -> SettingsScreen(
                            settings = settings,
                            projects = projects,
                            onThemeChange = viewModel::setThemeMode,
                            onDigitsChange = viewModel::setPersianDigits,
                            onDefaultReminderChange = viewModel::setDefaultReminder,
                            onFocusMinutesChange = viewModel::setFocusMinutes,
                            onBreakMinutesChange = viewModel::setBreakMinutes,
                            onFocusGoalChange = viewModel::setFocusGoalMinutes,
                            onCreateProject = viewModel::createProject,
                            onDeleteProject = viewModel::deleteProject,
                            onExport = viewModel::exportBackup,
                            onImport = viewModel::importBackup,
                            onReschedule = viewModel::rescheduleReminders,
                            onBack = { moreRoute = null },
                        )
                    }
                }

                else -> Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            for (tab in AppTab.entries) {
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { tabName = tab.name },
                                    icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(21.dp)) },
                                    label = { Text(tab.label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentTab == AppTab.HOME || currentTab == AppTab.TASKS || currentTab == AppTab.CALENDAR) {
                            FloatingActionButton(
                                onClick = { activeDraft = TaskDraft(dueDate = today) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "افزودن کار")
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (currentTab) {
                            AppTab.HOME -> HomeScreen(
                                tasks = tasks,
                                projects = projects,
                                habits = habits,
                                habitLogs = habitLogs,
                                focusSessions = focusSessions,
                                settings = settings,
                                focusTimer = focusTimer,
                                onToggle = viewModel::toggleTask,
                                onDelete = viewModel::deleteTask,
                                onEdit = { activeDraft = it.toDraft() },
                                onQuickAdd = viewModel::quickAdd,
                                onOpenSettings = { moreRoute = MoreRoute.SETTINGS },
                                onOpenFocus = { tabName = AppTab.FOCUS.name },
                                onOpenHabits = { moreRoute = MoreRoute.HABITS },
                                onOpenTemplates = { moreRoute = MoreRoute.TEMPLATES },
                                onOpenStats = { moreRoute = MoreRoute.STATS },
                                onOpenTasks = { tabName = AppTab.TASKS.name },
                                onAddForDay = { day -> activeDraft = TaskDraft(dueDate = day) },
                            )

                            AppTab.TASKS -> TasksScreen(
                                tasks = tasks,
                                projects = projects,
                                settings = settings,
                                onToggle = viewModel::toggleTask,
                                onDelete = viewModel::deleteTask,
                                onEdit = { activeDraft = it.toDraft() },
                            )

                            AppTab.CALENDAR -> CalendarScreen(
                                tasks = tasks,
                                projects = projects,
                                settings = settings,
                                catalog = viewModel.eventCatalog,
                                personalEvents = personalEvents,
                                googleEvents = googleEvents,
                                onToggle = viewModel::toggleTask,
                                onDelete = viewModel::deleteTask,
                                onEdit = { activeDraft = it.toDraft() },
                                onAddForDay = { day -> activeDraft = TaskDraft(dueDate = day) },
                                onSaveEvent = { draft: EventDraft -> viewModel.saveEvent(draft) },
                                onDeleteEvent = viewModel::deleteEvent,
                                onToggleSource = { source, enabled ->
                                    viewModel.toggleCalendarSource(source, enabled)
                                },
                            )

                            AppTab.FOCUS -> FocusScreen(
                                tasks = tasks,
                                settings = settings,
                                timer = focusTimer,
                                sessions = focusSessions,
                                onStart = { minutes -> viewModel.startFocus(minutes) },
                                onStartBreak = viewModel::startBreak,
                                onPause = viewModel::pauseFocus,
                                onResume = viewModel::resumeFocus,
                                onStop = viewModel::stopFocus,
                                onSelectTask = viewModel::selectFocusTask,
                                onClearTask = viewModel::clearFocusTask,
                                onSetDuration = viewModel::setFocusMinutes,
                                onSetGoal = viewModel::setFocusGoalMinutes,
                                onOpenTimerSettings = { moreRoute = MoreRoute.SETTINGS },
                            )

                            AppTab.MORE -> MoreScreen(
                                settings = settings,
                                openTasks = tasks.count { !it.isCompleted },
                                eventCount = personalEvents.size + googleEvents.size,
                                focusSessions = focusSessions.count { it.completed },
                                onOpenHabits = { moreRoute = MoreRoute.HABITS },
                                onOpenStats = { moreRoute = MoreRoute.STATS },
                                onOpenTemplates = { moreRoute = MoreRoute.TEMPLATES },
                                onOpenCalendarSettings = { moreRoute = MoreRoute.CALENDAR },
                                onOpenGoogleCalendar = { moreRoute = MoreRoute.GOOGLE },
                                onOpenSettings = { moreRoute = MoreRoute.SETTINGS },
                            )
                        }
                    }
                }
            }
        }
    }
}
