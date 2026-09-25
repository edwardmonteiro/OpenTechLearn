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

private val Thumb=Color(0xFF64CFF4)
private val Index=Color(0xFFFF6B6B)
private val Middle=Color(0xFF70E0B6)
private val Ring=Color(0xFFFFC857)

private val LeftIndex=Color(0xFF64CFF4)
private val LeftMiddle=Color(0xFFFF6B6B)
private val LeftRing=Color(0xFF70E0B6)
private val LeftPinky=Color(0xFFFFC857)
private val CChordFrets=intArrayOf(0,0,0,3)

private const val BaseBpm=72

private data class FingerEvent(
    val stringIndex:Int,
    val stringName:String,
    val finger:String,
    val fingerName:String,
    val color:Color
)

private val LessonOne=listOf(
    FingerEvent(0,"G","P","thumb",Thumb),
    FingerEvent(1,"C","I","index",Index),
    FingerEvent(2,"E","M","middle",Middle),
    FingerEvent(3,"A","A","ring",Ring),
    FingerEvent(2,"E","M","middle",Middle),
    FingerEvent(1,"C","I","index",Index)
)

@Composable
fun FingerstyleLabScreen(onBack:()->Unit){
    var speed by remember{mutableFloatStateOf(.75f)}
    var playing by remember{mutableStateOf(false)}
    var elapsedMs by remember{mutableLongStateOf(0L)}
    var startedAt by remember{mutableLongStateOf(0L)}
    var loop by remember{mutableStateOf(true)}
    val engine=remember{OneChordLessonAudio()}

    val stepMs=(60_000.0/BaseBpm/2.0)/speed
    val totalMs=(stepMs*LessonOne.size).toLong().coerceAtLeast(1L)
    val rawStep=(elapsedMs/stepMs).coerceAtLeast(0.0)

    DisposableEffect(Unit){onDispose{engine.stop()}}

    LaunchedEffect(playing,speed,loop){
        if(playing){
            startedAt=SystemClock.elapsedRealtime()-elapsedMs
            engine.play(speed=speed,startMs=elapsedMs,loop=loop)
            while(playing){
                val now=SystemClock.elapsedRealtime()-startedAt
                if(loop){
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

    val activeIndex=floor(rawStep).toInt().mod(LessonOne.size)

    Box(
        Modifier.fillMaxSize().background(Night).windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(
            Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=12.dp)
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
                    Text("Fingerstyle Basics",color=White,fontSize=21.sp,fontWeight=FontWeight.SemiBold)
                    Text("Lesson 2 · hold C chord",color=Mint,fontSize=9.sp,fontWeight=FontWeight.Bold)
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "PLAY WHEN THE BALL HITS THE LINE",
                    color=Fog,fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=.6.sp
                )

                Spacer(Modifier.width(18.dp))

                Text(
                    BaseBpm.toString()+" BPM · "+String.format("%.2f×",speed),
                    color=White,fontSize=8.sp,fontWeight=FontWeight.Bold,
                    modifier=Modifier.background(Glass2,RoundedCornerShape(12.dp)).padding(horizontal=10.dp,vertical=7.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            CompactChordGuide(
                modifier=Modifier.fillMaxWidth().height(82.dp)
            )

            Spacer(Modifier.height(8.dp))

            SimpleRunner(
                rawStep=rawStep,
                loop=loop,
                modifier=Modifier.fillMaxWidth().weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().height(62.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                FingerLegend(
                    activeIndex=activeIndex,
                    modifier=Modifier.weight(1f).fillMaxHeight()
                )

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
                            if(!loop && elapsedMs>=totalMs)elapsedMs=0L
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
                        if(loop)"LOOP ON" else "LOOP",
                        color=if(loop)Night else Mint,
                        fontSize=7.sp,
                        fontWeight=FontWeight.Bold,
                        modifier=Modifier
                            .background(if(loop)Mint else Glass2,RoundedCornerShape(10.dp))
                            .clickable{
                                playing=false
                                elapsedMs=0L
                                loop=!loop
                            }
                            .padding(horizontal=10.dp,vertical=7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactChordGuide(
    modifier:Modifier=Modifier
){
    Row(
        modifier.background(Glass,RoundedCornerShape(18.dp))
            .border(1.dp,Line,RoundedCornerShape(18.dp))
            .padding(horizontal=14.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Column(Modifier.width(88.dp)){
            Text("LEFT HAND",color=Fog,fontSize=6.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)
            Text("HOLD C",color=Acid,fontSize=20.sp,fontWeight=FontWeight.ExtraBold)
            Text("Dó",color=Fog,fontSize=7.sp)
        }

        Canvas(
            Modifier.width(180.dp).fillMaxHeight()
        ){
            val left=size.width*.22f
            val right=size.width*.90f
            val top=size.height*.25f
            val bottom=size.height*.92f
            val stringGap=(right-left)/3f
            val fretGap=(bottom-top)/3f

            val paint=android.graphics.Paint().apply{
                isAntiAlias=true
                textAlign=android.graphics.Paint.Align.CENTER
                typeface=android.graphics.Typeface.DEFAULT_BOLD
            }

            listOf("G","C","E","A").forEachIndexed{i,label->
                val x=left+i*stringGap
                paint.color=android.graphics.Color.rgb(246,247,243)
                paint.textSize=11f
                drawContext.canvas.nativeCanvas.drawText(label,x,11f,paint)
            }

            for(i in 0..3){
                val x=left+i*stringGap
                drawLine(White.copy(alpha=.72f),Offset(x,top),Offset(x,bottom),1.5f)
            }
            for(fret in 0..3){
                val y=top+fret*fretGap
                drawLine(
                    if(fret==0)White else Fog.copy(alpha=.42f),
                    Offset(left,y),Offset(right,y),
                    if(fret==0)3.2f else 1.2f
                )
            }

            // Open strings G, C, E.
            for(i in 0..2){
                val x=left+i*stringGap
                drawCircle(
                    Mint,6.5f,Offset(x,top-8f),
                    style=Stroke(width=1.8f)
                )
            }

            // C chord: A string, fret 3. Number = fret, color = left ring finger.
            val dotX=left+3f*stringGap
            val dotY=top+2.5f*fretGap
            drawCircle(LeftRing,12f,Offset(dotX,dotY))

            paint.color=android.graphics.Color.rgb(7,9,8)
            paint.textSize=11f
            drawContext.canvas.nativeCanvas.drawText("3",dotX,dotY+4f,paint)

            for(fret in 1..3){
                val y=top+(fret-.5f)*fretGap
                paint.color=android.graphics.Color.rgb(133,143,137)
                paint.textSize=8f
                drawContext.canvas.nativeCanvas.drawText(fret.toString(),size.width*.08f,y+3f,paint)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column{
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(11.dp).background(LeftRing,CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("A string · fret 3",color=White,fontSize=8.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Text("3 = fret",color=Fog,fontSize=6.sp)
            Text("green = ring finger",color=Fog,fontSize=6.sp)
        }
    }
}

@Composable
private fun FingerLegend(
    activeIndex:Int,
    modifier:Modifier=Modifier
){
    Row(
        modifier.background(Glass,RoundedCornerShape(18.dp))
            .border(1.dp,Line,RoundedCornerShape(18.dp))
            .padding(horizontal=12.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Text("RIGHT HAND",color=Fog,fontSize=7.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
        Spacer(Modifier.width(12.dp))

        LessonOne.forEachIndexed{index,event->
            val active=index==activeIndex
            Row(
                Modifier.weight(1f)
                    .background(if(active)event.color.copy(alpha=.18f)else Color.Transparent,RoundedCornerShape(9.dp))
                    .padding(vertical=5.dp,horizontal=4.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.Center
            ){
                Box(
                    Modifier.size(if(active)24.dp else 20.dp).background(event.color,CircleShape),
                    contentAlignment=Alignment.Center
                ){
                    Text(event.finger,color=Night,fontSize=8.sp,fontWeight=FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(5.dp))
                Text(event.fingerName,color=if(active)White else Fog,fontSize=6.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SimpleRunner(
    rawStep:Double,
    loop:Boolean,
    modifier:Modifier=Modifier
){
    Canvas(
        modifier.background(Glass,RoundedCornerShape(24.dp))
            .border(1.dp,Line,RoundedCornerShape(24.dp))
            .padding(12.dp)
    ){
        val playX=size.width*.20f
        val top=size.height*.13f
        val bottom=size.height*.88f
        val laneGap=(bottom-top)/3f
        val ys=listOf(top,top+laneGap,top+laneGap*2f,bottom)
        val spacing=size.width*.145f
        val ballRadius=minOf(size.width*.026f,laneGap*.22f)

        val paint=android.graphics.Paint().apply{
            isAntiAlias=true
            textAlign=android.graphics.Paint.Align.CENTER
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        }

        drawRoundRect(
            color=Color(0xFF111512),
            topLeft=Offset(size.width*.04f,top-laneGap*.32f),
            size=androidx.compose.ui.geometry.Size(size.width*.92f,(bottom-top)+laneGap*.64f),
            cornerRadius=androidx.compose.ui.geometry.CornerRadius(22f,22f)
        )

        val stringNames=listOf("G","C","E","A")
        ys.forEachIndexed{index,y->
            drawLine(
                White.copy(alpha=.42f),
                Offset(size.width*.05f,y),
                Offset(size.width*.96f,y),
                if(index==0)1.8f else 1.35f
            )
            paint.color=android.graphics.Color.rgb(246,247,243)
            paint.textSize=14f
            drawContext.canvas.nativeCanvas.drawText(stringNames[index],size.width*.022f,y+5f,paint)
        }

        drawLine(
            Acid,
            Offset(playX,top-laneGap*.30f),
            Offset(playX,bottom+laneGap*.30f),
            3.4f
        )

        paint.color=android.graphics.Color.rgb(221,244,90)
        paint.textSize=10f
        drawContext.canvas.nativeCanvas.drawText("PLAY",playX,top-laneGap*.39f,paint)

        fun eventFor(step:Int):FingerEvent?{
            if(step<0)return null
            return if(loop) LessonOne[step.mod(LessonOne.size)]
            else LessonOne.getOrNull(step)
        }

        val center=floor(rawStep).toInt().coerceAtLeast(0)
        val phase=(rawStep-floor(rawStep)).toFloat().coerceIn(0f,1f)

        // Moving colored note balls. Position already tells the string.
        for(step in (center-1)..(center+7)){
            val event=eventFor(step)?:continue
            val x=playX+((step-rawStep)*spacing).toFloat()
            if(x < -ballRadius*2f || x > size.width+ballRadius*2f)continue
            val y=ys[event.stringIndex]
            val near=kotlin.math.abs(x-playX)<spacing*.16f

            if(near){
                drawCircle(event.color.copy(alpha=.16f),ballRadius*1.75f,Offset(x,y))
            }
            drawCircle(event.color,ballRadius,Offset(x,y))

            paint.color=android.graphics.Color.rgb(7,9,8)
            paint.textSize=ballRadius*.92f
            drawContext.canvas.nativeCanvas.drawText(event.finger,x,y+paint.textSize*.34f,paint)
        }

        // Convex guide arc between current and next string.
        val current=eventFor(center)
        val next=eventFor(center+1)
        if(current!=null && next!=null){
            val currentX=playX+((center-rawStep)*spacing).toFloat()
            val nextX=playX+(((center+1)-rawStep)*spacing).toFloat()
            val currentY=ys[current.stringIndex]
            val nextY=ys[next.stringIndex]
            val controlX=(currentX+nextX)/2f
            val controlY=(minOf(currentY,nextY)-laneGap*.72f).coerceAtLeast(top-laneGap*.12f)

            val arc=Path().apply{
                moveTo(currentX,currentY)
                quadraticBezierTo(controlX,controlY,nextX,nextY)
            }
            drawPath(arc,Acid.copy(alpha=.32f),style=Stroke(width=2.4f))

            val one=1f-phase
            val guideX=one*one*currentX+2f*one*phase*controlX+phase*phase*nextX
            val guideY=one*one*currentY+2f*one*phase*controlY+phase*phase*nextY

            drawCircle(Acid.copy(alpha=.15f),ballRadius*.95f,Offset(guideX,guideY))
            drawCircle(Acid,ballRadius*.48f,Offset(guideX,guideY))
        }

        paint.color=android.graphics.Color.rgb(133,143,137)
        paint.textSize=8f
        drawContext.canvas.nativeCanvas.drawText(
            "HOLD C   ·   G → C → E → A → E → C",
            size.width*.60f,
            size.height*.965f,
            paint
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

private class OneChordLessonAudio{
    private var track:AudioTrack?=null

    fun play(speed:Float,startMs:Long,loop:Boolean){
        stop()

        val sampleRate=44_100
        val stepSeconds=(60.0/BaseBpm/2.0)/speed
        val totalSamples=(LessonOne.size*stepSeconds*sampleRate).toInt().coerceAtLeast(1)
        val pcm=ShortArray(totalSamples)
        val frequencies=doubleArrayOf(392.00,261.63,329.63,440.00)
        val pluckSamples=(.20*sampleRate).toInt()

        LessonOne.forEachIndexed{step,event->
            val fret=CChordFrets[event.stringIndex]
            val frequency=frequencies[event.stringIndex]*2.0.pow(fret/12.0)
            val start=(step*stepSeconds*sampleRate).toInt()

            for(i in 0 until pluckSamples){
                val index=start+i
                if(index>=pcm.size)break
                val t=i.toDouble()/sampleRate
                val env=exp(-15.0*t)
                val sample=(
                    sin(2.0*PI*frequency*t)+
                    .30*sin(4.0*PI*frequency*t)+
                    .10*sin(6.0*PI*frequency*t)
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
