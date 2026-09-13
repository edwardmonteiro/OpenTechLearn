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

private val Night = Color(0xFF070908)
private val Glass = Color(0xFF101412)
private val Glass2 = Color(0xFF171C19)
private val White = Color(0xFFF6F7F3)
private val Fog = Color(0xFF858F89)
private val Acid = Color(0xFFDDF45A)
private val Mint = Color(0xFF70E0B6)
private val Line = Color(0xFF262D29)

@Composable
fun HomeScreen(
    songs: List<Song>, demoSong: Song, progressPercent: (Song) -> Int,
    onAddSong: () -> Unit, onPlaySong: (Song) -> Unit, onEditSong: (Song) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Night)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Ukulele", color = White, fontSize = 36.sp, fontWeight = FontWeight.ExtraLight)
                    Text("A quiet place to learn the songs you love.", color = Fog, fontSize = 13.sp)
                }
                Button(
                    onClick = onAddSong, shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Night),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 21.dp, vertical = 11.dp)
                ) { Text("＋ Add song", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(30.dp))
            Text("LIBRARY", color = Fog, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item { SongCard(demoSong, "DEMO", progressPercent(demoSong), { onPlaySong(demoSong) }) }
                items(songs, key = { it.id }) { song ->
                    SongCard(song, "ON DEVICE", progressPercent(song), { onPlaySong(song) }, { onEditSong(song) })
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth().weight(1f).background(Glass, RoundedCornerShape(30.dp)).border(1.dp, Line, RoundedCornerShape(30.dp)).padding(26.dp),
                Arrangement.SpaceBetween, Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SAFE LISTEN", color = Mint, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Hear the instrument, not the phone.", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraLight)
                    Spacer(Modifier.height(7.dp))
                    Text("When Listen is active, speaker clicks stop automatically. Beat guidance moves to touch and light.", color = Fog, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("STUDIO 0.6", color = Acid, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("Local audio", color = Fog, fontSize = 10.sp)
                    Text("Haptic pulse", color = Fog, fontSize = 10.sp)
                    Text("No account", color = Fog, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SongCard(song: Song, label: String, progress: Int, onPlay: () -> Unit, onEdit: (() -> Unit)? = null) {
    Column(
        Modifier.width(250.dp).height(150.dp).background(Glass, RoundedCornerShape(25.dp)).border(1.dp, Line, RoundedCornerShape(25.dp))
            .clickable(onClick = onPlay).padding(18.dp), Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, color = if (label == "DEMO") Acid else Mint, fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            onEdit?.let {
                Box(Modifier.background(Glass2, RoundedCornerShape(12.dp)).clickable(onClick = it).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Edit", color = Fog, fontSize = 10.sp)
                }
            }
        }
        Column {
            Text(song.title, color = White, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${song.bpm} BPM   ·   ${song.events.size} changes", color = Fog, fontSize = 10.sp)
            if (progress > 0) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Line, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(progress / 100f).height(2.dp).background(White, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
