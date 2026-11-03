package com.mplayer.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
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

private data class MusicTrack(val id: Long, val title: String, val artist: String, val album: String, val duration: Long)
private data class UserAlbum(val id: String, val name: String, val trackIds: Set<Long>)

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
    var currentIndex by rememberSaveable { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackError by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val preferences = remember { context.getSharedPreferences("mplayer_user_data", android.content.Context.MODE_PRIVATE) }
    var favoriteIds by remember { mutableStateOf(preferences.getStringSet("favorite_ids", emptySet())?.toSet() ?: emptySet()) }
    var albums by remember { mutableStateOf(loadAlbums(preferences)) }
    var showAlbumDialog by remember { mutableStateOf(false) }
    var albumName by rememberSaveable { mutableStateOf("") }
    var editingAlbumId by remember { mutableStateOf<String?>(null) }
    var openedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingTracks by rememberSaveable { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<Long>()) }
    fun toggleFavorite(id: Long) {
        val updated = favoriteIds.toMutableSet()
        if (!updated.add(id.toString())) updated.remove(id.toString())
        favoriteIds = updated
        preferences.edit().putStringSet("favorite_ids", updated).apply()
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun refreshSongs() {
        val result = mutableListOf<MusicTrack>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                result += MusicTrack(cursor.getLong(id), cursor.getString(title) ?: "Unknown title", cursor.getString(artist) ?: "Unknown artist", cursor.getString(album) ?: "Unknown album", cursor.getLong(duration))
            }
        }
        songs = result
    }

    fun playTrack(index: Int) {
        if (index !in songs.indices) return
        try {
            player?.release()
            player = null
            isPlaying = false
            playbackError = false
            currentIndex = index
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs[index].id)
            val nextPlayer = MediaPlayer()
            player = nextPlayer
            nextPlayer.setOnPreparedListener { mp -> mp.start(); isPlaying = true }
            nextPlayer.setOnCompletionListener {
                if (index + 1 < songs.size) playTrack(index + 1) else isPlaying = false
            }
            nextPlayer.setOnErrorListener { _, _, _ -> isPlaying = false; playbackError = true; true }
            nextPlayer.setDataSource(context, uri)
            nextPlayer.prepareAsync()
        } catch (_: Exception) {
            isPlaying = false
            playbackError = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { player?.release(); player = null }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) refreshSongs()
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED) refreshSongs()
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MBlack, contentColor = SoftWhite) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(NeonGreen, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text("M", color = MBlack, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("M PLAYER", color = SoftWhite, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                            Text("YOUR MUSIC. YOUR WORLD.", color = NeonGreen, fontSize = 9.sp, letterSpacing = 1.2.sp)
                        }
                    }
                    TextButton(onClick = { isPersian = !isPersian }) { Text(if (isPersian) "EN" else "فا", color = Orange, fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(20.dp))
                if (selectedTab == "library" || selectedTab == "favorites" || selectedTab == "album") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedTab = "home" }) { Icon(Icons.Default.ArrowBack, "Back", tint = NeonGreen) }
                        Text(if (selectedTab == "favorites") { if (isPersian) "علاقه‌مندی‌ها" else "Favorites" } else if (selectedTab == "album") { albums.firstOrNull { it.id == openedAlbumId }?.name ?: if (isPersian) "آلبوم" else "Album" } else { if (isPersian) "همه آهنگ‌ها" else "All Songs" }, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    val activeAlbum = albums.firstOrNull { it.id == openedAlbumId }
                    val visibleSongs = songs.filter { song -> (selectedTab != "favorites" || favoriteIds.contains(song.id.toString())) && (selectedTab != "album" || addingTracks || activeAlbum?.trackIds?.contains(song.id) == true) && (searchQuery.isBlank() || song.title.contains(searchQuery, true) || song.artist.contains(searchQuery, true) || song.album.contains(searchQuery, true)) }
                    if (selectedTab == "album" && activeAlbum != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { addingTracks = !addingTracks; selectedTrackIds = emptySet(); searchQuery = "" }, colors = ButtonDefaults.buttonColors(containerColor = if (addingTracks) Orange else NeonGreen, contentColor = MBlack)) {
                                Icon(if (addingTracks) Icons.Default.Close else Icons.Default.Add, null)
                                Text(if (addingTracks) { if (isPersian) "انصراف" else "Cancel" } else { if (isPersian) "افزودن آهنگ" else "Add songs" })
                            }
                            if (addingTracks) Button(onClick = {
                                albums = albums.map { album -> if (album.id == activeAlbum.id) album.copy(trackIds = album.trackIds + selectedTrackIds) else album }
                                saveAlbums(preferences, albums)
                                selectedTrackIds = emptySet()
                                addingTracks = false
                            }, enabled = selectedTrackIds.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = MBlack)) {
                                Icon(Icons.Default.Check, null)
                                Text(if (isPersian) "افزودن ${selectedTrackIds.size}" else "Add ${selectedTrackIds.size}")
                            }
                        }
                    }
                    Text(if (isPersian) "${visibleSongs.size} آهنگ" else "${visibleSongs.size} songs", color = Color(0xFF9BA49D), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), placeholder = { Text(if (isPersian) "جست‌وجوی آهنگ، خواننده یا آلبوم" else "Search songs, artists or albums") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonGreen) }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = Color(0xFF39423B), focusedTextColor = SoftWhite, unfocusedTextColor = SoftWhite))
                    if (songs.isEmpty() || visibleSongs.isEmpty()) {
                        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.LibraryMusic, null, tint = NeonGreen, modifier = Modifier.size(54.dp))
                            Text(if (songs.isEmpty() && permissionDenied) { if (isPersian) "دسترسی موسیقی داده نشد" else "Music permission denied" } else if (selectedTab == "favorites" && favoriteIds.isEmpty()) { if (isPersian) "هنوز آهنگی به علاقه‌مندی‌ها اضافه نکردی" else "No favorites yet" } else { if (isPersian) "نتیجه‌ای پیدا نشد" else "No matching songs" }, color = SoftWhite)
                            Spacer(Modifier.height(14.dp))
                            if (songs.isEmpty()) Button(onClick = { permissionLauncher.launch(audioPermission) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = MBlack)) { Text(if (isPersian) "اجازه دسترسی و اسکن" else "Allow access & scan") }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(visibleSongs, key = { _, song -> song.id }) { _, song ->
                                val index = songs.indexOfFirst { it.id == song.id }
                                if (selectedTab == "album" && !addingTracks) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) {
                                            SongRow(song, isPersian, index == currentIndex, isPlaying, favoriteIds.contains(song.id.toString()), { playTrack(index) }, { toggleFavorite(song.id) })
                                        }
                                        IconButton(onClick = {
                                            val albumId = activeAlbum?.id
                                            if (albumId != null) {
                                                albums = albums.map { album -> if (album.id == albumId) album.copy(trackIds = album.trackIds - song.id) else album }
                                                saveAlbums(preferences, albums)
                                            }
                                        }) { Icon(Icons.Default.Close, contentDescription = "Remove from album", tint = Orange) }
                                    }
                                } else if (selectedTab == "album" && addingTracks) {
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable {
                                        selectedTrackIds = if (selectedTrackIds.contains(song.id)) selectedTrackIds - song.id else selectedTrackIds + song.id
                                    }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = selectedTrackIds.contains(song.id), onCheckedChange = { checked -> selectedTrackIds = if (checked) selectedTrackIds + song.id else selectedTrackIds - song.id }, colors = CheckboxDefaults.colors(checkedColor = NeonGreen, checkmarkColor = MBlack))
                                        Column(Modifier.weight(1f).padding(8.dp)) {
                                            Text(song.title, color = SoftWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(song.artist, color = Color(0xFF9BA49D), fontSize = 11.sp)
                                        }
                                    }
                                } else {
                                    SongRow(song, isPersian, index == currentIndex, isPlaying, favoriteIds.contains(song.id.toString()), { playTrack(index) }, { toggleFavorite(song.id) })
                                }
                            }
                        }
                        TextButton(onClick = { refreshSongs() }, modifier = Modifier.align(Alignment.End)) { Text(if (isPersian) "اسکن دوباره" else "Rescan", color = NeonGreen) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isPersian) "آلبوم‌های من" else "My Albums", color = SoftWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        FilledTonalIconButton(onClick = { albumName = ""; editingAlbumId = null; showAlbumDialog = true }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = NeonGreen, contentColor = MBlack)) { Icon(Icons.Default.Add, contentDescription = "Create album") }
                    }
                    Text(if (isPersian) "موسیقی‌هایت را به سلیقه خودت مرتب کن" else "Organize your music your way", color = Color(0xFF9BA49D), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 20.dp))
                    if (albums.isEmpty()) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Album, null, tint = NeonGreen, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(if (isPersian) "هنوز آلبومی ساخته نشده" else "No albums yet", color = SoftWhite, fontWeight = FontWeight.SemiBold)
                                Text(if (isPersian) "برای ساخت اولین آلبوم، دکمه + را بزن." else "Tap + to create your first album.", color = Color(0xFF9BA49D), fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                            }
                        }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(albums, key = { it.id }) { album ->
                                Card(onClick = { openedAlbumId = album.id; selectedTab = "album"; searchQuery = "" }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Box(Modifier.fillMaxWidth().height(105.dp).background(Color(0xFF252D27), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Album, null, tint = NeonGreen, modifier = Modifier.size(48.dp))
                                        }
                                        Text(album.name, color = SoftWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                                        Text(if (isPersian) "${album.trackIds.size} آهنگ" else "${album.trackIds.size} tracks", color = Orange, fontSize = 11.sp)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            IconButton(onClick = { albumName = album.name; editingAlbumId = album.id; showAlbumDialog = true }) { Icon(Icons.Default.Edit, "Rename", tint = SoftWhite) }
                                            IconButton(onClick = { albums = albums.filterNot { it.id == album.id }; saveAlbums(preferences, albums) }) { Icon(Icons.Default.Delete, "Delete", tint = Orange) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(if (isPersian) "دسترسی سریع" else "Quick Access", color = SoftWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    QuickTile(if (isPersian) "همه آهنگ‌ها" else "All Songs") { selectedTab = "library" }
                    Spacer(Modifier.height(9.dp))
                    QuickTile(if (isPersian) "علاقه‌مندی‌ها  •  ${favoriteIds.size}" else "Favorites  •  ${favoriteIds.size}") { selectedTab = "favorites"; searchQuery = "" }
                    Spacer(Modifier.weight(1f))
                    Text("M Player • 1.0.1", color = Color(0xFF68716A), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                if (currentIndex in songs.indices) {
                    val current = songs[currentIndex]
                    Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = NeonGreen, modifier = Modifier.size(30.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(current.title, color = SoftWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(current.artist, color = Color(0xFF9BA49D), fontSize = 11.sp, maxLines = 1)
                                }
                                IconButton(onClick = {
                                    val p = player
                                    if (p != null) {
                                        try { if (p.isPlaying) { p.pause(); isPlaying = false } else { p.start(); isPlaying = true } } catch (_: Exception) { playbackError = true }
                                    } else playTrack(currentIndex)
                                }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = NeonGreen, modifier = Modifier.size(32.dp)) }
                                IconButton(onClick = { player?.release(); player = null; isPlaying = false; currentIndex = -1 }) { Icon(Icons.Default.Stop, "Stop", tint = Orange) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (currentIndex > 0) playTrack(currentIndex - 1) }) { Icon(Icons.Default.SkipPrevious, "Previous", tint = SoftWhite) }
                                Text(if (playbackError) { if (isPersian) "خطا در پخش آهنگ" else "Playback error" } else { if (isPersian) "در حال پخش" else "Now playing" }, color = if (playbackError) Orange else NeonGreen, fontSize = 11.sp)
                                IconButton(onClick = { if (currentIndex + 1 < songs.size) playTrack(currentIndex + 1) }) { Icon(Icons.Default.SkipNext, "Next", tint = SoftWhite) }
                            }
                        }
                    }
                }
            }
            if (showAlbumDialog) {
                AlertDialog(onDismissRequest = { showAlbumDialog = false }, containerColor = Panel, titleContentColor = SoftWhite, textContentColor = SoftWhite,
                    title = { Text(if (editingAlbumId == null) { if (isPersian) "ساخت آلبوم جدید" else "Create album" } else { if (isPersian) "تغییر نام آلبوم" else "Rename album" }) },
                    text = { OutlinedTextField(value = albumName, onValueChange = { albumName = it }, singleLine = true, label = { Text(if (isPersian) "نام آلبوم" else "Album name") }) },
                    confirmButton = { TextButton(onClick = { val clean = albumName.trim(); if (clean.isNotEmpty()) { val id = editingAlbumId; albums = if (id == null) albums + UserAlbum(java.util.UUID.randomUUID().toString(), clean, emptySet()) else albums.map { if (it.id == id) it.copy(name = clean) else it }; saveAlbums(preferences, albums); showAlbumDialog = false } }) { Text(if (isPersian) "ذخیره" else "Save", color = NeonGreen) } },
                    dismissButton = { TextButton(onClick = { showAlbumDialog = false }) { Text(if (isPersian) "انصراف" else "Cancel", color = Orange) } })
            }
        }
    }
}

private fun loadAlbums(prefs: android.content.SharedPreferences): List<UserAlbum> {
    return try {
        val array = org.json.JSONArray(prefs.getString("user_albums", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val ids = obj.getJSONArray("tracks")
            UserAlbum(obj.getString("id"), obj.getString("name"), (0 until ids.length()).map { ids.getLong(it) }.toSet())
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveAlbums(prefs: android.content.SharedPreferences, albums: List<UserAlbum>) {
    val array = org.json.JSONArray()
    albums.forEach { album ->
        val ids = org.json.JSONArray()
        album.trackIds.forEach { ids.put(it) }
        array.put(org.json.JSONObject().put("id", album.id).put("name", album.name).put("tracks", ids))
    }
    prefs.edit().putString("user_albums", array.toString()).apply()
}

@Composable
private fun SongRow(song: MusicTrack, isPersian: Boolean, selected: Boolean, playing: Boolean, isFavorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(if (selected) Color(0xFF203321) else Panel, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).background(Color(0xFF252D27), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(if (selected && playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = NeonGreen)
        }
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(song.title, color = SoftWhite, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} • ${song.album}", color = Color(0xFF9BA49D), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(song.duration), color = Orange, fontSize = 11.sp)
        IconButton(onClick = onFavorite) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Orange else Color(0xFF9BA49D)) }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun QuickTile(title: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote, null, tint = NeonGreen)
            Text(title, color = SoftWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp))
        }
    }
}
