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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.DifficultyLevel
import com.edward.ukuleleai.domain.PracticeState
import com.edward.ukuleleai.domain.Song
import kotlin.math.abs
import kotlin.math.floor

private val Ink = Color(0xFFF7F8F5)
private val Muted = Color(0xFF89928D)
private val Bg = Color(0xFF080A09)
private val Surface = Color(0xFF101412)
private val Surface2 = Color(0xFF171C19)
private val Hairline = Color(0xFF252B28)
private val Lime = Color(0xFFDDF85B)
private val Mint = Color(0xFF72E0B7)
private val Cyan = Color(0xFF64CFF4)
private val Pink = Color(0xFFFF79B5)
private val Amber = Color(0xFFFFB15B)

@Composable
fun PracticeRoute(song: Song, onExit: () -> Unit, viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening()
    }

    LaunchedEffect(song.id) { viewModel.loadSong(song) }

    val toggleListening = {
        if (state.listeningEnabled) viewModel.stopListening()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startListening()
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    PracticeScreen(
        state = state,
        onExit = { viewModel.exit(onExit) },
        onPlayPause = viewModel::togglePlayback,
        onRestart = viewModel::restart,
        onTempoChange = viewModel::changeTempo,
        onDifficulty = viewModel::setDifficulty,
        onToggleSound = viewModel::toggleSound,
        onToggleListening = toggleListening,
        onClearMelody = viewModel::clearMelodyTrail
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
    onToggleListening: () -> Unit,
    onClearMelody: () -> Unit
) {
    val currentChord = currentChord(state)
    val progress = songProgress(state)

    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onExit,
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Ink)
                ) { Text("‹", fontSize = 24.sp) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(state.song.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("${state.bpm} BPM  ·  ${progress}% complete", color = Muted, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Capsule(if (state.detectedNote != null) state.detectedNote!! else "—", "LIVE NOTE", if (state.listeningEnabled) Mint else Muted)
                Capsule(currentChord ?: "—", "CHORD", Lime)
                SoftButton(if (state.listeningEnabled) "Listening" else "Listen", state.listeningEnabled, onToggleListening)
                SoftButton(if (state.soundEnabled) "Sound" else "Muted", state.soundEnabled, onToggleSound)
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(Surface, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
        ) {
            LiveCanvas(state, Modifier.fillMaxSize())

            if (!state.listeningEnabled) {
                Column(
                    Modifier.align(Alignment.TopStart).padding(22.dp)
                        .background(Color(0xCC171C19), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Hear yourself play", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Turn on Listen to draw your melody live.", color = Muted, fontSize = 11.sp)
                }
            }

            state.countdown?.let { count ->
                Box(Modifier.fillMaxSize().background(Color(0x99080A09)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), color = Lime, fontSize = 92.sp, fontWeight = FontWeight.Light)
                        Text("READY", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(22.dp)).border(1.dp, Hairline, RoundedCornerShape(22.dp)).padding(10.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                DockButton("−5", onClick = { onTempoChange(-5) })
                DockButton("↺", onClick = onRestart)
                DockButton("Clear melody", onClick = onClearMelody)
            }

            Button(
                onClick = onPlayPause,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 34.dp, vertical = 12.dp)
            ) {
                Text(if (state.isPlaying || state.countdown != null) "PAUSE" else "PLAY", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                DifficultyLevel.entries.forEach { level ->
                    val selected = level == state.difficulty
                    Button(
                        onClick = { onDifficulty(level) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Ink else Surface2,
                            contentColor = if (selected) Bg else Muted
                        )
                    ) { Text(level.level.toString(), fontWeight = FontWeight.Bold) }
                }
                DockButton("+5", onClick = { onTempoChange(5) })
            }
        }
    }
}

@Composable
private fun Capsule(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SoftButton(text: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFF1C2923) else Surface2,
            contentColor = if (active) Mint else Muted
        )
    ) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun DockButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Ink),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, fontSize = 11.sp) }
}

@Composable
private fun LiveCanvas(state: PracticeState, modifier: Modifier) {
    val density = LocalDensity.current
    val labelPaint = remember(density) { paint(11.sp.value * density.density, android.graphics.Color.rgb(137,146,141), false) }
    val chordPaint = remember(density) { paint(24.sp.value * density.density, android.graphics.Color.WHITE, true) }
    val notePaint = remember(density) { paint(13.sp.value * density.density, android.graphics.Color.rgb(114,224,183), true) }
    val downPaint = remember(density) { paint(17.sp.value * density.density, android.graphics.Color.rgb(100,207,244), true) }
    val upPaint = remember(density) { paint(17.sp.value * density.density, android.graphics.Color.rgb(255,121,181), true) }
    val mutePaint = remember(density) { paint(15.sp.value * density.density, android.graphics.Color.rgb(255,177,91), true) }

    Canvas(modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        val playX = size.width * 0.31f
        val pxPerBeat = 94.dp.toPx()
        val melodyTop = 46.dp.toPx()
        val melodyBottom = size.height * 0.56f
        val chordTop = size.height * 0.67f
        val chordHeight = 92.dp.toPx()

        // Quiet pitch guide. C4..C6 mapped vertically, enough for typical melody practice.
        for (midi in 60..84 step 2) {
            val y = midiToY(midi, melodyTop, melodyBottom)
            drawLine(Hairline.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }

        val firstBeat = floor(state.positionBeats - playX / pxPerBeat).toInt() - 1
        val lastBeat = floor(state.positionBeats + (size.width - playX) / pxPerBeat).toInt() + 2
        for (beat in firstBeat..lastBeat) {
            val x = playX + ((beat - state.positionBeats) * pxPerBeat).toFloat()
            if (x in 0f..size.width) {
                val bar = beat >= 0 && beat % state.song.beatsPerBar == 0
                drawLine(
                    if (bar) Color(0xFF38403C) else Hairline.copy(alpha = 0.7f),
                    Offset(x, 24.dp.toPx()),
                    Offset(x, size.height - 24.dp.toPx()),
                    if (bar) 1.5.dp.toPx() else 1.dp.toPx()
                )
                if (beat >= 0 && bar) {
                    drawContext.canvas.nativeCanvas.drawText("${beat / state.song.beatsPerBar + 1}", x + 7.dp.toPx(), 18.dp.toPx(), labelPaint)
                }
            }
        }

        // Melody trail from microphone.
        val visibleTrail = state.melodyTrail.filter { point ->
            val x = playX + ((point.beat - state.positionBeats) * pxPerBeat).toFloat()
            x in -20.dp.toPx()..(size.width + 20.dp.toPx())
        }
        if (visibleTrail.isNotEmpty()) {
            val path = Path()
            visibleTrail.forEachIndexed { index, point ->
                val x = playX + ((point.beat - state.positionBeats) * pxPerBeat).toFloat()
                val y = midiToY(point.midi, melodyTop, melodyBottom)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Mint.copy(alpha = 0.22f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Mint, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

            visibleTrail.forEachIndexed { index, point ->
                if (index % 4 == 0 || index == visibleTrail.lastIndex) {
                    val x = playX + ((point.beat - state.positionBeats) * pxPerBeat).toFloat()
                    val y = midiToY(point.midi, melodyTop, melodyBottom)
                    drawCircle(Mint, 4.2.dp.toPx(), Offset(x, y))
                }
            }
        }

        // Current detected pitch floats at the execution axis.
        state.detectedMidi?.let { midi ->
            val y = midiToY(midi, melodyTop, melodyBottom)
            drawCircle(Mint.copy(alpha = 0.18f), 16.dp.toPx(), Offset(playX, y))
            drawCircle(Mint, 5.5.dp.toPx(), Offset(playX, y))
            state.detectedNote?.let { note ->
                drawContext.canvas.nativeCanvas.drawText(note, playX + 30.dp.toPx(), y + 5.dp.toPx(), notePaint)
            }
        }

        // Chord lane, quieter than the melody.
        state.song.events.forEach { event ->
            val x = playX + ((event.beat - state.positionBeats) * pxPerBeat).toFloat()
            val w = (event.durationBeats * pxPerBeat).toFloat().coerceAtLeast(74.dp.toPx())
            if (x + w >= 0f && x <= size.width) {
                val active = state.positionBeats >= event.beat && state.positionBeats < event.beat + event.durationBeats
                drawRoundRect(
                    color = if (active) Color(0xFF222B26) else Color(0xFF171C19),
                    topLeft = Offset(x + 4.dp.toPx(), chordTop),
                    size = Size((w - 8.dp.toPx()).coerceAtLeast(1f), chordHeight),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
                if (active) drawRoundRect(
                    Lime.copy(alpha = 0.8f),
                    Offset(x + 4.dp.toPx(), chordTop),
                    Size((w - 8.dp.toPx()).coerceAtLeast(1f), chordHeight),
                    CornerRadius(18.dp.toPx()),
                    style = Stroke(2.dp.toPx())
                )
                val cx = x + w / 2
                drawContext.canvas.nativeCanvas.drawText(event.chord, cx, chordTop + 37.dp.toPx(), chordPaint)
                val rhythm = strumPattern(state.difficulty)
                val spacing = 20.dp.toPx()
                val start = cx - (rhythm.size - 1) * spacing / 2
                rhythm.forEachIndexed { i, s ->
                    val p = when (s) { "↓" -> downPaint; "↑" -> upPaint; else -> mutePaint }
                    drawContext.canvas.nativeCanvas.drawText(s, start + i * spacing, chordTop + 68.dp.toPx(), p)
                }
            }
        }

        // Execution axis: one luminous line, nothing else competes with it.
        drawLine(Lime.copy(alpha = 0.18f), Offset(playX, 12.dp.toPx()), Offset(playX, size.height - 12.dp.toPx()), 12.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Lime, Offset(playX, 12.dp.toPx()), Offset(playX, size.height - 12.dp.toPx()), 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Lime, 5.dp.toPx(), Offset(playX, chordTop + chordHeight / 2))
        drawContext.canvas.nativeCanvas.drawText("NOW", playX, size.height - 5.dp.toPx(), labelPaint)
    }
}

private fun midiToY(midi: Int, top: Float, bottom: Float): Float {
    val clamped = midi.coerceIn(60, 84)
    val ratio = (clamped - 60) / 24f
    return bottom - ratio * (bottom - top)
}

private fun currentChord(state: PracticeState): String? = state.song.events
    .lastOrNull { state.positionBeats >= it.beat && state.positionBeats < it.beat + it.durationBeats }?.chord
    ?: state.song.events.firstOrNull()?.chord

private fun songProgress(state: PracticeState): Int {
    val end = state.song.events.maxOfOrNull { it.beat + it.durationBeats } ?: return 0
    return ((state.positionBeats / end) * 100.0).toInt().coerceIn(0, 100)
}

private fun strumPattern(level: DifficultyLevel): List<String> = when (level) {
    DifficultyLevel.ONE -> listOf("↓")
    DifficultyLevel.TWO -> listOf("↓", "↓")
    DifficultyLevel.THREE -> listOf("↓", "↓", "↑", "↑", "↓", "↑")
    DifficultyLevel.FOUR -> listOf("↓", "x", "↓", "↑", "↑", "↓", "↑")
}

private fun paint(size: Float, color: Int, bold: Boolean): Paint = Paint().apply {
    this.color = color
    textSize = size
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
}
