package com.edward.ukuleleai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.edward.ukuleleai.data.song.LocalSongRepository
import com.edward.ukuleleai.data.song.LocalProgressRepository
import com.edward.ukuleleai.domain.DemoSong
import com.edward.ukuleleai.domain.Song
import com.edward.ukuleleai.ui.home.HomeScreen
import com.edward.ukuleleai.ui.importsong.ImportSongScreen
import com.edward.ukuleleai.ui.practice.PracticeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = LocalSongRepository(applicationContext)
        val progressRepository = LocalProgressRepository(applicationContext)

        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF101312)) {
                    UkuleleApp(repository, progressRepository)
                }
            }
        }
    }
}

private sealed interface AppScreen {
    data object Home : AppScreen
    data object ImportSong : AppScreen
    data class Practice(val song: Song) : AppScreen
}

@Composable
private fun UkuleleApp(
    repository: LocalSongRepository,
    progressRepository: LocalProgressRepository
) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var localSongs by remember { mutableStateOf(repository.listSongs()) }

    when (val current = screen) {
        AppScreen.Home -> HomeScreen(
            songs = localSongs,
            demoSong = DemoSong.song,
            progressPercent = { progressRepository.load(it.id)?.completionPercent ?: 0 },
            onAddSong = { screen = AppScreen.ImportSong },
            onPlaySong = { screen = AppScreen.Practice(it) }
        )

        AppScreen.ImportSong -> ImportSongScreen(
            onCancel = { screen = AppScreen.Home },
            onSave = { raw -> repository.saveChart(raw) },
            onSavedAndPlay = { song ->
                localSongs = repository.listSongs()
                screen = AppScreen.Practice(song)
            }
        )

        is AppScreen.Practice -> PracticeRoute(
            song = current.song,
            onExit = { screen = AppScreen.Home }
        )
    }
}
