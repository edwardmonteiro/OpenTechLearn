package com.edward.ukuleleai.ui.practice

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edward.ukuleleai.core.audio.LocalPitchDetector
import com.edward.ukuleleai.data.song.LocalProgressRepository
import com.edward.ukuleleai.data.song.ProgressSnapshot
import com.edward.ukuleleai.domain.DemoSong
import com.edward.ukuleleai.domain.DifficultyLevel
import com.edward.ukuleleai.domain.MelodyPoint
import com.edward.ukuleleai.domain.PracticeState
import com.edward.ukuleleai.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.floor

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepository = LocalProgressRepository(application)
    private val pitchDetector = LocalPitchDetector()
    private val _state = MutableStateFlow(PracticeState(song = DemoSong.song))
    val state: StateFlow<PracticeState> = _state.asStateFlow()

    private var playbackJob: Job? = null
    private var startedAtNanos = 0L
    private var startedAtBeat = 0.0
    private var completedRuns = 0
    private var loadedSongId: String? = null
    private var lastMetronomeBeat = -1
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 38)

    fun loadSong(song: Song) {
        if (loadedSongId == song.id) return
        if (loadedSongId != null) saveProgress()
        playbackJob?.cancel()
        stopListening()
        loadedSongId = song.id
        val saved = progressRepository.load(song.id)
        completedRuns = saved?.completedRuns ?: 0
        val difficulty = DifficultyLevel.entries.firstOrNull { it.level == saved?.difficultyLevel } ?: DifficultyLevel.ONE
        val endBeat = song.events.maxOf { it.beat + it.durationBeats }
        _state.value = PracticeState(
            song = song,
            difficulty = difficulty,
            bpm = saved?.bpm?.coerceIn(40, 160) ?: song.bpm,
            positionBeats = saved?.positionBeats?.coerceIn(0.0, endBeat) ?: 0.0
        )
    }

    fun exit(onExit: () -> Unit) { pause(true); stopListening(); onExit() }

    fun togglePlayback() {
        if (_state.value.isPlaying || _state.value.countdown != null) pause(true) else startWithCountIn()
    }

    fun toggleSound() {
        // Speaker clicks and microphone analysis are mutually exclusive by design.
        if (_state.value.listeningEnabled) return
        _state.value = _state.value.copy(soundEnabled = !_state.value.soundEnabled)
    }

    fun startListening() {
        if (_state.value.listeningEnabled) return
        // Safe Listen: immediately silence app-generated tones before opening AudioRecord.
        _state.value = _state.value.copy(listeningEnabled = true, soundEnabled = false)
        tone.stopTone()
        pitchDetector.start { pitch ->
            val current = _state.value
            if (!current.listeningEnabled) return@start
            if (pitch == null) {
                _state.value = current.copy(
                    detectedNote = null, detectedMidi = null, detectedFrequencyHz = null,
                    detectedCents = null, pitchConfidence = 0f
                )
            } else {
                val capture = current.isPlaying && pitch.confidence >= 0.70f
                val previous = current.melodyTrail.lastOrNull()
                val farEnough = previous == null || current.positionBeats - previous.beat >= 0.07
                val changed = previous?.midi != pitch.midi
                val trail = if (capture && (farEnough || changed)) {
                    (current.melodyTrail + MelodyPoint(
                        beat = current.positionBeats,
                        note = pitch.note,
                        midi = pitch.midi,
                        cents = pitch.cents,
                        confidence = pitch.confidence
                    )).takeLast(320)
                } else current.melodyTrail
                _state.value = current.copy(
                    detectedNote = pitch.note,
                    detectedMidi = pitch.midi,
                    detectedFrequencyHz = pitch.frequencyHz,
                    detectedCents = pitch.cents,
                    pitchConfidence = pitch.confidence,
                    melodyTrail = trail
                )
            }
        }
    }

    fun stopListening() {
        pitchDetector.stop()
        _state.value = _state.value.copy(
            listeningEnabled = false,
            detectedNote = null,
            detectedMidi = null,
            detectedFrequencyHz = null,
            detectedCents = null,
            pitchConfidence = 0f
        )
    }

    fun clearMelodyTrail() { _state.value = _state.value.copy(melodyTrail = emptyList()) }

    private fun click(accent: Boolean) {
        val s = _state.value
        if (!s.soundEnabled || s.listeningEnabled) return
        tone.startTone(if (accent) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, if (accent) 58 else 32)
    }

    private fun pulseBeat(accent: Boolean) {
        val s = _state.value
        _state.value = s.copy(beatPulse = s.beatPulse + 1)
        click(accent)
    }

    private fun startWithCountIn() {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        val endBeat = _state.value.song.events.maxOf { it.beat + it.durationBeats }
        if (_state.value.positionBeats >= endBeat - 0.001) {
            _state.value = _state.value.copy(positionBeats = 0.0, melodyTrail = emptyList())
        }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val beatDurationMs = (60_000.0 / _state.value.bpm).toLong()
            for (count in 3 downTo 1) {
                _state.value = _state.value.copy(countdown = count)
                pulseBeat(count == 1)
                delay(beatDurationMs)
            }
            startedAtBeat = _state.value.positionBeats
            startedAtNanos = SystemClock.elapsedRealtimeNanos()
            lastMetronomeBeat = floor(startedAtBeat).toInt() - 1
            _state.value = _state.value.copy(isPlaying = true, countdown = null)
            while (_state.value.isPlaying) {
                val elapsed = SystemClock.elapsedRealtimeNanos() - startedAtNanos
                val beat = startedAtBeat + elapsed / 60_000_000_000.0 * _state.value.bpm
                val songEnd = _state.value.song.events.maxOf { it.beat + it.durationBeats }
                if (beat >= songEnd) {
                    completedRuns++
                    _state.value = _state.value.copy(positionBeats = songEnd, isPlaying = false, countdown = null)
                    saveProgress()
                    break
                }
                val integerBeat = floor(beat).toInt()
                if (integerBeat > lastMetronomeBeat) {
                    lastMetronomeBeat = integerBeat
                    pulseBeat(integerBeat % _state.value.song.beatsPerBar == 0)
                }
                _state.value = _state.value.copy(positionBeats = beat)
                delay(12)
            }
        }
    }

    private fun pause(save: Boolean) {
        playbackJob?.cancel(); playbackJob = null
        _state.value = _state.value.copy(isPlaying = false, countdown = null)
        if (save) saveProgress()
    }

    fun restart() {
        playbackJob?.cancel(); playbackJob = null
        _state.value = _state.value.copy(positionBeats = 0.0, isPlaying = false, countdown = null, melodyTrail = emptyList())
        saveProgress()
    }

    fun changeTempo(delta: Int) {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        _state.value = _state.value.copy(bpm = (_state.value.bpm + delta).coerceIn(40, 160)); saveProgress()
    }

    fun setDifficulty(level: DifficultyLevel) {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        _state.value = _state.value.copy(difficulty = level); saveProgress()
    }

    private fun saveProgress() {
        val s = _state.value
        val end = s.song.events.maxOfOrNull { it.beat + it.durationBeats } ?: return
        progressRepository.save(s.song.id, ProgressSnapshot(s.positionBeats, end, s.bpm, s.difficulty.level, completedRuns))
    }

    override fun onCleared() { playbackJob?.cancel(); stopListening(); tone.release(); saveProgress() }
}
