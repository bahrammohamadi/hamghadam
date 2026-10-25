package ir.hamghadam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hamghadam.app.data.TaskEntity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HamghadamApp() }
    }
}

@Composable
private fun HamghadamApp(taskViewModel: TaskViewModel = viewModel()) {
    val tasks by taskViewModel.tasks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val ink = Color(0xFF172C2A)
    val accent = Color(0xFF237A69)

    CompositionLocalProviderRtl {
        MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = Color(0xFFF6F7F4), surface = Color.White)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("فضای شخصی", color = Color(0xFF63716E), fontSize = 14.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("کارهای من", color = ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("هم‌قدم", color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(20.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = ink), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("با یک قدم شروع کن", color = Color(0xFFB9D7CE), fontSize = 14.sp)
                            Spacer(Modifier.height(7.dp))
                            Text("کارهای مهمت را ثبت کن؛\nحتی بدون اینترنت در دسترس‌اند.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) { Text("＋  افزودن کار") }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("فهرست کارها", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("${tasks.count { it.isCompleted }} از ${tasks.size} انجام‌شده", color = Color(0xFF71807C), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (tasks.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("هنوز کاری ثبت نشده", color = ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(7.dp))
                                Text("برای شروع، یک کار تازه اضافه کن.", color = Color(0xFF71807C), fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(tasks, key = { it.id }) { task ->
                                TaskRow(task = task, onToggle = { taskViewModel.toggle(task) }, onDelete = { taskViewModel.delete(task) })
                            }
                        }
                    }
                    Text("ذخیره‌شده روی این دستگاه", Modifier.padding(top = 12.dp), color = Color(0xFF7A8985), fontSize = 11.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("کار تازه") },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان کار") }, singleLine = true)
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    taskViewModel.addTask(title)
                    showAddDialog = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun CompositionLocalProviderRtl(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 7.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Text(
                task.title,
                Modifier.weight(1f).padding(horizontal = 8.dp),
                color = if (task.isCompleted) Color(0xFF89938F) else Color(0xFF172C2A),
                fontSize = 15.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            )
            TextButton(onClick = onDelete) { Text("حذف", color = Color(0xFF9B4545)) }
        }
    }
}
