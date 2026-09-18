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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.ceil
import kotlin.math.floor

private val Night=Color(0xFF070908); private val Glass=Color(0xFF101412); private val Glass2=Color(0xFF171C19)
private val Line=Color(0xFF262D29); private val White=Color(0xFFF6F7F3); private val Fog=Color(0xFF858F89)
private val Acid=Color(0xFFDDF45A); private val Mint=Color(0xFF70E0B6)

@Composable
fun PracticeRoute(
    song: Song,
    onExit: () -> Unit,
    onEditAnalysis: () -> Unit = {},
    viewModel: PracticeViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) viewModel.startListening() }
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
    PlayAlongScreen(
        s=s,
        exit={viewModel.exit(onExit)},
        play=viewModel::togglePlayback,
        restart=viewModel::restart,
        setMode=viewModel::setPlayAlongMode,
        setRhythm=viewModel::setRhythmPattern,
        toggleBeginner=viewModel::toggleBeginner,
        toggleBacking=viewModel::toggleBacking,
        toggleHaptics=viewModel::toggleBarHaptics,
        listen=listen,
        editAnalysis=onEditAnalysis,
        tempo=viewModel::changeTempo
    )
}

@Composable
private fun PlayAlongScreen(
    s: PracticeState,
    exit:()->Unit,
    play:()->Unit,
    restart:()->Unit,
    setMode:(PlayAlongMode)->Unit,
    setRhythm:(RhythmPattern)->Unit,
    toggleBeginner:()->Unit,
    toggleBacking:()->Unit,
    toggleHaptics:()->Unit,
    listen:()->Unit,
    editAnalysis:()->Unit,
    tempo:(Int)->Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val currentIndex = s.song.events.indexOfLast { s.positionBeats >= it.beat }.coerceAtLeast(0)
    val current = s.song.events.getOrNull(currentIndex)
    val next = s.song.events.drop(currentIndex + 1).firstOrNull { it.chord != current?.chord }
    val beatsUntilNext = next?.let { (it.beat - s.positionBeats).coerceAtLeast(0.0) }
    val secondsUntilNext = beatsUntilNext?.times(60.0 / s.bpm)
    val beatInBar = (floor(s.positionBeats).toInt().mod(s.song.beatsPerBar)) + 1

    Column(
        Modifier.fillMaxSize()
            .background(Night)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal=14.dp, vertical=8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Glass2,CircleShape).clickable(onClick=exit),contentAlignment=Alignment.Center){Text("‹",color=White,fontSize=26.sp)}
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(s.song.title,color=White,fontSize=19.sp,fontWeight=FontWeight.Medium,maxLines=1)
                    Text("${s.bpm} BPM${if(s.analysisAvailable) "  ·  ${s.analysisKey ?: "?"} ${s.analysisScale ?: ""}" else ""}",color=Fog,fontSize=10.sp)
                }
            }
            Box {
                Text("⋯",modifier=Modifier.size(42.dp).background(Glass2,CircleShape).clickable{menuOpen=true}.wrapContentSize(Alignment.Center),color=White,fontSize=25.sp,textAlign=TextAlign.Center)
                DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}) {
                    DropdownMenuItem(text={Text(if(s.beginnerMode)"Use full chords" else "Use beginner triads")},onClick={toggleBeginner();menuOpen=false})
                    if(s.backingAvailable) DropdownMenuItem(text={Text(if(s.backingEnabled)"Mute original audio" else "Play original audio")},onClick={toggleBacking();menuOpen=false})
                    DropdownMenuItem(text={Text(if(s.barHapticsEnabled)"Turn beat haptics off" else "Turn beat haptics on")},onClick={toggleHaptics();menuOpen=false})
                    DropdownMenuItem(text={Text(if(s.listeningEnabled)"Stop microphone listen" else "Microphone listen")},onClick={listen();menuOpen=false})
                    DropdownMenuItem(text={Text("Restart song")},onClick={restart();menuOpen=false})
                    if(!s.backingEnabled){DropdownMenuItem(text={Text("Tempo −5")},onClick={tempo(-5);menuOpen=false});DropdownMenuItem(text={Text("Tempo +5")},onClick={tempo(5);menuOpen=false})}
                    if(s.analysisAvailable) DropdownMenuItem(text={Text("Edit chord analysis")},onClick={editAnalysis();menuOpen=false})
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().background(Glass,RoundedCornerShape(18.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            PlayAlongMode.entries.forEach { mode ->
                val selected=s.playAlongMode==mode
                Box(Modifier.weight(1f).background(if(selected) Acid else Color.Transparent,RoundedCornerShape(14.dp)).clickable{setMode(mode)}.padding(vertical=9.dp),contentAlignment=Alignment.Center){Text(mode.name,color=if(selected)Night else Fog,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
            }
        }

        Spacer(Modifier.height(12.dp))
        if(s.analysisAvailable) {
            Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically) {
                Text("OFFLINE ANALYZED · ${s.analysisSegments} segments",modifier=Modifier.testTag("analysis-status"),color=Mint,fontSize=9.sp,fontWeight=FontWeight.Bold)
                if(s.backingEnabled) Text("● Original audio ON",color=Mint,fontSize=9.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(
            Modifier.fillMaxWidth().weight(1f)
                .background(Glass,RoundedCornerShape(30.dp))
                .border(1.dp,Line,RoundedCornerShape(30.dp))
                .padding(horizontal=20.dp,vertical=18.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                ChordCard(
                    modifier=Modifier.weight(1.15f).testTag("current-chord"),
                    label="PLAY NOW",
                    chord=current?.chord ?: "—",
                    accent=true
                )
                ChordCard(
                    modifier=Modifier.weight(.85f).testTag("next-chord"),
                    label=if(secondsUntilNext != null && secondsUntilNext <= 4.0) "GET READY" else "NEXT",
                    chord=next?.chord ?: "—",
                    accent=false,
                    footer=secondsUntilNext?.let { "${String.format("%.1f",it)}s" }
                )
            }

            Spacer(Modifier.height(10.dp))
            if(next != null && beatsUntilNext != null) {
                val progress=(1.0-(beatsUntilNext/8.0)).coerceIn(0.0,1.0).toFloat()
                LinearProgressIndicator(
                    progress={progress},
                    modifier=Modifier.fillMaxWidth().height(5.dp),
                    color=if(beatsUntilNext<=4.0) Acid else Mint,
                    trackColor=Glass2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        beatsUntilNext <= .35 -> "CHANGE NOW → ${next.chord}"
                        beatsUntilNext <= 1.0 -> "CHANGE ON THE NEXT BEAT → ${next.chord}"
                        beatsUntilNext <= 4.0 -> "Prepare ${next.chord} · ${String.format("%.1f",beatsUntilNext)} beats"
                        else -> "${next.chord} coming in ${String.format("%.1f",beatsUntilNext)} beats"
                    },
                    color=if(beatsUntilNext<=4.0) White else Fog,
                    fontSize=11.sp,
                    fontWeight=if(beatsUntilNext<=1.0) FontWeight.Bold else FontWeight.Medium,
                    maxLines=1
                )
            }

            Spacer(Modifier.height(12.dp))
            RhythmCoach(s, beatInBar, setRhythm)

            Spacer(Modifier.height(12.dp))
            ChordTimeline(s)

            if(s.playAlongMode==PlayAlongMode.LEARN && next!=null) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().background(Color(0xFF141A17),RoundedCornerShape(14.dp)).padding(horizontal=12.dp,vertical=8.dp),contentAlignment=Alignment.Center){
                    Text("Keep ${current?.chord ?: "—"} · prepare ${next.chord}",color=White,fontSize=12.sp,fontWeight=FontWeight.Medium,maxLines=1)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick=play,
            modifier=Modifier.fillMaxWidth().height(56.dp),
            shape=RoundedCornerShape(20.dp),
            colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night)
        ) {
            Text(if(s.isPlaying||s.countdown!=null)"PAUSE" else "PLAY",fontSize=13.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp)
        }
        Spacer(Modifier.height(4.dp))
    }

    s.countdown?.let { count ->
        Box(Modifier.fillMaxSize().background(Color(0xD9070908)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(count.toString(),color=White,fontSize=96.sp,fontWeight=FontWeight.ExtraLight);Text("COUNT IN",color=Fog,fontSize=10.sp,letterSpacing=2.sp)}}
    }
}

@Composable
private fun ChordCard(modifier:Modifier,label:String,chord:String,accent:Boolean,footer:String?=null) {
    val size = when {
        chord.length <= 2 -> 56.sp
        chord.length <= 4 -> 46.sp
        chord.length <= 6 -> 37.sp
        else -> 30.sp
    }
    Column(
        modifier.background(if(accent)Color(0xFF1B221B)else Glass2,RoundedCornerShape(20.dp))
            .border(1.dp,if(accent)Acid.copy(alpha=.45f)else Line,RoundedCornerShape(20.dp))
            .padding(horizontal=8.dp,vertical=10.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        Text(label,color=if(accent)Acid else Fog,fontSize=8.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Bold,maxLines=1)
        Text(chord,color=if(accent)Acid else White,fontSize=size,fontWeight=FontWeight.Light,maxLines=1,softWrap=false,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
        footer?.let{Text(it,color=Fog,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)}
    }
}

@Composable
private fun RhythmCoach(s:PracticeState,beatInBar:Int,setRhythm:(RhythmPattern)->Unit) {
    val eighth=((s.positionBeats*2.0).toInt().mod(8))
    val pattern=when(s.rhythmPattern){
        RhythmPattern.BASIC -> listOf("↓","·","↓","·","↓","·","↓","·")
        RhythmPattern.GROOVE -> listOf("↓","·","↓","↑","·","↑","↓","↑")
    }
    Column(Modifier.fillMaxWidth().testTag("rhythm-coach")) {
        Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RHYTHM COACH",color=Fog,fontSize=8.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Bold)
                Text(if(s.rhythmPattern==RhythmPattern.BASIC)"1 downstroke per beat" else "Suggested groove · D D U U D U",color=White,fontSize=11.sp,fontWeight=FontWeight.Medium,maxLines=1)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                RhythmPattern.entries.forEach { p ->
                    val on=s.rhythmPattern==p
                    Box(Modifier.background(if(on)Acid else Glass2,RoundedCornerShape(10.dp)).clickable{setRhythm(p)}.padding(horizontal=8.dp,vertical=6.dp)){
                        Text(p.name,color=if(on)Night else Fog,fontSize=8.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            pattern.forEachIndexed { index, stroke ->
                val active=s.isPlaying && index==eighth
                val isBeat=index%2==0
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(if(isBeat)(index/2+1).toString() else "&",color=Fog,fontSize=8.sp)
                    Box(Modifier.fillMaxWidth().height(38.dp).background(if(active)Acid else Glass2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){
                        Text(stroke,color=if(active)Night else if(stroke=="·")Fog else White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text("Beat $beatInBar of ${s.song.beatsPerBar} · highlighted box = move your hand now",color=Fog,fontSize=9.sp)
    }
}

@Composable
private fun ChordTimeline(s: PracticeState) {
    val index=s.song.events.indexOfLast{s.positionBeats>=it.beat}.coerceAtLeast(0)
    val events=s.song.events.drop(index).take(5)
    Row(Modifier.fillMaxWidth().testTag("detected-chord-strip"),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically) {
        events.forEachIndexed { i,e ->
            val active=i==0
            val chordSize=if(e.chord.length<=4)15.sp else 12.sp
            Column(Modifier.weight(1f).background(if(active)Color(0xFF202822)else Glass2,RoundedCornerShape(12.dp)).padding(horizontal=3.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text(e.chord,color=if(active)Acid else White,fontSize=chordSize,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false)
                Text(if(active)"NOW" else "${String.format("%.1f",(e.beat-s.positionBeats).coerceAtLeast(0.0))}b",color=Fog,fontSize=7.sp,maxLines=1)
            }
        }
    }
}

private fun vibrateBeat(c:Context,accent:Boolean){try{val v=if(Build.VERSION.SDK_INT>=31)(c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)as VibratorManager).defaultVibrator else @Suppress("DEPRECATION")(c.getSystemService(Context.VIBRATOR_SERVICE)as Vibrator);val duration=if(accent)32L else 14L;val amplitude=if(accent)180 else 80;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(duration,amplitude))else @Suppress("DEPRECATION")v.vibrate(duration)}catch(_:Throwable){}}
