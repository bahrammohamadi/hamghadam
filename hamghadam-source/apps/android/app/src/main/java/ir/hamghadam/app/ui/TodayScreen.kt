package ir.hamghadam.app.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.QuickAddParser
import ir.hamghadam.app.domain.TaskQuery
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.ProgressCard
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionHeader
import ir.hamghadam.app.ui.components.TaskRow
import java.time.LocalTime

@Composable
fun TodayScreen(
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    settings: AppSettings,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onQuickAdd: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits

    val buckets = remember(tasks, todayEpoch) {
        TaskQuery.bucket(
            items = tasks,
            todayEpochDay = todayEpoch,
            epochDay = { it.dueEpochDay },
            isCompleted = { it.isCompleted },
            minuteOfDay = { it.dueMinuteOfDay },
            priority = { it.priority },
            createdAt = { it.createdAtEpochMillis },
        )
    }
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    val todayDone = tasks.count { it.isCompleted && it.dueEpochDay == todayEpoch }
    val todayTotal = tasks.count { it.dueEpochDay == todayEpoch }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "امروز",
                subtitle = PersianCalendar.greeting(LocalTime.now().hour) + " • " + PersianCalendar.longDate(today, digits),
                onSettings = onOpenSettings,
            )
        }
        item { Spacer(Modifier.height(2.dp)) }
        item { QuickAddCard(today = today, digits = digits, onQuickAdd = onQuickAdd) }
        item { ProgressCard(done = todayDone, total = todayTotal, persianDigits = digits) }

        if (buckets.overdue.isNotEmpty()) {
            item { SectionHeader("عقب‌افتاده", buckets.overdue.size) }
            items(buckets.overdue, key = { it.id }) { task ->
                TaskRow(task, today, digits, projectNames[task.projectId], { onEdit(task) }, { onToggle(task) }, { onDelete(task) })
            }
        }
        item { SectionHeader("برنامهٔ امروز", buckets.today.size) }
        if (buckets.today.isEmpty()) {
            item {
                EmptyState(
                    title = "امروز خالی است",
                    subtitle = "از بالا یک کار اضافه کن یا از تقویم روز دیگری را انتخاب کن.",
                )
            }
        } else {
            items(buckets.today, key = { it.id }) { task ->
                TaskRow(task, today, digits, projectNames[task.projectId], { onEdit(task) }, { onToggle(task) }, { onDelete(task) })
            }
        }

        if (buckets.upcoming.isNotEmpty()) {
            item { SectionHeader("بعداً", buckets.upcoming.size) }
            items(buckets.upcoming, key = { it.id }) { task ->
                TaskRow(task, today, digits, projectNames[task.projectId], { onEdit(task) }, { onToggle(task) }, { onDelete(task) })
            }
        }

        if (buckets.undated.isNotEmpty()) {
            item { SectionHeader("بدون تاریخ", buckets.undated.size) }
            items(buckets.undated, key = { it.id }) { task ->
                TaskRow(task, today, digits, projectNames[task.projectId], { onEdit(task) }, { onToggle(task) }, { onDelete(task) })
            }
        }

        if (buckets.completed.isNotEmpty()) {
            item { SectionHeader("انجام‌شده", buckets.completed.size) }
            items(buckets.completed, key = { it.id }) { task ->
                TaskRow(task, today, digits, projectNames[task.projectId], { onEdit(task) }, { onToggle(task) }, { onDelete(task) })
            }
        }

        item {
            Text(
                text = "همه‌چیز فقط روی همین دستگاه ذخیره می‌شود.",
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun QuickAddCard(
    today: JalaliDate,
    digits: Boolean,
    onQuickAdd: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) {
        if (text.isBlank()) null else QuickAddParser.parse(text, today)
    }

    fun submit() {
        if (text.isBlank()) return
        onQuickAdd(text)
        text = ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("فردا ساعت ۹ جلسه با تیم", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            if (parsed != null && !parsed.isPlainText) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val due = PersianCalendar.dueLabel(
                        parsed.date,
                        parsed.time?.let { it.hour * 60 + it.minute },
                        today,
                        digits,
                    )
                    if (due != null) Badge(due)
                    if (parsed.date == null && parsed.time != null) Badge("امروز", MaterialTheme.colorScheme.secondary)
                    when (parsed.priority) {
                        QuickAddParser.PRIORITY_HIGH -> Badge("مهم", MaterialTheme.colorScheme.error)
                        QuickAddParser.PRIORITY_LOW -> Badge("کم‌اهمیت", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (parsed.title.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "«${parsed.title}»",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { submit() },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("افزودن", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
