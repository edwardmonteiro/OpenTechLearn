package com.edward.ukuleleai.ui.practice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.*
import kotlin.math.floor

private val Night=Color(0xFF070908)
private val Glass=Color(0xFF101412)
private val Glass2=Color(0xFF171C19)
private val Line=Color(0xFF303834)
private val White=Color(0xFFF6F7F3)
private val Fog=Color(0xFF858F89)
private val Acid=Color(0xFFDDF45A)
private val Mint=Color(0xFF70E0B6)
private val FingerColors=listOf(Color(0xFF64CFF4),Color(0xFFFF6B6B),Color(0xFF70E0B6),Color(0xFFFFC857))

@Composable
fun PracticeRoute(song:Song,onExit:()->Unit,onEditAnalysis:()->Unit={},viewModel:PracticeViewModel=viewModel()){
    val s by viewModel.state.collectAsState()
    val ctx=LocalContext.current
    val ask=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)viewModel.startListening()}
    LaunchedEffect(song.id){viewModel.loadSong(song)}
    LaunchedEffect(s.beatPulse){
        if(s.beatPulse>0&&s.barHapticsEnabled){
            val beat=floor(s.positionBeats).toInt()
            vibrateBeat(ctx,beat%s.song.beatsPerBar==0)
        }
    }
    val listen={
        if(s.listeningEnabled)viewModel.stopListening()
        else if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)viewModel.startListening()
        else ask.launch(Manifest.permission.RECORD_AUDIO)
    }
    PracticeHud(
        s=s,exit={viewModel.exit(onExit)},play=viewModel::togglePlayback,restart=viewModel::restart,
        setRhythm=viewModel::setRhythmPattern,toggleBeginner=viewModel::toggleBeginner,
        toggleBacking=viewModel::toggleBacking,toggleHaptics=viewModel::toggleBarHaptics,
        listen=listen,editAnalysis=onEditAnalysis,
        setPlaybackRate=viewModel::setPlaybackRate,
        toggleLyricLoop=viewModel::toggleLyricLoop,
        saveTappedLyrics=viewModel::saveTappedLyrics,
        autoDistributeLyrics=viewModel::autoDistributeLyrics,
        seekToBeat=viewModel::seekToBeat
    )
}

@Composable
private fun PracticeHud(
    s:PracticeState,exit:()->Unit,play:()->Unit,restart:()->Unit,
    setRhythm:(RhythmPattern)->Unit,toggleBeginner:()->Unit,toggleBacking:()->Unit,
    toggleHaptics:()->Unit,listen:()->Unit,editAnalysis:()->Unit,
    setPlaybackRate:(Float)->Unit,toggleLyricLoop:(Int)->Unit,
    saveTappedLyrics:(List<String>,List<Double>)->Unit,
    autoDistributeLyrics:(String)->Unit,seekToBeat:(Double)->Unit
){
    var menuOpen by remember{mutableStateOf(false)}
    var showLyricsEditor by remember{mutableStateOf(false)}
    var lyricDraft by remember(s.song.id){mutableStateOf(s.song.lyrics.joinToString("\n"){it.text})}
    var tapSyncMode by remember{mutableStateOf(false)}
    var syncIndex by remember{mutableIntStateOf(0)}
    val syncBeats=remember{mutableStateListOf<Double>()}
    val currentIndex=s.song.events.indexOfLast{s.positionBeats>=it.beat}.coerceAtLeast(0)
    val current=s.song.events.getOrNull(currentIndex)
    val next=s.song.events.drop(currentIndex+1).firstOrNull{it.chord!=current?.chord}
    val beatsUntilNext=next?.let{(it.beat-s.positionBeats).coerceAtLeast(0.0)}
    val secondsUntilNext=beatsUntilNext?.times(60.0/s.bpm)
    val beatInBar=floor(s.positionBeats).toInt().mod(s.song.beatsPerBar)+1

    Box(
        Modifier.fillMaxSize()
            .background(Night)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(
            Modifier.fillMaxSize()
                .padding(horizontal=10.dp,vertical=5.dp)
                .padding(bottom=52.dp)
        ){
        CompactHeader(
            title=s.song.title,bpm=s.bpm,key=s.analysisKey,audioOn=s.backingEnabled,
            exit=exit,menuOpen=menuOpen,setMenuOpen={menuOpen=it},
            beginner=s.beginnerMode,toggleBeginner=toggleBeginner,
            backingAvailable=s.backingAvailable,toggleBacking=toggleBacking,
            haptics=s.barHapticsEnabled,toggleHaptics=toggleHaptics,
            listening=s.listeningEnabled,listen=listen,
            analyzed=s.analysisAvailable,editAnalysis=editAnalysis
        )
        Spacer(Modifier.height(4.dp))
        CurrentNextFingering(current?.chord?:"—",next?.chord?:"—",beatsUntilNext,secondsUntilNext)
        Spacer(Modifier.height(4.dp))
        LyricsLane(
            s=s,
            onEdit={showLyricsEditor=true},
            onLoop=toggleLyricLoop,
            onRate=setPlaybackRate,
            onSeek=seekToBeat
        )
        Spacer(Modifier.height(4.dp))
        RhythmCoach(s,beatInBar,setRhythm)
        Spacer(Modifier.height(4.dp))
        UpcomingStrip(s)
        Spacer(Modifier.weight(1f))
        }

        BottomControls(
            isPlaying=s.isPlaying||s.countdown!=null,
            play=play,
            restart=restart,
            modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=2.dp)
        )
    }

    s.countdown?.let{count->
        Box(Modifier.fillMaxSize().background(Color(0xE0070908)),contentAlignment=Alignment.Center){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text(count.toString(),color=White,fontSize=84.sp,fontWeight=FontWeight.ExtraLight)
                Text("COUNT IN · PLAY ON 1",color=Fog,fontSize=9.sp,letterSpacing=1.4.sp)
            }
        }
    }

    if(showLyricsEditor){
        val lines=lyricDraft.lines().map{it.trim()}.filter{it.isNotBlank()}
        Dialog(onDismissRequest={
            showLyricsEditor=false
            tapSyncMode=false
            syncIndex=0
            syncBeats.clear()
        }){
            Column(
                Modifier.fillMaxWidth()
                    .background(Color(0xFF111512),RoundedCornerShape(22.dp))
                    .border(1.dp,Line,RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ){
                Text("LYRICS COACH",color=Acid,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
                Spacer(Modifier.height(8.dp))
                if(!tapSyncMode){
                    Text("Paste one phrase per line",color=Fog,fontSize=10.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value=lyricDraft,
                        onValueChange={lyricDraft=it},
                        modifier=Modifier.fillMaxWidth().height(210.dp),
                        textStyle=androidx.compose.ui.text.TextStyle(color=White,fontSize=13.sp),
                        colors=OutlinedTextFieldDefaults.colors(
                            focusedTextColor=White,unfocusedTextColor=White,
                            focusedBorderColor=Acid,unfocusedBorderColor=Line,
                            cursorColor=Acid
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(
                            onClick={
                                if(lines.isNotEmpty()){
                                    autoDistributeLyrics(lyricDraft)
                                    showLyricsEditor=false
                                }
                            },
                            enabled=lines.isNotEmpty(),
                            modifier=Modifier.weight(1f),
                            colors=ButtonDefaults.buttonColors(containerColor=Glass2,contentColor=White)
                        ){Text("AUTO PLACE β",fontSize=9.sp,fontWeight=FontWeight.Bold)}
                        Button(
                            onClick={
                                syncIndex=0
                                syncBeats.clear()
                                tapSyncMode=true
                            },
                            enabled=lines.isNotEmpty(),
                            modifier=Modifier.weight(1f),
                            colors=ButtonDefaults.buttonColors(containerColor=Acid,contentColor=Night)
                        ){Text("TAP TO SYNC",fontSize=9.sp,fontWeight=FontWeight.Bold)}
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("AUTO PLACE is a local timing estimate. TAP TO SYNC is the precise mode.",color=Fog,fontSize=8.sp)
                }else{
                    Text("Play the song, then tap when each phrase starts.",color=Fog,fontSize=10.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if(syncIndex<lines.size)lines[syncIndex] else "Done",
                        color=White,fontSize=20.sp,fontWeight=FontWeight.Medium,
                        modifier=Modifier.fillMaxWidth(),
                        textAlign=TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${syncIndex.coerceAtMost(lines.size)} / ${lines.size}",color=Fog,fontSize=9.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick={
                            if(syncIndex<lines.size){
                                syncBeats.add(s.positionBeats)
                                syncIndex++
                                if(syncIndex>=lines.size){
                                    saveTappedLyrics(lines,syncBeats.toList())
                                    showLyricsEditor=false
                                    tapSyncMode=false
                                    syncIndex=0
                                    syncBeats.clear()
                                }
                            }
                        },
                        enabled=syncIndex<lines.size,
                        modifier=Modifier.fillMaxWidth().height(64.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Acid,contentColor=Night),
                        shape=RoundedCornerShape(18.dp)
                    ){Text("TAP PHRASE",fontSize=15.sp,fontWeight=FontWeight.ExtraBold)}
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(
                            onClick=play,
                            modifier=Modifier.weight(1f)
                        ){Text(if(s.isPlaying)"PAUSE" else "PLAY",fontSize=10.sp)}
                        OutlinedButton(
                            onClick={
                                tapSyncMode=false
                                syncIndex=0
                                syncBeats.clear()
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("BACK",fontSize=10.sp)}
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactHeader(
    title:String,bpm:Int,key:String?,audioOn:Boolean,
    exit:()->Unit,menuOpen:Boolean,setMenuOpen:(Boolean)->Unit,
    beginner:Boolean,toggleBeginner:()->Unit,
    backingAvailable:Boolean,toggleBacking:()->Unit,
    haptics:Boolean,toggleHaptics:()->Unit,
    listening:Boolean,listen:()->Unit,
    analyzed:Boolean,editAnalysis:()->Unit
){
    Row(Modifier.fillMaxWidth().height(32.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(30.dp).background(Glass2,CircleShape).clickable(onClick=exit),contentAlignment=Alignment.Center){
            Text("‹",color=White,fontSize=20.sp)
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)){
            Text(title,color=White,fontSize=11.sp,fontWeight=FontWeight.Medium,maxLines=1)
            Text(
                "$bpm BPM${if(key!=null)" · $key" else ""}${if(audioOn)" · AUDIO" else ""}",
                color=Fog,fontSize=7.sp,maxLines=1
            )
        }
        Box{
            Box(Modifier.size(30.dp).background(Glass2,CircleShape).clickable{setMenuOpen(true)},contentAlignment=Alignment.Center){
                Text("⋯",color=White,fontSize=19.sp)
            }
            DropdownMenu(expanded=menuOpen,onDismissRequest={setMenuOpen(false)}){
                DropdownMenuItem(text={Text(if(beginner)"Use full chords" else "Use beginner triads")},onClick={toggleBeginner();setMenuOpen(false)})
                if(backingAvailable)DropdownMenuItem(text={Text(if(audioOn)"Mute original audio" else "Play original audio")},onClick={toggleBacking();setMenuOpen(false)})
                DropdownMenuItem(text={Text(if(haptics)"Beat haptics off" else "Beat haptics on")},onClick={toggleHaptics();setMenuOpen(false)})
                DropdownMenuItem(text={Text(if(listening)"Stop microphone listen" else "Microphone listen")},onClick={listen();setMenuOpen(false)})
                if(analyzed)DropdownMenuItem(text={Text("Edit chord analysis")},onClick={editAnalysis();setMenuOpen(false)})
            }
        }
    }
}

@Composable
private fun CurrentNextFingering(current:String,next:String,beatsUntilNext:Double?,secondsUntilNext:Double?){
    val currentShape=remember(current){UkuleleFingeringEngine.forChord(current)}
    val nextShape=remember(next){UkuleleFingeringEngine.forChord(next)}
    Column(
        Modifier.fillMaxWidth().height(190.dp)
            .background(Glass,RoundedCornerShape(18.dp))
            .border(1.dp,Line,RoundedCornerShape(18.dp))
            .padding(7.dp)
    ){
        Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            FingeringCard(Modifier.weight(1f).testTag("current-chord"),"CURRENT",current,currentShape,true)
            FingeringCard(
                Modifier.weight(1f).testTag("next-chord"),
                if((beatsUntilNext?:99.0)<=4.0)"GET READY" else "NEXT",
                next,nextShape,false
            )
        }
        Spacer(Modifier.height(4.dp))
        if(beatsUntilNext!=null){
            val progress=(1.0-(beatsUntilNext/8.0)).coerceIn(0.0,1.0).toFloat()
            LinearProgressIndicator(
                progress={progress},modifier=Modifier.fillMaxWidth().height(4.dp),
                color=if(beatsUntilNext<=4.0)Acid else Mint,trackColor=Glass2
            )
            Spacer(Modifier.height(3.dp))
            Text(
                when{
                    beatsUntilNext<=.35->"CHANGE NOW → $next"
                    beatsUntilNext<=1.0->"NEXT BEAT → $next"
                    beatsUntilNext<=4.0->"Prepare $next · ${String.format("%.1f",beatsUntilNext)} beats"
                    else->"$next in ${String.format("%.1f",beatsUntilNext)} beats · ${String.format("%.1f",secondsUntilNext?:0.0)}s"
                },
                color=if(beatsUntilNext<=4.0)White else Fog,fontSize=8.sp,
                fontWeight=if(beatsUntilNext<=1.0)FontWeight.Bold else FontWeight.Medium,maxLines=1
            )
        }
        val handHint=if((beatsUntilNext?:99.0)<=2.0)prepareHandsHint(current,next) else null
        if(handHint!=null){
            Spacer(Modifier.height(2.dp))
            Text(handHint,color=Acid,fontSize=6.sp,fontWeight=FontWeight.Bold,maxLines=1)
        }
        Spacer(Modifier.height(2.dp))
        FingerLegend()
    }
}

@Composable
private fun FingeringCard(modifier:Modifier,label:String,chord:String,shape:UkuleleFingering?,accent:Boolean){
    Column(
        modifier.background(if(accent)Color(0xFF1B221B)else Glass2,RoundedCornerShape(13.dp))
            .border(1.dp,if(accent)Acid.copy(alpha=.5f)else Line,RoundedCornerShape(13.dp))
            .padding(4.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ){
        Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){
            Text(label,color=if(accent)Acid else Fog,fontSize=6.sp,letterSpacing=.8.sp,fontWeight=FontWeight.Bold)
            Text(chord,color=if(accent)Acid else White,fontSize=if(chord.length<=4)22.sp else 17.sp,fontWeight=FontWeight.Bold,maxLines=1)
        }
        if(shape!=null)UkuleleDiagram(shape,Modifier.fillMaxWidth().weight(1f).testTag("fingering-diagram"))
        else Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text("—",color=Fog,fontSize=18.sp)}
    }
}

@Composable
private fun UkuleleDiagram(shape:UkuleleFingering,modifier:Modifier=Modifier){
    Canvas(modifier.padding(horizontal=9.dp,vertical=3.dp)){
        val left=size.width*.17f
        val right=size.width*.83f
        val top=size.height*.14f
        val bottom=size.height*.90f
        val stringGap=(right-left)/3f
        val fretGap=(bottom-top)/4f

        for(i in 0..3){
            val x=left+i*stringGap
            drawLine(White.copy(alpha=.55f),Offset(x,top),Offset(x,bottom),1.3f)
        }
        for(f in 0..4){
            val y=top+f*fretGap
            drawLine(if(f==0)White else Fog.copy(alpha=.45f),Offset(left,y),Offset(right,y),if(f==0)3.2f else 1.2f)
        }

        val paint=Paint().apply{
            color=android.graphics.Color.rgb(7,9,8)
            textAlign=Paint.Align.CENTER
            isAntiAlias=true
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        }

        shape.frets.forEachIndexed{stringIndex,fret->
            val x=left+stringIndex*stringGap
            if(fret>0){
                val finger=shape.fingers[stringIndex].coerceIn(1,4)
                val y=top+(fret-.5f)*fretGap
                val radius=minOf(stringGap,fretGap)*.27f
                drawCircle(FingerColors[finger-1],radius,Offset(x,y))
                paint.textSize=radius*1.25f
                drawContext.canvas.nativeCanvas.drawText(finger.toString(),x,y+paint.textSize*.34f,paint)
            }else{
                drawCircle(Mint,3.8f,Offset(x,top-6f))
            }
        }

        val labelPaint=Paint().apply{
            color=android.graphics.Color.rgb(133,143,137)
            textAlign=Paint.Align.CENTER
            isAntiAlias=true
            typeface=android.graphics.Typeface.DEFAULT_BOLD
            textSize=9f
        }
        listOf("G","C","E","A").forEachIndexed{i,label->
            val x=left+i*stringGap
            drawContext.canvas.nativeCanvas.drawText(label,x,10f,labelPaint)
        }
    }
}

@Composable
private fun FingerLegend(){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){
        val labels=listOf("1 index","2 middle","3 ring","4 pinky")
        labels.forEachIndexed{i,label->
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(6.dp).background(FingerColors[i],CircleShape))
                Spacer(Modifier.width(2.dp))
                Text(label,color=Fog,fontSize=5.sp)
                if(i<labels.lastIndex)Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun LyricsLane(
    s:PracticeState,
    onEdit:()->Unit,
    onLoop:(Int)->Unit,
    onRate:(Float)->Unit,
    onSeek:(Double)->Unit
){
    val lyrics=s.song.lyrics
    val currentIndex=lyrics.indexOfLast{s.positionBeats>=it.beat}
    val current=lyrics.getOrNull(currentIndex)
    val next=lyrics.getOrNull(currentIndex+1)
    val looping=currentIndex>=0&&s.loopLyricIndex==currentIndex

    Column(
        Modifier.fillMaxWidth().height(70.dp).testTag("lyrics-lane")
            .background(Glass,RoundedCornerShape(16.dp))
            .border(1.dp,Line,RoundedCornerShape(16.dp))
            .padding(horizontal=8.dp,vertical=6.dp)
    ){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text("LYRICS",color=Mint,fontSize=7.sp,letterSpacing=1.sp,fontWeight=FontWeight.Bold)
                Text(
                    current?.text ?: if(lyrics.isEmpty())"Paste lyrics and sync them" else "Get ready…",
                    color=White,fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1
                )
                Text(
                    next?.text ?: if(lyrics.isEmpty())"Tap LYRICS to add text" else " ",
                    color=Fog,fontSize=8.sp,maxLines=1
                )
            }
            Text(
                "EDIT",
                color=Acid,fontSize=7.sp,fontWeight=FontWeight.Bold,
                modifier=Modifier
                    .background(Glass2,RoundedCornerShape(8.dp))
                    .clickable(onClick=onEdit)
                    .padding(horizontal=7.dp,vertical=5.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            if(currentIndex>=0){
                Text(
                    if(looping)"LOOP ON" else "LOOP LINE",
                    color=if(looping)Acid else Fog,
                    fontSize=6.sp,fontWeight=FontWeight.Bold,
                    modifier=Modifier.clickable{onLoop(currentIndex)}.padding(end=8.dp)
                )
                Text(
                    "↺ LINE",
                    color=Fog,fontSize=6.sp,
                    modifier=Modifier.clickable{onSeek(current!!.beat)}.padding(end=10.dp)
                )
            }else{
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
            listOf(.75f,.85f,1f).forEach{rate->
                val selected=kotlin.math.abs(s.playbackRate-rate)<.01f
                Text(
                    "${if(rate==1f)"1.0" else rate}×",
                    color=if(selected)Night else Fog,
                    fontSize=6.sp,fontWeight=FontWeight.Bold,
                    modifier=Modifier
                        .background(if(selected)Acid else Glass2,RoundedCornerShape(7.dp))
                        .clickable{onRate(rate)}
                        .padding(horizontal=6.dp,vertical=3.dp)
                )
                Spacer(Modifier.width(3.dp))
            }
        }
    }
}

private fun prepareHandsHint(current:String,next:String):String?{
    val from=UkuleleFingeringEngine.forChord(current)?:return null
    val to=UkuleleFingeringEngine.forChord(next)?:return null
    val strings=listOf("G","C","E","A")
    val changed=to.frets.indices.filter{to.frets[it]!=from.frets[it]}
    if(changed.isEmpty())return "Keep the same shape"
    val target=changed.firstOrNull{to.frets[it]>0}?:changed.first()
    return if(to.frets[target]>0){
        val finger=to.fingers[target].coerceIn(1,4)
        "PREPARE HANDS · finger $finger → ${strings[target]} fret ${to.frets[target]}"
    }else{
        "PREPARE HANDS · release ${strings[target]} string"
    }
}

@Composable
private fun RhythmCoach(s:PracticeState,beatInBar:Int,setRhythm:(RhythmPattern)->Unit){
    val eighth=(s.positionBeats*2.0).toInt().mod(8)
    val pattern=when(s.rhythmPattern){
        RhythmPattern.BASIC->listOf("↓","·","↓","·","↓","·","↓","·")
        RhythmPattern.GROOVE->listOf("↓","·","↓","↑","·","↑","↓","↑")
    }
    Column(
        Modifier.fillMaxWidth().height(108.dp).testTag("rhythm-coach")
            .background(Glass,RoundedCornerShape(16.dp)).border(1.dp,Line,RoundedCornerShape(16.dp)).padding(7.dp)
    ){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text("RHYTHM COACH",color=Acid,fontSize=7.sp,letterSpacing=1.sp,fontWeight=FontWeight.Bold)
                Text(if(s.rhythmPattern==RhythmPattern.BASIC)"DOWN on 1 · 2 · 3 · 4" else "Groove · ↓  ↓↑  ↑↓↑",color=White,fontSize=8.sp,fontWeight=FontWeight.Medium,maxLines=1)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(2.dp)){
                RhythmPattern.entries.forEach{p->
                    val selected=s.rhythmPattern==p
                    Box(Modifier.background(if(selected)Acid else Glass2,RoundedCornerShape(7.dp)).clickable{setRhythm(p)}.padding(horizontal=5.dp,vertical=3.dp)){
                        Text(p.name,color=if(selected)Night else Fog,fontSize=5.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){
            pattern.forEachIndexed{index,stroke->
                val active=s.isPlaying&&index==eighth
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(if(index%2==0)(index/2+1).toString() else "&",color=if(active)Acid else Fog,fontSize=6.sp)
                    Box(Modifier.fillMaxWidth().height(32.dp).background(if(active)Acid else Glass2,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center){
                        Text(stroke,color=if(active)Night else if(stroke=="·")Fog else White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("Beat $beatInBar/${s.song.beatsPerBar} · yellow = move now",color=Fog,fontSize=6.sp)
    }
}

@Composable
private fun UpcomingStrip(s:PracticeState){
    val index=s.song.events.indexOfLast{s.positionBeats>=it.beat}.coerceAtLeast(0)
    val events=s.song.events.drop(index).take(4)
    Row(
        Modifier.fillMaxWidth().height(46.dp).testTag("detected-chord-strip")
            .background(Glass,RoundedCornerShape(13.dp)).padding(4.dp),
        horizontalArrangement=Arrangement.spacedBy(3.dp)
    ){
        events.forEachIndexed{i,e->
            Column(
                Modifier.weight(1f).background(if(i==0)Color(0xFF202822)else Glass2,RoundedCornerShape(8.dp)).padding(vertical=3.dp,horizontal=2.dp),
                horizontalAlignment=Alignment.CenterHorizontally
            ){
                Text(e.chord,color=if(i==0)Acid else White,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
                Text(if(i==0)"NOW" else "${String.format("%.1f",(e.beat-s.positionBeats).coerceAtLeast(0.0))}b",color=Fog,fontSize=5.sp,maxLines=1)
            }
        }
    }
}

@Composable
private fun BottomControls(
    isPlaying:Boolean,
    play:()->Unit,
    restart:()->Unit,
    modifier:Modifier=Modifier
){
    Row(
        modifier.height(48.dp)
            .background(Night.copy(alpha=.94f),RoundedCornerShape(24.dp))
            .padding(horizontal=6.dp),
        horizontalArrangement=Arrangement.Center,
        verticalAlignment=Alignment.CenterVertically
    ){
        OutlinedButton(
            onClick=restart,
            modifier=Modifier.size(34.dp),
            shape=CircleShape,
            contentPadding=PaddingValues(0.dp),
            colors=ButtonDefaults.outlinedButtonColors(contentColor=White)
        ){
            Text("↺",fontSize=15.sp)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick=play,
            modifier=Modifier.width(88.dp).height(40.dp).testTag("play-button"),
            shape=RoundedCornerShape(20.dp),
            contentPadding=PaddingValues(horizontal=10.dp,vertical=0.dp),
            colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night)
        ){
            Text(
                if(isPlaying)"Ⅱ  PAUSE" else "▶  PLAY",
                fontSize=11.sp,
                fontWeight=FontWeight.ExtraBold,
                letterSpacing=.5.sp
            )
        }
    }
}

private fun vibrateBeat(c:Context,accent:Boolean){
    try{
        val v=if(Build.VERSION.SDK_INT>=31)(c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)as VibratorManager).defaultVibrator else @Suppress("DEPRECATION")(c.getSystemService(Context.VIBRATOR_SERVICE)as Vibrator)
        val duration=if(accent)32L else 14L
        val amplitude=if(accent)180 else 80
        if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(duration,amplitude))else @Suppress("DEPRECATION")v.vibrate(duration)
    }catch(_:Throwable){}
}
