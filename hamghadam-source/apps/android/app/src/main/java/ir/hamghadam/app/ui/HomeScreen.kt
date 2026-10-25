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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
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
import ir.hamghadam.app.ui.components.CircleIconButton
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.IconTile
import ir.hamghadam.app.ui.components.MiniBars
import ir.hamghadam.app.ui.components.OverviewTile
import ir.hamghadam.app.ui.components.ProgressRing
import ir.hamghadam.app.ui.components.QuickAction
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.SoftCard
import ir.hamghadam.app.ui.components.TimelineRow
import ir.hamghadam.app.ui.components.softShadow
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
    todayHighlight: String? = null,
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

    val todayTasks = remember(tasks, todayEpoch) {
        tasks.filter { it.dueEpochDay == todayEpoch }
            .sortedWith(compareBy({ it.isCompleted }, { it.dueMinuteOfDay ?: -1 }, { -it.priority }))
    }
    val doneToday = todayTasks.count { it.isCompleted }
    val openToday = todayTasks.count { !it.isCompleted }
    val focusRecords = remember(focusSessions) { focusSessions.map { it.toRecord() } }
    val focusMinutesToday = FocusStats.minutesOnDay(focusRecords, todayEpoch)
    val focusWeek = remember(focusRecords, todayEpoch) { FocusStats.week(focusRecords, todayEpoch, days = 7) }
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
    val openPerProject = remember(tasks, projects) {
        projects.map { project -> project to tasks.count { it.projectId == project.id && !it.isCompleted } }
    }
    val openTotal = tasks.count { !it.isCompleted }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                greeting = PersianCalendar.greeting(hours),
                subtitle = PersianCalendar.longDate(today, digits),
                highlight = todayHighlight,
                onSettings = onOpenSettings,
            )
        }

        item {
            DailyTaskCard(
                done = doneToday,
                total = todayTasks.size,
                minutes = focusMinutesToday,
                digits = digits,
                onOpenTasks = onOpenTasks,
            )
        }

        item {
            QuickActionRow(
                onOpenTasks = onOpenTasks,
                onOpenFocus = onOpenFocus,
                onOpenHabits = onOpenHabits,
                onOpenTemplates = onOpenTemplates,
            )
        }

        item {
            FocusWeekCard(
                values = focusWeek.map { it.minutes.toFloat() },
                minutes = focusMinutesToday,
                sessions = focusTimer.completedToday,
                goalMinutes = settings.focusGoalMinutes,
                digits = digits,
                running = focusTimer.running,
                onStart = onOpenFocus,
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
            DailyChallengeCard(
                title = challenge.subtitle,
                percent = challenge.percent,
                digits = digits,
                steps = challenge.steps.map { step -> Triple(step.title, step.label, step.isComplete) },
            )
        }

        item {
            SectionTitle(
                title = "نمای کلی",
                action = { SectionChevron("این هفته", onOpenStats) },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverviewTile(
                        value = PersianText.number(openToday, digits),
                        label = "کار امروز",
                        hint = "باز از ${PersianText.number(todayTasks.size, digits)}",
                        tint = HamghadamPalette.Violet,
                        soft = HamghadamPalette.VioletSoft,
                        onClick = onOpenTasks,
                        modifier = Modifier.weight(1f),
                    )
                    OverviewTile(
                        value = PersianText.number(focusMinutesToday, digits),
                        label = "دقیقه تمرکز",
                        hint = "هدف ${PersianText.number(settings.focusGoalMinutes, digits)}",
                        tint = HamghadamPalette.Terracotta,
                        soft = HamghadamPalette.Peach,
                        onClick = onOpenFocus,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverviewTile(
                        value = PersianText.number(projects.size, digits),
                        label = "پروژه‌ها",
                        hint = "لیست‌های فعال",
                        tint = HamghadamPalette.Sky,
                        soft = HamghadamPalette.SkySoft,
                        onClick = onOpenTasks,
                        modifier = Modifier.weight(1f),
                    )
                    OverviewTile(
                        value = PersianText.number(openTotal, digits),
                        label = "همهٔ کارها",
                        hint = "انجام‌نشده",
                        tint = HamghadamPalette.Olive,
                        soft = HamghadamPalette.OliveSoft,
                        onClick = onOpenTasks,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "پروژه‌ها",
                action = { SectionChevron("همهٔ کارها", onOpenTasks) },
            )
        }
        item {
            if (openPerProject.isEmpty()) {
                HamghadamCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "هنوز لیستی نساخته‌ای؛ کارها را در لیست‌های رنگی دسته‌بندی کن.",
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onOpenSettings) { Text("ساخت لیست", fontSize = 11.sp) }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(openPerProject, key = { it.first.id }) { (project, openCount) ->
                        ProjectCard(project = project, openCount = openCount, digits = digits, onClick = onOpenTasks)
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "کارهای امروز",
                count = todayTasks.size,
                persianDigits = digits,
                action = {
                    Text(
                        text = "+ افزودن",
                        modifier = Modifier.clickable { onAddForDay(today) },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
        item {
            if (todayTasks.isEmpty()) {
                HamghadamCard {
                    EmptyState(
                        title = "امروز خالی است",
                        subtitle = "یک کار کوچک بنویس؛ ثبت سریع فارسی هم کار می‌کند.",
                        emoji = "🌤️",
                    )
                }
            } else {
                SoftCard {
                    todayTasks.take(6).forEachIndexed { index, task ->
                        TimelineRow(
                            emoji = "📝",
                            kind = projectNames[task.projectId] ?: "کار امروز",
                            title = task.title,
                            tint = HamghadamPalette.Violet,
                            soft = if (task.isCompleted) HamghadamPalette.OliveSoft else HamghadamPalette.VioletSoft,
                            meta = task.dueMinuteOfDay?.let { minute ->
                                PersianText.clock(minute / 60, minute % 60, digits)
                            } ?: "بدون ساعت",
                            checked = task.isCompleted,
                            onClick = { onEdit(task) },
                            onToggle = { onToggle(task) },
                        )
                        if (index != todayTasks.take(6).lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            )
                        }
                    }
                    if (todayTasks.size > 6) {
                        TextButton(onClick = onOpenTasks) {
                            Text("همهٔ ${PersianText.number(todayTasks.size, digits)} کار", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    subtitle: String,
    highlight: String?,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HamghadamPalette.Violet, HamghadamPalette.Terracotta))),
            contentAlignment = Alignment.Center,
        ) {
            Text("ه", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = subtitle + (highlight?.let { " • " + it } ?: ""),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = greeting,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CircleIconButton(contentDescription = "تنظیمات", onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun DailyTaskCard(
    done: Int,
    total: Int,
    minutes: Int,
    digits: Boolean,
    onOpenTasks: () -> Unit,
) {
    val progress = if (total <= 0) 0f else done.toFloat() / total.toFloat()
    SoftCard(onClick = onOpenTasks) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress,
                size = 84.dp,
                strokeWidth = 8.dp,
                color = HamghadamPalette.Terracotta,
                track = HamghadamPalette.Peach,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = PersianText.number((progress * 100).toInt(), digits) + "٪",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = HamghadamPalette.Terracotta,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "کار روزانه",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Badge("امروز", HamghadamPalette.Terracotta)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (total == 0) "امروز کاری ثبت نشده" else "انجام ${PersianText.number(done, digits)} از ${PersianText.number(total, digits)} کار",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${PersianText.number(minutes, digits)} دقیقه تمرکز امروز",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onOpenTasks: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenTemplates: () -> Unit,
) {
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickAction("📝", "کارها", HamghadamPalette.Terracotta, onOpenTasks, Modifier.weight(1f))
            QuickAction("🎯", "تمرکز", HamghadamPalette.Violet, onOpenFocus, Modifier.weight(1f))
            QuickAction("🌱", "عادت‌ها", HamghadamPalette.Olive, onOpenHabits, Modifier.weight(1f))
            QuickAction("🧩", "تمپلیت", HamghadamPalette.Amber, onOpenTemplates, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FocusWeekCard(
    values: List<Float>,
    minutes: Int,
    sessions: Int,
    goalMinutes: Int,
    digits: Boolean,
    running: Boolean,
    onStart: () -> Unit,
) {
    SoftCard(onClick = onStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "تمرکز این هفته",
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Badge(if (running) "در جریان" else "۷ روز", HamghadamPalette.Violet)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniBars(
                values = values.ifEmpty { List(7) { 0f } },
                tint = HamghadamPalette.Violet,
                modifier = Modifier.weight(1f),
                height = 62.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PersianText.number(minutes, digits) + " دقیقه",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${PersianText.number(sessions, digits)} جلسه • هدف ${PersianText.number(goalMinutes, digits)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

    SoftCard {
        Text(
            text = "ثبت سریع",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("فردا ۹ صبح جلسه #کار", fontSize = 12.sp) },
                singleLine = true,
                shape = CardRadius.field,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = HamghadamPalette.Violet,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                leadingIcon = { Text("🔍", fontSize = 13.sp) },
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .softShadow(radius = 24.dp, elevation = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HamghadamPalette.Violet)
                    .clickable { submit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "ثبت کار", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        if (parsed != null && text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                parsed.date?.let { Badge(PersianCalendar.relativeDate(it, today, digits), HamghadamPalette.Violet) }
                parsed.time?.let { Badge(PersianText.clock(it.hour, it.minute, digits), HamghadamPalette.Sky) }
                if (parsed.priority == QuickAddParser.PRIORITY_HIGH) Badge("مهم", HamghadamPalette.Terracotta)
                for (tag in parsed.tags.take(2)) {
                    Badge(Tags.display(tag), HamghadamPalette.Olive)
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, openCount: Int, digits: Boolean, onClick: () -> Unit) {
    val color = Color(project.colorArgb)
    SoftCard(modifier = Modifier.width(158.dp), onClick = onClick) {
        IconTile("📁", color, color.copy(alpha = 0.14f), size = 38.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = project.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${PersianText.number(openCount, digits)} کار باز",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionChevron(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CardRadius.chip)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(3.dp))
        Text("‹", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyChallengeCard(
    title: String,
    percent: Int,
    digits: Boolean,
    steps: List<Triple<String, String, Boolean>>,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .softShadow(radius = 24.dp, elevation = 14.dp)
            .clip(CardRadius.card)
            .background(
                Brush.linearGradient(
                    listOf(HamghadamPalette.Violet, Color(0xFF9B7CF9)),
                ),
            ),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("برنامهٔ تو", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(3.dp))
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = PersianText.number(percent, digits) + "٪",
                    color = Color.White,
                    fontSize = 24.sp,
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
                            .size(18.dp)
                            .background(if (complete) Color.White else Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (complete) {
                            Text("✓", color = HamghadamPalette.Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
