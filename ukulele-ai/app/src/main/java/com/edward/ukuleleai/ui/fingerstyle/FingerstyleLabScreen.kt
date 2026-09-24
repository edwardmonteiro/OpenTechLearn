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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

private data class FingerStep(val stringIndex:Int,val stringName:String,val finger:String)
private data class LessonChord(val name:String,val latin:String,val frets:IntArray)

private val Pattern=listOf(
    FingerStep(0,"G","P"),
    FingerStep(1,"C","I"),
    FingerStep(2,"E","M"),
    FingerStep(3,"A","A"),
    FingerStep(2,"E","M"),
    FingerStep(1,"C","I"),
    FingerStep(0,"G","P"),
    FingerStep(1,"C","I")
)

private val FirstSong=listOf(
    LessonChord("C","Dó",intArrayOf(0,0,0,3)),
    LessonChord("Am","Lá menor",intArrayOf(2,0,0,0)),
    LessonChord("F","Fá",intArrayOf(2,0,1,0)),
    LessonChord("G","Sol",intArrayOf(0,2,3,2))
)

@Composable
fun FingerstyleLabScreen(onBack:()->Unit){
    var bpm by remember{mutableIntStateOf(72)}
    var playing by remember{mutableStateOf(false)}
    var elapsedMs by remember{mutableLongStateOf(0L)}
    var startedAt by remember{mutableLongStateOf(0L)}
    var showHelp by remember{mutableStateOf(true)}
    val engine=remember{FingerstyleAudioEngine()}

    val stepMs=60_000.0/bpm/2.0
    val totalSteps=FirstSong.size*Pattern.size
    val totalMs=(stepMs*totalSteps).toLong()

    DisposableEffect(Unit){onDispose{engine.stop()}}

    LaunchedEffect(playing,bpm){
        if(playing){
            startedAt=SystemClock.elapsedRealtime()-elapsedMs
            engine.playLesson(bpm)
            while(playing){
                elapsedMs=(SystemClock.elapsedRealtime()-startedAt).coerceAtMost(totalMs)
                if(elapsedMs>=totalMs){
                    playing=false
                    engine.stop()
                    break
                }
                delay(16)
            }
        }else{
            engine.stop()
        }
    }

    val rawStep=(elapsedMs/stepMs).coerceIn(0.0,(totalSteps-1).toDouble())
    val absoluteStep=floor(rawStep).toInt()
    val stepIndex=absoluteStep.mod(Pattern.size)
    val chordIndex=(absoluteStep/Pattern.size).coerceIn(0,FirstSong.lastIndex)
    val progress=(rawStep-floor(rawStep)).toFloat().coerceIn(0f,1f)
    val chord=FirstSong[chordIndex]
    val nextChord=FirstSong.getOrNull(chordIndex+1)
    val currentStep=Pattern[stepIndex]
    val nextStep=Pattern[(stepIndex+1).mod(Pattern.size)]

    Box(
        Modifier.fillMaxSize().background(Night).windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Box(
                    Modifier.size(36.dp).background(Glass2,CircleShape).clickable(onClick=onBack),
                    contentAlignment=Alignment.Center
                ){Text("‹",color=White,fontSize=24.sp)}
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)){
                    Text("Fingerstyle Lab",color=White,fontSize=21.sp,fontWeight=FontWeight.SemiBold)
                    Text("Lesson 1 · First Arpeggio",color=Fog,fontSize=9.sp)
                }
                Text(
                    bpm.toString()+" BPM",
                    color=Acid,fontSize=9.sp,fontWeight=FontWeight.Bold,
                    modifier=Modifier.background(Glass2,RoundedCornerShape(12.dp)).padding(horizontal=10.dp,vertical=7.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Column(
                    Modifier.weight(.72f).fillMaxHeight()
                        .background(Glass,RoundedCornerShape(20.dp))
                        .border(1.dp,Line,RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ){
                    Text("RIGHT HAND",color=Mint,fontSize=8.sp,letterSpacing=1.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    HandRow("P","Polegar","G")
                    HandRow("I","Indicador","C")
                    HandRow("M","Médio","E")
                    HandRow("A","Anelar","A")

                    Spacer(Modifier.height(12.dp))
                    Text("FIRST SONG",color=Acid,fontSize=8.sp,letterSpacing=1.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    FirstSong.forEachIndexed{index,item->
                        val active=index==chordIndex
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if(active)Glass2 else Color.Transparent,RoundedCornerShape(9.dp))
                                .padding(horizontal=7.dp,vertical=5.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Text(
                                (index+1).toString(),
                                color=if(active)Acid else Fog,fontSize=7.sp,
                                modifier=Modifier.width(18.dp)
                            )
                            Text(item.name,color=if(active)White else Fog,fontSize=12.sp,fontWeight=FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(item.latin,color=Fog,fontSize=7.sp)
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Text(
                        "The ball lands when you pluck. Follow the finger letter and string together.",
                        color=Fog,fontSize=8.sp,lineHeight=11.sp
                    )
                }

                Column(
                    Modifier.weight(1.8f).fillMaxHeight()
                        .background(Glass,RoundedCornerShape(20.dp))
                        .border(1.dp,Line,RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column{
                            Text("NOW",color=Acid,fontSize=7.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                            Row(verticalAlignment=Alignment.Bottom){
                                Text(chord.name,color=White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                                Spacer(Modifier.width(7.dp))
                                Text(chord.latin,color=Fog,fontSize=9.sp,modifier=Modifier.padding(bottom=5.dp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment=Alignment.End){
                            Text("NEXT",color=Fog,fontSize=6.sp,fontWeight=FontWeight.Bold)
                            Text(nextChord?.name?:"FINISH",color=if(nextChord!=null)Mint else Fog,fontSize=15.sp,fontWeight=FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    BounceStage(
                        current=currentStep,
                        next=nextStep,
                        progress=progress,
                        activeIndex=stepIndex,
                        modifier=Modifier.fillMaxWidth().weight(1f)
                    )

                    Spacer(Modifier.height(6.dp))
                    PatternStrip(activeIndex=stepIndex)
                }

                Column(
                    Modifier.weight(.62f).fillMaxHeight()
                        .background(Glass,RoundedCornerShape(20.dp))
                        .border(1.dp,Line,RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    Text("WATCH",color=Mint,fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        currentStep.finger,
                        color=Acid,fontSize=38.sp,fontWeight=FontWeight.ExtraBold
                    )
                    Text(currentStep.fingerName(),color=White,fontSize=9.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text("string "+currentStep.stringName,color=Fog,fontSize=8.sp)

                    Spacer(Modifier.weight(1f))

                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        OutlinedButton(
                            onClick={
                                playing=false
                                elapsedMs=0L
                            },
                            modifier=Modifier.size(38.dp),
                            contentPadding=PaddingValues(0.dp),
                            shape=CircleShape
                        ){Text("↺",fontSize=14.sp)}
                        Button(
                            onClick={
                                if(elapsedMs>=totalMs)elapsedMs=0L
                                playing=!playing
                            },
                            modifier=Modifier.width(84.dp).height(38.dp),
                            shape=RoundedCornerShape(19.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night)
                        ){Text(if(playing)"PAUSE" else "▶ WATCH",fontSize=9.sp,fontWeight=FontWeight.Bold)}
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        SpeedPill("SLOW",60,bpm){playing=false;bpm=60;elapsedMs=0L}
                        SpeedPill("NORMAL",72,bpm){playing=false;bpm=72;elapsedMs=0L}
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Soft reference plucks are generated locally.",
                        color=Fog,fontSize=6.sp,textAlign=TextAlign.Center,lineHeight=8.sp
                    )
                }
            }
        }

        if(showHelp){
            Box(
                Modifier.fillMaxSize().background(Color(0xD9070908)).clickable{showHelp=false},
                contentAlignment=Alignment.Center
            ){
                Column(
                    Modifier.width(520.dp)
                        .background(Color(0xFF111512),RoundedCornerShape(24.dp))
                        .border(1.dp,Line,RoundedCornerShape(24.dp))
                        .padding(22.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    Text("FINGERSTYLE, SIMPLY",color=Acid,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("One finger. One string. One landing.",color=White,fontSize=23.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "P = polegar · I = indicador · M = médio · A = anelar",
                        color=Mint,fontSize=10.sp,fontWeight=FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Press WATCH. Every time the ball lands on a string, pluck that string with the finger shown inside the ball.",
                        color=Fog,fontSize=10.sp,textAlign=TextAlign.Center,lineHeight=14.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("TAP ANYWHERE TO START",color=Acid,fontSize=8.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

private fun FingerStep.fingerName():String=when(finger){
    "P"->"Polegar"
    "I"->"Indicador"
    "M"->"Médio"
    else->"Anelar"
}

@Composable
private fun HandRow(letter:String,name:String,string:String){
    Row(
        Modifier.fillMaxWidth().padding(vertical=3.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Box(
            Modifier.size(25.dp).background(Glass2,CircleShape),
            contentAlignment=Alignment.Center
        ){Text(letter,color=Acid,fontSize=10.sp,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)){
            Text(name,color=White,fontSize=9.sp,fontWeight=FontWeight.Medium)
            Text("normally starts on "+string,color=Fog,fontSize=6.sp)
        }
    }
}

@Composable
private fun PatternStrip(activeIndex:Int){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){
        Pattern.forEachIndexed{index,step->
            val active=index==activeIndex
            Column(
                Modifier.weight(1f)
                    .background(if(active)Acid else Glass2,RoundedCornerShape(8.dp))
                    .padding(vertical=4.dp),
                horizontalAlignment=Alignment.CenterHorizontally
            ){
                Text(step.finger,color=if(active)Night else White,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Text(step.stringName,color=if(active)Night.copy(alpha=.65f) else Fog,fontSize=6.sp)
            }
        }
    }
}

@Composable
private fun BounceStage(
    current:FingerStep,
    next:FingerStep,
    progress:Float,
    activeIndex:Int,
    modifier:Modifier=Modifier
){
    Canvas(modifier){
        val xs=listOf(size.width*.08f,size.width*.36f,size.width*.64f,size.width*.92f)
        val baseY=size.height*.76f
        val arcTop=size.height*.16f
        val names=listOf("G","C","E","A")

        xs.forEachIndexed{index,x->
            drawLine(Line,Offset(x,size.height*.23f),Offset(x,baseY),1.2f)
            val active=index==current.stringIndex
            drawCircle(if(active)Mint else Glass2,if(active)10f else 7f,Offset(x,baseY))
            drawCircle(if(active)Mint.copy(alpha=.18f) else Line.copy(alpha=.25f),if(active)20f else 13f,Offset(x,baseY))
        }

        val currentX=xs[current.stringIndex]
        val nextX=xs[next.stringIndex]
        val controlX=(currentX+nextX)/2f
        val controlY=arcTop

        val arc=Path().apply{
            moveTo(currentX,baseY)
            quadraticBezierTo(controlX,controlY,nextX,baseY)
        }
        drawPath(arc,Acid.copy(alpha=.34f),style=Stroke(width=2.5f))

        val one=1f-progress
        val ballX=one*one*currentX+2f*one*progress*controlX+progress*progress*nextX
        val ballY=one*one*baseY+2f*one*progress*controlY+progress*progress*baseY
        drawCircle(Acid.copy(alpha=.14f),18f,Offset(ballX,ballY))
        drawCircle(Acid,10f,Offset(ballX,ballY))

        val paint=android.graphics.Paint().apply{
            isAntiAlias=true
            textAlign=android.graphics.Paint.Align.CENTER
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        }
        paint.color=android.graphics.Color.rgb(7,9,8)
        paint.textSize=11f
        drawContext.canvas.nativeCanvas.drawText(current.finger,ballX,ballY+4f,paint)

        names.forEachIndexed{index,name->
            paint.color=android.graphics.Color.rgb(246,247,243)
            paint.textSize=14f
            drawContext.canvas.nativeCanvas.drawText(name,xs[index],size.height-7f,paint)
        }

        paint.color=android.graphics.Color.rgb(133,143,137)
        paint.textSize=9f
        drawContext.canvas.nativeCanvas.drawText("LAND = PLUCK",size.width/2f,size.height*.10f,paint)
    }
}

@Composable
private fun SpeedPill(label:String,value:Int,current:Int,onClick:()->Unit){
    val selected=value==current
    Text(
        label,
        color=if(selected)Night else Fog,
        fontSize=6.sp,fontWeight=FontWeight.Bold,
        modifier=Modifier
            .background(if(selected)Acid else Glass2,RoundedCornerShape(8.dp))
            .clickable(onClick=onClick)
            .padding(horizontal=7.dp,vertical=5.dp)
    )
}

private class FingerstyleAudioEngine{
    private var track:AudioTrack?=null

    fun playLesson(bpm:Int){
        stop()
        val sampleRate=44_100
        val stepSeconds=60.0/bpm/2.0
        val totalSteps=FirstSong.size*Pattern.size
        val totalSamples=(totalSteps*stepSeconds*sampleRate).toInt()
        val pcm=ShortArray(totalSamples)
        val openFrequencies=doubleArrayOf(392.00,261.63,329.63,440.00)
        val pluckSeconds=.20
        val pluckSamples=(pluckSeconds*sampleRate).toInt()

        for(step in 0 until totalSteps){
            val chord=FirstSong[step/Pattern.size]
            val fingerStep=Pattern[step.mod(Pattern.size)]
            val fret=chord.frets[fingerStep.stringIndex]
            val frequency=openFrequencies[fingerStep.stringIndex]*2.0.pow(fret/12.0)
            val start=(step*stepSeconds*sampleRate).toInt()
            for(i in 0 until pluckSamples){
                val index=start+i
                if(index>=pcm.size)break
                val t=i.toDouble()/sampleRate
                val env=exp(-15.0*t)
                val sample=(sin(2.0*PI*frequency*t)+.32*sin(4.0*PI*frequency*t)+.12*sin(6.0*PI*frequency*t))*env
                val value=(sample*7800.0).toInt().coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt())
                pcm[index]=(pcm[index]+value).coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt()).toShort()
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
        audioTrack.setVolume(.55f)
        audioTrack.play()
        track=audioTrack
    }

    fun stop(){
        runCatching{track?.stop()}
        track?.release()
        track=null
    }
}
