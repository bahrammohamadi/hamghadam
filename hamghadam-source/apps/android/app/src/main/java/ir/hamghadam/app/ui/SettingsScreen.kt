package ir.hamghadam.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import ir.hamghadam.app.domain.ReminderOptions
import ir.hamghadam.app.ui.components.PillChip
import java.time.ZoneId

@Composable
fun SettingsScreen(
    settings: AppSettings,
    projects: List<ProjectEntity>,
    onThemeChange: (ThemeMode) -> Unit,
    onDigitsChange: (Boolean) -> Unit,
    onDefaultReminderChange: (Int?) -> Unit,
    onCreateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onReschedule: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val digits = settings.persianDigits
    var newList by remember { mutableStateOf("") }
    var showReminderMenu by remember { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = "تنظیمات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCard(title = "ظاهر") {
                Text("حالت نمایش", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (mode in ThemeMode.entries) {
                        PillChip(
                            text = mode.label,
                            selected = settings.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ارقام فارسی", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "۱۴۰۵ به‌جای 1405",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.persianDigits, onCheckedChange = onDigitsChange)
                }
            }

            SettingsCard(title = "یادآور پیش‌فرض") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = ReminderOptions.label(settings.defaultReminderMinutes),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "برای کارهای تازه با تاریخ اعمال می‌شود.",
                            fontSize = 11.sp,
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
            }

            SettingsCard(title = "اعلان‌ها") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (notificationsGranted) "اعلان‌ها مجاز است" else "اعلان‌ها غیرفعال است",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (notificationsGranted) {
                                "یادآورها در زمان مقرر نمایش داده می‌شوند."
                            } else {
                                "برای دریافت یادآور، اجازهٔ اعلان را فعال کن."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!notificationsGranted) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("فعال‌سازی", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onReschedule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("زمان‌بندی مجدد یادآورها", fontSize = 12.sp)
                }
            }

            SettingsCard(title = "لیست‌ها") {
                if (projects.isEmpty()) {
                    Text(
                        text = "هنوز لیستی نساخته‌ای. لیست‌ها برای دسته‌بندی کارها هستند.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                for (project in projects) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { onDeleteProject(project.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "حذف لیست",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newList,
                        onValueChange = { newList = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("نام لیست تازه", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Button(
                        onClick = {
                            onCreateProject(newList)
                            newList = ""
                        },
                        enabled = newList.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("افزودن", fontSize = 12.sp)
                    }
                }
            }

            SettingsCard(title = "داده‌ها") {
                Text(
                    text = "همهٔ داده‌ها روی همین دستگاه ذخیره می‌شوند. با پشتیبان می‌توانی آن‌ها را به دستگاه دیگری منتقل کنی.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("hamghadam-backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("خروجی گرفتن", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("بازیابی", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "بازیابی، داده‌های فعلی را با محتوای فایل جایگزین می‌کند.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SettingsCard(title = "درباره") {
                Text(
                    text = "هم‌قدم $versionName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "برنامه‌ریز فارسیِ آفلاین با تقویم شمسی، یادآور محلی و ثبت سریع.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "امروز: ${PersianCalendar.longDate(JalaliDate.today(), digits)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "منطقهٔ زمانی: ${ZoneId.systemDefault().id}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "این نسخه آفلاین است و به سرور وصل نمی‌شود؛ همگام‌سازی چنددستگاهی هنوز پیاده‌سازی نشده.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
