package ir.hamghadam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onSettings: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        trailing?.invoke()
        if (onSettings != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "تنظیمات",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    count: Int? = null,
    persianDigits: Boolean = true,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (count != null) {
                Spacer(Modifier.width(6.dp))
                Surface(shape = CardRadius.chip, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = PersianText.number(count, persianDigits),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        action?.invoke()
    }
}

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val background = if (selected) tint else MaterialTheme.colorScheme.surface
    val foreground = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = CardRadius.chip,
        color = background,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun TagChip(text: String, tint: Color = HamghadamPalette.Olive) {
    Surface(shape = CardRadius.chip, color = tint.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun Badge(text: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = CardRadius.chip, color = tint.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun MetaLabel(text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        color = tint,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier, emoji: String = "🌿") {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Every block on the dashboard uses the same 24dp paper card. */
@Composable
fun HamghadamCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardRadius.card,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
    strokeWidth: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val clamped = progress.coerceIn(0f, 1f)
            if (clamped > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    emoji: String,
    tint: Color,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Card(
        modifier = modifier,
        shape = CardRadius.tile,
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 14.sp)
                Spacer(Modifier.width(5.dp))
                Text(label, fontSize = 11.sp, color = tint, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (hint != null) {
                Text(hint, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun TaskRow(
    task: TaskEntity,
    today: JalaliDate,
    persianDigits: Boolean,
    projectName: String?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    projectColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val done = task.isCompleted
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardRadius.tile,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        if (done) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        CircleShape,
                    )
                    .border(
                        width = if (done) 0.dp else 1.6.dp,
                        color = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "انجام شد",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val dueLabel = PersianCalendar.dueLabel(task.dueDate, task.dueMinuteOfDay, today, persianDigits)
                val meta = listOfNotNull(projectName, dueLabel).joinToString(" • ")
                if (meta.isNotBlank() || task.tagList.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (projectName != null) {
                            Box(Modifier.size(6.dp).background(projectColor, CircleShape))
                            Spacer(Modifier.width(5.dp))
                        }
                        if (meta.isNotBlank()) {
                            MetaLabel(meta, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        for (tag in task.tagList.take(2)) {
                            Spacer(Modifier.width(5.dp))
                            TagChip(PersianText.toPersianDigits("#$tag"))
                        }
                    }
                }
            }
            if (task.priority == TaskEntity.PRIORITY_HIGH) {
                Text("!", color = MaterialTheme.colorScheme.error, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "حذف کار",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
fun QuickAction(
    emoji: String,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(min = 64.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(tint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun WeekStrip(
    days: List<JalaliDate>,
    todayEpochDay: Long,
    selectedEpochDay: Long,
    persianDigits: Boolean,
    onSelect: (JalaliDate) -> Unit,
    dotDays: Set<Long> = emptySet(),
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (day in days) {
            val epoch = day.toEpochDay()
            val selected = epoch == selectedEpochDay
            val isToday = epoch == todayEpochDay
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(day) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = PersianCalendar.weekdayInitial(day.dayOfWeek()),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            when {
                                selected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = PersianText.number(day.day, persianDigits),
                        fontSize = 12.sp,
                        fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            selected -> Color.White
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(4.dp)
                        .background(
                            if (dotDays.contains(epoch)) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
fun WarmDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    )
}
