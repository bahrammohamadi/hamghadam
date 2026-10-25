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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.EventCatalog
import ir.hamghadam.app.data.PersonalEventEntity
import ir.hamghadam.app.domain.CalendarSource
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun CalendarSettingsScreen(
    settings: AppSettings,
    catalog: EventCatalog,
    personalEvents: List<PersonalEventEntity>,
    googleEventCount: Int,
    onBack: () -> Unit,
    onToggleSource: (CalendarSource, Boolean) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onOpenGoogle: () -> Unit,
) {
    val digits = settings.persianDigits
    val year = JalaliDate.today().year
    val bundled = catalog.eventsForYear(year)
    val counts = mapOf(
        CalendarSource.OFFICIAL to bundled.count { it.source == CalendarSource.OFFICIAL },
        CalendarSource.CULTURAL to bundled.count { it.source == CalendarSource.CULTURAL },
        CalendarSource.PERSONAL to personalEvents.size,
        CalendarSource.GOOGLE to googleEventCount,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SimpleHeader("مناسبت‌ها و تقویم", onBack) }
        item {
            Text(
                text = "کدام لایه‌ها در ماه‌نما دیده شوند؟",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            HamghadamCard {
                for (source in CalendarSource.entries) {
                    val enabled = source.id in settings.calendarSources
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(Color(source.colorArgb), CircleShape))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = source.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = when (source) {
                                    CalendarSource.OFFICIAL -> "تعطیلات رسمی ${PersianText.number(counts[source] ?: 0, digits)} روز"
                                    CalendarSource.CULTURAL -> "مناسبت‌های فرهنگی ${PersianText.number(counts[source] ?: 0, digits)} مورد"
                                    CalendarSource.PERSONAL -> "رویدادهای شخصی ${PersianText.number(counts[source] ?: 0, digits)} مورد"
                                    CalendarSource.GOOGLE -> "رویدادهای گوگل ${PersianText.number(counts[source] ?: 0, digits)} مورد"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { onToggleSource(source, it) },
                        )
                    }
                }
                if (CalendarSource.GOOGLE.id in settings.calendarSources && googleEventCount == 0) {
                    TextButton(onClick = onOpenGoogle) {
                        Text("هنوز رویدادی از گوگل ذخیره نشده — اتصال را بررسی کن", fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            HamghadamCard {
                Text("داده‌های همراه اپ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "سال‌های موجود: " + catalog.availableYears.joinToString("، ") { PersianText.number(it, digits) },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "تعطیلات رسمی و مناسبت‌های فرهنگی از فایل‌های نسخه‌بندی‌شدهٔ محلی خوانده می‌شوند؛ به اینترنت نیاز ندارند. تاریخ‌های قمری با برچسب «تقریبی» نشان داده می‌شوند.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionTitle(
                title = "رویدادهای شخصی",
                count = personalEvents.size,
                persianDigits = digits,
                action = {
                    Text(
                        text = "افزودن از تقویم",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
        if (personalEvents.isEmpty()) {
            item {
                HamghadamCard {
                    Text(
                        text = "هنوز رویداد شخصی نساخته‌ای. در صفحهٔ تقویم روی «+ رویداد» بزن؛ رنگ، تکرار سالانه و یادداشت دارد.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(personalEvents, key = { it.id }) { event ->
                val date = JalaliDate.fromEpochDay(event.epochDay)
                HamghadamCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(34.dp)
                                .background(Color(event.colorArgb), CircleShape),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = PersianCalendar.longDate(date, digits),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (event.yearlyRepeat) {
                            Badge("هر سال", HamghadamPalette.Violet)
                        }
                        IconButton(onClick = { onDeleteEvent(event.id) }) {
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
    }
}
