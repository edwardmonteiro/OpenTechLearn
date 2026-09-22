package com.edward.ukuleleai.ui.tools

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edward.ukuleleai.core.audio.LocalPitchDetector
import kotlin.math.abs
import kotlin.math.ln

private val Night=Color(0xFF070908)
private val Glass=Color(0xFF101412)
private val Glass2=Color(0xFF171C19)
private val White=Color(0xFFF6F7F3)
private val Fog=Color(0xFF858F89)
private val Acid=Color(0xFFDDF45A)
private val Mint=Color(0xFF70E0B6)
private val Line=Color(0xFF303834)

private data class TuningTarget(
    val note:String,
    val solfege:String,
    val frequency:Double
)

private val StandardUkulele=listOf(
    TuningTarget("G4","Sol",392.00),
    TuningTarget("C4","Dó",261.63),
    TuningTarget("E4","Mi",329.63),
    TuningTarget("A4","Lá",440.00)
)

@Composable
fun TunerScreen(onBack:()->Unit,onSettings:()->Unit){
    val context=LocalContext.current
    var granted by remember{
        mutableStateOf(
            ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
        )
    }
    var pitch by remember{mutableStateOf<LocalPitchDetector.Pitch?>(null)}
    val detector=remember{LocalPitchDetector()}
    val mainHandler=remember{Handler(Looper.getMainLooper())}

    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ok->
        granted=ok
        if(!ok)pitch=null
    }

    DisposableEffect(granted){
        if(granted){
            detector.start{value->mainHandler.post{pitch=value}}
        }
        onDispose{detector.stop()}
    }

    val detected=pitch
    val target=detected?.let{p->
        StandardUkulele.minByOrNull{t->abs(centsBetween(p.frequencyHz.toDouble(),t.frequency))}
    }
    val cents=if(detected!=null&&target!=null)centsBetween(detected.frequencyHz.toDouble(),target.frequency).coerceIn(-50.0,50.0) else 0.0
    val inTune=detected!=null&&abs(cents)<=5.0
    val stateText=when{
        detected==null->"PLUCK A STRING"
        inTune->"IN TUNE"
        cents<0->"LOW"
        else->"HIGH"
    }

    Box(
        Modifier.fillMaxSize().background(Night).windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=14.dp)){
            ToolHeader(title="Ukulele Tuner",subtitle="Standard · G C E A",onBack=onBack,onSettings=onSettings)
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                StandardUkulele.forEach{string->
                    val selected=target?.note==string.note
                    Box(
                        Modifier.weight(1f).height(52.dp)
                            .background(if(selected)Acid else Glass,RoundedCornerShape(14.dp))
                            .border(1.dp,if(selected)Acid else Line,RoundedCornerShape(14.dp)),
                        contentAlignment=Alignment.Center
                    ){
                        Column(horizontalAlignment=Alignment.CenterHorizontally){
                            Text(string.note.dropLast(1),color=if(selected)Night else White,fontSize=18.sp,fontWeight=FontWeight.Bold)
                            Text(string.solfege,color=if(selected)Night.copy(alpha=.65f) else Fog,fontSize=7.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(.8f))

            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
                Text(
                    target?.note?.dropLast(1)?:"—",
                    color=if(inTune)Mint else White,
                    fontSize=76.sp,
                    fontWeight=FontWeight.ExtraLight
                )
                Text(
                    target?.let{"${it.solfege} · ${it.note}"}?:"Standard GCEA",
                    color=Fog,fontSize=11.sp
                )
                Spacer(Modifier.height(16.dp))

                TunerMeter(cents=cents,active=detected!=null,inTune=inTune)

                Spacer(Modifier.height(12.dp))
                Text(
                    stateText,
                    color=if(inTune)Mint else if(detected==null)Fog else Acid,
                    fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    if(detected==null)"Listen locally through the phone microphone"
                    else "${String.format("%.1f",detected.frequencyHz)} Hz  ·  ${if(cents>=0) "+" else ""}${cents.toInt()} cents",
                    color=Fog,fontSize=9.sp
                )
            }

            Spacer(Modifier.weight(1f))

            if(!granted){
                Button(
                    onClick={permission.launch(Manifest.permission.RECORD_AUDIO)},
                    modifier=Modifier.align(Alignment.CenterHorizontally),
                    colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Night),
                    shape=RoundedCornerShape(20.dp)
                ){Text("ENABLE MICROPHONE",fontSize=10.sp,fontWeight=FontWeight.Bold)}
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "100% offline · no recording is stored",
                color=Fog,fontSize=8.sp,
                modifier=Modifier.fillMaxWidth(),
                textAlign=TextAlign.Center
            )
        }
    }
}

@Composable
private fun TunerMeter(cents:Double,active:Boolean,inTune:Boolean){
    Box(
        Modifier.fillMaxWidth(.78f).height(54.dp)
            .background(Glass,RoundedCornerShape(18.dp))
            .border(1.dp,Line,RoundedCornerShape(18.dp))
    ){
        Row(
            Modifier.fillMaxSize().padding(horizontal=12.dp),
            horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically
        ){
            listOf("-50","-25","0","+25","+50").forEach{
                Text(it,color=if(it=="0")White else Fog,fontSize=7.sp)
            }
        }
        Box(
            Modifier.align(Alignment.Center)
                .width(2.dp).fillMaxHeight(.72f)
                .background(if(inTune)Mint else White,RoundedCornerShape(2.dp))
        )
        if(active){
            val normalized=((cents+50.0)/100.0).toFloat().coerceIn(0f,1f)
            Box(
                Modifier.fillMaxWidth(normalized.coerceAtLeast(.01f))
                    .align(Alignment.CenterStart)
            ){
                Box(
                    Modifier.align(Alignment.CenterEnd)
                        .size(17.dp)
                        .background(if(inTune)Mint else Acid,CircleShape)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    songCount:Int,
    onBack:()->Unit,
    onTuner:()->Unit
){
    val context=LocalContext.current
    val pkg=remember{
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName,0)
    }
    val version=pkg.versionName?:"—"
    val build=if(Build.VERSION.SDK_INT>=28)pkg.longVersionCode.toString() else @Suppress("DEPRECATION") pkg.versionCode.toString()
    val micGranted=ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    val abi=Build.SUPPORTED_ABIS.firstOrNull()?:"unknown"

    Box(
        Modifier.fillMaxSize().background(Night).windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(
            Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=14.dp)
        ){
            ToolHeader(title="Settings & About",subtitle="Ukulele Studio",onBack=onBack)
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                SettingsCard(
                    modifier=Modifier.weight(1f),
                    kicker="TOOL",
                    title="Ukulele Tuner",
                    detail="Standard GCEA · local microphone",
                    accent=Acid,
                    onClick=onTuner
                )
                SettingsCard(
                    modifier=Modifier.weight(1f),
                    kicker="PRIVACY",
                    title="Offline by design",
                    detail="Audio, MIDI, analysis and practice data stay on this device.",
                    accent=Mint
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                InfoPanel(
                    modifier=Modifier.weight(1f),
                    title="ABOUT",
                    rows=listOf(
                        "Product" to "Ukulele Studio",
                        "Version" to version,
                        "Build" to build,
                        "Developer" to "Edward Research Labs Studio"
                    )
                )
                InfoPanel(
                    modifier=Modifier.weight(1f),
                    title="DIAGNOSTICS",
                    rows=listOf(
                        "Android" to "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
                        "ABI" to abi,
                        "Microphone" to if(micGranted)"Granted" else "Not granted",
                        "Local songs" to songCount.toString()
                    )
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Designed & developed by Edward Research Labs Studio",
                color=Fog,fontSize=9.sp,
                modifier=Modifier.fillMaxWidth(),
                textAlign=TextAlign.Center
            )
        }
    }
}

@Composable
private fun ToolHeader(
    title:String,
    subtitle:String,
    onBack:()->Unit,
    onSettings:(()->Unit)?=null
){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Box(
            Modifier.size(36.dp).background(Glass2,CircleShape).clickable(onClick=onBack),
            contentAlignment=Alignment.Center
        ){Text("‹",color=White,fontSize=24.sp)}
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)){
            Text(title,color=White,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            Text(subtitle,color=Fog,fontSize=9.sp)
        }
        onSettings?.let{
            Box(
                Modifier.size(36.dp).background(Glass2,CircleShape).clickable(onClick=it),
                contentAlignment=Alignment.Center
            ){Text("⚙",color=White,fontSize=16.sp)}
        }
    }
}

@Composable
private fun SettingsCard(
    modifier:Modifier,
    kicker:String,
    title:String,
    detail:String,
    accent:Color,
    onClick:(()->Unit)?=null
){
    Column(
        modifier.height(102.dp)
            .background(Glass,RoundedCornerShape(18.dp))
            .border(1.dp,Line,RoundedCornerShape(18.dp))
            .then(if(onClick!=null)Modifier.clickable(onClick=onClick) else Modifier)
            .padding(14.dp)
    ){
        Text(kicker,color=accent,fontSize=7.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
        Spacer(Modifier.height(7.dp))
        Text(title,color=White,fontSize=14.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(detail,color=Fog,fontSize=8.sp,lineHeight=11.sp)
    }
}

@Composable
private fun InfoPanel(
    modifier:Modifier,
    title:String,
    rows:List<Pair<String,String>>
){
    Column(
        modifier.background(Glass,RoundedCornerShape(18.dp))
            .border(1.dp,Line,RoundedCornerShape(18.dp))
            .padding(14.dp)
    ){
        Text(title,color=Acid,fontSize=7.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
        Spacer(Modifier.height(8.dp))
        rows.forEach{(label,value)->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(label,color=Fog,fontSize=8.sp)
                Text(value,color=White,fontSize=8.sp,fontWeight=FontWeight.Medium,maxLines=1)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun centsBetween(frequency:Double,target:Double):Double =
    1200.0 * (ln(frequency/target)/ln(2.0))
