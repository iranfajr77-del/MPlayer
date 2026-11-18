package ir.pasoor.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun BluetoothLobby(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(BluetoothManager::class.java) }
    val adapter = remember { manager?.adapter }
    var permissionOk by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(adapter?.isEnabled == true) }
    var hosting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("برای بازی بلوتوثی، ابتدا مجوزها را بدهید.") }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<List<String>>(emptyList()) }
    var transport by remember { mutableStateOf<BluetoothTransport?>(null) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionOk = requiredPermissions.all { result[it] == true || ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        status = if (permissionOk) "مجوزها صادر شد." else "برای ادامه باید مجوز بلوتوث را فعال کنی."
    }
    val enableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        enabled = adapter?.isEnabled == true
        status = if (enabled) "بلوتوث روشن است." else "بلوتوث روشن نشد."
    }

    fun refreshDevices() {
        if (!permissionOk || adapter == null) return
        try {
            devices = adapter.bondedDevices.toList().sortedBy { it.name ?: it.address }
            status = if (devices.isEmpty()) "هنوز دستگاه جفت‌شده‌ای نیست؛ از تنظیمات بلوتوث گوشی‌ها را Pair کن." else "دستگاه‌های جفت‌شده نمایش داده شدند."
        } catch (_: SecurityException) { status = "مجوز دسترسی به دستگاه‌های بلوتوث لازم است." }
    }

    DisposableEffect(Unit) { onDispose { transport?.close() } }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("اتاق بلوتوث پاسور", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("اتصال کاملاً آفلاین و مستقیم بین گوشی‌هاست. حداکثر ۳ دوست می‌توانند به میزبان وصل شوند.")
        Text(status)
        if (!permissionOk) Button(onClick = { permissionLauncher.launch(requiredPermissions) }, modifier = Modifier.fillMaxWidth()) { Text("دادن مجوز بلوتوث") }
        if (permissionOk && !enabled) Button(onClick = { enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }, modifier = Modifier.fillMaxWidth()) { Text("روشن کردن بلوتوث") }
        if (permissionOk && enabled) {
            Button(onClick = {
                val a = adapter
                if (a == null) { status = "این گوشی بلوتوث ندارد."; return@Button }
                transport?.close()
                val t = BluetoothTransport(
                    onMessage = { _, message -> status = "پیام از بازیکن دریافت شد: ${message.take(80)}" },
                    onConnection = { device, isConnected ->
                        val label = try { device.name ?: device.address } catch (_: SecurityException) { "دستگاه بلوتوث" }
                        connected = if (isConnected) (connected + label).distinct() else connected - label
                        status = if (isConnected) "بازیکن وصل شد: $label" else "بازیکن جدا شد: $label"
                    },
                    onError = { status = it }
                )
                transport = t
                t.startHost(a)
                hosting = true
                status = "اتاق ساخته شد. دوستان باید گوشی میزبان را از فهرست دستگاه‌های جفت‌شده انتخاب کنند."
            }, modifier = Modifier.fillMaxWidth()) { Text(if (hosting) "میزبان فعال است" else "ساخت اتاق (میزبان)") }
            OutlinedButton(onClick = { refreshDevices() }, modifier = Modifier.fillMaxWidth()) { Text("نمایش گوشی‌های Pair شده") }
            Text("دستگاه‌های در دسترس")
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(devices) { device ->
                    val label = try { device.name ?: device.address } catch (_: SecurityException) { "دستگاه" }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(label)
                            Text(try { device.address } catch (_: SecurityException) { "" }, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            val t = transport ?: BluetoothTransport(
                                onMessage = { _, message -> status = "پیام دریافت شد: ${message.take(80)}" },
                                onConnection = { d, ok ->
                                    val n = try { d.name ?: d.address } catch (_: SecurityException) { "دستگاه" }
                                    connected = if (ok) (connected + n).distinct() else connected - n
                                },
                                onError = { status = it }
                            ).also { transport = it }
                            t.connect(device)
                            status = "در حال اتصال به $label ..."
                        }) { Text("اتصال") }
                    }
                    HorizontalDivider()
                }
            }
            Text("بازیکنان متصل: ${connected.size}/3")
            connected.forEach { Text("• $it") }
            Text("نکته: ابتدا گوشی‌ها را از تنظیمات اندروید Pair کنید. در این نسخه، اتصال و لابی پایه فراهم شده؛ همگام‌سازی نوبت‌ها و کارت‌های بازی هنوز نیازمند تکمیل و تست چندگوشی است.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("بازگشت") }
    }
}
