package com.edward.ukuleleai.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.LyricEvent
import com.edward.ukuleleai.domain.Song

@Composable
fun PracticeRoute07(song: Song, onExit: () -> Unit, viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        PracticeRoute(song, onExit, viewModel)

        if (state.backingAvailable) {
            val click = if (!state.listeningEnabled) Modifier.clickable { viewModel.toggleBacking() } else Modifier
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 13.dp)
                    .background(if (state.backingEnabled) Color(0xFF263A32) else Color(0xFF171C19), RoundedCornerShape(15.dp))
                    .then(click).padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    when {
                        state.listeningEnabled -> "Original audio · silent while Listen is on"
                        state.backingEnabled -> "● Original audio ON"
                        else -> "Original audio OFF"
                    },
                    color = if (state.backingEnabled) Color(0xFF70E0B6) else Color(0xFF858F89),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (state.song.lyrics.isNotEmpty()) {
            KaraokePanel(
                lyrics = state.song.lyrics,
                beat = state.positionBeats,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 42.dp, end = 42.dp, bottom = 82.dp)
            )
        }
    }
}

@Composable
private fun KaraokePanel(lyrics: List<LyricEvent>, beat: Double, modifier: Modifier = Modifier) {
    val currentIndex = lyrics.indexOfLast { beat >= it.beat }
    val current = lyrics.getOrNull(currentIndex.coerceAtLeast(0))
    val next = lyrics.getOrNull((currentIndex + 1).coerceAtLeast(0))
    val active = current?.takeIf { beat < it.beat + it.durationBeats }
    val line = active ?: next ?: current
    val upcoming = when {
        active != null -> lyrics.getOrNull(currentIndex + 1)
        next != null -> lyrics.getOrNull(currentIndex + 2)
        else -> null
    }

    Column(
        modifier.background(Color(0xE6101412), RoundedCornerShape(22.dp)).padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("KARAOKE", color = Color(0xFF858F89), fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            line?.text ?: "♪",
            color = Color(0xFFF6F7F3),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        upcoming?.let {
            Spacer(Modifier.height(3.dp))
            Text(it.text, color = Color(0xFF858F89), fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
