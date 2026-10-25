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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.CircleIconButton
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.IconTile
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.softShadow
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun MoreScreen(
    settings: AppSettings,
    openTasks: Int,
    eventCount: Int,
    focusSessions: Int,
    onOpenFocus: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenCalendarSettings: () -> Unit,
    onOpenGoogleCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val digits = settings.persianDigits
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "بیشتر",
                subtitle = "همهٔ ابزارهای هم‌قدم در یک جا",
                onSettings = onOpenSettings,
            )
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .softShadow(radius = 24.dp, elevation = 14.dp)
                    .clip(CardRadius.card)
                    .background(Brush.linearGradient(listOf(HamghadamPalette.Violet, Color(0xFF9B7CF9)))),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .background(Color.White.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🌿", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("هم‌قدم", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "کاملاً آفلاین • نسخهٔ ${PersianText.number(3, digits)}.${PersianText.number(0, digits)}",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                            )
                        }
                        SurfaceishBadge()
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("کار باز", PersianText.number(openTasks, digits))
                        MiniStat("رویداد", PersianText.number(eventCount, digits))
                        MiniStat("جلسهٔ تمرکز", PersianText.number(focusSessions, digits))
                    }
                }
            }
        }

        item { SectionTitle("برنامه‌ریزی", persianDigits = digits) }
        item {
            HamghadamCard {
                MoreRow("🎯", "تمرکز", "تایمر پومودورو و هدف روزانه", HamghadamPalette.Violet, onOpenFocus)
                Spacer(Modifier.height(2.dp))
                MoreRow("🌱", "عادت‌ها", "زنجیرهٔ روزانه و این هفته", HamghadamPalette.Olive, onOpenHabits)
                Spacer(Modifier.height(2.dp))
                MoreRow("📊", "آمار و مرور", "روند هفتگی کارها و تمرکز", HamghadamPalette.Sky, onOpenStats)
                Spacer(Modifier.height(2.dp))
                MoreRow("🧩", "تمپلیت‌ها", "کارهای تکراری را یک‌ضربه‌ای بساز", HamghadamPalette.Amber, onOpenTemplates)
            }
        }

        item { SectionTitle("تقویم", persianDigits = digits) }
        item {
            HamghadamCard {
                MoreRow("🗓️", "مناسبت‌ها و رویدادها", "تعطیلات ۱۴۰۴ و ۱۴۰۵ و تقویم فعال", HamghadamPalette.Terracotta, onOpenCalendarSettings)
                Spacer(Modifier.height(2.dp))
                MoreRow("🔗", "تقویم گوگل (اختیاری)", "اتصال با OAuth؛ آفلاین هم کار می‌کند", HamghadamPalette.Sky, onOpenGoogleCalendar)
            }
        }

        item { SectionTitle("اپلیکیشن", persianDigits = digits) }
        item {
            HamghadamCard {
                MoreRow("⚙️", "تنظیمات", "تم، ارقام فارسی، یادآور و پشتیبان", HamghadamPalette.Violet, onOpenSettings)
            }
        }
        item {
            Text(
                text = "داده‌ها روی همین دستگاه می‌مانند. اتصال گوگل فقط برای خواندن رویدادها و کاملاً اختیاری است.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun SurfaceishBadge() {
    Box(
        Modifier
            .clip(CardRadius.chip)
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("بدون سرور", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp)
    }
}

@Composable
private fun MoreRow(
    emoji: String,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardRadius.small)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(emoji, tint, tint.copy(alpha = 0.14f), size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Back circle + big bold title, used by every sub-screen. */
@Composable
fun SimpleHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(contentDescription = "بازگشت", onClick = onBack) {
            Icon(
                Icons.Filled.KeyboardArrowLeft,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
