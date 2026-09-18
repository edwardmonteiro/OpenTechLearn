package com.edward.ukuleleai.ui.practice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.*
import kotlin.math.floor

private val Night=Color(0xFF070908)
private val Glass=Color(0xFF101412)
private val Glass2=Color(0xFF171C19)
private val Line=Color(0xFF262D29)
private val White=Color(0xFFF6F7F3)
private val Fog=Color(0xFF858F89)
private val Acid=Color(0xFFDDF45A)
private val Mint=Color(0xFF70E0B6)

@Composable
fun PracticeRoute(
    song: Song,
    onExit: () -> Unit,
    onEditAnalysis: () -> Unit = {},
    viewModel: PracticeViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.startListening()
    }

    LaunchedEffect(song.id) { viewModel.loadSong(song) }
    LaunchedEffect(s.beatPulse) {
        if (s.beatPulse > 0 && s.barHapticsEnabled) {
            val beat = floor(s.positionBeats).toInt()
            vibrateBeat(ctx, beat % s.song.beatsPerBar == 0)
        }
    }

    val listen = {
        if (s.listeningEnabled) viewModel.stopListening()
        else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.startListening()
        else ask.launch(Manifest.permission.RECORD_AUDIO)
    }

    PracticeHud(
        s=s,
        exit={ viewModel.exit(onExit) },
        play=viewModel::togglePlayback,
        restart=viewModel::restart,
        setRhythm=viewModel::setRhythmPattern,
        toggleBeginner=viewModel::toggleBeginner,
        toggleBacking=viewModel::toggleBacking,
        toggleHaptics=viewModel::toggleBarHaptics,
        listen=listen,
        editAnalysis=onEditAnalysis
    )
}

@Composable
private fun PracticeHud(
    s: PracticeState,
    exit:()->Unit,
    play:()->Unit,
    restart:()->Unit,
    setRhythm:(RhythmPattern)->Unit,
    toggleBeginner:()->Unit,
    toggleBacking:()->Unit,
    toggleHaptics:()->Unit,
    listen:()->Unit,
    editAnalysis:()->Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    val currentIndex = s.song.events.indexOfLast { s.positionBeats >= it.beat }.coerceAtLeast(0)
    val current = s.song.events.getOrNull(currentIndex)
    val next = s.song.events.drop(currentIndex + 1).firstOrNull { it.chord != current?.chord }
    val beatsUntilNext = next?.let { (it.beat - s.positionBeats).coerceAtLeast(0.0) }
    val secondsUntilNext = beatsUntilNext?.times(60.0 / s.bpm)
    val beatInBar = floor(s.positionBeats).toInt().mod(s.song.beatsPerBar) + 1

    Column(
        Modifier.fillMaxSize()
            .background(Night)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal=14.dp, vertical=8.dp)
    ) {
        CompactHeader(
            title=s.song.title,
            bpm=s.bpm,
            key=s.analysisKey,
            analyzed=s.analysisAvailable,
            audioOn=s.backingEnabled,
            exit=exit,
            menuOpen=menuOpen,
            setMenuOpen={ menuOpen=it },
            beginner=s.beginnerMode,
            toggleBeginner=toggleBeginner,
            backingAvailable=s.backingAvailable,
            toggleBacking=toggleBacking,
            haptics=s.barHapticsEnabled,
            toggleHaptics=toggleHaptics,
            listening=s.listeningEnabled,
            listen=listen,
            editAnalysis=editAnalysis
        )

        Spacer(Modifier.height(8.dp))

        Column(
            Modifier.weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            CurrentNextPanel(
                current=current?.chord ?: "—",
                next=next?.chord ?: "—",
                beatsUntilNext=beatsUntilNext,
                secondsUntilNext=secondsUntilNext
            )

            RhythmCoach(
                s=s,
                beatInBar=beatInBar,
                setRhythm=setRhythm
            )

            UpcomingChords(s)

            if (s.analysisAvailable) {
                Text(
                    "OFFLINE ANALYZED · ${s.analysisSegments} segments",
                    modifier=Modifier.testTag("analysis-status"),
                    color=Mint,
                    fontSize=8.sp,
                    fontWeight=FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        BottomControls(
            isPlaying=s.isPlaying || s.countdown!=null,
            play=play,
            restart=restart
        )
    }

    s.countdown?.let { count ->
        Box(
            Modifier.fillMaxSize().background(Color(0xE0070908)),
            contentAlignment=Alignment.Center
        ) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text(count.toString(), color=White, fontSize=92.sp, fontWeight=FontWeight.ExtraLight)
                Text("COUNT IN · PLAY ON 1", color=Fog, fontSize=10.sp, letterSpacing=1.5.sp)
            }
        }
    }
}

@Composable
private fun CompactHeader(
    title:String,
    bpm:Int,
    key:String?,
    analyzed:Boolean,
    audioOn:Boolean,
    exit:()->Unit,
    menuOpen:Boolean,
    setMenuOpen:(Boolean)->Unit,
    beginner:Boolean,
    toggleBeginner:()->Unit,
    backingAvailable:Boolean,
    toggleBacking:()->Unit,
    haptics:Boolean,
    toggleHaptics:()->Unit,
    listening:Boolean,
    listen:()->Unit,
    editAnalysis:()->Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).background(Glass2,CircleShape).clickable(onClick=exit),
            contentAlignment=Alignment.Center
        ) { Text("‹", color=White, fontSize=24.sp) }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(title,color=White,fontSize=16.sp,fontWeight=FontWeight.Medium,maxLines=1)
            Text(
                "$bpm BPM${if(analyzed) " · ${key ?: "?"}" else ""}${if(audioOn) " · AUDIO ON" else ""}",
                color=Fog,
                fontSize=9.sp,
                maxLines=1
            )
        }

        Box {
            Box(
                Modifier.size(38.dp).background(Glass2,CircleShape).clickable{setMenuOpen(true)},
                contentAlignment=Alignment.Center
            ) { Text("⋯",color=White,fontSize=23.sp) }

            DropdownMenu(expanded=menuOpen,onDismissRequest={setMenuOpen(false)}) {
                DropdownMenuItem(
                    text={Text(if(beginner)"Use full chords" else "Use beginner triads")},
                    onClick={toggleBeginner();setMenuOpen(false)}
                )
                if(backingAvailable) DropdownMenuItem(
                    text={Text(if(audioOn)"Mute original audio" else "Play original audio")},
                    onClick={toggleBacking();setMenuOpen(false)}
                )
                DropdownMenuItem(
                    text={Text(if(haptics)"Turn beat haptics off" else "Turn beat haptics on")},
                    onClick={toggleHaptics();setMenuOpen(false)}
                )
                DropdownMenuItem(
                    text={Text(if(listening)"Stop microphone listen" else "Microphone listen")},
                    onClick={listen();setMenuOpen(false)}
                )
                if(analyzed) DropdownMenuItem(
                    text={Text("Edit chord analysis")},
                    onClick={editAnalysis();setMenuOpen(false)}
                )
            }
        }
    }
}

@Composable
private fun CurrentNextPanel(
    current:String,
    next:String,
    beatsUntilNext:Double?,
    secondsUntilNext:Double?
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Glass,RoundedCornerShape(24.dp))
            .border(1.dp,Line,RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FixedChordCard(
                modifier=Modifier.weight(1f).testTag("current-chord"),
                label="CURRENT",
                chord=current,
                accent=true
            )
            FixedChordCard(
                modifier=Modifier.weight(1f).testTag("next-chord"),
                label=if((beatsUntilNext ?: 99.0)<=4.0)"GET READY" else "NEXT",
                chord=next,
                accent=false
            )
        }

        Spacer(Modifier.height(8.dp))

        if(beatsUntilNext != null) {
            val progress=(1.0-(beatsUntilNext/8.0)).coerceIn(0.0,1.0).toFloat()
            LinearProgressIndicator(
                progress={progress},
                modifier=Modifier.fillMaxWidth().height(6.dp),
                color=if(beatsUntilNext<=4.0) Acid else Mint,
                trackColor=Glass2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    beatsUntilNext <= .35 -> "CHANGE NOW → $next"
                    beatsUntilNext <= 1.0 -> "NEXT BEAT → $next"
                    beatsUntilNext <= 4.0 -> "Prepare $next · ${String.format("%.1f",beatsUntilNext)} beats"
                    else -> "$next in ${String.format("%.1f",beatsUntilNext)} beats · ${String.format("%.1f",secondsUntilNext ?: 0.0)}s"
                },
                color=if(beatsUntilNext<=4.0)White else Fog,
                fontSize=12.sp,
                fontWeight=if(beatsUntilNext<=1.0)FontWeight.Bold else FontWeight.Medium,
                maxLines=1
            )
        }
    }
}

@Composable
private fun FixedChordCard(
    modifier:Modifier,
    label:String,
    chord:String,
    accent:Boolean
) {
    val fontSize=when {
        chord.length<=2 -> 48.sp
        chord.length<=4 -> 40.sp
        chord.length<=6 -> 32.sp
        else -> 26.sp
    }

    Column(
        modifier.height(106.dp)
            .background(if(accent)Color(0xFF1B221B)else Glass2,RoundedCornerShape(18.dp))
            .border(1.dp,if(accent)Acid.copy(alpha=.5f)else Line,RoundedCornerShape(18.dp))
            .padding(horizontal=6.dp,vertical=8.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center
    ) {
        Text(label,color=if(accent)Acid else Fog,fontSize=8.sp,letterSpacing=1.3.sp,fontWeight=FontWeight.Bold,maxLines=1)
        Spacer(Modifier.height(2.dp))
        Text(
            chord,
            color=if(accent)Acid else White,
            fontSize=fontSize,
            fontWeight=FontWeight.Medium,
            maxLines=1,
            softWrap=false,
            textAlign=TextAlign.Center,
            modifier=Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RhythmCoach(
    s:PracticeState,
    beatInBar:Int,
    setRhythm:(RhythmPattern)->Unit
) {
    val eighth=(s.positionBeats*2.0).toInt().mod(8)
    val pattern=when(s.rhythmPattern) {
        RhythmPattern.BASIC -> listOf("↓","·","↓","·","↓","·","↓","·")
        RhythmPattern.GROOVE -> listOf("↓","·","↓","↑","·","↑","↓","↑")
    }

    Column(
        Modifier.fillMaxWidth()
            .testTag("rhythm-coach")
            .background(Glass,RoundedCornerShape(22.dp))
            .border(1.dp,Line,RoundedCornerShape(22.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RHYTHM COACH",color=Acid,fontSize=9.sp,letterSpacing=1.4.sp,fontWeight=FontWeight.Bold)
                Text(
                    if(s.rhythmPattern==RhythmPattern.BASIC)"Strum DOWN on 1 · 2 · 3 · 4"
                    else "Suggested groove · ↓  ↓↑  ↑↓↑",
                    color=White,
                    fontSize=11.sp,
                    fontWeight=FontWeight.Medium,
                    maxLines=1
                )
            }

            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                RhythmPattern.entries.forEach { p ->
                    val selected=s.rhythmPattern==p
                    Box(
                        Modifier.background(if(selected)Acid else Glass2,RoundedCornerShape(9.dp))
                            .clickable{setRhythm(p)}
                            .padding(horizontal=7.dp,vertical=5.dp)
                    ) {
                        Text(p.name,color=if(selected)Night else Fog,fontSize=7.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            pattern.forEachIndexed { index, stroke ->
                val active=s.isPlaying && index==eighth
                val beatLabel=if(index%2==0)(index/2+1).toString() else "&"

                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(beatLabel,color=if(active)Acid else Fog,fontSize=8.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier.fillMaxWidth().height(42.dp)
                            .background(if(active)Acid else Glass2,RoundedCornerShape(10.dp)),
                        contentAlignment=Alignment.Center
                    ) {
                        Text(
                            stroke,
                            color=if(active)Night else if(stroke=="·")Fog else White,
                            fontSize=21.sp,
                            fontWeight=FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Beat $beatInBar/${s.song.beatsPerBar} · yellow = move your hand now",
            color=Fog,
            fontSize=9.sp,
            maxLines=1
        )
    }
}

@Composable
private fun UpcomingChords(s:PracticeState) {
    val index=s.song.events.indexOfLast{s.positionBeats>=it.beat}.coerceAtLeast(0)
    val events=s.song.events.drop(index).take(4)

    Column(
        Modifier.fillMaxWidth()
            .testTag("detected-chord-strip")
            .background(Glass,RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Text("UPCOMING",color=Fog,fontSize=8.sp,letterSpacing=1.2.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            events.forEachIndexed { i,e ->
                Column(
                    Modifier.weight(1f)
                        .background(if(i==0)Color(0xFF202822)else Glass2,RoundedCornerShape(11.dp))
                        .padding(vertical=8.dp,horizontal=3.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ) {
                    Text(
                        e.chord,
                        color=if(i==0)Acid else White,
                        fontSize=if(e.chord.length<=4)15.sp else 12.sp,
                        fontWeight=FontWeight.Bold,
                        maxLines=1,
                        softWrap=false
                    )
                    Text(
                        if(i==0)"NOW" else "${String.format("%.1f",(e.beat-s.positionBeats).coerceAtLeast(0.0))} beats",
                        color=Fog,
                        fontSize=7.sp,
                        maxLines=1
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControls(
    isPlaying:Boolean,
    play:()->Unit,
    restart:()->Unit
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement=Arrangement.Center,
        verticalAlignment=Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick=restart,
            modifier=Modifier.size(48.dp),
            shape=CircleShape,
            contentPadding=PaddingValues(0.dp),
            border=ButtonDefaults.outlinedButtonBorder,
            colors=ButtonDefaults.outlinedButtonColors(contentColor=White)
        ) { Text("↺",fontSize=20.sp) }

        Spacer(Modifier.width(18.dp))

        Button(
            onClick=play,
            modifier=Modifier.size(60.dp).testTag("play-button"),
            shape=CircleShape,
            contentPadding=PaddingValues(0.dp),
            colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night)
        ) {
            Text(if(isPlaying)"Ⅱ" else "▶",fontSize=20.sp,fontWeight=FontWeight.Bold)
        }
    }
}

private fun vibrateBeat(c:Context,accent:Boolean) {
    try {
        val v=if(Build.VERSION.SDK_INT>=31)
            (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION")
            (c.getSystemService(Context.VIBRATOR_SERVICE)as Vibrator)

        val duration=if(accent)32L else 14L
        val amplitude=if(accent)180 else 80

        if(Build.VERSION.SDK_INT>=26) v.vibrate(VibrationEffect.createOneShot(duration,amplitude))
        else @Suppress("DEPRECATION") v.vibrate(duration)
    } catch(_:Throwable) {}
}
