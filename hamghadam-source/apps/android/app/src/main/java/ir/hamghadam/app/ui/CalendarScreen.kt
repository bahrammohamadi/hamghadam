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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.EmptyState
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.TaskRow

@Composable
fun CalendarScreen(
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    settings: AppSettings,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onAddForDay: (JalaliDate) -> Unit,
) {
    val today = JalaliDate.today()
    val digits = settings.persianDigits
    var anchorEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var selectedEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val anchor = JalaliDate.fromEpochDay(anchorEpoch)
    val selected = JalaliDate.fromEpochDay(selectedEpoch)

    val grid = remember(anchor) { PersianCalendar.monthGrid(anchor) }
    val daysWithTasks = remember(tasks) { tasks.filter { !it.isCompleted && it.dueEpochDay != null }.map { it.dueEpochDay!! }.toSet() }
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }
    val selectedTasks = remember(tasks, selectedEpoch) {
        tasks.filter { it.dueEpochDay == selectedEpoch }.sortedBy { it.completedAtEpochMillis != null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(title = "تقویم", subtitle = PersianCalendar.longDate(today, digits))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { anchorEpoch = anchor.plusMonths(-1).toEpochDay() }) {
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = PersianCalendar.monthTitle(anchor, digits),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (anchorEpoch != today.toEpochDay()) {
                                Text(
                                    text = "بازگشت به امروز",
                                    modifier = Modifier.clickable {
                                        anchorEpoch = today.toEpochDay()
                                        selectedEpoch = today.toEpochDay()
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        IconButton(onClick = { anchorEpoch = anchor.plusMonths(1).toEpochDay() }) {
                            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        for (dayOfWeek in PersianCalendar.weekOrder()) {
                            Text(
                                text = PersianCalendar.weekdayInitial(dayOfWeek),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    for (week in grid) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            for (cell in week) {
                                Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                    if (cell != null) {
                                        DayCell(
                                            date = cell,
                                            isToday = cell.toEpochDay() == today.toEpochDay(),
                                            isSelected = cell.toEpochDay() == selectedEpoch,
                                            hasTasks = daysWithTasks.contains(cell.toEpochDay()),
                                            digits = digits,
                                            onClick = { selectedEpoch = cell.toEpochDay() },
                                        )
                                    } else {
                                        Spacer(Modifier.size(40.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = PersianCalendar.longDate(selected, digits),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "افزودن کار",
                    modifier = Modifier.clickable { onAddForDay(selected) },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (selectedTasks.isEmpty()) {
            item { EmptyState(title = "این روز خالی است", subtitle = "برای همین روز یک کار تازه بساز.") }
        } else {
            items(selectedTasks, key = { it.id }) { task ->
                TaskRow(task, today, digits, projectNames[task.projectId], { onEdit(task) }, { onToggle(task) }, { onDelete(task) })
            }
        }
    }
}

@Composable
private fun DayCell(
    date: JalaliDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasTasks: Boolean,
    digits: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(background, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = PersianText.number(date.day, digits),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            if (hasTasks) {
                Box(
                    Modifier
                        .padding(top = 1.dp)
                        .size(4.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            CircleShape,
                        ),
                )
            }
        }
    }
}
