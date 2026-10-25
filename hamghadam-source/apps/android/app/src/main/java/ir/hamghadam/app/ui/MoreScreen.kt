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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

@Composable
fun MoreScreen(
    settings: AppSettings,
    openTasks: Int,
    eventCount: Int,
    focusSessions: Int,
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
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    .background(HamghadamPalette.Terracotta, CardRadius.card),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🌿", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("هم‌قدم", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "نسخهٔ ۰.۳.۰ • کاملاً آفلاین",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                            )
                        }
                        Badge("بدون سرور", Color.White)
                    }
                    Spacer(Modifier.height(14.dp))
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
            HamghadamCard(Modifier) {
                MoreRow("🌱", "عادت‌ها", "زنجیرهٔ روزانه و این هفته", HamghadamPalette.Olive, onOpenHabits)
                MoreRow("📊", "آمار و مرور", "روند هفتگی کارها و تمرکز", HamghadamPalette.Sky, onOpenStats)
                MoreRow("🧩", "تمپلیت‌ها", "کارهای تکراری را یک‌ضربه‌ای بساز", HamghadamPalette.Amber, onOpenTemplates)
            }
        }

        item { SectionTitle("تقویم", persianDigits = digits) }
        item {
            HamghadamCard(Modifier) {
                MoreRow("🗓️", "مناسبت‌ها و رویدادها", "تعطیلات ۱۴۰۴ و ۱۴۰۵ و تقویم فعال", HamghadamPalette.Terracotta, onOpenCalendarSettings)
                MoreRow("🔗", "تقویم گوگل (اختیاری)", "اتصال با OAuth؛ آفلاین هم کار می‌کند", HamghadamPalette.Sky, onOpenGoogleCalendar)
            }
        }

        item { SectionTitle("اپلیکیشن", persianDigits = digits) }
        item {
            HamghadamCard(Modifier) {
                MoreRow("⚙️", "تنظیمات", "تم، ارقام فارسی، یادآور و پشتیبان", HamghadamPalette.Violet, onOpenSettings)
            }
        }
        item {
            Text(
                text = "داده‌ها روی همین دستگاه می‌مانند. اتصال گوگل فقط برای خواندن رویدادها و کاملاً اختیاری است.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
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
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(tint.copy(alpha = 0.13f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 15.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun SimpleHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.clickable { onBack() }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "بازگشت",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text("بازگشت", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}
