package ir.hamghadam.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.GoogleUiState
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.ExternalEventEntity
import ir.hamghadam.app.domain.CalendarSource
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.domain.PersianCalendar
import ir.hamghadam.app.domain.PersianText
import ir.hamghadam.app.ui.components.Badge
import ir.hamghadam.app.ui.components.HamghadamCard
import ir.hamghadam.app.ui.components.ScreenHeader
import ir.hamghadam.app.ui.components.SectionTitle
import ir.hamghadam.app.ui.theme.CardRadius

@Composable
fun GoogleCalendarScreen(
    state: GoogleUiState,
    settings: AppSettings,
    events: List<ExternalEventEntity>,
    onBack: () -> Unit,
    onClientIdChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onAuthUrlOpened: () -> Unit,
) {
    val context = LocalContext.current
    val digits = settings.persianDigits
    var clientId by remember(state.clientId) { mutableStateOf(state.clientId) }

    LaunchedEffect(state.authUrl) {
        val url = state.authUrl ?: return@LaunchedEffect
        val opened = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (opened) onAuthUrlOpened()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SimpleHeader("تقویم گوگل", onBack) }
        item {
            Text(
                text = "اتصال اختیاری • فقط خواندن رویدادها • آفلاین هرگز نمی‌شکند",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            HamghadamCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).background(Color(CalendarSource.GOOGLE.colorArgb), CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.connected) "متصل" else "متصل نیست",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.connected) Badge("OAuth", CalendarSource.GOOGLE.colorArgb.let { Color(it) })
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.statusMessage,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.error.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(state.error, fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
                if (state.lastSyncMillis > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "آخرین همگام‌سازی: " + PersianText.number(
                            ((System.currentTimeMillis() - state.lastSyncMillis) / 60_000).coerceAtLeast(0),
                            digits,
                        ) + " دقیقه پیش",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.connected) {
                        OutlinedButton(onClick = onSync, shape = CardRadius.field, enabled = !state.busy) {
                            Text("همگام‌سازی الان", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onDisconnect) {
                            Text("قطع اتصال", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(onClick = onConnect, shape = CardRadius.field, enabled = !state.busy) {
                            Text("اتصال به گوگل", fontSize = 12.sp)
                        }
                    }
                    if (state.busy) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
        item {
            HamghadamCard {
                Text("شناسهٔ کلاینت OAuth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = clientId,
                    onValueChange = {
                        clientId = it
                        onClientIdChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("1234567890-abc.apps.googleusercontent.com", fontSize = 11.sp) },
                    singleLine = true,
                    shape = CardRadius.field,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "در Google Cloud یک OAuth Client از نوع «Desktop app» بساز و شناسه‌اش را اینجا بگذار. Redirect از نوع loopback است (http://127.0.0.1) و نیازی به اثر انگشت امضای APK ندارد.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            HamghadamCard {
                Text("حریم خصوصی و آفلاین", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "بدون اتصال، هیچ درخواستی به شبکه ارسال نمی‌شود. با اتصال، فقط رویدادهای تقویم اصلی خوانده و در حافظهٔ محلی نگه داشته می‌شوند؛ توکن‌ها رمزنگاری‌شده روی همین دستگاه می‌مانند و هر زمان با «قطع اتصال» پاک می‌شوند.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionTitle(
                title = "رویدادهای ذخیره‌شده",
                count = events.size,
                persianDigits = digits,
                action = {
                    if (state.connected) {
                        Text(
                            text = "به‌روزرسانی",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }
        if (events.isEmpty()) {
            item {
                HamghadamCard {
                    Text(
                        text = "پس از اتصال و همگام‌سازی، رویدادهای گوگل با نقطهٔ آبی در ماه‌نما دیده می‌شوند. تا آن زمان، تقویم کاملاً از دادهٔ محلی کار می‌کند.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(events.take(8), key = { it.id }) { event ->
                // Guard against a corrupt cached row; one bad event must not
                // close the screen (audit C-07).
                val date = runCatching { JalaliDate.fromEpochDay(event.epochDay) }.getOrNull()
                    ?: return@items
                HamghadamCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color(event.colorArgb), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = PersianCalendar.longDate(date, digits) +
                                    if (event.location.isNotBlank()) " • " + event.location else "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (event.allDay) Badge("تمام‌روز", CalendarSource.GOOGLE.colorArgb.let { Color(it) })
                    }
                }
            }
        }
    }
}
