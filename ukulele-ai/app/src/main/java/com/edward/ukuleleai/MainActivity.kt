package com.edward.ukuleleai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.edward.ukuleleai.data.song.*
import com.edward.ukuleleai.domain.*
import com.edward.ukuleleai.ui.home.HomeScreen
import com.edward.ukuleleai.ui.importsong.ImportSongScreen
import com.edward.ukuleleai.ui.practice.PracticeRoute07

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val songs=LocalSongRepository(applicationContext);val progress=LocalProgressRepository(applicationContext);val audio=LocalAudioRepository(applicationContext);setContent{MaterialTheme{Surface(color=Color(0xFF0D100F)){UkuleleApp(songs,progress,audio)}}}}}
private sealed interface AppScreen{data object Home:AppScreen;data object ImportSong:AppScreen;data class EditSong(val song:Song):AppScreen;data class Practice(val song:Song):AppScreen}
@Composable private fun UkuleleApp(repository:LocalSongRepository,progressRepository:LocalProgressRepository,audioRepository:LocalAudioRepository){
 var screen by remember{mutableStateOf<AppScreen>(AppScreen.Home)};var localSongs by remember{mutableStateOf(repository.listSongs())}
 fun attach(song:Song,uri:android.net.Uri?){if(uri!=null)audioRepository.import(song.id,uri).getOrThrow()}
 when(val current=screen){
  AppScreen.Home->HomeScreen(songs=localSongs,demoSong=DemoSong.song,progressPercent={progressRepository.load(it.id)?.completionPercent?:0},onAddSong={screen=AppScreen.ImportSong},onPlaySong={screen=AppScreen.Practice(it)},onEditSong={screen=AppScreen.EditSong(it)})
  AppScreen.ImportSong->ImportSongScreen(onCancel={screen=AppScreen.Home},onSave={repository.saveChart(it)},onSavedAndPlay={song,uri->runCatching{attach(song,uri)};localSongs=repository.listSongs();screen=AppScreen.Practice(song)})
  is AppScreen.EditSong->ImportSongScreen(initialChart=repository.getRawChart(current.song.id),isEditing=true,onCancel={screen=AppScreen.Home},onSave={repository.updateChart(current.song.id,it)},onSavedAndPlay={song,uri->runCatching{attach(song,uri)};localSongs=repository.listSongs();screen=AppScreen.Practice(song)})
  is AppScreen.Practice->PracticeRoute07(song=current.song,onExit={localSongs=repository.listSongs();screen=AppScreen.Home})
 }
}
