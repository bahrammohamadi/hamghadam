package ir.hamghadam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.domain.TaskQuery
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.IconTile
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.TaskRow
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

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
    onLongPress: (TaskEntity) -> Unit = {},
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits

    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.ALL.name) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = TaskFilter.entries.firstOrNull { it.name == filterName } ?: TaskFilter.ALL

    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    val tagCounts = remember(tasks) {
        Tags.counts(tasks.filter { !it.isCompleted }.map { it.tags })
    }

    val visible = remember(tasks, query, filter, projectId, tag, todayEpoch) {
        val scoped = tasks.filter { task ->
            val inProject = projectId == null || task.projectId == projectId
            val hasTag = tag == null || task.tagList.contains(tag)
            val matchesSearch = query.isBlank() ||
                TaskQuery.search(listOf(task), query) { "${it.title} ${it.notes} ${it.tags}" }.isNotEmpty()
            inProject && hasTag && matchesSearch
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
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "کارها",
                subtitle = "${PersianText.number(tasks.count { !it.isCompleted }, digits)} کار باز • " +
                    "${PersianText.number(projects.size, digits)} لیست",
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جست‌وجو در عنوان، یادداشت و برچسب", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = CardRadius.chip,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = HamghadamPalette.Violet,
                ),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TaskFilter.entries.toList()) { entry ->
                    PillChip(
                        text = entry.label,
                        selected = filter == entry,
                        onClick = { filterName = entry.name },
                    )
                }
            }
        }
        if (tagCounts.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        PillChip(
                            text = "همهٔ برچسب‌ها",
                            selected = tag == null,
                            onClick = { tag = null },
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    items(tagCounts) { (name, count) ->
                        PillChip(
                            text = PersianText.toPersianDigits("#$name") + " " + PersianText.number(count, digits),
                            selected = tag == name,
                            onClick = { tag = if (tag == name) null else name },
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        if (projects.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        PillChip(
                            text = "همهٔ لیست‌ها",
                            selected = projectId == null,
                            onClick = { projectId = null },
                        )
                    }
                    items(projects, key = { it.id }) { project ->
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${PersianText.number(visible.size, digits)} کار",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                if (tag != null || projectId != null || query.isNotBlank()) {
                    Text(
                        text = "پاک کردن فیلترها",
                        modifier = Modifier.clickable {
                            tag = null
                            projectId = null
                            query = ""
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
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
                    subtitle = if (query.isBlank()) "با دکمهٔ + یک کار تازه بساز." else "عبارت دیگری را امتحان کن.",
                    emoji = "🗂️",
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
                    onLongPress = { onLongPress(task) },
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile("💡", HamghadamPalette.Amber, HamghadamPalette.AmberSoft, size = 34.dp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "در ثبت سریع از «#برچسب» و «+نام لیست» استفاده کن؛ همه‌چیز آفلاین است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
