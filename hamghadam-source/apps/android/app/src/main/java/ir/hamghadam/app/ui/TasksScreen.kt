package ir.hamghadam.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.TaskQuery
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.TaskRow

private enum class TaskFilter(val label: String) {
    ALL("همه"),
    TODAY("امروز"),
    UPCOMING("آینده"),
    DONE("انجام‌شده"),
}

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    settings: AppSettings,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits

    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.ALL.name) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = TaskFilter.entries.firstOrNull { it.name == filterName } ?: TaskFilter.ALL

    val projectNames = remember(projects) { projects.associate { it.id to it.name } }

    val visible = remember(tasks, query, filter, projectId, todayEpoch) {
        val scoped = tasks.filter { task ->
            val inProject = projectId == null || task.projectId == projectId
            val matchesSearch = query.isBlank() ||
                TaskQuery.search(listOf(task), query) { "${it.title} ${it.notes}" }.isNotEmpty()
            inProject && matchesSearch
        }
        val byFilter = scoped.filter { task ->
            when (filter) {
                TaskFilter.ALL -> !task.isCompleted
                TaskFilter.TODAY -> !task.isCompleted && task.dueEpochDay == todayEpoch
                TaskFilter.UPCOMING -> !task.isCompleted && (task.dueEpochDay ?: Long.MIN_VALUE) > todayEpoch
                TaskFilter.DONE -> task.isCompleted
            }
        }
        TaskQuery.sortForDisplay(
            items = byFilter,
            epochDay = { it.dueEpochDay },
            minuteOfDay = { it.dueMinuteOfDay },
            priority = { it.priority },
            createdAt = { it.createdAtEpochMillis },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "کارها",
                subtitle = "${PersianText.number(tasks.count { !it.isCompleted }, digits)} کار باز در " +
                    "${PersianText.number(projects.size, digits)} لیست",
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جست‌وجو در عنوان و یادداشت", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskFilter.entries.toList()) { entry ->
                    PillChip(
                        text = entry.label,
                        selected = filter == entry,
                        onClick = { filterName = entry.name },
                    )
                }
            }
        }
        if (projects.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PillChip(
                            text = "همهٔ لیست‌ها",
                            selected = projectId == null,
                            onClick = { projectId = null },
                        )
                    }
                    items(projects) { project ->
                        PillChip(
                            text = project.name,
                            selected = projectId == project.id,
                            onClick = { projectId = project.id },
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "${PersianText.number(visible.size, digits)} کار",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    title = when (filter) {
                        TaskFilter.DONE -> "هنوز کاری انجام نشده"
                        TaskFilter.TODAY -> "برای امروز کاری نیست"
                        TaskFilter.UPCOMING -> "برنامهٔ آینده خالی است"
                        TaskFilter.ALL -> if (query.isBlank()) "فهرست خالی است" else "چیزی پیدا نشد"
                    },
                    subtitle = if (query.isBlank()) {
                        "با دکمهٔ + یک کار تازه بساز."
                    } else {
                        "عبارت دیگری را امتحان کن."
                    },
                )
            }
        } else {
            items(visible, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    today = today,
                    persianDigits = digits,
                    projectName = projectNames[task.projectId],
                    onClick = { onEdit(task) },
                    onToggle = { onToggle(task) },
                    onDelete = { onDelete(task) },
                )
            }
        }
    }
}
