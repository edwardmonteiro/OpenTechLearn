package com.edward.ukuleleai.ui.importsong

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

private val Background=Color(0xFF0D100F);private val Primary=Color(0xFFF7F8F4);private val Secondary=Color(0xFFA7B0AA);private val Accent=Color(0xFFE9F45E)
private val exampleChart="""Title: Hoje Eu Vou
BPM: 117
| C | Am | F | G |
| C | Am | F | G |
[Lyrics]
@1 Hoje eu vou
@2 aprender ukulele"""

@Composable fun ImportSongScreen(initialChart:String?=null,isEditing:Boolean=false,onCancel:()->Unit,onSave:(String)->Result<Song>,onSavedAndPlay:(Song,Uri?)->Unit,onAnalyzeAudio:((Uri,String)->Unit)?=null,onImportMidi:((Uri,String)->Unit)?=null){
 var chart by remember(initialChart){mutableStateOf(initialChart?:exampleChart)}
 var error by remember{mutableStateOf<String?>(null)}
 var audio by remember{mutableStateOf<Uri?>(null)}
 var title by remember{mutableStateOf("Imported Song")}
 var launchingAnalysis by remember{mutableStateOf(false)}
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
 Column(Modifier.fillMaxSize().background(Background).padding(22.dp)){
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){
   Column{
    Text(if(isEditing)"Edit track" else "Add track",color=Primary,fontSize=27.sp,fontWeight=FontWeight.Bold)
    Text(if(isEditing)"Edit your chart." else "Import MP3 or MIDI. Everything stays offline.",color=Secondary,fontSize=12.sp)
   }
   Button(onClick=onCancel,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF242A27),contentColor=Primary)){Text("Cancel")}
  }
  Spacer(Modifier.height(10.dp))
  if(!isEditing){
   OutlinedTextField(value=title,onValueChange={title=it},modifier=Modifier.fillMaxWidth(),label={Text("Song title")},colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Primary,unfocusedTextColor=Primary,focusedBorderColor=Accent,unfocusedBorderColor=Color(0xFF3F4743),focusedLabelColor=Accent,unfocusedLabelColor=Secondary))
   Spacer(Modifier.height(8.dp))
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
   Button(
    onClick={pick.launch(arrayOf("audio/mpeg","audio/mp4","audio/aac","audio/wav","audio/*"))},
    enabled=!launchingAnalysis,
    modifier=Modifier.weight(1f),
    colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Background)
   ){
    Text(if(launchingAnalysis)"WORKING…" else "MP3 / AUDIO",fontWeight=FontWeight.Bold,fontSize=11.sp)
   }
   Button(
    onClick={pickMidi.launch(arrayOf("audio/midi","audio/x-midi","application/x-midi","application/octet-stream"))},
    enabled=!launchingAnalysis && onImportMidi!=null,
    modifier=Modifier.weight(1f),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF70E0B6),contentColor=Background)
   ){
    Text("MIDI .MID",fontWeight=FontWeight.Bold,fontSize=11.sp)
   }
  }
  Spacer(Modifier.height(5.dp))
  Text("Audio → Essentia · MIDI → note/chord parser · no upload · no server",color=Secondary,fontSize=10.sp)
  if(!isEditing){
   Spacer(Modifier.height(8.dp))
   Text("MP3 analyzes BPM/key/chords with Essentia. MIDI reads its notes, tempo and timing directly and infers the chord timeline locally.",color=Secondary,fontSize=11.sp)
  }
  Spacer(Modifier.height(14.dp))
  Text("Manual chart / lyrics${if(isEditing)"" else " (optional fallback)"}",color=Secondary,fontSize=11.sp)
  OutlinedTextField(value=chart,onValueChange={chart=it;error=null},modifier=Modifier.fillMaxWidth().weight(1f),label={Text(if(isEditing)"Edit chord chart" else "Optional manual chart")},textStyle=androidx.compose.ui.text.TextStyle(color=Primary,fontFamily=FontFamily.Monospace,fontSize=14.sp),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Accent,unfocusedBorderColor=Color(0xFF3F4743),focusedLabelColor=Accent,unfocusedLabelColor=Secondary,cursorColor=Accent,focusedContainerColor=Color(0xFF151A18),unfocusedContainerColor=Color(0xFF151A18)))
  error?.let{Spacer(Modifier.height(6.dp));Text(it,color=Color(0xFFFFA7A7),fontSize=12.sp)}
  Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){
   Text("Offline analysis is the primary import flow",color=Secondary,fontSize=11.sp)
   Button(onClick={onSave(chart).onSuccess{onSavedAndPlay(it,audio)}.onFailure{error=it.message?:"Could not parse chart."}},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF242A27),contentColor=Primary)){
    Text(if(isEditing)"Save changes" else "Use manual chart",fontWeight=FontWeight.Bold)
   }
  }
 }
}
