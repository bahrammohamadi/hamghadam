package ir.hamghadam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.PillChip

/**
 * Jalali month picker. Built in-house because the platform date pickers are
 * Gregorian-only; the grid reuses the same month maths as the calendar screen.
 */
@Composable
fun JalaliDatePickerDialog(
    initial: JalaliDate,
    persianDigits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (JalaliDate) -> Unit,
    onClear: () -> Unit,
) {
    val today = JalaliDate.today()
    var anchorEpoch by remember { mutableStateOf(initial.toEpochDay()) }
    var selectedEpoch by remember { mutableStateOf(initial.toEpochDay()) }
    val anchor = JalaliDate.fromEpochDay(anchorEpoch)
    val selected = JalaliDate.fromEpochDay(selectedEpoch)
    val grid = remember(anchor) { PersianCalendar.monthGrid(anchor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { anchorEpoch = anchor.plusMonths(-1).toEpochDay() }) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                }
                Text(
                    text = PersianCalendar.monthTitle(anchor, persianDigits),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { anchorEpoch = anchor.plusMonths(1).toEpochDay() }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    for (dayOfWeek in PersianCalendar.weekOrder()) {
                        Text(
                            text = PersianCalendar.weekdayInitial(dayOfWeek),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                for (week in grid) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        for (cell in week) {
                            Box(Modifier.weight(1f).padding(1.dp), contentAlignment = Alignment.Center) {
                                if (cell == null) {
                                    Spacer(Modifier.size(32.dp))
                                } else {
                                    val isSelected = cell.toEpochDay() == selectedEpoch
                                    val isToday = cell.toEpochDay() == today.toEpochDay()
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> Color.Transparent
                                                },
                                                CircleShape,
                                            )
                                            .clickable { selectedEpoch = cell.toEpochDay() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = PersianText.number(cell.day, persianDigits),
                                            fontSize = 12.sp,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = PersianCalendar.longDate(selected, persianDigits),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("انتخاب") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClear) { Text("پاک کردن") }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        },
    )
}

/** Simple hour/minute chooser; returns minutes from midnight. */
@Composable
fun TimePickerDialog(
    initialMinuteOfDay: Int?,
    persianDigits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val start = initialMinuteOfDay ?: 9 * 60
    var hour by remember { mutableStateOf(start / 60) }
    var minute by remember { mutableStateOf(start % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ساعت ${PersianText.clock(hour, minute, persianDigits)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("ساعت", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (column in 0 until 6) {
                            val value = row * 6 + column
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                PillChip(
                                    text = PersianText.twoDigits(value, persianDigits),
                                    selected = value == hour,
                                    onClick = { hour = value },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("دقیقه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (value in listOf(0, 15, 30, 45)) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            PillChip(
                                text = PersianText.twoDigits(value, persianDigits),
                                selected = value == minute,
                                onClick = { minute = value },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour * 60 + minute) }) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}
