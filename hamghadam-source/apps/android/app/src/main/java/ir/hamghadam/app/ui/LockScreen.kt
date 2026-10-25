package ir.hamghadam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.LockUiState
import ir.hamghadam.app.data.LocalProfile
import ir.hamghadam.app.ui.theme.CardRadius
import ir.hamghadam.app.ui.theme.HamghadamPalette
import kotlinx.coroutines.delay

/**
 * Full-screen PIN lock. Keypad is 4 columns × 3 rows, every key 72dp (well above
 * the 48dp minimum), digits shown in Persian, with an accessible description on
 * each key. Wrong attempts lock the pad for a growing number of seconds.
 */
@Composable
fun LockScreen(
    state: LockUiState,
    profile: LocalProfile,
    onUnlock: (String) -> Unit,
    onLockedOut: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var remaining by remember { mutableLongStateOf(state.blockedSeconds) }

    LaunchedEffect(state.blockedSeconds) {
        remaining = state.blockedSeconds
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }

    LaunchedEffect(state.failures) { pin = "" }

    val blocked = remaining > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.emoji, fontSize = 34.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text("سلام ${profile.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (blocked) "برای امنیت بیشتر، $remaining ثانیه صبر کن" else "برای باز کردن قفل، PIN را وارد کن",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { index ->
                val filled = index < pin.length
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            CircleShape,
                        ),
                )
            }
        }
        if (state.error != null && !blocked) {
            Spacer(Modifier.height(12.dp))
            Text(state.error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))

        Keypad(
            enabled = !blocked,
            onDigit = { digit ->
                if (pin.length < 8) {
                    pin += digit
                    if (pin.length >= 4) onUnlock(pin)
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
        )

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PIN را فراموش کرده‌ای؟",
                modifier = Modifier
                    .clip(CardRadius.chip)
                    .clickable { onLockedOut() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Keypad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (digit in row) KeyButton(digit.toString(), enabled) { onDigit(digit) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.size(72.dp))
            KeyButton("0", enabled) { onDigit(0) }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onBackspace() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "پاک کردن",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ir.hamghadam.app.domain.PersianText.toPersianDigits(label),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else HamghadamPalette.InkMuted.copy(alpha = 0.4f),
        )
    }
}

/** Small inline keypad dialog used when setting or changing the PIN. */
@Composable
fun PinDialog(
    title: String,
    subtitle: String,
    confirmLabel: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val needRepeat = title.contains("ثبت") || title.contains("تغییر")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardRadius.card,
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.OutlinedTextField(
                    value = pin,
                    onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) pin = value },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN (۴ تا ۸ رقم)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                if (needRepeat) {
                    androidx.compose.material3.OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) confirmPin = value },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("تکرار PIN", fontSize = 12.sp) },
                        singleLine = true,
                        shape = CardRadius.field,
                    )
                }
                val message = error ?: when {
                    pin.length in 1..3 -> "حداقل ۴ رقم"
                    needRepeat && confirmPin.isNotEmpty() && pin != confirmPin -> "دو PIN یکسان نیستند"
                    else -> null
                }
                if (message != null) {
                    Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            val valid = pin.length in 4..8 && (!needRepeat || pin == confirmPin)
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(pin) },
                enabled = valid,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}
