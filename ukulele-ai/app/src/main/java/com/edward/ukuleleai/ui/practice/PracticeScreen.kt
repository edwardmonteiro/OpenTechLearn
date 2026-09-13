package com.edward.ukuleleai.ui.practice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
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

private val Night = Color(0xFF070908)
private val Glass = Color(0xFF101412)
private val Glass2 = Color(0xFF171C19)
private val Line = Color(0xFF262D29)
private val White = Color(0xFFF6F7F3)
private val Fog = Color(0xFF858F89)
private val Acid = Color(0xFFDDF45A)
private val Mint = Color(0xFF70E0B6)
private val Blue = Color(0xFF64CFF4)
private val Rose = Color(0xFFFF79B5)
private val Gold = Color(0xFFFFB15B)

@Composable
fun PracticeRoute(song: Song, onExit: () -> Unit, viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) viewModel.startListening() }
    LaunchedEffect(song.id) { viewModel.loadSong(song) }

    LaunchedEffect(state.beatPulse) {
        if (state.beatPulse > 0 && state.listeningEnabled) vibrateBeat(context)
    }

    val toggleListen = {
        if (state.listeningEnabled) viewModel.stopListening()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.startListening()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    StudioScreen(
        state, { viewModel.exit(onExit) }, viewModel::togglePlayback, viewModel::restart,
        viewModel::changeTempo, viewModel::setDifficulty, viewModel::toggleSound,
        toggleListen, viewModel::clearMelodyTrail
    )
}

@Composable
private fun StudioScreen(
    state: PracticeState,
    onExit: () -> Unit,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onTempo: (Int) -> Unit,
    onDifficulty: (DifficultyLevel) -> Unit,
    onSound: () -> Unit,
    onListen: () -> Unit,
    onClear: () -> Unit
) {
    val currentChord = currentChord(state)
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(state.beatPulse) {
        if (state.beatPulse > 0) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
        }
    }

    Box(Modifier.fillMaxSize().background(Night)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * (0.25f + pulse.value * 0.05f)
            drawCircle(Acid.copy(alpha = 0.018f + pulse.value * 0.025f), r, Offset(size.width * .32f, size.height * .50f))
            drawCircle(Mint.copy(alpha = 0.012f), size.minDimension * .34f, Offset(size.width * .72f, size.height * .25f))
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleAction("‹", onExit)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(state.song.title, color = White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                        Text("${state.bpm} BPM   ·   Level ${state.difficulty.level}", color = Fog, fontSize = 10.sp, letterSpacing = .5.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.listeningEnabled) {
                        TinyStatus("SAFE LISTEN", "speaker silent", Mint)
                    } else {
                        TinyStatus("CHORD", currentChord ?: "—", Acid)
                    }
                    TextPill(if (state.listeningEnabled) "● Listening" else "Listen", state.listeningEnabled, onListen)
                    TextPill(if (state.listeningEnabled) "Silent" else if (state.soundEnabled) "Click on" else "Click off", state.soundEnabled && !state.listeningEnabled, onSound, enabled = !state.listeningEnabled)
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier.weight(1f).fillMaxWidth().background(Glass.copy(alpha = .90f), RoundedCornerShape(30.dp))
                    .border(1.dp, Line.copy(alpha = .8f), RoundedCornerShape(30.dp))
            ) {
                PerformanceCanvas(state, pulse.value, Modifier.fillMaxSize())

                if (state.listeningEnabled) {
                    Row(
                        Modifier.align(Alignment.TopStart).padding(18.dp).background(Color(0xB8171C19), RoundedCornerShape(18.dp)).padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(7.dp).height(7.dp).background(Mint, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(state.detectedNote ?: "Play a single note", color = if (state.detectedNote != null) White else Fog, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        state.detectedCents?.let { cents ->
                            Spacer(Modifier.width(8.dp))
                            Text(if (abs(cents) <= 6) "in tune" else "${if (cents > 0) "+" else ""}$cents¢", color = if (abs(cents) <= 6) Mint else Fog, fontSize = 10.sp)
                        }
                    }
                }

                state.countdown?.let { count ->
                    Box(Modifier.fillMaxSize().background(Color(0xB8070908)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(count.toString(), color = White, fontSize = 94.sp, fontWeight = FontWeight.ExtraLight)
                            Text(if (state.listeningEnabled) "FEEL THE PULSE" else "COUNT IN", color = Fog, fontSize = 9.sp, letterSpacing = 2.4.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            ControlDock(state, onPlayPause, onRestart, onTempo, onDifficulty, onClear)
        }
    }
}

@Composable
private fun ControlDock(
    state: PracticeState,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onTempo: (Int) -> Unit,
    onDifficulty: (DifficultyLevel) -> Unit,
    onClear: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Glass.copy(alpha = .88f), RoundedCornerShape(24.dp)).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Ghost("−5", { onTempo(-5) })
            Ghost("↺", onRestart)
            Ghost("Clear", onClear)
        }

        Button(
            onClick = onPlayPause,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Night),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp, vertical = 11.dp)
        ) { Text(if (state.isPlaying || state.countdown != null) "PAUSE" else "PLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp) }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            DifficultyLevel.entries.forEach { level ->
                val active = level == state.difficulty
                Box(
                    Modifier.background(if (active) Acid else Glass2, RoundedCornerShape(13.dp)).clickable { onDifficulty(level) }.padding(horizontal = 13.dp, vertical = 8.dp)
                ) { Text(level.level.toString(), color = if (active) Night else Fog, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            Ghost("+5", { onTempo(5) })
        }
    }
}

@Composable
private fun PerformanceCanvas(state: PracticeState, pulse: Float, modifier: Modifier) {
    val density = LocalDensity.current
    val chordPaint = remember(density) { nativePaint(25.sp.value * density.density, android.graphics.Color.WHITE, true) }
    val tiny = remember(density) { nativePaint(10.sp.value * density.density, android.graphics.Color.rgb(133,143,137), false) }
    val note = remember(density) { nativePaint(12.sp.value * density.density, android.graphics.Color.rgb(112,224,182), true) }
    val down = remember(density) { nativePaint(17.sp.value * density.density, android.graphics.Color.rgb(100,207,244), true) }
    val up = remember(density) { nativePaint(17.sp.value * density.density, android.graphics.Color.rgb(255,121,181), true) }
    val mute = remember(density) { nativePaint(15.sp.value * density.density, android.graphics.Color.rgb(255,177,91), true) }

    Canvas(modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        val playX = size.width * .30f
        val pxPerBeat = 96.dp.toPx()
        val melodyTop = 38.dp.toPx()
        val melodyBottom = size.height * .58f
        val chordTop = size.height * .69f
        val chordHeight = (size.height - chordTop - 24.dp.toPx()).coerceAtMost(88.dp.toPx())

        // Minimal music paper: only octave anchors and bar lines.
        for (midi in listOf(60, 64, 67, 72, 76, 79, 84)) {
            val y = midiToY(midi, melodyTop, melodyBottom)
            drawLine(Line.copy(alpha = if (midi % 12 == 0) .65f else .28f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }

        val firstBeat = floor(state.positionBeats - playX / pxPerBeat).toInt() - 1
        val lastBeat = floor(state.positionBeats + (size.width - playX) / pxPerBeat).toInt() + 2
        for (beat in firstBeat..lastBeat) {
            val x = playX + ((beat - state.positionBeats) * pxPerBeat).toFloat()
            if (x in 0f..size.width && beat >= 0) {
                val bar = beat % state.song.beatsPerBar == 0
                if (bar) {
                    drawLine(Line.copy(alpha = .85f), Offset(x, 18.dp.toPx()), Offset(x, size.height - 10.dp.toPx()), 1.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText("${beat / state.song.beatsPerBar + 1}", x + 7.dp.toPx(), 13.dp.toPx(), tiny)
                }
            }
        }

        val visible = state.melodyTrail.filter { p ->
            val x = playX + ((p.beat - state.positionBeats) * pxPerBeat).toFloat()
            x in -30.dp.toPx()..size.width + 30.dp.toPx()
        }
        if (visible.isNotEmpty()) {
            val path = Path()
            visible.forEachIndexed { i, p ->
                val x = playX + ((p.beat - state.positionBeats) * pxPerBeat).toFloat()
                val y = midiToY(p.midi, melodyTop, melodyBottom)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Mint.copy(alpha = .10f), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Mint.copy(alpha = .95f), style = Stroke(2.7.dp.toPx(), cap = StrokeCap.Round))
        }

        state.detectedMidi?.let { midi ->
            val y = midiToY(midi, melodyTop, melodyBottom)
            drawCircle(Mint.copy(alpha = .10f + pulse * .10f), (17 + pulse * 5).dp.toPx(), Offset(playX, y))
            drawCircle(Mint, 5.dp.toPx(), Offset(playX, y))
            state.detectedNote?.let { drawContext.canvas.nativeCanvas.drawText(it, playX + 27.dp.toPx(), y + 4.dp.toPx(), note) }
        }

        state.song.events.forEach { event ->
            val x = playX + ((event.beat - state.positionBeats) * pxPerBeat).toFloat()
            val w = (event.durationBeats * pxPerBeat).toFloat().coerceAtLeast(72.dp.toPx())
            if (x + w >= 0f && x <= size.width) {
                val active = state.positionBeats >= event.beat && state.positionBeats < event.beat + event.durationBeats
                drawRoundRect(
                    if (active) Color(0xFF202822) else Color(0xFF151A17),
                    Offset(x + 4.dp.toPx(), chordTop), Size((w - 8.dp.toPx()).coerceAtLeast(1f), chordHeight), CornerRadius(18.dp.toPx())
                )
                if (active) drawRoundRect(Acid.copy(alpha = .65f), Offset(x + 4.dp.toPx(), chordTop), Size((w - 8.dp.toPx()).coerceAtLeast(1f), chordHeight), CornerRadius(18.dp.toPx()), style = Stroke(1.7.dp.toPx()))
                val cx = x + w / 2
                drawContext.canvas.nativeCanvas.drawText(event.chord, cx, chordTop + chordHeight * .42f, chordPaint)
                val rhythm = strumPattern(state.difficulty)
                val gap = 19.dp.toPx(); val start = cx - (rhythm.size - 1) * gap / 2
                rhythm.forEachIndexed { i, s ->
                    drawContext.canvas.nativeCanvas.drawText(s, start + i