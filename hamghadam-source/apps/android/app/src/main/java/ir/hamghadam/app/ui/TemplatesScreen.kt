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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.data.TemplateEntity
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.SectionArtwork
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun TemplatesScreen(
    templates: List<TemplateEntity>,
    settings: AppSettings,
    onBack: () -> Unit,
    onUse: (TemplateEntity) -> Unit,
    onCreate: (name: String, title: String, notes: String, priority: Int, tags: String, minuteOfDay: Int?) -> Unit,
    onDelete: (String) -> Unit,
) {
    val digits = settings.persianDigits
    var showCreate by remember { mutableStateOf(false) }
    val builtin = remember(templates) { templates.filter { it.builtin } }
    val custom = remember(templates) { templates.filterNot { it.builtin } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SimpleHeader("تمپلیت‌ها", onBack) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "یک بار بساز، هر روز با یک ضربه اجرا کن",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showCreate = true }) { Text("+ تازه", fontSize = 12.sp) }
            }
        }
        item {
            HamghadamCard {
                Text(
                    text = "تمپلیت‌ها مثل دستور پخت‌اند: عنوان، برچسب و ساعت را ذخیره می‌کنند تا ساخت کارهای تکراری سریع شود.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (builtin.isNotEmpty()) {
            item { SectionTitle("پیش‌فرض‌ها", persianDigits = digits) }
            items(builtin, key = { it.id }) { template ->
                TemplateCard(template, digits, onUse, null)
            }
        }
        item { SectionTitle("ساختهٔ من", count = custom.size, persianDigits = digits) }
        if (custom.isEmpty()) {
            item {
                EmptyState(
                    title = "تمپلیت شخصی نداری",
                    subtitle = "از دکمهٔ «+ تازه» یا از داخل ویرایشگر کار، یک قالب بساز.",
                    emoji = "🧩", art = SectionArtwork.TEMPLATES,)
            }
        } else {
            items(custom, key = { it.id }) { template ->
                TemplateCard(template, digits, onUse, { onDelete(template.id) })
            }
        }
    }

    if (showCreate) {
        TemplateDialog(
            persianDigits = digits,
            onDismiss = { showCreate = false },
            onCreate = { name, title, notes, priority, tags, minute ->
                onCreate(name, title, notes, priority, tags, minute)
                showCreate = false
            },
        )
    }
}

@Composable
private fun TemplateCard(
    template: TemplateEntity,
    digits: Boolean,
    onUse: (TemplateEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    HamghadamCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = template.title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (template.builtin) {
                Badge("پیش‌فرض", HamghadamPalette.Amber)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            template.minuteOfDay?.let { minute ->
                Badge(PersianText.clock(minute / 60, minute % 60, digits), HamghadamPalette.Sky)
            }
            if (template.priority == TaskEntity.PRIORITY_HIGH) {
                Badge("مهم", MaterialTheme.colorScheme.error)
            }
            for (tag in Tags.parseCsv(template.tagCsv)) {
                Badge(PersianText.toPersianDigits(Tags.display(tag)), HamghadamPalette.Olive)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onUse(template) }, shape = CardRadius.field) {
                Text("ساخت کار امروز", fontSize = 12.sp)
            }
            if (onDelete != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDelete, shape = CardRadius.field) {
                    Text("حذف", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TemplateDialog(
    persianDigits: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String, Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskEntity.PRIORITY_NORMAL) }
    val minuteOfDay = remember(timeText) { parseClock(timeText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تمپلیت تازه", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام قالب", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان کار", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("برچسب‌ها با کاما (اختیاری)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ساعت مثل 09:30 (اختیاری)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (value in listOf(TaskEntity.PRIORITY_LOW, TaskEntity.PRIORITY_NORMAL, TaskEntity.PRIORITY_HIGH)) {
                        PillChip(
                            text = TaskEntity.priorityLabel(value),
                            selected = priority == value,
                            onClick = { priority = value },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("یادداشت (اختیاری)", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 3,
                    shape = CardRadius.field,
                )
                if (timeText.isNotBlank() && minuteOfDay == null) {
                    Text("ساعت را به شکل ۰۹:۳۰ یا 09:30 بنویس.", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, title, notes, priority, Tags.toCsv(Tags.parseCsv(tagText)), minuteOfDay) },
                enabled = name.isNotBlank() && title.isNotBlank(),
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

/** Parses `09:30`, `9:30` and Persian digits into minutes from midnight. */
internal fun parseClock(raw: String): Int? {
    val normalized = PersianText.toLatinDigits(raw.trim())
    if (normalized.isBlank()) return null
    val parts = normalized.split(':', '：')
    if (parts.size != 2) return null
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}
