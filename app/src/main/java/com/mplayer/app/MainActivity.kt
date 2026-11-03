package com.mplayer.app

import android.Manifest
import android.content.Intent
import android.content.ComponentName
import android.net.Uri
import android.graphics.BitmapFactory
import android.content.ContentUris
import android.content.pm.PackageManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.concurrent.Executor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
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

private data class MusicTrack(val id: Long, val title: String, val artist: String, val album: String, val duration: Long, val albumId: Long = -1L)
private data class UserAlbum(val id: String, val name: String, val trackIds: Set<Long>, val coverUri: String? = null)

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
    var currentTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var playbackError by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var queueDialog by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val preferences = remember { context.getSharedPreferences("mplayer_user_data", android.content.Context.MODE_PRIVATE) }
    var favoriteIds by remember { mutableStateOf(preferences.getStringSet("favorite_ids", emptySet())?.toSet() ?: emptySet()) }
    var albums by remember { mutableStateOf(loadAlbums(preferences)) }
    var showAlbumDialog by remember { mutableStateOf(false) }
    var albumName by rememberSaveable { mutableStateOf("") }
    var pendingCoverUri by rememberSaveable { mutableStateOf<String?>(null) }
    var editingAlbumId by remember { mutableStateOf<String?>(null) }
    var openedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingTracks by rememberSaveable { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<Long>()) }
    var librarySelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedLibraryIds by remember { mutableStateOf(setOf<Long>()) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    fun toggleFavorite(id: Long) {
        val updated = favoriteIds.toMutableSet()
        if (!updated.add(id.toString())) updated.remove(id.toString())
        favoriteIds = updated
        preferences.edit().putStringSet("favorite_ids", updated).apply()
    }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pendingCoverUri = uri.toString()
            } catch (_: Exception) {
                pendingCoverUri = uri.toString()
            }
        }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun refreshSongs() {
        val result = mutableListOf<MusicTrack>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                result += MusicTrack(cursor.getLong(id), cursor.getString(title) ?: "Unknown title", cursor.getString(artist) ?: "Unknown artist", cursor.getString(album) ?: "Unknown album", cursor.getLong(duration), cursor.getLong(albumId))
            }
        }
        songs = result
    }

    fun playTrack(index: Int) {
        if (index !in songs.indices) return
        val controller = mediaController ?: return
        try {
            playbackError = false
            val mediaItems = songs.map { song ->
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .build()
                    )
                    .build()
            }
            controller.setMediaItems(mediaItems, index, 0L)
            controller.prepare()
            controller.play()
        } catch (_: Exception) {
            isPlaying = false
            playbackError = true
        }
    }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val directExecutor = Executor { command -> command.run() }
        future.addListener({
            try {
                mediaController = future.get()
            } catch (_: Exception) {
                playbackError = true
            }
        }, directExecutor)
        onDispose {
            MediaController.releaseFuture(future)
            mediaController = null
        }
    }

    DisposableEffect(mediaController) {
        val controller = mediaController
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackDuration = controller?.duration?.takeIf { it > 0L } ?: 0L
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = songs.indexOfFirst { it.id.toString() == mediaItem?.mediaId }
                currentTrack = songs.getOrNull(currentIndex) ?: mediaItem?.let { item ->
                    MusicTrack(item.mediaId.toLongOrNull() ?: -1L, item.mediaMetadata.title?.toString() ?: "Unknown title", item.mediaMetadata.artist?.toString() ?: "Unknown artist", item.mediaMetadata.albumTitle?.toString() ?: "Unknown album", controller?.duration?.takeIf { it > 0L } ?: 0L)
                }
                playbackPosition = 0L
                playbackDuration = controller?.duration?.takeIf { it > 0L } ?: 0L
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = true
                isPlaying = false
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(mediaController, isPlaying, currentIndex) {
        while (true) {
            val controller = mediaController
            if (controller != null) {
                val activeMediaId = controller.currentMediaItem?.mediaId
                val activeIndex = if (activeMediaId == null) -1 else songs.indexOfFirst { it.id.toString() == activeMediaId }
                if (activeIndex != currentIndex) currentIndex = activeIndex
                if (activeMediaId == null) {
                    currentTrack = null
                    isPlaying = false
                    playbackPosition = 0L
                    playbackDuration = 0L
                } else {
                    val activeItem = controller.currentMediaItem
                    currentTrack = songs.getOrNull(activeIndex) ?: activeItem?.let { item ->
                        MusicTrack(item.mediaId.toLongOrNull() ?: -1L, item.mediaMetadata.title?.toString() ?: "Unknown title", item.mediaMetadata.artist?.toString() ?: "Unknown artist", item.mediaMetadata.albumTitle?.toString() ?: "Unknown album", controller.duration.takeIf { it > 0L } ?: 0L)
                    }
                    if (!isSeeking) playbackPosition = controller.currentPosition.coerceAtLeast(0L)
                    playbackDuration = controller.duration.takeIf { it > 0L } ?: 0L
                }
                shuffleEnabled = controller.shuffleModeEnabled
                repeatMode = controller.repeatMode
            }
            delay(500L)
        }
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
                        IconButton(onClick = { selectedTab = "home"; librarySelectionMode = false; selectedLibraryIds = emptySet() }) { Icon(Icons.Default.ArrowBack, "Back", tint = NeonGreen) }
                        Text(if (selectedTab == "favorites") { if (isPersian) "علاقه‌مندی‌ها" else "Favorites" } else if (selectedTab == "album") { albums.firstOrNull { it.id == openedAlbumId }?.name ?: if (isPersian) "آلبوم" else "Album" } else { if (isPersian) "همه آهنگ‌ها" else "All Songs" }, modifier = Modifier.weight(1f), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        if (selectedTab == "library") TextButton(onClick = { librarySelectionMode = !librarySelectionMode; selectedLibraryIds = emptySet(); searchQuery = "" }) { Text(if (librarySelectionMode) { if (isPersian) "پایان" else "Done" } else { if (isPersian) "انتخاب" else "Select" }, color = Orange, fontWeight = FontWeight.Bold) }
                    }
                    val activeAlbum = albums.firstOrNull { it.id == openedAlbumId }
                    if (selectedTab == "album" && activeAlbum != null && !addingTracks) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AlbumCover(activeAlbum.coverUri, Modifier.size(76.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(activeAlbum.name, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(if (isPersian) "${activeAlbum.trackIds.size} آهنگ" else "${activeAlbum.trackIds.size} tracks", color = Orange, fontSize = 12.sp)
                            }
                            TextButton(onClick = { albumName = activeAlbum.name; editingAlbumId = activeAlbum.id; pendingCoverUri = activeAlbum.coverUri; showAlbumDialog = true }) {
                                Text(if (isPersian) "ویرایش" else "Edit", color = NeonGreen)
                            }
                        }
                    }
                    val visibleSongs = songs.filter { song -> (selectedTab != "favorites" || favoriteIds.contains(song.id.toString())) && (selectedTab != "album" || addingTracks || activeAlbum?.trackIds?.contains(song.id) == true) && (searchQuery.isBlank() || song.title.contains(searchQuery, true) || song.artist.contains(searchQuery, true) || song.album.contains(searchQuery, true)) }
                    if (selectedTab == "library" && librarySelectionMode) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isPersian) "\${selectedLibraryIds.size} آهنگ انتخاب‌شده" else "\${selectedLibraryIds.size} selected", color = Orange, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    val visibleIds = visibleSongs.map { it.id }.toSet()
                                    selectedLibraryIds = if (visibleIds.isNotEmpty() && visibleIds.all { selectedLibraryIds.contains(it) }) selectedLibraryIds - visibleIds else selectedLibraryIds + visibleIds
                                }, enabled = visibleSongs.isNotEmpty()) { Text(if (visibleSongs.isNotEmpty() && visibleSongs.all { selectedLibraryIds.contains(it.id) }) { if (isPersian) "لغو همه" else "Deselect all" } else { if (isPersian) "انتخاب همه" else "Select all" }, color = NeonGreen) }
                                Button(onClick = { showAddToAlbumDialog = true }, enabled = selectedLibraryIds.isNotEmpty() && albums.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = MBlack)) { Text(if (isPersian) "افزودن به آلبوم" else "Add to album") }
                            }
                        }
                    }
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
                    if (selectedTab == "album" && addingTracks && visibleSongs.isNotEmpty()) {
                        val allVisibleSelected = visibleSongs.all { selectedTrackIds.contains(it.id) }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isPersian) "${selectedTrackIds.size} انتخاب‌شده" else "${selectedTrackIds.size} selected", color = Orange, fontSize = 12.sp)
                            TextButton(onClick = {
                                selectedTrackIds = if (allVisibleSelected) selectedTrackIds - visibleSongs.map { it.id }.toSet()
                                else selectedTrackIds + visibleSongs.map { it.id }
                            }) {
                                Text(if (allVisibleSelected) { if (isPersian) "لغو انتخاب همه" else "Deselect all" } else { if (isPersian) "انتخاب همه" else "Select all" }, color = NeonGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                                } else if (selectedTab == "library" && librarySelectionMode) {
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable {
                                        selectedLibraryIds = if (selectedLibraryIds.contains(song.id)) selectedLibraryIds - song.id else selectedLibraryIds + song.id
                                    }.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = selectedLibraryIds.contains(song.id), onCheckedChange = { checked -> selectedLibraryIds = if (checked) selectedLibraryIds + song.id else selectedLibraryIds - song.id }, colors = CheckboxDefaults.colors(checkedColor = NeonGreen, checkmarkColor = MBlack))
                                        Column(Modifier.weight(1f).padding(8.dp)) {
                                            Text(song.title, color = SoftWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${song.artist} • ${song.album}", color = Color(0xFF9BA49D), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text(formatDuration(song.duration), color = Orange, fontSize = 11.sp)
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
                        FilledTonalIconButton(onClick = { albumName = ""; editingAlbumId = null; pendingCoverUri = null; showAlbumDialog = true }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = NeonGreen, contentColor = MBlack)) { Icon(Icons.Default.Add, contentDescription = "Create album") }
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
                                        AlbumCover(album.coverUri, Modifier.fillMaxWidth().height(105.dp))
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

                if (currentTrack != null) {
                    val current = currentTrack!!
                    Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TrackArtwork(current.albumId, Modifier.size(42.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(current.title, color = SoftWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(current.artist, color = Color(0xFF9BA49D), fontSize = 11.sp, maxLines = 1)
                                }
                                IconButton(onClick = {
                                    val controller = mediaController
                                    if (controller != null) {
                                        if (controller.isPlaying) controller.pause() else controller.play()
                                    } else playTrack(currentIndex)
                                }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = NeonGreen, modifier = Modifier.size(32.dp)) }
                                IconButton(onClick = { mediaController?.stop(); mediaController?.clearMediaItems(); isPlaying = false; currentIndex = -1; currentTrack = null }) { Icon(Icons.Default.Stop, "Stop", tint = Orange) }
                            }
                            if (playbackDuration > 0L) {
                                Slider(
                                    value = playbackPosition.coerceIn(0L, playbackDuration).toFloat(),
                                    onValueChange = { isSeeking = true; playbackPosition = it.toLong() },
                                    onValueChangeFinished = { mediaController?.seekTo(playbackPosition); isSeeking = false },
                                    valueRange = 0f..playbackDuration.toFloat(),
                                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen, inactiveTrackColor = Color(0xFF465047))
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(formatDuration(playbackPosition), color = Color(0xFF9BA49D), fontSize = 10.sp)
                                    Text(formatDuration(playbackDuration), color = Color(0xFF9BA49D), fontSize = 10.sp)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; shuffleEnabled = it.shuffleModeEnabled } }) {
                                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffleEnabled) NeonGreen else SoftWhite)
                                }
                                IconButton(onClick = { mediaController?.seekToPreviousMediaItem() }) { Icon(Icons.Default.SkipPrevious, "Previous", tint = SoftWhite) }
                                Text(if (playbackError) { if (isPersian) "خطا در پخش آهنگ" else "Playback error" } else { if (isPersian) "در حال پخش" else "Now playing" }, color = if (playbackError) Orange else NeonGreen, fontSize = 11.sp)
                                IconButton(onClick = { mediaController?.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, "Next", tint = SoftWhite) }
                                IconButton(onClick = {
                                    mediaController?.let { controller ->
                                        controller.repeatMode = when (controller.repeatMode) {
                                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                            else -> Player.REPEAT_MODE_OFF
                                        }
                                        repeatMode = controller.repeatMode
                                    }
                                }) { Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (repeatMode == Player.REPEAT_MODE_OFF) SoftWhite else NeonGreen) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { queueDialog = true }) {
                                    Icon(Icons.Default.QueueMusic, null, tint = NeonGreen)
                                    Text(if (isPersian) "صف پخش (${mediaController?.mediaItemCount ?: 0})" else "Queue (${mediaController?.mediaItemCount ?: 0})", color = NeonGreen)
                                }
                            }
                        }
                    }
                }
            }
            if (queueDialog) {
                AlertDialog(
                    onDismissRequest = { queueDialog = false },
                    containerColor = Panel,
                    titleContentColor = SoftWhite,
                    title = { Text(if (isPersian) "صف پخش" else "Playback queue") },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed((0 until (mediaController?.mediaItemCount ?: 0)).mapNotNull { index ->
                                val item = try { mediaController?.getMediaItemAt(index) } catch (_: Exception) { null }
                                item?.let { Triple(index, it.mediaMetadata.title?.toString() ?: "Unknown title", it.mediaMetadata.artist?.toString() ?: "Unknown artist") }
                            }, key = { _, item -> item.first }) { _, entry ->
                                Row(
                                    Modifier.fillMaxWidth().background(if (entry.first == mediaController?.currentMediaItemIndex) Color(0xFF203321) else Panel, RoundedCornerShape(10.dp)).clickable {
                                        mediaController?.seekTo(entry.first, 0L)
                                        mediaController?.play()
                                        queueDialog = false
                                    }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.second, color = SoftWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.third, color = Color(0xFF9BA49D), fontSize = 11.sp, maxLines = 1)
                                    }
                                    if (entry.first == mediaController?.currentMediaItemIndex) Text(if (isPersian) "اکنون" else "Playing", color = NeonGreen, fontSize = 10.sp)
                                    else IconButton(onClick = { mediaController?.removeMediaItem(entry.first) }) { Icon(Icons.Default.Close, "Remove from queue", tint = Orange) }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { queueDialog = false }) { Text(if (isPersian) "بستن" else "Close", color = NeonGreen) } }
                )
            }
            if (showAddToAlbumDialog) {
                AlertDialog(onDismissRequest = { showAddToAlbumDialog = false }, containerColor = Panel, titleContentColor = SoftWhite, textContentColor = SoftWhite,
                    title = { Text(if (isPersian) "انتخاب آلبوم مقصد" else "Choose destination album") },
                    text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        albums.forEach { album ->
                            TextButton(onClick = {
                                albums = albums.map { item -> if (item.id == album.id) item.copy(trackIds = item.trackIds + selectedLibraryIds) else item }
                                saveAlbums(preferences, albums)
                                showAddToAlbumDialog = false
                                librarySelectionMode = false
                                selectedLibraryIds = emptySet()
                            }, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Album, null, tint = NeonGreen)
                                    Text(album.name, color = SoftWhite, modifier = Modifier.weight(1f).padding(start = 10.dp))
                                    Text("${album.trackIds.size}", color = Orange)
                                }
                            }
                        }
                    } },
                    confirmButton = { TextButton(onClick = { showAddToAlbumDialog = false }) { Text(if (isPersian) "بستن" else "Close", color = Orange) } })
            }
            if (showAlbumDialog) {
                AlertDialog(onDismissRequest = { showAlbumDialog = false }, containerColor = Panel, titleContentColor = SoftWhite, textContentColor = SoftWhite,
                    title = { Text(if (editingAlbumId == null) { if (isPersian) "ساخت آلبوم جدید" else "Create album" } else { if (isPersian) "تغییر نام آلبوم" else "Rename album" }) },
                    text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = albumName, onValueChange = { albumName = it }, singleLine = true, label = { Text(if (isPersian) "نام آلبوم" else "Album name") }); Row(verticalAlignment = Alignment.CenterVertically) { AlbumCover(pendingCoverUri, Modifier.size(68.dp)); Spacer(Modifier.width(12.dp)); Column { TextButton(onClick = { coverPicker.launch(arrayOf("image/*")) }) { Text(if (isPersian) "انتخاب تصویر از گالری" else "Choose gallery image", color = NeonGreen) }; if (pendingCoverUri != null) TextButton(onClick = { pendingCoverUri = null }) { Text(if (isPersian) "حذف تصویر انتخابی" else "Remove selected image", color = Orange) } } } } },
                    confirmButton = { TextButton(onClick = { val clean = albumName.trim(); if (clean.isNotEmpty()) { val id = editingAlbumId; albums = if (id == null) albums + UserAlbum(java.util.UUID.randomUUID().toString(), clean, emptySet(), pendingCoverUri) else albums.map { if (it.id == id) it.copy(name = clean, coverUri = pendingCoverUri) else it }; saveAlbums(preferences, albums); showAlbumDialog = false } }) { Text(if (isPersian) "ذخیره" else "Save", color = NeonGreen) } },
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
            UserAlbum(obj.getString("id"), obj.getString("name"), (0 until ids.length()).map { ids.getLong(it) }.toSet(), obj.optString("coverUri").takeIf { it.isNotBlank() && it != "null" })
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveAlbums(prefs: android.content.SharedPreferences, albums: List<UserAlbum>) {
    val array = org.json.JSONArray()
    albums.forEach { album ->
        val ids = org.json.JSONArray()
        album.trackIds.forEach { ids.put(it) }
        array.put(org.json.JSONObject().put("id", album.id).put("name", album.name).put("tracks", ids).put("coverUri", album.coverUri))
    }
    prefs.edit().putString("user_albums", array.toString()).apply()
}

@Composable
private fun AlbumCover(uri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        if (uri.isNullOrBlank()) null else try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (_: Exception) { null }
    }
    Box(modifier.background(Color(0xFF252D27), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "Album cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.Album, null, tint = NeonGreen, modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun TrackArtwork(albumId: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(albumId) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(albumId) {
        bitmap = if (albumId > 0L) withContext(Dispatchers.IO) {
            try {
                val artUri = Uri.parse("content://media/external/audio/albumart/$albumId")
                context.contentResolver.openInputStream(artUri)?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        } else null
    }
    Box(modifier.background(Color(0xFF252D27), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Track artwork", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.MusicNote, null, tint = NeonGreen, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun SongRow(song: MusicTrack, isPersian: Boolean, selected: Boolean, playing: Boolean, isFavorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(if (selected) Color(0xFF203321) else Panel, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).background(Color(0xFF252D27), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            TrackArtwork(song.albumId, Modifier.fillMaxSize())
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
