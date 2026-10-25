package ir.hamghadam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.EventDraft
import ir.hamghadam.app.toDraft
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.EventCatalog
import ir.hamghadam.app.data.ExternalEventEntity
import ir.hamghadam.app.data.PersonalEventEntity
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.CalendarEvent
import ir.hamghadam.app.domain.CalendarSource
import ir.hamghadam.app.domain.EventPalette
import ir.hamghadam.app.domain.EventProjection
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.TaskRow
import ir.hamghadam.app.ui.theme.CardRadius

@Composable
fun CalendarScreen(
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    settings: AppSettings,
    catalog: EventCatalog,
    personalEvents: List<PersonalEventEntity>,
    googleEvents: List<ExternalEventEntity>,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onAddForDay: (JalaliDate) -> Unit,
    onSaveEvent: (EventDraft) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onToggleSource: (CalendarSource, Boolean) -> Unit,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits
    var anchorEpoch by rememberSaveable { mutableStateOf(todayEpoch) }
    var selectedEpoch by rememberSaveable { mutableStateOf(todayEpoch) }
    var editing by remember { mutableStateOf<EventDraft?>(null) }

    val anchor = JalaliDate.fromEpochDay(anchorEpoch)
    val selected = JalaliDate.fromEpochDay(selectedEpoch)
    val grid = remember(anchor) { PersianCalendar.monthGrid(anchor) }

    val personalModels = remember(personalEvents) { personalEvents.map { it.toModel() } }
    val events = remember(catalog, personalModels, googleEvents, anchor.year, settings.calendarSources) {
        val google = googleEvents.mapNotNull { it.toEvent() }
        val base = catalog.eventsForYear(anchor.year) +
            EventProjection.expandPersonal(personalModels, anchor.year) +
            google
        EventProjection.enabled(base, settings.calendarSources)
    }
    val byDay = remember(events, anchor.year, anchor.month) {
        EventProjection.byDay(events, anchor.year, anchor.month)
    }
    val selectedEvents = remember(events, selectedEpoch) { EventProjection.forDay(events, selected) }
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    val selectedTasks = remember(tasks, selectedEpoch) {
        tasks.filter { it.dueEpochDay == selectedEpoch }.sortedBy { it.completedAtEpochMillis != null }
    }
    val personalById = remember(personalEvents) { personalEvents.associateBy { it.id } }

    fun openMonth(monthOffset: Int) {
        val next = anchor.plusMonths(monthOffset)
        anchorEpoch = next.toEpochDay()
        selectedEpoch = if (next.year == today.year && next.month == today.month) todayEpoch else next.toEpochDay()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "تقویم",
                subtitle = PersianCalendar.longDate(today, digits) + " • " + catalog.describeFeeds(),
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardRadius.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { openMonth(-1) }) {
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = PersianCalendar.monthTitle(anchor, digits),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (anchorEpoch != todayEpoch) {
                                Text(
                                    text = "بازگشت به امروز",
                                    modifier = Modifier.clickable {
                                        anchorEpoch = todayEpoch
                                        selectedEpoch = todayEpoch
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        IconButton(onClick = { openMonth(1) }) {
                            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        for (dayOfWeek in PersianCalendar.weekOrder()) {
                            Text(
                                text = PersianCalendar.weekdayInitial(dayOfWeek),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    for (week in grid) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            for (cell in week) {
                                Box(Modifier.weight(1f).padding(1.dp), contentAlignment = Alignment.Center) {
                                    if (cell != null) {
                                        DayCell(
                                            date = cell,
                                            isToday = cell.toEpochDay() == todayEpoch,
                                            isSelected = cell.toEpochDay() == selectedEpoch,
                                            dots = EventProjection.dotColors(byDay[cell.toEpochDay()].orEmpty()),
                                            isHoliday = byDay[cell.toEpochDay()].orEmpty().any { it.holiday },
                                            digits = digits,
                                            onClick = { selectedEpoch = cell.toEpochDay() },
                                        )
                                    } else {
                                        Spacer(Modifier.size(42.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CalendarSource.entries.toList()) { source ->
                    val active = source.id in settings.calendarSources
                    PillChip(
                        text = source.label,
                        selected = active,
                        tint = Color(source.colorArgb),
                        onClick = { onToggleSource(source, !active) },
                    )
                }
            }
        }
        item {
            SectionTitle(
                title = "رویدادهای " + PersianCalendar.relativeDate(selected, today, digits),
                count = selectedEvents.size,
                persianDigits = digits,
                action = {
                    Text(
                        text = "+ رویداد",
                        modifier = Modifier.clickable { editing = EventDraft(date = selected) },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
        if (selectedEvents.isEmpty()) {
            item {
                EmptyState(
                    title = "رویدادی ثبت نشده",
                    subtitle = "مناسبت‌های رسمی و فرهنگی به‌صورت آفلاین همراه اپ هستند؛ می‌توانی رویداد شخصی هم اضافه کنی.",
                    emoji = "🗓️",
                )
            }
        } else {
            items(selectedEvents, key = { it.id }) { event ->
                val personalEntity = personalById[event.id.substringBefore('@')]
                EventCard(
                    event = event,
                    digits = digits,
                    editable = personalEntity != null || event.source == CalendarSource.PERSONAL,
                    onEdit = {
                        personalEntity?.let { editing = it.toDraft() }
                    },
                    onDelete = { personalEntity?.let { onDeleteEvent(it.id) } },
                )
            }
        }
        item {
            SectionTitle(
                title = "کارهای این روز",
                count = selectedTasks.size,
                persianDigits = digits,
                action = {
                    Text(
                        text = "+ کار",
                        modifier = Modifier.clickable { onAddForDay(selected) },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
        if (selectedTasks.isEmpty()) {
            item {
                EmptyState(title = "کاری برای این روز نیست", subtitle = "روی «+ کار» بزن تا برای همین روز بسازی.", emoji = "☕")
            }
        } else {
            items(selectedTasks, key = { it.id }) { task ->
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
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (source in CalendarSource.entries) {
                    Box(Modifier.size(8.dp).background(Color(source.colorArgb), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = source.shortLabel,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }
        }
    }

    editing?.let { draft ->
        EventEditorDialog(
            initial = draft,
            persianDigits = digits,
            onDismiss = { editing = null },
            onSave = { saved ->
                onSaveEvent(saved)
                editing = null
            },
            onDelete = { id ->
                onDeleteEvent(id)
                editing = null
            },
        )
    }
}

@Composable
private fun DayCell(
    date: JalaliDate,
    isToday: Boolean,
    isSelected: Boolean,
    dots: List<Int>,
    isHoliday: Boolean,
    digits: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isHoliday -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = PersianText.number(date.day, digits),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isToday || isSelected || isHoliday) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dot in dots) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(Color(dot), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    digits: Boolean,
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardRadius.tile,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = editable) { onEdit() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .background(Color(event.colorArgb), CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (event.holiday) {
                        Spacer(Modifier.width(5.dp))
                        Badge("تعطیل", MaterialTheme.colorScheme.error)
                    }
                    if (event.approximate) {
                        Spacer(Modifier.width(4.dp))
                        Badge("تقریبی", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = event.subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (editable) {
                IconButton(onClick = onEdit) {
                    Text("✎", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "حذف رویداد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventEditorDialog(
    initial: EventDraft,
    persianDigits: Boolean,
    onDismiss: () -> Unit,
    onSave: (EventDraft) -> Unit,
    onDelete: (String) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (draft.isNew) "رویداد شخصی تازه" else "ویرایش رویداد",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تاریخ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = PersianCalendar.longDate(draft.date, persianDigits),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = { showDatePicker = true }) { Text("تغییر", fontSize = 12.sp) }
                }
                Text("رنگ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (color in EventPalette.picks) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(color), CircleShape)
                                .clickable { draft = draft.copy(colorArgb = color) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (draft.colorArgb == color) {
                                Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تکرار سالانه", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "هر سال در همین روز شمسی",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = draft.yearlyRepeat,
                        onCheckedChange = { draft = draft.copy(yearlyRepeat = it) },
                    )
                }
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("یادداشت (اختیاری)", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 3,
                    shape = CardRadius.field,
                )
                if (!draft.isNew) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("حذف این رویداد", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }, enabled = draft.title.isNotBlank()) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = draft.date,
            persianDigits = persianDigits,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                draft = draft.copy(date = picked)
                showDatePicker = false
            },
            onClear = { showDatePicker = false },
        )
    }

    val deleteId = draft.id
    if (confirmDelete && deleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف رویداد؟") },
            text = { Text("«${draft.title}» حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(deleteId)
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("انصراف") }
            },
        )
    }
}
