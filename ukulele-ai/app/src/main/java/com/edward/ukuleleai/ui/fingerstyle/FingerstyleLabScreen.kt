package com.edward.ukuleleai.ui.fingerstyle

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

private val Night=Color(0xFF070908)
private val Glass=Color(0xFF101412)
private val Glass2=Color(0xFF171C19)
private val White=Color(0xFFF6F7F3)
private val Fog=Color(0xFF858F89)
private val Acid=Color(0xFFDDF45A)
private val Mint=Color(0xFF70E0B6)
private val Line=Color(0xFF303834)
private val Wood=Color(0xFF151914)
private val Wood2=Color(0xFF1C211C)

private const val BaseBpm=72

private data class FingerEvent(val stringIndex:Int,val stringName:String,val finger:String)
private data class LessonChord(val name:String,val latin:String,val frets:IntArray)

private val Pattern=listOf(
    FingerEvent(0,"G","P"),
    FingerEvent(1,"C","I"),
    FingerEvent(2,"E","M"),
    FingerEvent(3,"A","A"),
    FingerEvent(2,"E","M"),
    FingerEvent(1,"C","I"),
    FingerEvent(0,"G","P"),
    FingerEvent(1,"C","I")
)

private val FirstSong=listOf(
    LessonChord("C","Dó",intArrayOf(0,0,0,3)),
    LessonChord("Am","Lá menor",intArrayOf(2,0,0,0)),
    LessonChord("F","Fá",intArrayOf(2,0,1,0)),
    LessonChord("G","Sol",intArrayOf(0,2,3,2))
)

@Composable
fun FingerstyleLabScreen(onBack:()->Unit){
    var speed by remember{mutableFloatStateOf(.75f)}
    var playing by remember{mutableStateOf(false)}
    var elapsedMs by remember{mutableLongStateOf(0L)}
    var startedAt by remember{mutableLongStateOf(0L)}
    var loopPattern by remember{mutableStateOf(false)}
    var loopChordIndex by remember{mutableIntStateOf(0)}
    var showHelp by remember{mutableStateOf(true)}
    val engine=remember{FingerstyleAudioEngine()}

    val stepMs=(60_000.0/BaseBpm/2.0)/speed
    val chordCount=if(loopPattern)1 else FirstSong.size
    val totalSteps=chordCount*Pattern.size
    val totalMs=(stepMs*totalSteps).toLong().coerceAtLeast(1L)

    val rawStep=(elapsedMs/stepMs).coerceIn(0.0,(totalSteps-.0001).coerceAtLeast(0.0))
    val absoluteStep=floor(rawStep).toInt().coerceAtLeast(0)
    val localStepIndex=absoluteStep.mod(Pattern.size)
    val songChordIndex=if(loopPattern)loopChordIndex else (absoluteStep/Pattern.size).coerceIn(0,FirstSong.lastIndex)
    val chord=FirstSong[songChordIndex]
    val nextChord=if(loopPattern)chord else FirstSong.getOrNull(songChordIndex+1)

    DisposableEffect(Unit){onDispose{engine.stop()}}

    LaunchedEffect(playing,speed,loopPattern,loopChordIndex){
        if(playing){
            startedAt=SystemClock.elapsedRealtime()-elapsedMs
            val chords=if(loopPattern)listOf(FirstSong[loopChordIndex]) else FirstSong
            engine.playLesson(chords=chords,speed=speed,startMs=elapsedMs,loop=loopPattern)
            while(playing){
                val now=SystemClock.elapsedRealtime()-startedAt
                if(loopPattern){
                    elapsedMs=now.mod(totalMs)
                }else{
                    elapsedMs=now.coerceAtMost(totalMs)
                    if(elapsedMs>=totalMs){
                        playing=false
                        engine.stop()
                        break
                    }
                }
                delay(16)
            }
        }else{
            engine.stop()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Night).windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(
            Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=10.dp)
        ){
            RunnerHeader(
                chord=chord,
                nextChord=nextChord,
                speed=speed,
                onBack=onBack
            )

            Spacer(Modifier.height(8.dp))

            NeckRunner(
                rawStep=rawStep,
                chordIndex=songChordIndex,
                loopPattern=loopPattern,
                modifier=Modifier.fillMaxWidth().weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().height(58.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Row(
                    Modifier.weight(1f).fillMaxHeight()
                        .background(Glass,RoundedCornerShape(18.dp))
                        .border(1.dp,Line,RoundedCornerShape(18.dp))
                        .padding(horizontal=12.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Text("PATTERN",color=Fog,fontSize=7.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                    Spacer(Modifier.width(9.dp))
                    Pattern.forEachIndexed{index,event->
                        val active=index==localStepIndex
                        Column(
                            Modifier.weight(1f)
                                .background(if(active)Acid else Glass2,RoundedCornerShape(8.dp))
                                .padding(vertical=4.dp),
                            horizontalAlignment=Alignment.CenterHorizontally
                        ){
                            Text(event.finger,color=if(active)Night else White,fontSize=9.sp,fontWeight=FontWeight.Bold)
                            Text(event.stringName,color=if(active)Night.copy(alpha=.65f) else Fog,fontSize=6.sp)
                        }
                        if(index<Pattern.lastIndex)Spacer(Modifier.width(3.dp))
                    }
                }

                Spacer(Modifier.width(8.dp))

                Row(
                    Modifier.fillMaxHeight()
                        .background(Glass,RoundedCornerShape(18.dp))
                        .border(1.dp,Line,RoundedCornerShape(18.dp))
                        .padding(horizontal=10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    OutlinedButton(
                        onClick={
                            playing=false
                            elapsedMs=0L
                        },
                        modifier=Modifier.size(38.dp),
                        contentPadding=PaddingValues(0.dp),
                        shape=CircleShape
                    ){Text("↺",fontSize=14.sp)}

                    Spacer(Modifier.width(6.dp))

                    Button(
                        onClick={
                            if(!loopPattern && elapsedMs>=totalMs)elapsedMs=0L
                            playing=!playing
                        },
                        modifier=Modifier.width(92.dp).height(38.dp),
                        shape=RoundedCornerShape(19.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night)
                    ){
                        Text(if(playing)"Ⅱ PAUSE" else "▶ PLAY",fontSize=9.sp,fontWeight=FontWeight.Bold)
                    }

                    Spacer(Modifier.width(8.dp))

                    SpeedSelector(
                        speed=speed,
                        onSpeed={
                            playing=false
                            elapsedMs=0L
                            speed=it
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if(loopPattern)"LOOP ON" else "LOOP PATTERN",
                        color=if(loopPattern)Night else Mint,
                        fontSize=7.sp,
                        fontWeight=FontWeight.Bold,
                        modifier=Modifier
                            .background(if(loopPattern)Mint else Glass2,RoundedCornerShape(10.dp))
                            .clickable{
                                playing=false
                                loopChordIndex=songChordIndex
                                elapsedMs=0L
                                loopPattern=!loopPattern
                            }
                            .padding(horizontal=9.dp,vertical=7.dp)
                    )
                }
            }
        }

        if(showHelp){
            RunnerHelp{showHelp=false}
        }
    }
}

@Composable
private fun RunnerHeader(
    chord:LessonChord,
    nextChord:LessonChord?,
    speed:Float,
    onBack:()->Unit
){
    Row(
        Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Box(
            Modifier.size(38.dp).background(Glass2,CircleShape).clickable(onClick=onBack),
            contentAlignment=Alignment.Center
        ){Text("‹",color=White,fontSize=25.sp)}

        Spacer(Modifier.width(10.dp))

        Column{
            Text("Fingerstyle Runner",color=White,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            Text("Lesson 1 · C → Am → F → G",color=Fog,fontSize=8.sp)
        }

        Spacer(Modifier.weight(1f))

        Text("NOW",color=Fog,fontSize=7.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(5.dp))
        Text(chord.name,color=Acid,fontSize=23.sp,fontWeight=FontWeight.ExtraBold)
        Spacer(Modifier.width(5.dp))
        Text(chord.latin,color=Fog,fontSize=8.sp)

        Spacer(Modifier.width(18.dp))

        Text("NEXT",color=Fog,fontSize=7.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(5.dp))
        Text(nextChord?.name?:"FINISH",color=Mint,fontSize=13.sp,fontWeight=FontWeight.Bold)

        Spacer(Modifier.width(18.dp))

        Text(
            BaseBpm.toString()+" BPM · "+String.format("%.2f×",speed),
            color=White,fontSize=8.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.background(Glass2,RoundedCornerShape(12.dp)).padding(horizontal=10.dp,vertical=7.dp)
        )
    }
}

@Composable
private fun NeckRunner(
    rawStep:Double,
    chordIndex:Int,
    loopPattern:Boolean,
    modifier:Modifier=Modifier
){
    Canvas(
        modifier.background(Glass,RoundedCornerShape(24.dp))
            .border(1.dp,Line,RoundedCornerShape(24.dp))
            .padding(10.dp)
    ){
        val playX=size.width*.18f
        val neckTop=size.height*.10f
        val neckBottom=size.height*.88f
        val neckHeight=neckBottom-neckTop
        val laneGap=neckHeight/5f
        val ys=listOf(
            neckTop+laneGap,
            neckTop+laneGap*2f,
            neckTop+laneGap*3f,
            neckTop+laneGap*4f
        )
        val eventSpacing=size.width*.135f
        val capsuleW=size.width*.062f
        val capsuleH=laneGap*.62f

        drawRoundRect(
            color=Wood,
            topLeft=Offset(size.width*.035f,neckTop),
            size=Size(size.width*.93f,neckHeight),
            cornerRadius=CornerRadius(22f,22f)
        )
        drawRoundRect(
            color=Wood2.copy(alpha=.35f),
            topLeft=Offset(size.width*.035f,neckTop),
            size=Size(size.width*.93f,neckHeight*.18f),
            cornerRadius=CornerRadius(22f,22f)
        )

        val fretCount=9
        for(i in 0..fretCount){
            val x=size.width*.035f+(size.width*.93f/fretCount)*i
            drawLine(Fog.copy(alpha=.13f),Offset(x,neckTop),Offset(x,neckBottom),if(i==0)3f else 1.2f)
        }

        val names=listOf("G","C","E","A")
        ys.forEachIndexed{index,y->
            drawLine(
                if(index==0)White.copy(alpha=.46f) else White.copy(alpha=.34f),
                Offset(size.width*.035f,y),
                Offset(size.width*.965f,y),
                if(index==0)1.7f else 1.25f
            )
        }

        drawLine(
            Acid,
            Offset(playX,neckTop-size.height*.025f),
            Offset(playX,neckBottom+size.height*.025f),
            3.2f
        )
        drawCircle(Acid.copy(alpha=.17f),11f,Offset(playX,neckTop-size.height*.045f))

        val textPaint=android.graphics.Paint().apply{
            isAntiAlias=true
            textAlign=android.graphics.Paint.Align.CENTER
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        }

        textPaint.color=android.graphics.Color.rgb(221,244,90)
        textPaint.textSize=10f
        drawContext.canvas.nativeCanvas.drawText("PLAY LINE",playX,neckTop-size.height*.055f,textPaint)

        names.forEachIndexed{index,name->
            textPaint.color=android.graphics.Color.rgb(246,247,243)
            textPaint.textSize=13f
            drawContext.canvas.nativeCanvas.drawText(name,size.width*.018f,ys[index]+4f,textPaint)
        }

        val centerStep=floor(rawStep).toInt()
        for(eventStep in (centerStep-2)..(centerStep+9)){
            if(eventStep<0)continue

            val mappedStep:Int
            val eventChord:Int
            if(loopPattern){
                mappedStep=eventStep.mod(Pattern.size)
                eventChord=chordIndex
            }else{
                if(eventStep>=FirstSong.size*Pattern.size)continue
                mappedStep=eventStep.mod(Pattern.size)
                eventChord=eventStep/Pattern.size
            }

            val event=Pattern[mappedStep]
            val x=playX+((eventStep-rawStep)*eventSpacing).toFloat()
            if(x < -capsuleW || x > size.width+capsuleW)continue
            val y=ys[event.stringIndex]
            val isNear=kotlin.math.abs(x-playX)<eventSpacing*.22f

            drawRoundRect(
                color=if(isNear)Acid else if(eventChord==chordIndex)Mint else Color(0xFF59625D),
                topLeft=Offset(x-capsuleW/2f,y-capsuleH/2f),
                size=Size(capsuleW,capsuleH),
                cornerRadius=CornerRadius(capsuleH/2f,capsuleH/2f)
            )

            if(isNear){
                drawRoundRect(
                    color=Acid.copy(alpha=.14f),
                    topLeft=Offset(x-capsuleW*.68f,y-capsuleH*.75f),
                    size=Size(capsuleW*1.36f,capsuleH*1.5f),
                    cornerRadius=CornerRadius(capsuleH,capsuleH)
                )
            }

            textPaint.color=android.graphics.Color.rgb(7,9,8)
            textPaint.textSize=12f
            drawContext.canvas.nativeCanvas.drawText(event.finger,x,y+4f,textPaint)

            textPaint.color=android.graphics.Color.rgb(133,143,137)
            textPaint.textSize=7f
            drawContext.canvas.nativeCanvas.drawText(FirstSong[eventChord].name,x,neckTop-5f,textPaint)
        }

        textPaint.color=android.graphics.Color.rgb(133,143,137)
        textPaint.textSize=8f
        drawContext.canvas.nativeCanvas.drawText(
            "events move ←  ·  pluck when the capsule crosses the line",
            size.width*.62f,
            size.height*.965f,
            textPaint
        )
    }
}

@Composable
private fun SpeedSelector(speed:Float,onSpeed:(Float)->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){
        listOf(.5f,.75f,1f,1.25f).forEach{value->
            val selected=kotlin.math.abs(speed-value)<.01f
            Text(
                when(value){
                    .5f->".50×"
                    .75f->".75×"
                    1f->"1×"
                    else->"1.25×"
                },
                color=if(selected)Night else Fog,
                fontSize=7.sp,
                fontWeight=FontWeight.Bold,
                modifier=Modifier
                    .background(if(selected)Acid else Glass2,RoundedCornerShape(8.dp))
                    .clickable{onSpeed(value)}
                    .padding(horizontal=7.dp,vertical=6.dp)
            )
        }
    }
}

@Composable
private fun RunnerHelp(onDismiss:()->Unit){
    Box(
        Modifier.fillMaxSize().background(Color(0xD9070908)).clickable(onClick=onDismiss),
        contentAlignment=Alignment.Center
    ){
        Column(
            Modifier.width(560.dp)
                .background(Color(0xFF111512),RoundedCornerShape(26.dp))
                .border(1.dp,Line,RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ){
            Text("FINGERSTYLE RUNNER",color=Acid,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp)
            Spacer(Modifier.height(9.dp))
            Text("Read ahead. Hit the line.",color=White,fontSize=24.sp,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            Text(
                "P = polegar   ·   I = indicador   ·   M = médio   ·   A = anelar",
                color=Mint,fontSize=10.sp,fontWeight=FontWeight.Bold
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "The capsules travel from right to left on the ukulele strings. Pluck the indicated string with the indicated finger exactly when the capsule crosses the yellow PLAY LINE.",
                color=Fog,fontSize=10.sp,textAlign=TextAlign.Center,lineHeight=14.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Start at 0.50×. When the pattern feels automatic, move to 0.75× and then 1×.",
                color=White,fontSize=9.sp,textAlign=TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text("TAP ANYWHERE TO START",color=Acid,fontSize=8.sp,fontWeight=FontWeight.Bold)
        }
    }
}

private class FingerstyleAudioEngine{
    private var track:AudioTrack?=null

    fun playLesson(
        chords:List<LessonChord>,
        speed:Float,
        startMs:Long,
        loop:Boolean
    ){
        stop()

        val sampleRate=44_100
        val stepSeconds=(60.0/BaseBpm/2.0)/speed
        val totalSteps=chords.size*Pattern.size
        val totalSamples=(totalSteps*stepSeconds*sampleRate).toInt().coerceAtLeast(1)
        val pcm=ShortArray(totalSamples)
        val openFrequencies=doubleArrayOf(392.00,261.63,329.63,440.00)
        val pluckSamples=(.20*sampleRate).toInt()

        for(step in 0 until totalSteps){
            val chord=chords[step/Pattern.size]
            val event=Pattern[step.mod(Pattern.size)]
            val fret=chord.frets[event.stringIndex]
            val frequency=openFrequencies[event.stringIndex]*2.0.pow(fret/12.0)
            val start=(step*stepSeconds*sampleRate).toInt()

            for(i in 0 until pluckSamples){
                val index=start+i
                if(index>=pcm.size)break
                val t=i.toDouble()/sampleRate
                val env=exp(-15.0*t)
                val sample=(
                    sin(2.0*PI*frequency*t)+
                    .32*sin(4.0*PI*frequency*t)+
                    .12*sin(6.0*PI*frequency*t)
                )*env
                val value=(sample*7600.0).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt())
                pcm[index]=(pcm[index]+value)
                    .coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }

        val audioTrack=AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size*2)
            .build()

        audioTrack.write(pcm,0,pcm.size)
        audioTrack.setVolume(.52f)

        if(loop && pcm.size>1){
            runCatching{audioTrack.setLoopPoints(0,pcm.size,-1)}
        }else{
            val startFrame=((startMs/1000.0)*sampleRate).toInt().coerceIn(0,(pcm.size-1).coerceAtLeast(0))
            runCatching{audioTrack.setPlaybackHeadPosition(startFrame)}
        }

        audioTrack.play()
        track=audioTrack
    }

    fun stop(){
        runCatching{track?.stop()}
        track?.release()
        track=null
    }
}
