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
import com.edward.ukuleleai.domain.PlayAlongMode
import com.edward.ukuleleai.domain.displayChordAtBeat
import com.edward.ukuleleai.ui.practice.PracticeViewModel
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

        val analysis = OfflineChordAnalyzer(target).analyze(song.id, force = true)
        assertTrue("Expected several detected chord segments", analysis.chords.size > 2)
        assertTrue("Expected more than one detected chord", analysis.chords.map { it.chord }.distinct().size > 1)
        assertTrue("Expected plausible BPM", analysis.bpm in 40f..220f)
        assertTrue("Expected detected key", analysis.key.isNotBlank())
        assertTrue("Expected cached JSON", File(backing, "${song.id}.analysis.json").exists())

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Offline Suno Validation").assertExists().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("● Original audio ON").assertExists()
        compose.onNodeWithTag("analysis-status").assertExists()
        compose.onNodeWithTag("detected-chord-strip").assertExists()
        compose.onNodeWithTag("current-chord").assertExists()
        compose.onNodeWithText("LEARN").assertExists()
        compose.onNodeWithTag("next-chord").assertExists()\n        compose.onNodeWithTag("rhythm-coach").assertExists()\n        compose.onNodeWithText("BASIC").assertExists()

        val vm = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        assertTrue("Practice state should be hydrated from cached analysis", vm.state.value.analysisAvailable)
        assertTrue("Practice song should contain detected events", vm.state.value.song.events.size > 2)
        assertTrue("Analyzed songs should open in learn mode", vm.state.value.playAlongMode == PlayAlongMode.LEARN)

        val before = vm.state.value.positionBeats
        compose.activity.runOnUiThread { vm.togglePlayback() }
        val startedDeadline = System.currentTimeMillis() + 12_000
        while (!vm.state.value.isPlaying && System.currentTimeMillis() < startedDeadline) Thread.sleep(100)
        assertTrue("Playback should start after count-in", vm.state.value.isPlaying)

        Thread.sleep(1800)
        val duringPlayback = vm.state.value
        assertTrue("Playback position should advance", duringPlayback.positionBeats > before + 0.1)
        assertTrue(
            "Expected a chord at the live player position",
            !displayChordAtBeat(duringPlayback.song, duringPlayback.positionBeats, duringPlayback.beginnerMode).isNullOrBlank()
        )

        compose.activity.runOnUiThread { vm.togglePlayback() }
        val pausedDeadline = System.currentTimeMillis() + 4_000
        while (vm.state.value.isPlaying && System.currentTimeMillis() < pausedDeadline) Thread.sleep(50)
        assertTrue("Playback should pause", !vm.state.value.isPlaying)
        compose.waitForIdle()

        compose.onNodeWithTag("current-chord").assertExists()
        compose.onNodeWithTag("detected-chord-strip").assertExists()
        compose.onNodeWithText("PLAY").assertExists()
    }
}
