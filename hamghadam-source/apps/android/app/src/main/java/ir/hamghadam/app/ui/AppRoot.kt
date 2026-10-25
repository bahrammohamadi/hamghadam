package ir.hamghadam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import ir.hamghadam.app.TaskDraft
import ir.hamghadam.app.UiEvent
import ir.hamghadam.app.toDraft
import ir.hamghadam.app.data.ThemeMode
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.ui.theme.HamghadamTheme

enum class AppTab(val label: String) {
    TODAY("امروز"),
    TASKS("کارها"),
    CALENDAR("تقویم"),
    HABITS("عادت‌ها"),
    ;

    val icon: ImageVector
        get() = when (this) {
            TODAY -> Icons.Filled.Home
            TASKS -> Icons.AutoMirrored.Filled.List
            CALENDAR -> Icons.Filled.DateRange
            HABITS -> Icons.Filled.Favorite
        }
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val habitLogs by viewModel.habitLogs.collectAsStateWithLifecycle()

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HamghadamTheme(darkTheme = darkTheme) {
            var tabName by rememberSaveable { mutableStateOf(AppTab.TODAY.name) }
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var activeDraft by remember { mutableStateOf<TaskDraft?>(null) }
            val snackbarHostState = remember { SnackbarHostState() }
            val currentTab = AppTab.entries.firstOrNull { it.name == tabName } ?: AppTab.TODAY
            val today = JalaliDate.today()

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
            BackHandler(enabled = activeDraft == null && showSettings) { showSettings = false }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                val draft = activeDraft
                when {
                    draft != null -> TaskEditorScreen(
                        initial = draft,
                        projects = projects,
                        settings = settings,
                        onSave = { saved ->
                            viewModel.saveDraft(saved)
                            activeDraft = null
                        },
                        onCancel = { activeDraft = null },
                        onDelete = { toDelete ->
                            viewModel.deleteDraft(toDelete)
                            activeDraft = null
                        },
                    )

                    showSettings -> SettingsScreen(
                        settings = settings,
                        projects = projects,
                        onThemeChange = viewModel::setThemeMode,
                        onDigitsChange = viewModel::setPersianDigits,
                        onDefaultReminderChange = viewModel::setDefaultReminder,
                        onCreateProject = viewModel::createProject,
                        onDeleteProject = viewModel::deleteProject,
                        onExport = viewModel::exportBackup,
                        onImport = viewModel::importBackup,
                        onReschedule = viewModel::rescheduleReminders,
                        onBack = { showSettings = false },
                    )

                    else -> Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                for (tab in AppTab.entries) {
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { tabName = tab.name },
                                        icon = { Icon(tab.icon, contentDescription = null) },
                                        label = { Text(tab.label, fontSize = 11.sp) },
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { activeDraft = TaskDraft(dueDate = today) },
                                containerColor = MaterialTheme.colorScheme.primary,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "افزودن کار")
                            }
                        },
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            when (currentTab) {
                                AppTab.TODAY -> TodayScreen(
                                    tasks = tasks,
                                    projects = projects,
                                    settings = settings,
                                    onToggle = viewModel::toggleTask,
                                    onDelete = viewModel::deleteTask,
                                    onEdit = { activeDraft = it.toDraft() },
                                    onQuickAdd = viewModel::quickAdd,
                                    onOpenSettings = { showSettings = true },
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
                                    onToggle = viewModel::toggleTask,
                                    onDelete = viewModel::deleteTask,
                                    onEdit = { activeDraft = it.toDraft() },
                                    onAddForDay = { day -> activeDraft = TaskDraft(dueDate = day) },
                                )

                                AppTab.HABITS -> HabitsScreen(
                                    habits = habits,
                                    logs = habitLogs,
                                    settings = settings,
                                    onToggleDay = { habitId, date, done ->
                                        viewModel.toggleHabitDay(habitId, date, done)
                                    },
                                    onCreate = viewModel::createHabit,
                                    onDelete = viewModel::deleteHabit,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
