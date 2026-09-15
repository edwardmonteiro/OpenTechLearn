package com.edward.ukuleleai

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edward.ukuleleai.data.analysis.OfflineChordAnalyzer
import com.edward.ukuleleai.data.song.LocalSongRepository
import com.edward.ukuleleai.domain.simplifyUkuleleChord
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
        compose.onNodeWithText("PLAY").performClick()
        compose.waitUntil(timeoutMillis = 12_000) {
            runCatching { compose.onNodeWithText("PAUSE").fetchSemanticsNode(); true }.getOrDefault(false)
        }
        Thread.sleep(1800)
        compose.onNodeWithTag("current-chord").assertExists()
        compose.onNodeWithText("PAUSE").assertExists()
    }
}
