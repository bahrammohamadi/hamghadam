package ir.hamghadam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.TaskDraft
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.ReminderOptions
import ir.hamghadam.app.domain.RepeatRule
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.ui.components.PillChip

@Composable
fun TaskEditorScreen(
    initial: TaskDraft,
    projects: List<ProjectEntity>,
    settings: AppSettings,
    onSave: (TaskDraft) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((TaskDraft) -> Unit)? = null,
    knownTags: List<String> = emptyList(),
    onSaveAsTemplate: ((TaskDraft) -> Unit)? = null,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val today = JalaliDate.today()
    val digits = settings.persianDigits
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = if (draft.isNew) "کار تازه" else "ویرایش کار",
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(
                onClick = { onSave(draft) },
                enabled = draft.title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("ذخیره")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("عنوان کار") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("یادداشت (اختیاری)") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
            )

            EditorSection(title = "زمان‌بندی") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text("تاریخ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = draft.dueDate?.let { PersianCalendar.relativeDate(it, today, digits) } ?: "بدون تاریخ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).clickable(enabled = draft.dueDate != null) { showTimePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text("ساعت", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = draft.dueMinuteOfDay?.let {
                                    PersianText.clock(it / 60, it % 60, digits)
                                } ?: "تمام روز",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip(
                        text = "امروز",
                        selected = draft.dueDate == today,
                        onClick = { draft = draft.copy(dueDate = today) },
                    )
                    PillChip(
                        text = "فردا",
                        selected = draft.dueDate == today.plusDays(1),
                        onClick = { draft = draft.copy(dueDate = today.plusDays(1)) },
                    )
                    PillChip(
                        text = "پاک کردن",
                        selected = false,
                        onClick = { draft = draft.copy(dueDate = null, dueMinuteOfDay = null) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("یادآور", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = ReminderOptions.label(draft.reminderMinutesBefore),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        TextButton(
                            onClick = { showReminderMenu = true },
                            enabled = draft.dueDate != null,
                        ) {
                            Text("تغییر", fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = showReminderMenu,
                            onDismissRequest = { showReminderMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(ReminderOptions.label(null), fontSize = 13.sp) },
                                onClick = {
                                    draft = draft.copy(reminderMinutesBefore = null)
                                    showReminderMenu = false
                                },
                            )
                            for (minutes in ReminderOptions.all) {
                                DropdownMenuItem(
                                    text = { Text(ReminderOptions.label(minutes), fontSize = 13.sp) },
                                    onClick = {
                                        draft = draft.copy(reminderMinutesBefore = minutes)
                                        showReminderMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            EditorSection(title = "تکرار") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip(
                        text = RepeatRule.NONE.label,
                        selected = draft.repeat == RepeatRule.NONE,
                        onClick = { draft = draft.copy(repeat = RepeatRule.NONE) },
                    )
                    for (rule in RepeatRule.scheduled) {
                        PillChip(
                            text = rule.label,
                            selected = draft.repeat == rule,
                            onClick = { draft = draft.copy(repeat = rule) },
                        )
                    }
                }
                if (draft.repeat != RepeatRule.NONE && draft.dueDate == null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "برای تکرار، یک تاریخ انتخاب کن.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            }

            EditorSection(title = "برچسب‌ها") {
                OutlinedTextField(
                    value = draft.tags,
                    onValueChange = { draft = draft.copy(tags = it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثلاً کار, خرید — با کاما جدا کن", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                if (knownTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (tag in knownTags.take(5)) {
                            PillChip(
                                text = Tags.display(tag),
                                selected = Tags.parseCsv(draft.tags).contains(tag),
                                tint = ir.hamghadam.app.ui.theme.HamghadamPalette.Olive,
                                onClick = {
                                    draft = draft.copy(
                                        tags = if (Tags.parseCsv(draft.tags).contains(tag)) {
                                            Tags.remove(draft.tags, tag)
                                        } else {
                                            Tags.add(draft.tags, tag)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            EditorSection(title = "اهمیت") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (priority in listOf(TaskEntity.PRIORITY_LOW, TaskEntity.PRIORITY_NORMAL, TaskEntity.PRIORITY_HIGH)) {
                        PillChip(
                            text = TaskEntity.priorityLabel(priority),
                            selected = draft.priority == priority,
                            onClick = { draft = draft.copy(priority = priority) },
                        )
                    }
                }
            }

            if (projects.isNotEmpty()) {
                EditorSection(title = "لیست") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillChip(
                            text = "بدون لیست",
                            selected = draft.projectId == null,
                            onClick = { draft = draft.copy(projectId = null) },
                        )
                        for (project in projects) {
                            PillChip(
                                text = project.name,
                                selected = draft.projectId == project.id,
                                onClick = { draft = draft.copy(projectId = project.id) },
                            )
                        }
                    }
                }
            }

            if (onSaveAsTemplate != null && draft.title.isNotBlank()) {
                OutlinedButton(
                    onClick = { onSaveAsTemplate(draft) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("ذخیره به‌عنوان تمپلیت")
                }
            }

            if (!draft.isNew && onDelete != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("حذف این کار", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = draft.dueDate ?: today,
            persianDigits = digits,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                draft = draft.copy(dueDate = picked)
                showDatePicker = false
            },
            onClear = {
                draft = draft.copy(dueDate = null, dueMinuteOfDay = null)
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialMinuteOfDay = draft.dueMinuteOfDay,
            persianDigits = digits,
            onDismiss = { showTimePicker = false },
            onConfirm = { minuteOfDay ->
                draft = draft.copy(dueMinuteOfDay = minuteOfDay)
                showTimePicker = false
            },
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف کار؟") },
            text = { Text("«${draft.title}» حذف می‌شود. می‌توانی بلافاصله بازگردانی‌اش کنی.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(draft)
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("انصراف") }
            },
        )
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
