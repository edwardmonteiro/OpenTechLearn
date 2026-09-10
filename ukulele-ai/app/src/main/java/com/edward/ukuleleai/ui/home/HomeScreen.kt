package com.edward.ukuleleai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.ukuleleai.domain.Song

private val Background = Color(0xFF101312)
private val Surface = Color(0xFF181D1B)
private val Primary = Color(0xFFF5F6F2)
private val Secondary = Color(0xFFA8B0AB)
private val Accent = Color(0xFFE8F46A)

@Composable
fun HomeScreen(
    songs: List<Song>,
    demoSong: Song,
    progressPercent: (Song) -> Int,
    onAddSong: () -> Unit,
    onPlaySong: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Ukulele AI", color = Primary, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Your songs. Your level. Stored on this phone.", color = Secondary, fontSize = 13.sp)
            }
            Button(onClick = onAddSong, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)) {
                Text("+ Add song", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("START PLAYING", color = Secondary, fontSize = 11.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SongCard(demoSong, "DEMO", progressPercent(demoSong)) { onPlaySong(demoSong) } }
            items(songs, key = { it.id }) { song ->
                SongCard(song, "LOCAL", progressPercent(song)) { onPlaySong(song) }
            }
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(Surface, RoundedCornerShape(18.dp)).padding(20.dp)) {
            Column {
                Text("M0.2", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Paste a chord chart and play it as a moving timeline.", color = Primary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("No account. No cloud database. No internet permission.", color = Secondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SongCard(song: Song, label: String, progress: Int, onClick: () -> Unit) {
    Column(
        Modifier.width(210.dp).height(126.dp).background(Surface, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF303633), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Column {
            Text(song.title, color = Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${song.bpm} BPM · ${song.events.size} chord changes", color = Secondary, fontSize = 11.sp)
            if (progress > 0) Text("Resume at $progress%", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
