package ir.hamghadam.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.FocusSessionEntity
import ir.hamghadam.app.data.HabitEntity
import ir.hamghadam.app.data.HabitLogEntity
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.FocusStats
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.Tags
import ir.hamghadam.app.domain.TaskQuery
import ir.hamghadam.app.domain.TaskStats
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.SectionArtwork
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.StatTile
import ir.hamghadam.app.ui.components.TagChip
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun StatsScreen(
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    focusSessions: List<FocusSessionEntity>,
    settings: AppSettings,
    onBack: () -> Unit,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits

    val week = remember(tasks, todayEpoch) {
        TaskStats.week(
            items = tasks,
            todayEpochDay = todayEpoch,
            epochDay = { it.dueEpochDay },
            completedEpochDay = { task -> task.completedDate?.toEpochDay() },
            days = 7,
        )
    }
    val doneTotal = tasks.count { it.isCompleted }
    val openTotal = tasks.count { !it.isCompleted }
    val percent = TaskStats.completionPercent(tasks) { it.isCompleted }
    val focusRecords = remember(focusSessions) { focusSessions.map { it.toRecord() } }
    val focusToday = FocusStats.minutesOnDay(focusRecords, todayEpoch)
    val focusWeek = remember(focusRecords, todayEpoch) { FocusStats.week(focusRecords, todayEpoch) }
    val focusWeekTotal = focusWeek.sumOf { it.minutes }
    val maxWeekMinutes = focusWeek.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    val focusStreak = FocusStats.dayStreak(focusRecords, todayEpoch)
    val maxTaskDay = week.maxOfOrNull { it.total.coerceAtLeast(it.done) }?.coerceAtLeast(1) ?: 1
    val tags = remember(tasks) { Tags.counts(tasks.filter { !it.isCompleted }.map { it.tags }).take(8) }
    val habitStreaks = remember(habits, habitLogs, todayEpoch) {
        habits.map { habit ->
            habit to TaskQuery.currentStreak(
                habitLogs.filter { it.habitId == habit.id }.map { it.epochDay }.toSet(),
                todayEpoch,
            )
        }.sortedByDescending { it.second }
    }
    val projectNames = remember(projects) { projects.associate { it.name to it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SimpleHeader("آمار و مرور", onBack) }
        item {
            Text(
                text = "هفتهٔ تو در یک نگاه • " + PersianCalendar.longDate(today, digits),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "نرخ تکمیل",
                    value = "${PersianText.number(percent, digits)}٪",
                    emoji = "📈",
                    tint = HamghadamPalette.Olive,
                    hint = "همهٔ کارها",
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "انجام‌شده",
                    value = PersianText.number(doneTotal, digits),
                    emoji = "✅",
                    tint = HamghadamPalette.Terracotta,
                    hint = "کار باز: ${PersianText.number(openTotal, digits)}",
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "تمرکز امروز",
                    value = "${PersianText.number(focusToday, digits)} دقیقه",
                    emoji = "🎯",
                    tint = HamghadamPalette.Violet,
                    hint = "${PersianText.number(focusWeekTotal, digits)} دقیقه در هفته",
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "زنجیرهٔ تمرکز",
                    value = "${PersianText.number(focusStreak, digits)} روز",
                    emoji = "🔥",
                    tint = HamghadamPalette.Amber,
                    hint = "روزهای پیوسته",
                )
            }
        }
        item { SectionTitle("کارهای هفت روز گذشته", persianDigits = digits) }
        item {
            HamghadamCard {
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (day in week) {
                        val date = JalaliDate.fromEpochDay(day.epochDay)
                        val totalHeight = (10 + 78f * (day.total.toFloat() / maxTaskDay)).dp
                        val doneHeight = (10 + 78f * (day.done.toFloat() / maxTaskDay)).dp
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                text = PersianText.number(day.done, digits),
                                fontSize = 10.sp,
                                color = HamghadamPalette.Olive,
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .height(totalHeight)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    Modifier
                                        .width(16.dp)
                                        .height(doneHeight.coerceAtMost(totalHeight))
                                        .background(HamghadamPalette.Olive, CircleShape),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = PersianCalendar.weekdayInitial(date.dayOfWeek()),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item { SectionTitle("تمرکز هفتگی", persianDigits = digits) }
        item {
            HamghadamCard {
                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (day in focusWeek) {
                        val date = JalaliDate.fromEpochDay(day.epochDay)
                        val height = (8 + 70f * (day.minutes.toFloat() / maxWeekMinutes)).dp
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                text = if (day.minutes > 0) PersianText.number(day.minutes, digits) else "–",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .width(14.dp)
                                    .height(height)
                                    .background(
                                        if (day.minutes > 0) HamghadamPalette.Violet else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape,
                                    ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = PersianCalendar.weekdayInitial(date.dayOfWeek()),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        if (habitStreaks.any { it.second > 0 }) {
            item { SectionTitle("زنجیرهٔ عادت‌ها", persianDigits = digits) }
            item {
                HamghadamCard {
                    for ((habit, streak) in habitStreaks.take(4)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).background(HamghadamPalette.Olive, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = habit.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${PersianText.number(streak, digits)} روز",
                                fontSize = 11.sp,
                                color = HamghadamPalette.Olive,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        if (tags.isNotEmpty()) {
            item { SectionTitle("برچسب‌های فعال", persianDigits = digits) }
            item {
                HamghadamCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((tag, count) in tags.take(4)) {
                            TagChip(PersianText.toPersianDigits("#$tag") + " " + PersianText.number(count, digits))
                        }
                    }
                    if (tags.size > 4) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((tag, count) in tags.drop(4)) {
                                TagChip(PersianText.toPersianDigits("#$tag") + " " + PersianText.number(count, digits))
                            }
                        }
                    }
                }
            }
        }
        item {
            HamghadamCard {
                Text(
                    text = "هم‌قدم هیچ‌کدام از این آمار را به سرور نمی‌فرستد؛ همه‌چیز از دادهٔ محلی همین دستگاه محاسبه می‌شود.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (projectNames.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    EmptyState(
                        title = "هنوز لیستی نساخته‌ای",
                        subtitle = "با ساخت لیست، توزیع کارها هم در این صفحه دیده می‌شود.",
                        emoji = "📁", art = SectionArtwork.STATS,)
                }
            }
        }
    }
}
