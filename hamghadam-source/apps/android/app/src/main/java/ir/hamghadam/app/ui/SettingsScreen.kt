package ir.hamghadam.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ProjectEntity
import ir.hamghadam.app.data.ThemeMode
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.domain.ReminderOptions
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.theme.AccentColor
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette
import java.time.ZoneId

@Composable
fun SettingsScreen(
    settings: AppSettings,
    projects: List<ProjectEntity>,
    onThemeChange: (ThemeMode) -> Unit,
    onDigitsChange: (Boolean) -> Unit,
    onDefaultReminderChange: (Int?) -> Unit,
    onFocusMinutesChange: (Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    onFocusGoalChange: (Int) -> Unit,
    onCreateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onReschedule: () -> Unit,
    onAccentChange: (AccentColor) -> Unit = {},
    onCompactChange: (Boolean) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val digits = settings.persianDigits
    var newList by remember { mutableStateOf("") }
    var showReminderMenu by remember { mutableStateOf(false) }
    var confirmDeleteProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) onExport(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImport(uri) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SimpleHeader("تنظیمات", onBack) }
        item {
            Text(
                text = "تم، اعداد و یادآورها",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            HamghadamCard {
                Text("حالت نمایش", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (mode in ThemeMode.entries) {
                        PillChip(
                            text = mode.label,
                            selected = settings.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("رنگ تاکیدی", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in AccentColor.entries) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(option.primary, CircleShape)
                                .clickable { onAccentChange(option) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (settings.accent == option) {
                                Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نمایش فشرده", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("فاصله‌های کمتر برای صفحه‌های کوچک", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.compact, onCheckedChange = onCompactChange)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ارقام فارسی", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "۱۴۰۵ به‌جای 1405",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.persianDigits, onCheckedChange = onDigitsChange)
                }
            }
        }

        item {
            HamghadamCard {
                Text("تمرکز (پومودورو)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(7.dp))
                Text("طول جلسه", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (minutes in listOf(15, 25, 50, 90)) {
                        PillChip(
                            text = PersianText.number(minutes, digits),
                            selected = settings.focusMinutes == minutes,
                            tint = HamghadamPalette.Violet,
                            onClick = { onFocusMinutesChange(minutes) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("طول استراحت", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (minutes in listOf(3, 5, 10, 15)) {
                        PillChip(
                            text = PersianText.number(minutes, digits),
                            selected = settings.breakMinutes == minutes,
                            tint = HamghadamPalette.Violet,
                            onClick = { onBreakMinutesChange(minutes) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("هدف تمرکز روزانه", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "${PersianText.number(settings.focusGoalMinutes, digits)} دقیقه",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (goal in listOf(25, 50, 90)) {
                            PillChip(
                                text = PersianText.number(goal, digits),
                                selected = settings.focusGoalMinutes == goal,
                                tint = HamghadamPalette.Violet,
                                onClick = { onFocusGoalChange(goal) },
                            )
                        }
                    }
                }
            }
        }

        item {
            HamghadamCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("یادآور پیش‌فرض", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = ReminderOptions.label(settings.defaultReminderMinutes) + " • برای کارهای تازه با تاریخ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        TextButton(onClick = { showReminderMenu = true }) { Text("تغییر", fontSize = 12.sp) }
                        DropdownMenu(
                            expanded = showReminderMenu,
                            onDismissRequest = { showReminderMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(ReminderOptions.label(null), fontSize = 13.sp) },
                                onClick = {
                                    onDefaultReminderChange(null)
                                    showReminderMenu = false
                                },
                            )
                            for (minutes in ReminderOptions.all) {
                                DropdownMenuItem(
                                    text = { Text(ReminderOptions.label(minutes), fontSize = 13.sp) },
                                    onClick = {
                                        onDefaultReminderChange(minutes)
                                        showReminderMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReschedule, shape = CardRadius.field) {
                        Text("بازسازی یادآورها", fontSize = 11.sp)
                    }
                    if (!notificationsGranted) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            shape = CardRadius.field,
                        ) {
                            Text("اجازهٔ اعلان", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item { SectionTitle("حساب کاربری", persianDigits = digits) }
        item {
            HamghadamCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("👤", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("حساب‌ها، قفل ورود و ظاهر", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "چند حساب محلی، PIN، شخصی‌سازی رنگ و پشتیبان هر حساب",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("‹", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { SectionTitle("لیست‌ها", count = projects.size, persianDigits = digits) }
        item {
            HamghadamCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newList,
                        onValueChange = { newList = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("نام لیست تازه", fontSize = 12.sp) },
                        singleLine = true,
                        shape = CardRadius.field,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onCreateProject(newList)
                            newList = ""
                        },
                        enabled = newList.isNotBlank(),
                        shape = CardRadius.field,
                    ) {
                        Text("افزودن", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                for (project in projects) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).background(Color(project.colorArgb), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = project.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { confirmDeleteProject = project }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "حذف لیست",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("پشتیبان‌گیری", persianDigits = digits) }
        item {
            HamghadamCard {
                Text(
                    text = "همهٔ داده‌ها (کارها، لیست‌ها، عادت‌ها، رویدادهای شخصی، تمپلیت‌ها و جلسه‌های تمرکز) در یک فایل JSON ذخیره می‌شوند.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("hamghadam-backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = CardRadius.field,
                    ) {
                        Text("خروجی", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = CardRadius.field,
                    ) {
                        Text("بازیابی", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "بازیابی، داده‌های فعلی را با محتوای فایل جایگزین می‌کند.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item { SectionTitle("درباره", persianDigits = digits) }
        item {
            HamghadamCard {
                Text(
                    text = "هم‌قدم $versionName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "برنامه‌ریز فارسیِ آفلاین با تقویم شمسی، مناسبت‌های ۱۴۰۴ و ۱۴۰۵، تمرکز، عادت، برچسب، تمپلیت و ثبت سریع زبان طبیعی.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "امروز: " + PersianCalendar.longDate(JalaliDate.today(), digits),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "منطقهٔ زمانی: " + ZoneId.systemDefault().id,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "همه‌چیز آفلاین است؛ اتصال اختیاری تقویم گوگل فقط یک کش محلی می‌سازد و بدون آن هم همهٔ قابلیت‌ها کار می‌کنند.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    confirmDeleteProject?.let { project ->
        AlertDialog(
            onDismissRequest = { confirmDeleteProject = null },
            title = { Text("حذف لیست؟") },
            text = { Text("«${project.name}» حذف می‌شود؛ کارهای آن بدون لیست می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProject(project.id)
                    confirmDeleteProject = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteProject = null }) { Text("انصراف") }
            },
        )
    }
}
