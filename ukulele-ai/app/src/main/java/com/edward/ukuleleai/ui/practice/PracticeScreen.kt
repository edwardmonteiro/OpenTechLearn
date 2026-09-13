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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.*
import kotlin.math.abs
import kotlin.math.floor

private val Night=Color(0xFF070908); private val Glass=Color(0xFF101412); private val Glass2=Color(0xFF171C19)
private val Line=Color(0xFF262D29); private val White=Color(0xFFF6F7F3); private val Fog=Color(0xFF858F89)
private val Acid=Color(0xFFDDF45A); private val Mint=Color(0xFF70E0B6); private val Blue=Color(0xFF64CFF4)
private val Rose=Color(0xFFFF79B5); private val Gold=Color(0xFFFFB15B)

@Composable fun PracticeRoute(song:Song,onExit:()->Unit,viewModel:PracticeViewModel=viewModel()){
 val s by viewModel.state.collectAsState(); val ctx=LocalContext.current
 val ask=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)viewModel.startListening()}
 LaunchedEffect(song.id){viewModel.loadSong(song)}
 LaunchedEffect(s.beatPulse){if(s.beatPulse>0&&s.listeningEnabled)vibrateBeat(ctx)}
 val listen={if(s.listeningEnabled)viewModel.stopListening() else if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)viewModel.startListening() else ask.launch(Manifest.permission.RECORD_AUDIO)}
 Studio(s,{viewModel.exit(onExit)},viewModel::togglePlayback,viewModel::restart,viewModel::changeTempo,viewModel::setDifficulty,viewModel::toggleSound,listen,viewModel::clearMelodyTrail)
}

@Composable private fun Studio(s:PracticeState,exit:()->Unit,play:()->Unit,restart:()->Unit,tempo:(Int)->Unit,diff:(DifficultyLevel)->Unit,sound:()->Unit,listen:()->Unit,clear:()->Unit){
 Column(Modifier.fillMaxSize().background(Night).padding(18.dp,14.dp)){
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){
   Row(verticalAlignment=Alignment.CenterVertically){CircleAction("‹",exit);Spacer(Modifier.width(13.dp));Column{Text(s.song.title,color=White,fontSize=21.sp,fontWeight=FontWeight.Medium);Text("${s.bpm} BPM  ·  Level ${s.difficulty.level}",color=Fog,fontSize=10.sp)}}
   Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically){
    TinyStatus(if(s.listeningEnabled)"SAFE LISTEN" else "CHORD",if(s.listeningEnabled)"speaker silent" else currentChord(s)?:"—",if(s.listeningEnabled)Mint else Acid)
    Pill(if(s.listeningEnabled)"● Listening" else "Listen",s.listeningEnabled,listen,true)
    Pill(if(s.listeningEnabled)"Silent" else if(s.soundEnabled)"Click on" else "Click off",s.soundEnabled&&!s.listeningEnabled,sound,!s.listeningEnabled)
   }
  }
  Spacer(Modifier.height(10.dp))
  Box(Modifier.weight(1f).fillMaxWidth().background(Glass,RoundedCornerShape(30.dp)).border(1.dp,Line,RoundedCornerShape(30.dp))){
   PerformanceCanvas(s,Modifier.fillMaxSize())
   if(s.listeningEnabled) Row(Modifier.align(Alignment.TopStart).padding(18.dp).background(Color(0xCC171C19),RoundedCornerShape(18.dp)).padding(13.dp,9.dp),verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(7.dp).background(Mint,CircleShape));Spacer(Modifier.width(8.dp));Text(s.detectedNote?:"Play a single note",color=if(s.detectedNote!=null)White else Fog,fontSize=12.sp)
    s.detectedCents?.let{Spacer(Modifier.width(8.dp));Text(if(abs(it)<=6)"in tune" else "${if(it>0)"+" else ""}$it¢",color=if(abs(it)<=6)Mint else Fog,fontSize=10.sp)}
   }
   s.countdown?.let{Box(Modifier.fillMaxSize().background(Color(0xB8070908)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(it.toString(),color=White,fontSize=92.sp,fontWeight=FontWeight.ExtraLight);Text(if(s.listeningEnabled)"FEEL THE PULSE" else "COUNT IN",color=Fog,fontSize=9.sp,letterSpacing=2.sp)}}}
  }
  Spacer(Modifier.height(10.dp));Dock(s,play,restart,tempo,diff,clear)
 }
}

@Composable private fun Dock(s:PracticeState,play:()->Unit,restart:()->Unit,tempo:(Int)->Unit,diff:(DifficultyLevel)->Unit,clear:()->Unit){
 Row(Modifier.fillMaxWidth().background(Glass,RoundedCornerShape(24.dp)).border(1.dp,Line,RoundedCornerShape(24.dp)).padding(9.dp),Arrangement.SpaceBetween,Alignment.CenterVertically){
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){Ghost("−5"){tempo(-5)};Ghost("↺",restart);Ghost("Clear",clear)}
  Button(play,shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night),contentPadding=PaddingValues(36.dp,11.dp)){Text(if(s.isPlaying||s.countdown!=null)"PAUSE" else "PLAY",fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){DifficultyLevel.entries.forEach{l->Box(Modifier.background(if(l==s.difficulty)Acid else Glass2,RoundedCornerShape(13.dp)).clickable{diff(l)}.padding(13.dp,8.dp)){Text(l.level.toString(),color=if(l==s.difficulty)Night else Fog,fontSize=11.sp,fontWeight=FontWeight.Bold)}};Ghost("+5"){tempo(5)}}
 }
}

@Composable private fun PerformanceCanvas(s:PracticeState,m:Modifier){
 val d=LocalDensity.current;val chord=remember(d){np(25.sp.value*d.density,android.graphics.Color.WHITE,true)};val tiny=remember(d){np(10.sp.value*d.density,android.graphics.Color.GRAY,false)};val note=remember(d){np(12.sp.value*d.density,android.graphics.Color.rgb(112,224,182),true)}
 val down=remember(d){np(17.sp.value*d.density,android.graphics.Color.rgb(100,207,244),true)};val up=remember(d){np(17.sp.value*d.density,android.graphics.Color.rgb(255,121,181),true)};val mute=remember(d){np(15.sp.value*d.density,android.graphics.Color.rgb(255,177,91),true)}
 Canvas(m.padding(18.dp,12.dp)){
  val px=96.dp.toPx();val playX=size.width*.30f;val mt=38.dp.toPx();val mb=size.height*.58f;val ct=size.height*.69f;val ch=(size.height-ct-20.dp.toPx()).coerceAtMost(88.dp.toPx())
  listOf(60,64,67,72,76,79,84).forEach{drawLine(Line.copy(alpha=if(it%12==0).65f else .25f),Offset(0f,midiY(it,mt,mb)),Offset(size.width,midiY(it,mt,mb)),1.dp.toPx())}
  val first=floor(s.positionBeats-playX/px).toInt()-1;val last=floor(s.positionBeats+(size.width-playX)/px).toInt()+2
  for(b in first..last)if(b>=0&&b%s.song.beatsPerBar==0){val x=playX+((b-s.positionBeats)*px).toFloat();if(x in 0f..size.width){drawLine(Line,Offset(x,15.dp.toPx()),Offset(x,size.height),1.dp.toPx());drawContext.canvas.nativeCanvas.drawText("${b/s.song.beatsPerBar+1}",x+7.dp.toPx(),12.dp.toPx(),tiny)}}
  val visible=s.melodyTrail.filter{val x=playX+((it.beat-s.positionBeats)*px).toFloat();x in -30.dp.toPx()..size.width+30.dp.toPx()}
  if(visible.isNotEmpty()){val p=Path();visible.forEachIndexed{i,n->val x=playX+((n.beat-s.positionBeats)*px).toFloat();val y=midiY(n.midi,mt,mb);if(i==0)p.moveTo(x,y)else p.lineTo(x,y)};drawPath(p,Mint.copy(alpha=.12f),style=Stroke(12.dp.toPx(),cap=StrokeCap.Round));drawPath(p,Mint,style=Stroke(2.7.dp.toPx(),cap=StrokeCap.Round))}
  s.detectedMidi?.let{val y=midiY(it,mt,mb);drawCircle(Mint.copy(alpha=.15f),16.dp.toPx(),Offset(playX,y));drawCircle(Mint,5.dp.toPx(),Offset(playX,y));s.detectedNote?.let{n->drawContext.canvas.nativeCanvas.drawText(n,playX+27.dp.toPx(),y+4.dp.toPx(),note)}}
  s.song.events.forEach{e->val x=playX+((e.beat-s.positionBeats)*px).toFloat();val w=(e.durationBeats*px).toFloat().coerceAtLeast(72.dp.toPx());if(x+w>=0&&x<=size.width){val active=s.positionBeats>=e.beat&&s.positionBeats<e.beat+e.durationBeats;drawRoundRect(if(active)Color(0xFF202822)else Color(0xFF151A17),Offset(x+4.dp.toPx(),ct),Size((w-8.dp.toPx()).coerceAtLeast(1f),ch),CornerRadius(18.dp.toPx()));if(active)drawRoundRect(Acid.copy(alpha=.65f),Offset(x+4.dp.toPx(),ct),Size((w-8.dp.toPx()).coerceAtLeast(1f),ch),CornerRadius(18.dp.toPx()),style=Stroke(1.7.dp.toPx()));val cx=x+w/2;drawContext.canvas.nativeCanvas.drawText(e.chord,cx,ct+ch*.42f,chord);val r=strums(s.difficulty);val gap=19.dp.toPx();val start=cx-(r.size-1)*gap/2;r.forEachIndexed{i,v->drawContext.canvas.nativeCanvas.drawText(v,start+i*gap,ct+ch*.75f,when(v){"↓"->down;"↑"->up;else->mute})}}}
  drawLine(Acid.copy(alpha=.10f),Offset(playX,8.dp.toPx()),Offset(playX,size.height-8.dp.toPx()),10.dp.toPx(),cap=StrokeCap.Round);drawLine(Acid,Offset(playX,8.dp.toPx()),Offset(playX,size.height-8.dp.toPx()),2.dp.toPx(),cap=StrokeCap.Round)
 }
}

@Composable private fun CircleAction(t:String,f:()->Unit)=Box(Modifier.size(42.dp).background(Glass2,CircleShape).clickable(onClick=f),contentAlignment=Alignment.Center){Text(t,color=White,fontSize=24.sp)}
@Composable private fun TinyStatus(a:String,b:String,c:Color)=Column(horizontalAlignment=Alignment.End){Text(a,color=Fog,fontSize=8.sp,letterSpacing=1.sp);Text(b,color=c,fontSize=14.sp,fontWeight=FontWeight.Medium)}
@Composable private fun Pill(t:String,a:Boolean,f:()->Unit,enabled:Boolean)=Box(Modifier.background(if(a)Color(0xFF1C2923)else Glass2,RoundedCornerShape(16.dp)).clickable(enabled=enabled,onClick=f).padding(13.dp,9.dp)){Text(t,color=if(a)Mint else Fog,fontSize=10.sp,fontWeight=FontWeight.Medium)}
@Composable private fun Ghost(t:String,f:()->Unit)=Box(Modifier.background(Glass2,RoundedCornerShape(13.dp)).clickable(onClick=f).padding(13.dp,8.dp)){Text(t,color=White,fontSize=10.sp)}
private fun currentChord(s:PracticeState)=s.song.events.lastOrNull{s.positionBeats>=it.beat&&s.positionBeats<it.beat+it.durationBeats}?.chord?:s.song.events.firstOrNull()?.chord
private fun midiY(m:Int,t:Float,b:Float):Float{val c=m.coerceIn(60,84);return b-(c-60)/24f*(b-t)}
private fun strums(l:DifficultyLevel)=when(l){DifficultyLevel.ONE->listOf("↓");DifficultyLevel.TWO->listOf("↓","↓");DifficultyLevel.THREE->listOf("↓","↓","↑","↑","↓","↑");DifficultyLevel.FOUR->listOf("↓","x","↓","↑","↑","↓","↑")}
private fun np(s:Float,c:Int,b:Boolean)=Paint().apply{color=c;textSize=s;textAlign=Paint.Align.CENTER;isAntiAlias=true;typeface=if(b)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT}
private fun vibrateBeat(c:Context){try{val v=if(Build.VERSION.SDK_INT>=31)(c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)as VibratorManager).defaultVibrator else @Suppress("DEPRECATION")(c.getSystemService(Context.VIBRATOR_SERVICE)as Vibrator);if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(18,VibrationEffect.DEFAULT_AMPLITUDE))else @Suppress("DEPRECATION")v.vibrate(18)}catch(_:Throwable){}}
