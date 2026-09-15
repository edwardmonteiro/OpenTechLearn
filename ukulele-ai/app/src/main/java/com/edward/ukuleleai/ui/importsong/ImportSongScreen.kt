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

@Composable fun ImportSongScreen(initialChart:String?=null,isEditing:Boolean=false,onCancel:()->Unit,onSave:(String)->Result<Song>,onSavedAndPlay:(Song,Uri?)->Unit,onAnalyzeAudio:((Uri,String)->Unit)?=null){
 var chart by remember(initialChart){mutableStateOf(initialChart?:exampleChart)};var error by remember{mutableStateOf<String?>(null)};var audio by remember{mutableStateOf<Uri?>(null)}
 var title by remember{mutableStateOf("Imported Suno Song")}
 val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->audio=uri}
 Column(Modifier.fillMaxSize().background(Background).padding(22.dp)){
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Column{Text(if(isEditing)"Edit track" else "Add track",color=Primary,fontSize=27.sp,fontWeight=FontWeight.Bold);Text("Import MP3 and analyze chords fully offline.",color=Secondary,fontSize=12.sp)};Button(onClick=onCancel,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF242A27),contentColor=Primary)){Text("Cancel")}}
  Spacer(Modifier.height(10.dp))
  if(!isEditing){OutlinedTextField(value=title,onValueChange={title=it},modifier=Modifier.fillMaxWidth(),label={Text("Song title")},colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Primary,unfocusedTextColor=Primary,focusedBorderColor=Accent,unfocusedBorderColor=Color(0xFF3F4743),focusedLabelColor=Accent,unfocusedLabelColor=Secondary));Spacer(Modifier.height(8.dp))}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
   Button(onClick={pick.launch(arrayOf("audio/mpeg","audio/mp4","audio/aac","audio/wav","audio/*"))},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF242A27),contentColor=Primary)){Text(if(audio==null)"＋ Select MP3 / WAV / M4A" else "✓ Audio selected")}
   Text(if(audio==null)"Audio stays on this phone" else "Ready for offline analysis",color=Secondary,fontSize=11.sp)
  }
  if(!isEditing && onAnalyzeAudio!=null){Spacer(Modifier.height(9.dp));Button(onClick={audio?.let{onAnalyzeAudio(it,title)}?:run{error="Select an audio file first."}},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Background)){Text("ANALYZE MP3 OFFLINE",fontWeight=FontWeight.Bold)};Text("Essentia runs locally. Results are cached beside the song.",color=Secondary,fontSize=10.sp,modifier=Modifier.padding(top=5.dp))}
  Spacer(Modifier.height(10.dp));Text("Manual chart / lyrics",color=Secondary,fontSize=11.sp)
  OutlinedTextField(value=chart,onValueChange={chart=it;error=null},modifier=Modifier.fillMaxWidth().weight(1f),label={Text(if(isEditing)"Edit chord chart" else "Optional manual chart")},textStyle=androidx.compose.ui.text.TextStyle(color=Primary,fontFamily=FontFamily.Monospace,fontSize=14.sp),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Accent,unfocusedBorderColor=Color(0xFF3F4743),focusedLabelColor=Accent,unfocusedLabelColor=Secondary,cursorColor=Accent,focusedContainerColor=Color(0xFF151A18),unfocusedContainerColor=Color(0xFF151A18)))
  error?.let{Spacer(Modifier.height(6.dp));Text(it,color=Color(0xFFFFA7A7),fontSize=12.sp)}
  Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("No server · no API · no Internet",color=Secondary,fontSize=11.sp);Button(onClick={onSave(chart).onSuccess{onSavedAndPlay(it,audio)}.onFailure{error=it.message?:"Could not parse chart."}},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF242A27),contentColor=Primary)){Text(if(isEditing)"Save changes" else "Save manual",fontWeight=FontWeight.Bold)}}
 }
}
