package com.edward.ukuleleai.ui.importsong

import android.net.Uri
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.ukuleleai.domain.Song

private val Background=Color(0xFF070908)
private val Panel=Color(0xFF111512)
private val Panel2=Color(0xFF171C19)
private val Primary=Color(0xFFF7F8F4)
private val Secondary=Color(0xFF8D9791)
private val Accent=Color(0xFFDDF45A)
private val Mint=Color(0xFF70E0B6)
private val Border=Color(0xFF303834)

private val exampleChart="""Title: Hoje Eu Vou
BPM: 117
| C | Am | F | G |
| C | Am | F | G |
[Lyrics]
@1 Hoje eu vou
@2 aprender ukulele"""

@Composable
fun ImportSongScreen(
    initialChart:String?=null,
    isEditing:Boolean=false,
    onCancel:()->Unit,
    onSave:(String)->Result<Song>,
    onSavedAndPlay:(Song,Uri?)->Unit,
    onAnalyzeAudio:((Uri,String)->Unit)?=null,
    onImportMidi:((Uri,String)->Unit)?=null
){
    var chart by remember(initialChart){mutableStateOf(initialChart?:exampleChart)}
    var error by remember{mutableStateOf<String?>(null)}
    var audio by remember{mutableStateOf<Uri?>(null)}
    var title by remember{mutableStateOf("Imported Song")}
    var launchingAnalysis by remember{mutableStateOf(false)}
    var advancedOpen by remember{mutableStateOf(isEditing)}

    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            audio=uri
            error=null
            if(!isEditing && onAnalyzeAudio!=null){
                launchingAnalysis=true
                onAnalyzeAudio(uri,title)
            }
        }
    }

    val pickMidi=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null && !isEditing && onImportMidi!=null){
            launchingAnalysis=true
            error=null
            onImportMidi(uri,title)
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ){
        Column(
            Modifier.fillMaxSize().padding(horizontal=22.dp,vertical=14.dp)
        ){
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically
            ){
                Box(
                    Modifier.size(36.dp)
                        .background(Panel2,CircleShape)
                        .clickable(onClick=onCancel),
                    contentAlignment=Alignment.Center
                ){
                    Text("‹",color=Primary,fontSize=24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)){
                    Text(
                        if(isEditing)"Edit track" else "Add a track",
                        color=Primary,fontSize=23.sp,fontWeight=FontWeight.SemiBold
                    )
                    Text(
                        if(isEditing)"Update chords or lyrics."
                        else "Choose a source. Analysis stays on this phone.",
                        color=Secondary,fontSize=10.sp
                    )
                }
                Surface(
                    color=Panel2,
                    shape=RoundedCornerShape(14.dp)
                ){
                    Text(
                        "OFFLINE",
                        color=Mint,fontSize=8.sp,fontWeight=FontWeight.Bold,
                        modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if(!isEditing){
                OutlinedTextField(
                    value=title,
                    onValueChange={title=it},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true,
                    label={Text("Song title")},
                    shape=RoundedCornerShape(14.dp),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedTextColor=Primary,
                        unfocusedTextColor=Primary,
                        focusedBorderColor=Accent,
                        unfocusedBorderColor=Border,
                        focusedLabelColor=Accent,
                        unfocusedLabelColor=Secondary,
                        focusedContainerColor=Panel,
                        unfocusedContainerColor=Panel
                    )
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    ImportSourceCard(
                        modifier=Modifier.weight(1f),
                        title="MP3 / AUDIO",
                        kicker="LISTEN & ANALYZE",
                        description="Detect BPM, key and chord changes from your audio.",
                        accent=Accent,
                        enabled=!launchingAnalysis,
                        onClick={pick.launch(arrayOf("audio/mpeg","audio/mp4","audio/aac","audio/wav","audio/*"))}
                    )
                    ImportSourceCard(
                        modifier=Modifier.weight(1f),
                        title="MIDI / .MID",
                        kicker="READ THE SCORE",
                        description="Use notes and timing directly. Import embedded lyrics when available.",
                        accent=Mint,
                        enabled=!launchingAnalysis && onImportMidi!=null,
                        onClick={pickMidi.launch(arrayOf("audio/midi","audio/x-midi","application/x-midi","application/octet-stream"))}
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    if(launchingAnalysis)"Preparing your track…"
                    else "Pick a file and you go straight to practice.",
                    color=if(launchingAnalysis)Accent else Secondary,
                    fontSize=9.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier=Modifier.fillMaxWidth()
                    .clickable{advancedOpen=!advancedOpen},
                color=Panel,
                shape=RoundedCornerShape(15.dp),
                border=androidx.compose.foundation.BorderStroke(1.dp,Border)
            ){
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=11.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Column(Modifier.weight(1f)){
                        Text(
                            if(isEditing)"CHART & LYRICS" else "ADVANCED · CHART & LYRICS",
                            color=Primary,fontSize=10.sp,fontWeight=FontWeight.Bold
                        )
                        Text(
                            if(isEditing)"Edit the source text directly."
                            else "Optional. Only if you want manual control.",
                            color=Secondary,fontSize=8.sp
                        )
                    }
                    Text(if(advancedOpen)"−" else "+",color=Accent,fontSize=20.sp,fontWeight=FontWeight.Light)
                }
            }

            if(advancedOpen){
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value=chart,
                    onValueChange={chart=it;error=null},
                    modifier=Modifier.fillMaxWidth().weight(1f),
                    label={Text("Manual chart / lyrics")},
                    textStyle=androidx.compose.ui.text.TextStyle(
                        color=Primary,fontFamily=FontFamily.Monospace,fontSize=12.sp
                    ),
                    shape=RoundedCornerShape(16.dp),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedBorderColor=Accent,
                        unfocusedBorderColor=Border,
                        focusedLabelColor=Accent,
                        unfocusedLabelColor=Secondary,
                        cursorColor=Accent,
                        focusedTextColor=Primary,
                        unfocusedTextColor=Primary,
                        focusedContainerColor=Panel,
                        unfocusedContainerColor=Panel
                    )
                )
                error?.let{
                    Spacer(Modifier.height(5.dp))
                    Text(it,color=Color(0xFFFFA7A7),fontSize=10.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.End
                ){
                    Button(
                        onClick={
                            onSave(chart)
                                .onSuccess{onSavedAndPlay(it,audio)}
                                .onFailure{error=it.message?:"Could not parse chart."}
                        },
                        colors=ButtonDefaults.buttonColors(containerColor=Primary,contentColor=Background),
                        shape=RoundedCornerShape(18.dp)
                    ){
                        Text(if(isEditing)"SAVE CHANGES" else "USE MANUAL CHART",fontSize=9.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }else{
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ImportSourceCard(
    modifier:Modifier,
    title:String,
    kicker:String,
    description:String,
    accent:Color,
    enabled:Boolean,
    onClick:()->Unit
){
    Surface(
        modifier=modifier.height(126.dp)
            .clickable(enabled=enabled,onClick=onClick),
        color=Panel,
        shape=RoundedCornerShape(20.dp),
        border=androidx.compose.foundation.BorderStroke(1.dp,if(enabled)Border else Border.copy(alpha=.4f))
    ){
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement=Arrangement.SpaceBetween
        ){
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(9.dp).background(accent,CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(kicker,color=accent,fontSize=7.sp,fontWeight=FontWeight.Bold,letterSpacing=.9.sp)
            }
            Column{
                Text(title,color=Primary,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(description,color=Secondary,fontSize=9.sp,lineHeight=12.sp)
            }
        }
    }
}
