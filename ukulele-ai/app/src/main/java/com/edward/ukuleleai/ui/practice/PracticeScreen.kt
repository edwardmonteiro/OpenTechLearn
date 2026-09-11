package com.edward.ukuleleai.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.ChordEvent
import com.edward.ukuleleai.domain.DifficultyLevel
import com.edward.ukuleleai.domain.PracticeState
import com.edward.ukuleleai.domain.Song
import kotlin.math.floor

private val Bg = Color(0xFF0D100F)
private val Panel = Color(0xFF151A18)
private val TextMain = Color(0xFFF7F8F4)
private val TextDim = Color(0xFFA7B0AA)
private val Accent = Color(0xFFE9F45E)
private val Grid = Color(0xFF303733)
private val Down = Color(0xFF67D8FF)
private val Up = Color(0xFFFF78B9)
private val Mute = Color(0xFFFFB454)
private val Melody = Color(0xFF8BE6C2)

@Composable
fun PracticeRoute(song: Song, onExit: () -> Unit, viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    LaunchedEffect(song.id) { viewModel.loadSong(song) }

    val toggleListening = {
        if (state.listeningEnabled) {
            viewModel.stopListening()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    PracticeScreen(
        state = state,
        onExit = { viewModel.exit(onExit) },
        onPlayPause = viewModel::togglePlayback,
        onRestart = viewModel::restart,
        onTempoChange = viewModel::changeTempo,
        onDifficulty = viewModel::setDifficulty,
        onToggleSound = viewModel::toggleSound,
        onToggleListening = toggleListening
    )
}

@Composable
private fun PracticeScreen(
    state: PracticeState,
    onExit: () -> Unit,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onTempoChange: (Int) -> Unit,
    onDifficulty: (DifficultyLevel) -> Unit,
    onToggleSound: () -> Unit,
    onToggleListening: () -> Unit
) {
    val currentChord = currentChord(state)
    val arrangement = basicArrangement(state.difficulty, currentChord)

    Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(state.song.title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${state.bpm} BPM  ·  Level ${state.difficulty.level}  ·  local", color = TextDim, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggleListening,
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.listeningEnabled) Color(0xFF20352E) else Grid)
                ) {
                    Text(if (state.listeningEnabled) "Listen ON" else "Listen", color = if (state.listeningEnabled) Melody else TextDim)
                }
                Button(onClick = onToggleSound, colors = ButtonDefaults.buttonColors(containerColor = if (state.soundEnabled) Color(0xFF27352F) else Grid)) {
                    Text(if (state.soundEnabled) "Sound ON" else "Sound OFF", color = if (state.soundEnabled) Accent else TextDim)
                }
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Grid)) { Text("Library") }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF121715), RoundedCornerShape(16.dp)).border(1.dp, Grid, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoCell("MELODY", state.detectedNote ?: if (state.listeningEnabled) "listening…" else "—", Melody)
            InfoCell("CHORD", currentChord ?: "—", Accent)
            InfoCell("ARRANGEMENT", arrangement, TextMain)
            val cents = state.detectedCents
            InfoCell("TUNING", if (cents == null) "—" else if (kotlin.math.abs(cents) <= 6) "in tune" else "${if (cents > 0) "+" else ""}$cents¢", if (cents != null && kotlin.math.abs(cents) <= 6) Melody else TextDim)
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).border(1.dp, Grid, RoundedCornerShape(22.dp))) {
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
                Button(onClick = { onTempoChange(-5) }) { Text("−5") }
                Button(onClick = onPlayPause, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) {
                    Text(if (state.isPlaying || state.countdown != null) "Pause" else "Play", fontWeight = FontWeight.Bold)
                }
                Button(onClick = onRestart) { Text("Restart") }
                Button(onClick = { onTempoChange(5) }) { Text("+5") }
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
private fun InfoCell(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun currentChord(state: PracticeState): String? = state.song.events
    .lastOrNull { state.positionBeats >= it.beat && state.positionBeats < it.beat + it.durationBeats }
    ?.chord
    ?: state.song.events.firstOrNull()?.chord

private fun basicArrangement(level: DifficultyLevel, chord: String?): String {
    if (chord == null) return "—"
    return when (level) {
        DifficultyLevel.ONE -> "$chord · one strum"
        DifficultyLevel.TWO -> "$chord · downbeats"
        DifficultyLevel.THREE -> "$chord · pop groove"
        DifficultyLevel.FOUR -> "$chord · mute + syncopation"
    }
}

@Composable
private fun Timeline(state: PracticeState, modifier: Modifier) {
    val density = LocalDensity.current
    val chordPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 28.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val smallPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val downPaint = remember(density) { rhythmPaint(density.run { 19.sp.toPx() }, android.graphics.Color.rgb(103, 216, 255)) }
    val upPaint = remember(density) { rhythmPaint(density.run { 19.sp.toPx() }, android.graphics.Color.rgb(255, 120, 185)) }
    val mutePaint = remember(density) { rhythmPaint(density.run { 17.sp.toPx() }, android.graphics.Color.rgb(255, 180, 84)) }
    val melodyPaint = remember(density) { rhythmPaint(density.run { 16.sp.toPx() }, android.graphics.Color.rgb(139, 230, 194)) }

    val strum = when (state.difficulty) {
        DifficultyLevel.ONE -> listOf("↓")
        DifficultyLevel.TWO -> listOf("↓", "↓")
        DifficultyLevel.THREE -> listOf("↓", "↓", "↑", "↑", "↓", "↑")
        DifficultyLevel.FOUR -> listOf("↓", "x", "↓", "↑", "↑", "↓", "↑")
    }

    Canvas(modifier.padding(14.dp)) {
        val playX = size.width * 0.24f
        val centerY = size.height * 0.54f
        val pxPerBeat = 92.dp.toPx()
        val top = centerY - 58.dp.toPx()
        val h = 116.dp.toPx()

        val firstBeat = floor(state.positionBeats - playX / pxPerBeat).toInt() - 1
        val lastBeat = floor(state.positionBeats + (size.width - playX) / pxPerBeat).toInt() + 2
        for (beat in firstBeat..lastBeat) {
            val x = playX + ((beat - state.positionBeats) * pxPerBeat).toFloat()
            if (x in 0f..size.width) {
                val bar = beat >= 0 && beat % state.song.beatsPerBar == 0
                drawLine(if (bar) TextDim else Grid, Offset(x, top - 34.dp.toPx()), Offset(x, top + h + 34.dp.toPx()), if (bar) 2.dp.toPx() else 1.dp.toPx())
                if (beat >= 0) drawContext.canvas.nativeCanvas.drawText(((beat % 4) + 1).toString(), x, top + h + 26.dp.toPx(), smallPaint)
            }
        }

        state.song.events.forEach { event ->
            val x = playX + ((event.beat - state.positionBeats) * pxPerBeat).toFloat()
            val w = (event.durationBeats * pxPerBeat).toFloat().coerceAtLeast(78.dp.toPx())
            if (x + w >= 0f && x <= size.width) {
                val active = state.positionBeats >= event.beat && state.positionBeats < event.beat + event.durationBeats
                drawRoundRect(
                    color = if (active) Color(0xFF24322C) else Color(0xFF202623),
                    topLeft = Offset(x + 5.dp.toPx(), top),
                    size = Size((w - 10.dp.toPx()).coerceAtLeast(1f), h),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
                if (active) {
                    drawRoundRect(Accent, Offset(x + 5.dp.toPx(), top), Size((w - 10.dp.toPx()).coerceAtLeast(1f), h), CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                }

                val cx = x + w / 2f
                drawContext.canvas.nativeCanvas.drawText(event.chord, cx, top + 44.dp.toPx(), chordPaint)

                val spacing = 22.dp.toPx()
                val startX = cx - ((strum.size - 1) * spacing / 2f)
                strum.forEachIndexed { index, symbol ->
                    val paint = when (symbol) {
                        "↓" -> downPaint
                        "↑" -> upPaint
                        else -> mutePaint
                    }
                    drawContext.canvas.nativeCanvas.drawText(symbol, startX + index * spacing, top + 82.dp.toPx(), paint)
                }
            }
        }

        drawLine(Accent, Offset(playX, 24.dp.toPx()), Offset(playX, size.height - 18.dp.toPx()), 4.dp.toPx())
        drawCircle(Accent, 7.dp.toPx(), Offset(playX, centerY))
        drawContext.canvas.nativeCanvas.drawText("PLAY", playX, 18.dp.toPx(), smallPaint)

        state.detectedNote?.let { note ->
            drawRoundRect(
                Melody.copy(alpha = 0.12f),
                Offset(playX - 36.dp.toPx(), centerY - 94.dp.toPx()),
                Size(72.dp.toPx(), 28.dp.toPx()),
                CornerRadius(12.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(note, playX, centerY - 74.dp.toPx(), melodyPaint)
        }

        drawContext.canvas.nativeCanvas.drawText("↓ down", size.width - 172.dp.toPx(), 22.dp.toPx(), downPaint)
        drawContext.canvas.nativeCanvas.drawText("↑ up", size.width - 102.dp.toPx(), 22.dp.toPx(), upPaint)
        drawContext.canvas.nativeCanvas.drawText("x mute", size.width - 34.dp.toPx(), 22.dp.toPx(), mutePaint)
    }
}

private fun rhythmPaint(size: Float, color: Int): Paint = Paint().apply {
    this.color = color
    textSize = size
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    typeface = android.graphics.Typeface.DEFAULT_BOLD
}
