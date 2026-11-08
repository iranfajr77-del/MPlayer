package com.many1.novel

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TOTAL_CHAPTERS = 80

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("novel_reader", MODE_PRIVATE)
        setContent {
            MaterialTheme {
                NovelScreen(
                    initialChapter = prefs.getInt("last_chapter", 1),
                    onChapterChanged = { prefs.edit().putInt("last_chapter", it).apply() }
                )
            }
        }
    }
}

@Composable
private fun NovelScreen(initialChapter: Int, onChapterChanged: (Int) -> Unit) {
    var chapter by remember { mutableIntStateOf(initialChapter.coerceIn(1, TOTAL_CHAPTERS)) }
    val context = LocalContext.current
    val paper = remember {
        runCatching {
            context.assets.open("paper.png").use {
                BitmapFactory.decodeStream(it).asImageBitmap()
            }
        }.getOrNull()
    }
    val sampleText = if (chapter == 1) {
        "هنوز داستان در این فصل نوشته نشده است. متن نهایی رمان پس از آماده‌شدن، در همین قسمت و روی تصویر برگه‌ای که خودت می‌فرستی قرار می‌گیرد.\n\nاین نسخه فعلاً چارچوب مطالعه را آماده کرده است."
    } else {
        "متن فصل $chapter هنوز وارد نشده است. هر فصل از رمان پس از نگارش در این صفحه نمایش داده خواهد شد."
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF171411)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("پشت درهای بسته", color = Color(0xFFF4D8AE), fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("فصل $chapter از $TOTAL_CHAPTERS", color = Color(0xFFD8B98F), fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (paper != null) {
                Image(
                    bitmap = paper,
                    contentDescription = "تصویر برگه رمان",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFFF1D9AE)))
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 34.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("فصل $chapter", color = Color(0xFF21170F), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text(
                    sampleText,
                    color = Color.Black,
                    fontSize = 17.sp,
                    lineHeight = 31.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { if (chapter > 1) { chapter--; onChapterChanged(chapter) } },
                enabled = chapter > 1,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6335))
            ) { Text("فصل قبل") }
            Button(
                onClick = { if (chapter < TOTAL_CHAPTERS) { chapter++; onChapterChanged(chapter) } },
                enabled = chapter < TOTAL_CHAPTERS,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6335))
            ) { Text("فصل بعد") }
        }
    }
}
