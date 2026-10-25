package ir.hamghadam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

/** Very soft ambient shadow used by every white card, like the reference screens. */
fun Modifier.softShadow(radius: Dp = 24.dp, elevation: Dp = 10.dp): Modifier =
    shadow(
        elevation = elevation,
        shape = RoundedCornerShape(radius),
        clip = false,
        ambientColor = Color(0x14101324),
        spotColor = Color(0x1A101324),
    )

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    radius: Dp = 24.dp,
    elevation: Dp = 10.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .softShadow(radius = radius, elevation = elevation)
        .clip(RoundedCornerShape(radius))
        .background(color)
    val click = onClick
    Column(
        modifier = if (click != null) base.clickable { click() } else base,
        content = { Column(Modifier.fillMaxWidth().padding(16.dp), content = content) },
    )
}

/** Circular white button with a soft shadow — settings, back, search actions. */
@Composable
fun CircleIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    background: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(42.dp)
            .softShadow(radius = 21.dp, elevation = 8.dp),
        shape = CircleShape,
        color = background,
        onClick = onClick,
        content = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides tint,
                ) { content() }
            }
        },
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onSettings: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Box(Modifier.padding(start = 6.dp)) { trailing() }
        }
        if (onSettings != null) {
            CircleIconButton(
                contentDescription = "تنظیمات",
                onClick = onSettings,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(19.dp))
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
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (count != null) {
                Spacer(Modifier.width(7.dp))
                Surface(shape = CardRadius.chip, color = MaterialTheme.colorScheme.surface) {
                    Text(
                        text = PersianText.number(count, persianDigits),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun TagChip(text: String, tint: Color = HamghadamPalette.Olive) {
    Surface(shape = CardRadius.chip, color = tint.copy(alpha = 0.14f)) {
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
    Surface(shape = CardRadius.chip, color = tint.copy(alpha = 0.14f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
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
        modifier = modifier.fillMaxWidth().padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 24.sp) }
        Spacer(Modifier.height(10.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

/** Every block on the dashboard uses the same 24dp white card. */
@Composable
fun HamghadamCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    SoftCard(modifier = modifier, color = color, content = content)
}

/**
 * Pastel overview tile: big value, small label, chevron and a hand-drawn trend
 * line — the "Overview" grid from the reference screens.
 */
@Composable
fun OverviewTile(
    value: String,
    label: String,
    tint: Color,
    soft: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(
        modifier = modifier
            .clip(CardRadius.tile)
            .background(soft)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = HamghadamPalette.Ink,
                modifier = Modifier.weight(1f),
            )
            Squiggle(tint = tint, modifier = Modifier.width(42.dp).height(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = HamghadamPalette.Ink.copy(alpha = 0.78f))
                if (hint != null) {
                    Text(hint, fontSize = 10.sp, color = HamghadamPalette.Ink.copy(alpha = 0.5f), maxLines = 1)
                }
            }
            Text("›", fontSize = 16.sp, color = HamghadamPalette.Ink.copy(alpha = 0.5f))
        }
    }
}

/** Tiny decaf line chart used inside overview tiles. */
@Composable
fun Squiggle(tint: Color, modifier: Modifier = Modifier, seed: Int = 0) {
    Canvas(modifier) {
        val points = listOf(0.55f, 0.2f, 0.75f, 0.4f, 0.9f, 0.3f, 0.6f, 0.15f)
        val step = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, value ->
            val y = size.height * (0.16f + value * 0.66f)
            val x = index * step
            if (index == 0) path.moveTo(x, y) else {
                val previous = points[index - 1]
                val previousY = size.height * (0.16f + previous * 0.66f)
                val controlX = x - step / 2f
                path.cubicTo(controlX, previousY, controlX, y, x, y)
            }
        }
        drawPath(path, color = tint.copy(alpha = 0.85f), style = Stroke(width = 2.2f.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Coloured square with an emoji — used by timelines, lists and quick actions. */
@Composable
fun IconTile(
    emoji: String,
    tint: Color,
    soft: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(soft, RoundedCornerShape(size * 0.36f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.4f).sp, color = tint)
    }
}

/** Pastel quick action: coloured bubble with an emoji and a caption beneath. */
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
            .widthIn(min = 62.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
    strokeWidth: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
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

/** Seven small vertical bars, like the "XP Earned" card. */
@Composable
fun MiniBars(
    values: List<Float>,
    tint: Color,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    barWidth: Dp = 7.dp,
    highlightLast: Boolean = true,
) {
    val max = values.maxOrNull()?.coerceAtLeast(0.001f) ?: 0.001f
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val ratio = (value / max).coerceIn(0.14f, 1f)
            val active = !highlightLast || index == values.lastIndex
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height * ratio)
                    .background(
                        if (active) tint else tint.copy(alpha = 0.35f),
                        RoundedCornerShape(barWidth / 2),
                    ),
            )
        }
    }
}

/** A row inside a "Today" timeline: icon, small kind label, bold title, time. */
@Composable
fun TimelineRow(
    emoji: String,
    kind: String,
    title: String,
    tint: Color,
    soft: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    checked: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(soft, RoundedCornerShape(15.dp))
                .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "انجام شد",
                    tint = Color.White,
                    modifier = Modifier
                        .size(19.dp)
                        .background(tint, CircleShape)
                        .padding(4.dp),
                )
            } else {
                Text(emoji, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(kind, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Spacer(Modifier.height(2.dp))
                Text(meta, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
    Column(
        modifier = modifier
            .clip(CardRadius.tile)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 13.sp)
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 11.sp, color = tint, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (hint != null) {
            Text(hint, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
    val dueLabel = PersianCalendar.dueLabel(task.dueDate, task.dueMinuteOfDay, today, persianDigits)
    val meta = listOfNotNull(projectName, dueLabel).joinToString(" • ")
    Surface(
        modifier = Modifier.fillMaxWidth().softShadow(radius = 20.dp, elevation = 6.dp),
        shape = CardRadius.tile,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(start = 12.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(27.dp)
                    .background(if (done) HamghadamPalette.Olive else Color.Transparent, CircleShape)
                    .border(
                        width = if (done) 0.dp else 1.8.dp,
                        color = if (done) HamghadamPalette.Olive else MaterialTheme.colorScheme.outline,
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
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotBlank() || task.tagList.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (projectName != null) {
                            Box(Modifier.size(6.dp).background(projectColor, CircleShape))
                            Spacer(Modifier.width(5.dp))
                        }
                        if (meta.isNotBlank()) MetaLabel(meta)
                        for (tag in task.tagList.take(2)) {
                            Spacer(Modifier.width(5.dp))
                            TagChip(PersianText.toPersianDigits("#$tag"))
                        }
                    }
                }
            }
            if (task.priority == TaskEntity.PRIORITY_HIGH) {
                Text("!", color = HamghadamPalette.Terracotta, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "حذف کار",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
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
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(day) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = PersianCalendar.weekdayInitial(day.dayOfWeek()),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            when {
                                selected -> HamghadamPalette.Scrim
                                isToday -> HamghadamPalette.AmberSoft
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
                            isToday -> HamghadamPalette.AmberDeep
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .background(
                            if (dotDays.contains(epoch)) HamghadamPalette.Violet else Color.Transparent,
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
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    )
}
