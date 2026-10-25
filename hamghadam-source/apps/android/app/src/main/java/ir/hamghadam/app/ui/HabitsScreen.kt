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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.HabitEntity
import ir.hamghadam.app.data.HabitLogEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.TaskQuery
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle

@Composable
fun HabitsScreen(
    habits: List<HabitEntity>,
    logs: List<HabitLogEntity>,
    settings: AppSettings,
    onToggleDay: (String, JalaliDate, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    val today = JalaliDate.today()
    val todayEpoch = today.toEpochDay()
    val digits = settings.persianDigits
    val week = remember(todayEpoch) { PersianCalendar.weekDays(today) }
    val logsByHabit = remember(logs) { logs.groupBy { it.habitId } }
    var newHabit by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SimpleHeader("عادت‌ها", onBack) }
        item {
            ScreenHeader(
                title = "عادت‌ها",
                subtitle = "${PersianText.number(habits.size, digits)} عادت روزانه",
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newHabit,
                        onValueChange = { newHabit = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("عادت تازه… مثلاً مطالعهٔ شبانه", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Button(
                        onClick = {
                            onCreate(newHabit)
                            newHabit = ""
                        },
                        enabled = newHabit.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "افزودن عادت")
                    }
                }
            }
        }

        if (habits.isEmpty()) {
            item {
                EmptyState(
                    title = "هنوز عادتی نساخته‌ای",
                    subtitle = "عادت‌ها را هر روز با یک ضربه ثبت کن و زنجیره‌ات را ببین.",
                )
            }
        } else {
            item { SectionTitle("این هفته") }
            items(habits, key = { it.id }) { habit ->
                val loggedDays = logsByHabit[habit.id].orEmpty().map { it.epochDay }.toSet()
                val streak = TaskQuery.currentStreak(loggedDays, todayEpoch)
                HabitCard(
                    habit = habit,
                    week = week,
                    loggedDays = loggedDays,
                    todayEpoch = todayEpoch,
                    streak = streak,
                    digits = digits,
                    onToggleDay = { date, done -> onToggleDay(habit.id, date, done) },
                    onDelete = { onDelete(habit.id) },
                )
            }
        }
    }
}

@Composable
private fun HabitCard(
    habit: HabitEntity,
    week: List<JalaliDate>,
    loggedDays: Set<Long>,
    todayEpoch: Long,
    streak: Int,
    digits: Boolean,
    onToggleDay: (JalaliDate, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = habit.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (streak > 0) {
                    Badge(
                        text = "زنجیرهٔ ${PersianText.number(streak, digits)} روز",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Badge(text = "امروز شروع کن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "حذف عادت",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (date in week) {
                    val done = loggedDays.contains(date.toEpochDay())
                    val isFuture = date.toEpochDay() > todayEpoch
                    val isToday = date.toEpochDay() == todayEpoch
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isFuture) { onToggleDay(date, !done) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    when {
                                        done -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = PersianText.number(date.day, digits),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    done -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Spacer(Modifier.height(3.dp))
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
}
