package com.edward.ukuleleai.ui.practice

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.DifficultyLevel
import com.edward.ukuleleai.domain.PracticeState
import com.edward.ukuleleai.domain.Song
import kotlin.math.floor

private val Bg = Color(0xFF101312)
private val Panel = Color(0xFF181D1B)
private val TextMain = Color(0xFFF5F6F2)
private val TextDim = Color(0xFFA8B0AB)
private val Accent = Color(0xFFE8F46A)
private val Grid = Color(0xFF303633)

@Composable
fun PracticeRoute(song: Song, onExit: () -> Unit, viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(song.id) { viewModel.loadSong(song) }
    PracticeScreen(
        state = state,
        onExit = { viewModel.exit(onExit) },
        onPlayPause = viewModel::togglePlayback,
        onRestart = viewModel::restart,
        onTempoChange = viewModel::changeTempo,
        onDifficulty = viewModel::setDifficulty
    )
}

@Composable
private fun PracticeScreen(
    state: PracticeState,
    onExit: () -> Unit,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onTempoChange: (Int) -> Unit,
    onDifficulty: (DifficultyLevel) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(state.song.title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${state.bpm} BPM  ·  Level ${state.difficulty.level}  ·  offline", color = TextDim, fontSize = 12.sp)
            }
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Grid)) { Text("Library") }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(1f).fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).border(1.dp, Grid, RoundedCornerShape(18.dp))) {
            Timeline(state, Modifier.fillMaxSize())
            state.countdown?.let {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(it.toString(), color = Accent, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onTempoChange(-5) }) { Text("-5 BPM") }
                Button(onClick = onPlayPause, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) {
                    Text(if (state.isPlaying || state.countdown != null) "Pause" else "Play", fontWeight = FontWeight.Bold)
                }
                Button(onClick = onRestart) { Text("Restart") }
                Button(onClick = { onTempoChange(5) }) { Text("+5 BPM") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DifficultyLevel.entries.forEach { level ->
                    Button(
                        onClick = { onDifficulty(level) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (level == state.difficulty) Accent else Grid, contentColor = if (level == state.difficulty) Bg else TextMain)
                    ) { Text(level.level.toString()) }
                }
            }
        }
    }
}

@Composable
private fun Timeline(state: PracticeState, modifier: Modifier) {
    val density = LocalDensity.current
    val chordPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 26.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val hintPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = with(density) { 14.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val strum = when (state.difficulty) {
        DifficultyLevel.ONE -> "↓"
        DifficultyLevel.TWO -> "↓  ↓"
        DifficultyLevel.THREE -> "↓  ↓↑  ↑↓↑"
        DifficultyLevel.FOUR -> "↓  x  ↓↑  ↑↓↑"
    }

    Canvas(modifier.padding(12.dp)) {
        val playX = size.width * 0.24f
        val centerY = size.height * 0.53f
        val pxPerBeat = 90.dp.toPx()
        val top = centerY - 50.dp.toPx()
        val h = 100.dp.toPx()

        val firstBeat = floor(state.positionBeats - playX / pxPerBeat).toInt() - 1
        val lastBeat = floor(state.positionBeats + (size.width - playX) / pxPerBeat).toInt() + 2
        for (beat in firstBeat..lastBeat) {
            val x = playX + ((beat - state.positionBeats) * pxPerBeat).toFloat()
            if (x in 0f..size.width) {
                drawLine(if (beat >= 0 && beat % state.song.beatsPerBar == 0) TextDim else Grid, Offset(x, top - 30.dp.toPx()), Offset(x, top + h + 30.dp.toPx()), if (beat % state.song.beatsPerBar == 0) 2.dp.toPx() else 1.dp.toPx())
            }
        }

        state.song.events.forEach { event ->
            val x = playX + ((event.beat - state.positionBeats) * pxPerBeat).toFloat()
            val w = (event.durationBeats * pxPerBeat).toFloat().coerceAtLeast(70.dp.toPx())
            if (x + w >= 0f && x <= size.width) {
                val active = state.positionBeats >= event.beat && state.positionBeats < event.beat + event.durationBeats
                drawRoundRect(
                    color = if (active) Color(0xFF465249) else Color(0xFF282E2B),
                    topLeft = Offset(x + 4.dp.toPx(), top), size = Size((w - 8.dp.toPx()).coerceAtLeast(1f), h), cornerRadius = CornerRadius(14.dp.toPx())
                )
                if (active) drawRoundRect(Accent, Offset(x + 4.dp.toPx(), top), Size((w - 8.dp.toPx()).coerceAtLeast(1f), h), CornerRadius(14.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                val cx = x + w / 2f
                drawContext.canvas.nativeCanvas.drawText(event.chord, cx, top + h * 0.44f, chordPaint)
                drawContext.canvas.nativeCanvas.drawText(strum, cx, top + h * 0.73f, hintPaint)
            }
        }

        drawLine(Accent, Offset(playX, 30.dp.toPx()), Offset(playX, size.height - 20.dp.toPx()), 4.dp.toPx())
        drawContext.canvas.nativeCanvas.drawText("PLAY", playX, 24.dp.toPx(), hintPaint)
    }
}
