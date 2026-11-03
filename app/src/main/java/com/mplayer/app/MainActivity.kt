package com.mplayer.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonGreen = Color(0xFF39FF14)
private val MBlack = Color(0xFF090B0A)
private val Panel = Color(0xFF171B18)
private val Orange = Color(0xFFFF8A24)
private val SoftWhite = Color(0xFFF2F5F2)

private data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MPlayerApp() }
    }
}

@Composable
private fun MPlayerApp() {
    val context = LocalContext.current
    var isPersian by rememberSaveable { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableStateOf("home") }
    var songs by remember { mutableStateOf(emptyList<MusicTrack>()) }
    var permissionDenied by remember { mutableStateOf(false) }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun refreshSongs() {
        val result = mutableListOf<MusicTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                result += MusicTrack(
                    id = cursor.getLong(idColumn),
                    title = cursor.getString(titleColumn) ?: "Unknown title",
                    artist = cursor.getString(artistColumn) ?: "Unknown artist",
                    album = cursor.getString(albumColumn) ?: "Unknown album",
                    duration = cursor.getLong(durationColumn)
                )
            }
        }
        songs = result
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) refreshSongs()
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED) {
            refreshSongs()
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MBlack, contentColor = SoftWhite) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(NeonGreen, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = MBlack, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("M PLAYER", color = SoftWhite, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                            Text("YOUR MUSIC. YOUR WORLD.", color = NeonGreen, fontSize = 9.sp, letterSpacing = 1.2.sp)
                        }
                    }
                    TextButton(onClick = { isPersian = !isPersian }) {
                        Text(if (isPersian) "EN" else "فا", color = Orange, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(22.dp))

                if (selectedTab == "library") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedTab = "home" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                        }
                        Text(if (isPersian) "همه آهنگ‌ها" else "All Songs", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (isPersian) "${songs.size} آهنگ در گوشی پیدا شد" else "${songs.size} songs found on device",
                        color = Color(0xFF9BA49D), fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )

                    if (songs.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.LibraryMusic, null, tint = NeonGreen, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (permissionDenied) {
                                    if (isPersian) "دسترسی موسیقی داده نشد" else "Music permission was not granted"
                                } else {
                                    if (isPersian) "هنوز آهنگی پیدا نشده" else "No music found yet"
                                },
                                color = SoftWhite, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isPersian) "برای دیدن آهنگ‌های گوشی، اجازه دسترسی بده." else "Allow access to scan music on your phone.",
                                color = Color(0xFF9BA49D), fontSize = 12.sp,
                                modifier = Modifier.padding(top = 7.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { permissionLauncher.launch(audioPermission) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = MBlack)
                            ) {
                                Text(if (isPersian) "اجازه دسترسی و اسکن" else "Allow access & scan")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(songs, key = { it.id }) { song ->
                                SongRow(song = song, isPersian = isPersian)
                            }
                        }
                        TextButton(onClick = { refreshSongs() }, modifier = Modifier.align(Alignment.End)) {
                            Text(if (isPersian) "اسکن دوباره" else "Rescan", color = NeonGreen)
                        }
                    }
                } else {
                    Text(
                        if (isPersian) "آلبوم‌های من" else "My Albums",
                        color = SoftWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isPersian) "موسیقی‌هایت را به سلیقه خودت مرتب کن" else "Organize your music your way",
                        color = Color(0xFF9BA49D), fontSize = 13.sp,
                        modifier = Modifier.padding(top = 5.dp, bottom = 20.dp)
                    )
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Panel)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Album, null, tint = NeonGreen, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(if (isPersian) "هنوز آلبومی ساخته نشده" else "No albums yet", color = SoftWhite, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isPersian) "برای شروع، آهنگ‌های گوشی را اسکن کن." else "Scan music on your phone to get started.",
                                color = Color(0xFF9BA49D), fontSize = 12.sp,
                                modifier = Modifier.padding(top = 7.dp)
                            )
                            Spacer(Modifier.height(17.dp))
                            Button(
                                onClick = { selectedTab = "library" },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = MBlack),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.LibraryMusic, null)
                                Text(if (isPersian) "انتخاب موسیقی" else "Choose Music", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(if (isPersian) "دسترسی سریع" else "Quick Access", color = SoftWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    QuickTile(if (isPersian) "همه آهنگ‌ها" else "All Songs", { Icon(Icons.Default.MusicNote, null, tint = NeonGreen) }) { selectedTab = "library" }
                    Spacer(Modifier.height(9.dp))
                    QuickTile(if (isPersian) "علاقه‌مندی‌ها" else "Favorites", { Icon(Icons.Default.Favorite, null, tint = Orange) }) { }
                    Spacer(Modifier.height(9.dp))
                    QuickTile(if (isPersian) "تنظیمات" else "Settings", { Icon(Icons.Default.Settings, null, tint = NeonGreen) }) { }
                    Spacer(Modifier.weight(1f))
                    Text("M Player  •  1.0.0", color = Color(0xFF68716A), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: MusicTrack, isPersian: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).background(Color(0xFF252D27), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = NeonGreen)
        }
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(song.title, color = SoftWhite, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${song.artist} • ${song.album}",
                color = Color(0xFF9BA49D), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(formatDuration(song.duration), color = Orange, fontSize = 11.sp)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun QuickTile(title: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(title, color = SoftWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp))
        }
    }
}
