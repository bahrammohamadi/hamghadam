package ir.hamghadam.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.LockUiState
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.DEFAULT_PROFILE_ID
import ir.hamghadam.app.data.LocalProfile
import ir.hamghadam.app.data.ProfileColors
import ir.hamghadam.app.data.ProfileEmojis
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.IconTile
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.components.SoftCard
import ir.hamghadam.app.ui.theme.AccentColor
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette

/** All the callbacks the account panel needs, grouped to keep the signature small. */
data class ProfileActions(
    val onSwitch: (String) -> Unit,
    val onCreate: (String, String, Int) -> Unit,
    val onUpdate: (LocalProfile, String, String, Int) -> Unit,
    val onDelete: (LocalProfile) -> Unit,
    val onEnableLock: (String) -> Boolean,
    val onDisableLock: (String) -> Boolean,
    val onLockAfter: (Int) -> Unit,
    val onSecureScreen: (Boolean) -> Unit,
    val onAccent: (AccentColor) -> Unit,
    val onCompact: (Boolean) -> Unit,
    val onExport: (Uri) -> Unit,
    val onImport: (Uri) -> Unit,
    val onOpenSettings: () -> Unit,
    val onBack: () -> Unit,
)

@Composable
fun ProfileScreen(
    profiles: List<LocalProfile>,
    activeId: String,
    settings: AppSettings,
    lockState: LockUiState,
    lockAfterMinutes: Int,
    secureScreen: Boolean,
    actions: ProfileActions,
) {
    val digits = settings.persianDigits
    val active = profiles.firstOrNull { it.id == activeId } ?: profiles.first()
    var editing by remember { mutableStateOf<LocalProfile?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<LocalProfile?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinMode by remember { mutableStateOf("set") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showDisablePin by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) actions.onExport(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) actions.onImport(uri) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SimpleHeader("حساب و پنل کاربری", actions.onBack) }

        // ---------------------------------------------------------- hero card
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CardRadius.card)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(active.colorArgb), MaterialTheme.colorScheme.primary),
                        ),
                    ),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).background(Color.White.copy(alpha = 0.24f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(active.emoji, fontSize = 27.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = active.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "حساب فعال • ${PersianText.number(profiles.size, digits)} حساب روی این دستگاه",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (lockState.enabled) Badge("قفل فعال", Color.White)
                            if (secureScreen) Badge("محافظت اسکرین‌شات", Color.White)
                        }
                    }
                    Badge("آفلاین", Color.White)
                }
            }
        }

        // ------------------------------------------------------------ accounts
        item {
            SectionTitle(
                title = "حساب‌ها",
                count = profiles.size,
                persianDigits = digits,
                action = {
                    TextButton(onClick = { showCreate = true }) { Text("+ حساب تازه", fontSize = 12.sp) }
                },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (profile in profiles) {
                    ProfileRow(
                        profile = profile,
                        active = profile.id == activeId,
                        onSwitch = { actions.onSwitch(profile.id) },
                        onEdit = { editing = profile },
                        onDelete = if (profile.id == DEFAULT_PROFILE_ID) null else ({ deleting = profile }),
                    )
                }
            }
        }
        item {
            Text(
                text = "هر حساب کارها، لیست‌ها، عادت‌ها، رویدادها و جلسه‌های تمرکز خودش را دارد. حساب‌ها فقط روی همین دستگاه ذخیره می‌شوند و رمز سروری ندارند.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------------------------------------------------------- appearance
        item { SectionTitle("شخصی‌سازی ظاهر", persianDigits = digits) }
        item {
            HamghadamCard {
                Text("رنگ تاکیدی", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (accent in AccentColor.entries) {
                        val selected = settings.accent == accent
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accent.primary)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape,
                                )
                                .clickable { actions.onAccent(accent) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = accent.label, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "انتخاب فعلی: ${settings.accent.label}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نمایش فشرده", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("فاصله‌های کمتر برای صفحه‌های کوچک", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.compact, onCheckedChange = actions.onCompact)
                }
            }
        }

        // ------------------------------------------------------------ security
        item { SectionTitle("امنیت", persianDigits = digits) }
        item {
            HamghadamCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile("🔒", HamghadamPalette.Violet, HamghadamPalette.VioletSoft, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("قفل ورود با PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (lockState.enabled) "فعال — هنگام ورود PIN لازم است" else "خاموش — هر کسی می‌تواند اپ را باز کند",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = lockState.enabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                pinMode = "set"
                                pinError = null
                                showPinDialog = true
                            } else {
                                showDisablePin = true
                            }
                        },
                    )
                }
                if (lockState.enabled) {
                    Spacer(Modifier.height(10.dp))
                    Text("قفل خودکار پس از", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (minutes in listOf(0, 1, 5, 15)) {
                            PillChip(
                                text = if (minutes == 0) "فوری" else "${PersianText.number(minutes, digits)} دقیقه",
                                selected = lockAfterMinutes == minutes,
                                onClick = { actions.onLockAfter(minutes) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pinMode = "change"
                                pinError = null
                                showPinDialog = true
                            },
                            shape = CardRadius.field,
                        ) { Text("تغییر PIN", fontSize = 11.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("محافظت از اسکرین‌شات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "در پیش‌نمایش برنامه‌ها محتوای اپ تار می‌شود",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = secureScreen, onCheckedChange = actions.onSecureScreen)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "PIN فقط به‌صورت هش PBKDF2-SHA256 روی همین دستگاه ذخیره می‌شود؛ هیچ‌جا ارسال نمی‌شود. با فراموشی PIN راه بازیابی وجود ندارد.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------------------------------------------------------------- data
        item { SectionTitle("داده و پشتیبان", persianDigits = digits) }
        item {
            HamghadamCard {
                Text(
                    text = "پشتیبان شامل دادهٔ همین حساب است (کارها، لیست‌ها، عادت‌ها، رویدادها، تمپلیت‌ها و جلسه‌های تمرکز).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("hamghadam-${active.name}-backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = CardRadius.field,
                    ) { Text("خروجی", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        modifier = Modifier.weight(1f),
                        shape = CardRadius.field,
                    ) { Text("بازیابی", fontSize = 12.sp) }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = actions.onOpenSettings) {
                    Text("تنظیمات کامل (یادآور، لیست‌ها، تمرکز)", fontSize = 11.sp)
                }
            }
        }

        item {
            Text(
                text = "هم‌قدم ۰.۳.۰ • بدون سرور • بدون ردیابی",
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // ------------------------------------------------------------- dialogs
    if (showCreate) {
        ProfileEditorDialog(
            title = "حساب تازه",
            initialName = "",
            initialEmoji = ProfileEmojis.first(),
            initialColor = ProfileColors.first(),
            onDismiss = { showCreate = false },
            onConfirm = { name, emoji, color ->
                actions.onCreate(name, emoji, color)
                showCreate = false
            },
        )
    }

    editing?.let { profile ->
        ProfileEditorDialog(
            title = "ویرایش حساب",
            initialName = profile.name,
            initialEmoji = profile.emoji,
            initialColor = profile.colorArgb,
            onDismiss = { editing = null },
            onConfirm = { name, emoji, color ->
                actions.onUpdate(profile, name, emoji, color)
                editing = null
            },
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = CardRadius.card,
            title = { Text("حذف حساب؟", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "«${profile.name}» و همهٔ کارها، عادت‌ها، رویدادها و جلسه‌های تمرکز آن برای همیشه حذف می‌شوند. این کار برگشت‌پذیر نیست.",
                    fontSize = 12.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDelete(profile)
                    deleting = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } },
        )
    }

    if (showPinDialog) {
        PinDialog(
            title = if (pinMode == "set") "ثبت PIN" else "تغییر PIN",
            subtitle = "۴ تا ۸ رقم؛ برای باز کردن قفل از آن استفاده می‌کنی.",
            confirmLabel = "ذخیره",
            error = pinError,
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                val ok = actions.onEnableLock(pin)
                if (ok) {
                    showPinDialog = false
                    pinError = null
                } else {
                    pinError = "PIN ذخیره نشد"
                }
            },
        )
    }

    if (showDisablePin) {
        PinDialog(
            title = "خاموش کردن قفل",
            subtitle = "برای تأیید، PIN فعلی را وارد کن.",
            confirmLabel = "خاموش کن",
            error = pinError,
            onDismiss = { showDisablePin = false },
            onConfirm = { pin ->
                val ok = actions.onDisableLock(pin)
                if (ok) {
                    showDisablePin = false
                    pinError = null
                } else {
                    pinError = "PIN درست نیست"
                }
            },
        )
    }
}

@Composable
private fun ProfileRow(
    profile: LocalProfile,
    active: Boolean,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    SoftCard(color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(Color(profile.colorArgb).copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(profile.emoji, fontSize = 21.sp) }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSwitch() },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (active) {
                        Spacer(Modifier.width(6.dp))
                        Badge("فعال", MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = if (profile.id == DEFAULT_PROFILE_ID) "حساب اصلی دستگاه" else "حساب محلی",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!active) {
                TextButton(onClick = onSwitch) { Text("ورود", fontSize = 11.sp) }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Text("✎", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "حذف حساب",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardRadius.card,
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام حساب", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                Text("آواتار", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (option in ProfileEmojis.take(6)) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (emoji == option) HamghadamPalette.VioletSoft else Color.Transparent)
                                .clickable { emoji = option },
                            contentAlignment = Alignment.Center,
                        ) { Text(option, fontSize = 18.sp) }
                    }
                }
                Text("رنگ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in ProfileColors) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(option))
                                .clickable { color = option },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color == option) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, emoji, color) }, enabled = name.isNotBlank()) {
                Text("ذخیره")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

