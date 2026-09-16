package com.edward.ukuleleai

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edward.ukuleleai.data.analysis.OfflineChordAnalyzer
import com.edward.ukuleleai.data.song.LocalSongRepository
import com.edward.ukuleleai.domain.displayChordAtBeat
import com.edward.ukuleleai.domain.simplifyUkuleleChord
import com.edward.ukuleleai.ui.practice.PracticeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflineSunoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun sunoMp3_opensAnalyzesPlaysAndShowsSyncedChordOffline() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val testContext = instrumentation.context
        val repo = LocalSongRepository(target)
        val song = repo.createAudioSong("Offline Suno Validation")
        val backing = File(target.filesDir, "backing_tracks").apply { mkdirs() }
        val audio = File(backing, "${song.id}.mp3")
        testContext.assets.open("suno-test.mp3").use { input -> audio.outputStream().use { input.copyTo(it) } }

        // Analysis is executed after the workflow disables emulator networking.
        val analysis = OfflineChordAnalyzer(target).analyze(song.id, force = true)
        assertTrue("Expected detected chords", analysis.chords.isNotEmpty())
        assertTrue("Expected plausible BPM", analysis.bpm in 40f..220f)
        assertTrue("Expected detected key", analysis.key.isNotBlank())
        assertTrue("Expected cached JSON", File(backing, "${song.id}.analysis.json").exists())

        val expectedFirst = simplifyUkuleleChord(analysis.chords.first().chord)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Offline Suno Validation").assertExists().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("● Original audio ON").assertExists()
        compose.onNodeWithTag("current-chord").assertExists()
        compose.onNodeWithText("NOW  $expectedFirst").assertExists()
        compose.onNodeWithText("PLAY").assertExists()

        // Use the Activity-scoped ViewModel while playback is active. Compose is intentionally
        // not asked to become idle during playback because the player clock updates state at ~60 Hz.
        val vm = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val before = vm.state.value.positionBeats
        compose.activity.runOnUiThread { vm.togglePlayback() }

        val startedDeadline = System.currentTimeMillis() + 12_000
        while (!vm.state.value.isPlaying && System.currentTimeMillis() < startedDeadline) {
            Thread.sleep(100)
        }
        assertTrue("Playback should start after count-in", vm.state.value.isPlaying)

        Thread.sleep(1800)
        val duringPlayback = vm.state.value
        assertTrue("Playback position should advance", duringPlayback.positionBeats > before + 0.1)
        val expectedAtPosition = displayChordAtBeat(
            duringPlayback.song,
            duringPlayback.positionBeats,
            duringPlayback.beginnerMode
        )
        assertTrue("Expected a chord at the live player position", !expectedAtPosition.isNullOrBlank())

        // Pause first so Compose can become idle, then verify the rendered chord equals the chord
        // derived from the exact player-driven position captured above.
        compose.activity.runOnUiThread { vm.togglePlayback() }
        val pausedDeadline = System.currentTimeMillis() + 4_000
        while (vm.state.value.isPlaying && System.currentTimeMillis() < pausedDeadline) {
            Thread.sleep(50)
        }
        assertTrue("Playback should pause", !vm.state.value.isPlaying)
        compose.waitForIdle()

        val paused = vm.state.value
        val expectedPaused = displayChordAtBeat(paused.song, paused.positionBeats, paused.beginnerMode)
        assertEquals("Rendered chord should track player-clock position", expectedAtPosition, expectedPaused)
        compose.onNodeWithTag("current-chord").assertExists()
        compose.onNodeWithText("NOW  $expectedPaused").assertExists()
    }
}
