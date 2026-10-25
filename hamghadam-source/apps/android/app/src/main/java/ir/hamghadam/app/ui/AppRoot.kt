package ir.hamghadam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hamghadam.app.AppViewModel
import ir.hamghadam.app.EventDraft
import ir.hamghadam.app.TaskDraft
import ir.hamghadam.app.UiEvent
import ir.hamghadam.app.data.ThemeMode
import ir.hamghadam.app.domain.EventProjection
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.toDraft
import ir.hamghadam.app.ui.components.softShadow
import ir.hamghadam.app.ui.theme.HamghadamPalette
import ir.hamghadam.app.ui.theme.HamghadamTheme

enum class AppTab(val label: String) {
    HOME("خانه"),
    TASKS("کارها"),
    CALENDAR("تقویم"),
    MORE("بیشتر"),
    ;

    val icon: ImageVector
        get() = when (this) {
            HOME -> Icons.Filled.Home
            TASKS -> Icons.AutoMirrored.Filled.List
            CALENDAR -> Icons.Filled.DateRange
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
    const val FOCUS = "focus"
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
            val todayHighlight = remember(personalEvents, googleEvents, settings.calendarSources, today) {
                val year = today.year
                val base = viewModel.eventCatalog.eventsForYear(year) +
                    EventProjection.expandPersonal(personalEvents.map { it.toModel() }, year) +
                    googleEvents.mapNotNull { it.toEvent() }
                EventProjection.enabled(base, settings.calendarSources)
                    .firstOrNull { it.date == today }?.title
            }

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
                        .statusBarsPadding()
                        .navigationBarsPadding()
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
                        .statusBarsPadding()
                        .navigationBarsPadding(),
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

                        MoreRoute.FOCUS -> FocusScreen(
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
                            onBack = { moreRoute = null },
                        )

                        else -> SettingsScreen(
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
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(bottom = 132.dp, start = 16.dp, end = 16.dp),
                        )
                    },
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                            when (currentTab) {
                                AppTab.HOME -> HomeScreen(
                                    tasks = tasks,
                                    projects = projects,
                                    habits = habits,
                                    habitLogs = habitLogs,
                                    focusSessions = focusSessions,
                                    settings = settings,
                                    focusTimer = focusTimer,
                                    todayHighlight = todayHighlight,
                                    onToggle = viewModel::toggleTask,
                                    onDelete = viewModel::deleteTask,
                                    onEdit = { activeDraft = it.toDraft() },
                                    onQuickAdd = viewModel::quickAdd,
                                    onOpenSettings = { moreRoute = MoreRoute.SETTINGS },
                                    onOpenFocus = { moreRoute = MoreRoute.FOCUS },
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
                                    onSaveEvent = { eventDraft: EventDraft -> viewModel.saveEvent(eventDraft) },
                                    onDeleteEvent = viewModel::deleteEvent,
                                    onToggleSource = { source, enabled ->
                                        viewModel.toggleCalendarSource(source, enabled)
                                    },
                                )

                                AppTab.MORE -> MoreScreen(
                                    settings = settings,
                                    openTasks = tasks.count { !it.isCompleted },
                                    eventCount = personalEvents.size + googleEvents.size,
                                    focusSessions = focusSessions.count { it.completed },
                                    onOpenFocus = { moreRoute = MoreRoute.FOCUS },
                                    onOpenHabits = { moreRoute = MoreRoute.HABITS },
                                    onOpenStats = { moreRoute = MoreRoute.STATS },
                                    onOpenTemplates = { moreRoute = MoreRoute.TEMPLATES },
                                    onOpenCalendarSettings = { moreRoute = MoreRoute.CALENDAR },
                                    onOpenGoogleCalendar = { moreRoute = MoreRoute.GOOGLE },
                                    onOpenSettings = { moreRoute = MoreRoute.SETTINGS },
                                )
                            }
                        }

                        FloatingNavBar(
                            current = currentTab,
                            onSelect = { tabName = it.name },
                            onAdd = { activeDraft = TaskDraft(dueDate = today) },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating pill navigation with the four destinations and a black centre "+"
 * button, matching the reference design. The bar floats above the content; every
 * screen keeps 104dp bottom padding so nothing hides underneath it.
 */
@Composable
private fun FloatingNavBar(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
            .height(72.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .softShadow(radius = 32.dp, elevation = 18.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(AppTab.HOME, current, onSelect, Modifier.weight(1f))
            NavItem(AppTab.TASKS, current, onSelect, Modifier.weight(1f))
            Spacer(Modifier.width(64.dp))
            NavItem(AppTab.CALENDAR, current, onSelect, Modifier.weight(1f))
            NavItem(AppTab.MORE, current, onSelect, Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .softShadow(radius = 28.dp, elevation = 18.dp)
                .clip(CircleShape)
                .background(HamghadamPalette.Scrim)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "افزودن کار",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: AppTab,
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = tab == current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect(tab) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (active) HamghadamPalette.VioletSoft else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = if (active) HamghadamPalette.Violet else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) HamghadamPalette.Violet else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
