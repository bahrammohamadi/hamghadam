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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.FocusSessionEntity
import ir.hamghadam.app.data.HabitEntity
import ir.hamghadam.app.data.HabitLogEntity
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.DailyChallengeCalculator
import ir.hamghadam.app.domain.FocusStats
import ir.hamghadam.app.domain.FocusTimerState
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.QuickAddParser
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ProgressRing
import ir.hamghadam.app.ui.components.QuickAction
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.StatTile
import ir.hamghadam.app.ui.components.TagChip
import ir.hamghadam.app.ui.components.TaskRow
import ir.hamghadam.app.ui.components.WeekStrip
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette
import java.time.LocalTime

@Composable
fun HomeScreen(
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    focusSessions: List<FocusSessionEntity>,
    settings: AppSettings,
    focusTimer: FocusTimerState,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onQuickAdd: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenTasks: () -> Unit,
    onAddForDay: (JalaliDate) -> Unit,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits
    val hours = LocalTime.now().hour

    var selectedEpoch by rememberSaveable { mutableStateOf(todayEpoch) }
    val selected = JalaliDate.fromEpochDay(selectedEpoch)
    val week = remember(todayEpoch) { PersianCalendar.weekDays(today) }

    val todayTasks = tasks.filter { it.dueEpochDay == todayEpoch }
    val doneToday = todayTasks.count { it.isCompleted }
    val openToday = todayTasks.count { !it.isCompleted }
    val focusRecords = remember(focusSessions) { focusSessions.map { it.toRecord() } }
    val focusMinutesToday = FocusStats.minutesOnDay(focusRecords, todayEpoch)
    val habitsDoneToday = habits.count { habit ->
        habitLogs.any { it.habitId == habit.id && it.epochDay == todayEpoch }
    }
    val challenge = remember(todayTasks.size, doneToday, focusMinutesToday, habits.size, habitsDoneToday, settings.focusGoalMinutes) {
        DailyChallengeCalculator.build(
            dueToday = todayTasks.size,
            doneToday = doneToday,
            focusMinutesToday = focusMinutesToday,
            focusGoalMinutes = settings.focusGoalMinutes,
            habitsTotal = habits.size,
            habitsDoneToday = habitsDoneToday,
            greeting = PersianCalendar.greeting(hours),
        )
    }
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    val projectColors = remember(projects) { projects.associate { it.id to Color(it.colorArgb) } }
    val dayTasks = remember(tasks, selectedEpoch) {
        tasks.filter { it.dueEpochDay == selectedEpoch }
            .sortedWith(compareBy({ it.isCompleted }, { it.dueMinuteOfDay ?: -1 }, { -it.priority }))
    }
    val dotDays = remember(tasks) { tasks.filter { !it.isCompleted && it.dueEpochDay != null }.mapNotNull { it.dueEpochDay }.toSet() }
    val openPerProject = remember(tasks, projects) {
        projects.map { project -> project to tasks.count { it.projectId == project.id && !it.isCompleted } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = PersianCalendar.greeting(hours),
                subtitle = PersianCalendar.longDate(today, digits),
                onSettings = onOpenSettings,
            )
        }

        item {
            QuickAddCard(
                today = today,
                digits = digits,
                onQuickAdd = onQuickAdd,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusHeroCard(
                    modifier = Modifier.weight(1.05f),
                    done = doneToday,
                    total = todayTasks.size,
                    focusMinutes = focusMinutesToday,
                    running = focusTimer.running,
                    onStart = onOpenFocus,
                )
                QuickActionsCard(
                    modifier = Modifier.weight(1f),
                    onOpenTasks = onOpenTasks,
                    onOpenFocus = onOpenFocus,
                    onOpenHabits = onOpenHabits,
                    onOpenTemplates = onOpenTemplates,
                )
            }
        }

        item {
            DailyChallengeCard(
                title = challenge.subtitle,
                percent = challenge.percent,
                steps = challenge.steps.map { step -> Triple(step.title, step.label, step.isComplete) },
            )
        }

        item { SectionTitle(title = "یک نگاه", action = { SeeAll("آمار", onOpenStats) }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "کار امروز",
                    value = PersianText.number(openToday, digits),
                    emoji = "☀️",
                    tint = HamghadamPalette.Terracotta,
                    hint = "باز از ${PersianText.number(todayTasks.size, digits)}",
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "انجام‌شده",
                    value = PersianText.number(doneToday, digits),
                    emoji = "✅",
                    tint = HamghadamPalette.Olive,
                    hint = "امروز",
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "تمرکز",
                    value = PersianText.number(focusMinutesToday, digits) + " دقیقه",
                    emoji = "🎯",
                    tint = HamghadamPalette.Violet,
                    hint = "${PersianText.number(focusTimer.completedToday, digits)} جلسه",
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "عادت‌ها",
                    value = "${PersianText.number(habitsDoneToday, digits)}/${PersianText.number(habits.size, digits)}",
                    emoji = "🌱",
                    tint = HamghadamPalette.Sky,
                    hint = "امروز",
                )
            }
        }

        if (openPerProject.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "پروژه‌ها",
                    action = {
                        SeeAll("همهٔ کارها", onOpenTasks)
                    },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(openPerProject, key = { it.first.id }) { (project, openCount) ->
                        ProjectPill(project = project, openCount = openCount, digits = digits)
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "کارهای " + PersianCalendar.relativeDate(selected, today, digits),
                action = {
                    Text(
                        text = "+ افزودن",
                        modifier = Modifier.clickable { onAddForDay(selected) },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
        item {
            HamghadamCard {
                WeekStrip(
                    days = week,
                    todayEpochDay = todayEpoch,
                    selectedEpochDay = selectedEpoch,
                    persianDigits = digits,
                    onSelect = { selectedEpoch = it.toEpochDay() },
                    dotDays = dotDays,
                )
            }
        }
        if (dayTasks.isEmpty()) {
            item {
                EmptyState(
                    title = "این روز خالی است",
                    subtitle = "یک کار کوچک اضافه کن یا روز دیگری را از نوار بالا انتخاب کن.",
                    emoji = "🍃",
                )
            }
        } else {
            items(dayTasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    today = today,
                    persianDigits = digits,
                    projectName = projectNames[task.projectId],
                    projectColor = projectColors[task.projectId] ?: MaterialTheme.colorScheme.secondary,
                    onClick = { onEdit(task) },
                    onToggle = { onToggle(task) },
                    onDelete = { onDelete(task) },
                )
            }
        }
    }
}

@Composable
private fun SeeAll(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ProjectPill(project: ProjectEntity, openCount: Int, digits: Boolean) {
    val color = Color(project.colorArgb)
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CardRadius.chip)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(project.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(6.dp))
        Text(PersianText.number(openCount, digits), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickAddCard(today: JalaliDate, digits: Boolean, onQuickAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) {
        if (text.isBlank()) null else QuickAddParser.parse(text, today)
    }

    fun submit() {
        if (text.isBlank()) return
        onQuickAdd(text)
        text = ""
    }

    HamghadamCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ثبت سریع", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "زبان طبیعی: «فردا ۹ صبح جلسه #کار»",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("چه کاری؟ مثال: پس‌فردا ساعت ۱۰ خرید #خانه", fontSize = 12.sp) },
                singleLine = true,
                shape = CardRadius.field,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { submit() },
                enabled = text.isNotBlank(),
                shape = CardRadius.field,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن")
            }
        }
        if (parsed != null && !parsed.isPlainText) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val due = PersianCalendar.dueLabel(parsed.date, parsed.time?.let { it.hour * 60 + it.minute }, today, digits)
                if (due != null) Badge(due)
                if (parsed.date == null && parsed.time != null) Badge("امروز", MaterialTheme.colorScheme.secondary)
                if (parsed.priority == QuickAddParser.PRIORITY_HIGH) Badge("مهم", MaterialTheme.colorScheme.error)
                for (tag in parsed.tags) {
                    TagChip(PersianText.toPersianDigits(Tags.display(tag)))
                }
            }
            if (parsed.title.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "«${parsed.title}»",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FocusHeroCard(
    modifier: Modifier = Modifier,
    done: Int,
    total: Int,
    focusMinutes: Int,
    running: Boolean,
    onStart: () -> Unit,
) {
    val progress = if (total <= 0) 0f else done.toFloat() / total.toFloat()
    Card(
        modifier = modifier,
        shape = CardRadius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = "تمرکز من",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = progress,
                    size = 64.dp,
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = PersianText.number((progress * 100).toInt()) + "٪",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = PersianText.number(focusMinutes) + " دقیقه",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${PersianText.number(done)} از ${PersianText.number(total)} کار",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (running) "جلسه در جریان است…" else "یک بلوک تمرکز شروع کن",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onStart, contentPadding = PaddingValues(0.dp)) {
                Text("شروع تمرکز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    modifier: Modifier = Modifier,
    onOpenTasks: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenTemplates: () -> Unit,
) {
    HamghadamCard(modifier = modifier) {
        Text(
            text = "دسترسی سریع",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            QuickAction(emoji = "📝", label = "کارها", tint = HamghadamPalette.Terracotta, onClick = onOpenTasks)
            QuickAction(emoji = "🎯", label = "تمرکز", tint = HamghadamPalette.Violet, onClick = onOpenFocus)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            QuickAction(emoji = "🌱", label = "عادت‌ها", tint = HamghadamPalette.Olive, onClick = onOpenHabits)
            QuickAction(emoji = "🧩", label = "تمپلیت", tint = HamghadamPalette.Amber, onClick = onOpenTemplates)
        }
    }
}

@Composable
private fun DailyChallengeCard(
    title: String,
    percent: Int,
    steps: List<Triple<String, String, Boolean>>,
) {
    val start = HamghadamPalette.Violet
    val end = Color(0xFF9B8AF5)
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(start, end)), CardRadius.card),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("چالش روزانه", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(3.dp))
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$percent٪",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(Color.White, CircleShape),
                )
            }
            Spacer(Modifier.height(12.dp))
            for ((stepTitle, label, complete) in steps) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(17.dp)
                            .background(
                                if (complete) Color.White else Color.White.copy(alpha = 0.2f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (complete) {
                            Text("✓", color = start, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stepTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }
}
