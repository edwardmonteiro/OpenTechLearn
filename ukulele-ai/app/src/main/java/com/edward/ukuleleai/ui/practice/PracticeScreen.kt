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
        if (s.beatPulse > 0 && s.barHapticsEnabled && floor(s.positionBeats).toInt() % s.song.beatsPerBar == 0) vibrateBeat(ctx)
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal=18.dp, vertical=10.dp)
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
                    DropdownMenuItem(text={Text(if(s.barHapticsEnabled)"Turn beat-1 haptic off" else "Turn beat-1 haptic on")},onClick={toggleHaptics();menuOpen=false})
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
            Text("PLAY NOW",color=Fog,fontSize=9.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold)
            Text(current?.chord ?: "—",modifier=Modifier.testTag("current-chord"),color=Acid,fontSize=72.sp,fontWeight=FontWeight.ExtraLight,maxLines=1)

            if(next != null) {
                Text(
                    if(secondsUntilNext != null && secondsUntilNext <= 2.2) "PREPARE  ${next.chord}  ·  ${String.format("%.1f",secondsUntilNext)}s" else "NEXT  ${next.chord}",
                    color=if(secondsUntilNext != null && secondsUntilNext <= 2.2) White else Fog,
                    fontSize=16.sp,
                    fontWeight=if(secondsUntilNext != null && secondsUntilNext <= 2.2) FontWeight.Bold else FontWeight.Medium
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("BEAT",color=Fog,fontSize=8.sp,letterSpacing=2.sp)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                (1..s.song.beatsPerBar).forEach { beat ->
                    Box(Modifier.size(if(beat==beatInBar)40.dp else 32.dp).background(if(beat==beatInBar)Acid else Glass2,CircleShape),contentAlignment=Alignment.Center){Text(beat.toString(),color=if(beat==beatInBar)Night else Fog,fontSize=12.sp,fontWeight=FontWeight.Bold)}
                }
            }

            Spacer(Modifier.height(22.dp))
            if(s.beginnerMode) {
                Text("BEGINNER STRUM",color=Fog,fontSize=8.sp,letterSpacing=1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("↓     ↓     ↓     ↓",color=White,fontSize=25.sp,fontWeight=FontWeight.Medium)
                Text("one downstroke on every beat",color=Fog,fontSize=10.sp)
            } else {
                Text("FOLLOW THE BEAT · STRUM IS A SUGGESTION",color=Fog,fontSize=9.sp,letterSpacing=1.sp)
            }

            Spacer(Modifier.height(22.dp))
            ChordTimeline(s)

            if(s.playAlongMode==PlayAlongMode.LEARN && next!=null) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().background(Color(0xFF141A17),RoundedCornerShape(18.dp)).padding(14.dp),contentAlignment=Alignment.Center){
                    Text("Get ready for  ${next.chord}",color=White,fontSize=15.sp,fontWeight=FontWeight.Medium)
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
private fun ChordTimeline(s: PracticeState) {
    val index=s.song.events.indexOfLast{s.positionBeats>=it.beat}.coerceAtLeast(0)
    val events=s.song.events.drop(index).take(4)
    Row(Modifier.fillMaxWidth().testTag("detected-chord-strip"),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
        events.forEachIndexed { i,e ->
            val active=i==0
            Column(Modifier.weight(1f).background(if(active)Color(0xFF202822)else Glass2,RoundedCornerShape(14.dp)).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(e.chord,color=if(active)Acid else White,fontSize=17.sp,fontWeight=FontWeight.Bold);if(active)Text("NOW",color=Fog,fontSize=7.sp)else Text("+${ceil((e.beat-s.positionBeats).coerceAtLeast(0.0)).toInt()} beats",color=Fog,fontSize=7.sp)}
        }
    }
}

private fun vibrateBeat(c:Context){try{val v=if(Build.VERSION.SDK_INT>=31)(c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)as VibratorManager).defaultVibrator else @Suppress("DEPRECATION")(c.getSystemService(Context.VIBRATOR_SERVICE)as Vibrator);if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(20,VibrationEffect.DEFAULT_AMPLITUDE))else @Suppress("DEPRECATION")v.vibrate(20)}catch(_:Throwable){}}
