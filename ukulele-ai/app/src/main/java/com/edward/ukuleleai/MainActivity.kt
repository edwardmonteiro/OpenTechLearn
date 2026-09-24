package com.edward.ukuleleai

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.ukuleleai.data.analysis.OfflineChordAnalyzer
import com.edward.ukuleleai.data.midi.MidiSongImporter
import com.edward.ukuleleai.data.song.*
import com.edward.ukuleleai.domain.*
import com.edward.ukuleleai.ui.analysis.ChordAnalysisEditScreen
import com.edward.ukuleleai.ui.home.HomeScreen
import com.edward.ukuleleai.ui.fingerstyle.FingerstyleLabScreen
import com.edward.ukuleleai.ui.importsong.ImportSongScreen
import com.edward.ukuleleai.ui.practice.PracticeRoute
import com.edward.ukuleleai.ui.tools.SettingsScreen
import com.edward.ukuleleai.ui.tools.TunerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  val songs=LocalSongRepository(applicationContext);val progress=LocalProgressRepository(applicationContext);val audio=LocalAudioRepository(applicationContext);val analyzer=OfflineChordAnalyzer(applicationContext);val midi=MidiSongImporter(applicationContext,songs)
  setContent{MaterialTheme{Surface(color=Color(0xFF0D100F)){UkuleleApp(songs,progress,audio,analyzer,midi)}}}
 }
}

private sealed interface AppScreen{
 data object Home:AppScreen;data object ImportSong:AppScreen
 data class EditSong(val song:Song):AppScreen;data class Practice(val song:Song):AppScreen
 data class Analyze(val song:Song):AppScreen;data class EditAnalysis(val song:Song):AppScreen
 data class AnalysisError(val song:Song,val message:String):AppScreen
 data object Tuner:AppScreen
 data object Settings:AppScreen
 data object FingerstyleLab:AppScreen
}

@Composable private fun UkuleleApp(repository:LocalSongRepository,progressRepository:LocalProgressRepository,audioRepository:LocalAudioRepository,analyzer:OfflineChordAnalyzer,midiImporter:MidiSongImporter){
 var showLaunch by remember{mutableStateOf(true)}
 var screen by remember{mutableStateOf<AppScreen>(AppScreen.Home)};var toolReturn by remember{mutableStateOf<AppScreen>(AppScreen.Home)};var localSongs by remember{mutableStateOf(repository.listSongs())}
 LaunchedEffect(Unit){delay(1400);showLaunch=false}
 fun attach(song:Song,uri:Uri?){if(uri!=null)audioRepository.import(song.id,uri).getOrThrow()}
 fun openPractice(song:Song){progressRepository.markLastOpened(song.id);screen=AppScreen.Practice(song)}
 if(showLaunch){LaunchExperience();return}
 val lastOpenedId=progressRepository.lastOpenedSongId()
 val continueSong=(listOf(DemoSong.song)+localSongs).firstOrNull{it.id==lastOpenedId}
 val continuePercent=continueSong?.let{progressRepository.load(it.id)?.completionPercent?:0}?:0
 when(val current=screen){
  AppScreen.Home->HomeScreen(songs=localSongs,demoSong=DemoSong.song,continueSong=continueSong,continuePercent=continuePercent,progressPercent={progressRepository.load(it.id)?.completionPercent?:0},onAddSong={screen=AppScreen.ImportSong},onPlaySong={openPractice(it)},onEditSong={screen=if(analyzer.cached(it.id)!=null)AppScreen.EditAnalysis(it)else AppScreen.EditSong(it)},onSettings={toolReturn=AppScreen.Home;screen=AppScreen.Settings},onTuner={toolReturn=AppScreen.Home;screen=AppScreen.Tuner},onFingerstyle={screen=AppScreen.FingerstyleLab})
  AppScreen.ImportSong->ImportSongScreen(
   onCancel={screen=AppScreen.Home},
   onSave={repository.saveChart(it)},
   onSavedAndPlay={song,uri->runCatching{attach(song,uri)};localSongs=repository.listSongs();openPractice(song)},
   onAnalyzeAudio={uri,title->
    runCatching{val song=repository.createAudioSong(title);attach(song,uri);screen=AppScreen.Analyze(song)}
     .onFailure{screen=AppScreen.AnalysisError(Song("import-error",title.ifBlank{"Imported MP3"},80,4,listOf(ChordEvent("C",0.0,4.0))),it.message?:"Could not import audio.")}
   },
   onImportMidi={uri,title->
    runCatching{midiImporter.import(uri,title)}
     .onSuccess{song->localSongs=repository.listSongs();screen=AppScreen.Practice(song)}
     .onFailure{screen=AppScreen.AnalysisError(Song("midi-import-error",title.ifBlank{"Imported MIDI"},120,4,listOf(ChordEvent("C",0.0,4.0))),it.message?:"Could not import MIDI.")}
   }
  )
  is AppScreen.EditSong->ImportSongScreen(initialChart=repository.getRawChart(current.song.id),isEditing=true,onCancel={screen=AppScreen.Home},onSave={repository.updateChart(current.song.id,it)},onSavedAndPlay={song,uri->runCatching{attach(song,uri)};localSongs=repository.listSongs();screen=AppScreen.Practice(song)})
  is AppScreen.Analyze->AnalysisProgress(current.song,analyzer){result,error->if(result!=null){localSongs=repository.listSongs();openPractice(result)}else screen=AppScreen.AnalysisError(current.song,error?:"Offline analysis failed.")}
  is AppScreen.AnalysisError->AnalysisErrorScreen(current.song,current.message,onRetry={screen=AppScreen.Analyze(current.song)},onBack={localSongs=repository.listSongs();screen=AppScreen.Home})
  is AppScreen.EditAnalysis->{
   val a=analyzer.cached(current.song.id)
   if(a==null){LaunchedEffect(current.song.id){screen=AppScreen.Home}}
   else ChordAnalysisEditScreen(a,onBack={localSongs=repository.listSongs();screen=AppScreen.Home},onSaveChord={index,chord->analyzer.editChord(current.song.id,index,chord);localSongs=repository.listSongs();screen=AppScreen.EditAnalysis(repository.get(current.song.id)?:current.song)},onReanalyze={screen=AppScreen.Analyze(current.song)})
  }
  is AppScreen.Practice->PracticeRoute(song=current.song,onExit={localSongs=repository.listSongs();screen=AppScreen.Home},onEditAnalysis={screen=AppScreen.EditAnalysis(current.song)},onTuner={toolReturn=current;screen=AppScreen.Tuner},onSettings={toolReturn=current;screen=AppScreen.Settings})
  AppScreen.Tuner->TunerScreen(onBack={screen=toolReturn},onSettings={screen=AppScreen.Settings})
  AppScreen.Settings->SettingsScreen(songCount=localSongs.size,onBack={screen=toolReturn},onTuner={screen=AppScreen.Tuner})
  AppScreen.FingerstyleLab->FingerstyleLabScreen(onBack={screen=AppScreen.Home})
 }
}

@Composable private fun LaunchExperience(){
 var entered by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){entered=true}
 val alpha by animateFloatAsState(if(entered)1f else 0f,tween(500),label="launchAlpha")
 val scale by animateFloatAsState(if(entered)1f else .92f,tween(650),label="launchScale")
 Box(
  Modifier.fillMaxSize().background(Color(0xFF070908)),
  contentAlignment=Alignment.Center
 ){
  Column(
   horizontalAlignment=Alignment.CenterHorizontally,
   modifier=Modifier.graphicsLayer{this.alpha=alpha;scaleX=scale;scaleY=scale}
  ){
   Box(
    Modifier.size(92.dp).background(Color(0xFF111512),androidx.compose.foundation.shape.RoundedCornerShape(26.dp)),
    contentAlignment=Alignment.Center
   ){
    Image(
     painter=painterResource(id=R.drawable.ic_launcher),
     contentDescription="Ukulele Studio",
     modifier=Modifier.size(82.dp)
    )
   }
   Spacer(Modifier.height(18.dp))
   Text("Ukulele Studio",color=Color(0xFFF6F7F3),fontSize=29.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.ExtraLight)
   Spacer(Modifier.height(6.dp))
   Text("Play. Learn. Offline.",color=Color(0xFFDDF45A),fontSize=10.sp,letterSpacing=1.4.sp)
   Spacer(Modifier.height(20.dp))
   Text("Edward Research Labs Studio",color=Color(0xFF858F89),fontSize=8.sp,letterSpacing=.8.sp)
  }
 }
}

@Composable private fun AnalysisProgress(song:Song,analyzer:OfflineChordAnalyzer,onFinished:(Song?,String?)->Unit){
 var message by remember{mutableStateOf("Decoding audio locally…")}
 LaunchedEffect(song.id){
  val result=runCatching{withContext(Dispatchers.Default){message="Running Essentia chord analysis…";analyzer.analyze(song.id,force=true).toSong(song.id,song.title,song.lyrics)}}
  onFinished(result.getOrNull(),result.exceptionOrNull()?.message)
 }
 Box(Modifier.fillMaxSize().background(Color(0xFF0D100F)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator(color=Color(0xFFE9F45E));Spacer(Modifier.height(18.dp));Text(message,color=Color(0xFFF7F8F4),fontSize=18.sp);Spacer(Modifier.height(6.dp));Text("No upload. No server. Processing stays on this phone.",color=Color(0xFFA7B0AA),fontSize=11.sp)}}
}

@Composable private fun AnalysisErrorScreen(song:Song,message:String,onRetry:()->Unit,onBack:()->Unit){
 Box(Modifier.fillMaxSize().background(Color(0xFF0D100F)).padding(24.dp),contentAlignment=Alignment.Center){
  Column(horizontalAlignment=Alignment.CenterHorizontally){
   Text("OFFLINE ANALYSIS FAILED",color=Color(0xFFFF9C9C),fontSize=12.sp)
   Spacer(Modifier.height(10.dp))
   Text(song.title,color=Color(0xFFF7F8F4),fontSize=22.sp)
   Spacer(Modifier.height(12.dp))
   Text(message,color=Color(0xFFA7B0AA),fontSize=13.sp)
   Spacer(Modifier.height(18.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Button(onClick=onRetry){Text("REANALYZE")}
    Button(onClick=onBack,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF242A27))){Text("BACK")}
   }
  }
 }
}
