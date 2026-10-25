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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.FocusSessionEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.FocusStats
import ir.hamghadam.app.domain.FocusTimerState
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ProgressRing
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.softShadow
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun FocusScreen(
    tasks: List<TaskEntity>,
    settings: AppSettings,
    timer: FocusTimerState,
    sessions: List<FocusSessionEntity>,
    onStart: (Int) -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSelectTask: (TaskEntity) -> Unit,
    onClearTask: () -> Unit,
    onSetDuration: (Int) -> Unit,
    onSetGoal: (Int) -> Unit,
    onOpenTimerSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits
    val records = remember(sessions) { sessions.map { it.toRecord() } }
    val minutesToday = FocusStats.minutesOnDay(records, todayEpoch)
    val sessionsToday = FocusStats.sessionsOnDay(records, todayEpoch)
    val week = remember(records, todayEpoch) { FocusStats.week(records, todayEpoch, days = 7) }
    val maxMinutes = remember(week) { FocusStats.maxMinutes(week).coerceAtLeast(1) }
    var showTaskPicker by remember { mutableStateOf(false) }
    val openTasks = remember(tasks) { tasks.filter { !it.isCompleted } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) {
            item { SimpleHeader("تمرکز") { onBack() } }
        }
        item {
            ScreenHeader(
                title = "تمرکز",
                subtitle = "پومودورو • ${PersianText.number(minutesToday, digits)} دقیقه امروز",
                onSettings = onOpenTimerSettings,
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(radius = 24.dp, elevation = 12.dp),
                shape = CardRadius.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (timer.onBreak) "استراحت" else "جلسهٔ تمرکز",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HamghadamPalette.Violet,
                        )
                        Spacer(Modifier.width(6.dp))
                        Badge(
                            text = if (timer.running) "در جریان" else "پومودورو",
                            tint = HamghadamPalette.Violet,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressRing(
                        progress = timer.progress,
                        size = 188.dp,
                        strokeWidth = 12.dp,
                        color = HamghadamPalette.Violet,
                        track = HamghadamPalette.VioletSoft,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val minutes = timer.remainingSeconds / 60
                            val seconds = timer.remainingSeconds % 60
                            Text(
                                text = PersianText.twoDigits(minutes, digits) + ":" + PersianText.twoDigits(seconds, digits),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = HamghadamPalette.VioletDeep,
                            )
                            Text(
                                text = if (timer.running) "در جریان" else if (timer.remainingSeconds == 0) "تمام شد" else "آماده",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    val taskTitle = timer.taskTitle
                    if (taskTitle != null) {
                        Row(
                            modifier = Modifier
                                .background(HamghadamPalette.VioletSoft, CardRadius.chip)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🎯 " + taskTitle,
                                fontSize = 11.sp,
                                color = HamghadamPalette.VioletDeep,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "×",
                                modifier = Modifier.clickable { onClearTask() },
                                fontSize = 13.sp,
                                color = HamghadamPalette.VioletDeep.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (timer.running) {
                            OutlinedButton(onClick = onPause, shape = CardRadius.field) { Text("توقف موقت", fontSize = 12.sp) }
                        } else {
                            Button(
                                onClick = { if (timer.remainingSeconds in 1 until timer.totalSeconds) onResume() else onStart(settings.focusMinutes) },
                                shape = CardRadius.field,
                                enabled = !timer.onBreak,
                            ) {
                                Text(
                                    text = if (timer.remainingSeconds in 1 until timer.totalSeconds) "ادامه" else "شروع تمرکز",
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        OutlinedButton(onClick = onStartBreak, shape = CardRadius.field) {
                            Text("استراحت", fontSize = 12.sp)
                        }
                        TextButton(onClick = onStop) { Text("پایان", fontSize = 12.sp) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (minutes in listOf(15, 25, 50)) {
                    PillChip(
                        text = "${PersianText.number(minutes, digits)} دقیقه",
                        selected = settings.focusMinutes == minutes,
                        tint = HamghadamPalette.Violet,
                        onClick = { onSetDuration(minutes) },
                    )
                }
                PillChip(
                    text = "انتخاب کار",
                    selected = false,
                    tint = HamghadamPalette.Violet,
                    onClick = { showTaskPicker = true },
                )
            }
        }
        item {
            HamghadamCard {
                Text("هدف امروز", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                val ratio = (minutesToday.toFloat() / settings.focusGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(ratio)
                            .height(8.dp)
                            .background(HamghadamPalette.Violet, CircleShape),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${PersianText.number(minutesToday, digits)} از ${PersianText.number(settings.focusGoalMinutes, digits)} دقیقه",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${PersianText.number(sessionsToday, digits)} جلسهٔ کامل",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (goal in listOf(25, 50, 90, 120)) {
                        PillChip(
                            text = PersianText.number(goal, digits) + " دقیقه",
                            selected = settings.focusGoalMinutes == goal,
                            tint = HamghadamPalette.Violet,
                            onClick = { onSetGoal(goal) },
                        )
                    }
                }
            }
        }
        item { SectionTitle("هفت روز گذشته", persianDigits = digits) }
        item {
            HamghadamCard {
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (day in week) {
                        val barHeight = if (maxMinutes <= 0) 0f else (day.minutes.toFloat() / maxMinutes)
                        val date = JalaliDate.fromEpochDay(day.epochDay)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                text = if (day.minutes > 0) PersianText.number(day.minutes, digits) else "–",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .height((6 + 84 * barHeight).dp)
                                    .background(
                                        if (day.minutes > 0) HamghadamPalette.Violet else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape,
                                    ),
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = PersianCalendar.weekdayInitial(date.dayOfWeek()),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = PersianText.number(date.day, digits),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
        item { SectionTitle("آخرین جلسه‌ها", persianDigits = digits) }
        if (sessions.isEmpty()) {
            item {
                EmptyState(
                    title = "هنوز جلسه‌ای ثبت نشده",
                    subtitle = "یک بلوک ۲۵ دقیقه‌ای شروع کن؛ نتایج همین‌جا جمع می‌شوند.",
                    emoji = "⏳",
                )
            }
        } else {
            items(sessions.take(6), key = { it.id }) { session ->
                val date = JalaliDate.fromEpochDay(session.epochDay)
                HamghadamCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).background(HamghadamPalette.Violet.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (session.completed) "✓" else "◔", color = HamghadamPalette.Violet, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${PersianText.number(session.seconds / 60, digits)} دقیقه",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = PersianCalendar.relativeDate(date, today, digits) +
                                    if (session.completed) " • کامل" else " • نیمه‌کاره",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = PersianCalendar.weekdayName(date.dayOfWeek()),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            title = { Text("کدام کار را می‌خواهی انجام دهی؟", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        TextButton(onClick = {
                            onClearTask()
                            showTaskPicker = false
                        }) {
                            Text("بدون اتصال به کار", fontSize = 12.sp)
                        }
                    }
                    items(openTasks, key = { it.id }) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTask(task)
                                    showTaskPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("•", color = HamghadamPalette.Violet, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = task.title,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) { Text("بستن") }
            },
        )
    }
}
