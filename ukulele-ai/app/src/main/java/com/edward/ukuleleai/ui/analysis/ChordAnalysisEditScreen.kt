package com.edward.ukuleleai.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.ukuleleai.domain.AudioAnalysis

private val Bg=Color(0xFF0D100F);private val White=Color(0xFFF7F8F4);private val Fog=Color(0xFFA7B0AA);private val Accent=Color(0xFFE9F45E)

@Composable fun ChordAnalysisEditScreen(
    analysis: AudioAnalysis,
    onBack:()->Unit,
    onSaveChord:(Int,String)->Unit,
    onReanalyze:()->Unit
){
 Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)){
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){
   Column{Text("Chord analysis",color=White,fontSize=26.sp,fontWeight=FontWeight.Bold);Text("${analysis.bpm.toInt()} BPM · ${analysis.key} ${analysis.scale} · ${analysis.chords.size} segments",color=Fog,fontSize=11.sp)}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onReanalyze,colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Bg)){Text("Reanalyze")};Button(onClick=onBack){Text("Done")}}
  }
  Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxSize()){
   itemsIndexed(analysis.chords,key={i,c->"${i}-${c.startMs}"}){index,c->
    var text by remember(c.chord){mutableStateOf(c.chord)}
    Row(Modifier.fillMaxWidth().background(Color(0xFF171C19),RoundedCornerShape(14.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
     Text("${format(c.startMs)} – ${format(c.endMs)}",color=Fog,fontSize=11.sp,modifier=Modifier.width(112.dp))
     OutlinedTextField(value=text,onValueChange={text=it},singleLine=true,modifier=Modifier.weight(1f),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=White,unfocusedTextColor=White,focusedBorderColor=Accent,unfocusedBorderColor=Color(0xFF3F4743)))
     Spacer(Modifier.width(8.dp));Button(onClick={if(text.isNotBlank())onSaveChord(index,text.trim())}){Text("Save")}
    }
   }
  }
 }
}
private fun format(ms:Long):String{val s=ms/1000;return "%d:%02d".format(s/60,s%60)}
