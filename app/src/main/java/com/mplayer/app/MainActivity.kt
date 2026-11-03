package com.mplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonGreen = Color(0xFF39FF14)
private val MBlack = Color(0xFF090B0A)
private val Panel = Color(0xFF171B18)
private val Orange = Color(0xFFFF8A24)
private val SoftWhite = Color(0xFFF2F5F2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MPlayerApp() }
    }
}

@Composable
private fun MPlayerApp() {
    var isPersian by rememberSaveable { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableStateOf("home") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MBlack,
            contentColor = SoftWhite
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).background(NeonGreen, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = MBlack, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.padding(start = 11.dp)) {
                            Text("M PLAYER", color = SoftWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            Text("YOUR MUSIC. YOUR WORLD.", color = NeonGreen, fontSize = 9.sp, letterSpacing = 1.4.sp)
                        }
                    }
                    TextButton(onClick = { isPersian = !isPersian }) {
                        Text(if (isPersian) "EN" else "فا", color = Orange, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    if (isPersian) "آلبوم‌های من" else "My Albums",
                    color = SoftWhite,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isPersian) "موسیقی‌هایت را به سلیقه خودت مرتب کن" else "Organize your music your way",
                    color = Color(0xFF9BA49D),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp, bottom = 20.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (isPersian) "هنوز آلبومی ساخته نشده" else "No albums yet",
                            color = SoftWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (isPersian) "برای شروع، آهنگ‌های گوشی را به برنامه اضافه کن." else "Start by adding songs from your phone.",
                            color = Color(0xFF9BA49D),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 7.dp)
                        )
                        Spacer(Modifier.height(17.dp))
                        Button(
                            onClick = { selectedTab = "library" },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = MBlack),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null)
                            Text(
                                if (isPersian) "انتخاب موسیقی" else "Choose Music",
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    if (isPersian) "دسترسی سریع" else "Quick Access",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        QuickTile(
                            title = if (isPersian) "همه آهنگ‌ها" else "All Songs",
                            icon = { Icon(Icons.Default.MusicNote, null, tint = NeonGreen) },
                            onClick = { selectedTab = "library" }
                        )
                    }
                    item {
                        QuickTile(
                            title = if (isPersian) "علاقه‌مندی‌ها" else "Favorites",
                            icon = { Icon(Icons.Default.Favorite, null, tint = Orange) },
                            onClick = { selectedTab = "favorites" }
                        )
                    }
                    item {
                        QuickTile(
                            title = if (isPersian) "تنظیمات" else "Settings",
                            icon = { Icon(Icons.Default.Settings, null, tint = NeonGreen) },
                            onClick = { selectedTab = "settings" }
                        )
                    }
                }
                Text(
                    "M Player  •  1.0.0",
                    color = Color(0xFF68716A),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                title,
                color = SoftWhite,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 9.dp)
            )
        }
    }
}
