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
import androidx.compose.foundation.shape.CircleShape
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

private val Bg = Color(0xFF080A09)
private val Surface = Color(0xFF111512)
private val Surface2 = Color(0xFF181D1A)
private val Ink = Color(0xFFF7F8F5)
private val Muted = Color(0xFF89928D)
private val Lime = Color(0xFFDDF85B)
private val Hairline = Color(0xFF262C29)

@Composable
fun HomeScreen(
    songs: List<Song>,
    demoSong: Song,
    progressPercent: (Song) -> Int,
    onAddSong: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 28.dp, vertical = 22.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Column {
                Text("Ukulele", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("Play what you love.", color = Muted, fontSize = 14.sp)
            }
            Button(
                onClick = onAddSong,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) { Text("＋  Add song", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(28.dp))
        Text("YOUR MUSIC", color = Muted, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SongCard(
                    song = demoSong,
                    label = "DEMO",
                    progress = progressPercent(demoSong),
                    onPlay = { onPlaySong(demoSong) }
                )
            }
            items(songs, key = { it.id }) { song ->
                SongCard(
                    song = song,
                    label = "LOCAL",
                    progress = progressPercent(song),
                    onPlay = { onPlaySong(song) },
                    onEdit = { onEditSong(song) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth().weight(1f).background(Surface, RoundedCornerShape(28.dp)).border(1.dp, Hairline, RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text("LIVE MELODY", color = Lime, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("See your playing become music.", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(8.dp))
                Text("Your notes draw themselves across the song while chords and rhythm stay in context.", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.width(28.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("M0.5", color = Muted, fontSize = 10.sp)
                Text("Local audio", color = Muted, fontSize = 10.sp)
                Text("No account", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    label: String,
    progress: Int,
    onPlay: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Column(
        Modifier.width(246.dp).height(148.dp).background(Surface, RoundedCornerShape(24.dp))
            .border(1.dp, Hairline, RoundedCornerShape(24.dp)).clickable(onClick = onPlay).padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, color = Lime, fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            onEdit?.let {
                Box(
                    Modifier.background(Surface2, RoundedCornerShape(12.dp)).clickable(onClick = it).padding(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("Edit", color = Muted, fontSize = 10.sp) }
            }
        }

        Column {
            Text(song.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text("${song.bpm} BPM  ·  ${song.events.size} changes", color = Muted, fontSize = 10.sp)
            if (progress > 0) {
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).background(Hairline, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(progress / 100f).height(3.dp).background(Lime, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
