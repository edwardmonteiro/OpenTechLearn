package com.edward.ukuleleai.ui.practice

import android.app.Application
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edward.ukuleleai.core.audio.LocalPitchDetector
import com.edward.ukuleleai.data.analysis.LocalAnalysisRepository
import com.edward.ukuleleai.data.song.LocalAudioRepository
import com.edward.ukuleleai.data.song.LocalProgressRepository
import com.edward.ukuleleai.data.song.LocalSongRepository
import com.edward.ukuleleai.data.song.ProgressSnapshot
import com.edward.ukuleleai.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.floor

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepository = LocalProgressRepository(application)
    private val audioRepository = LocalAudioRepository(application)
    private val analysisRepository = LocalAnalysisRepository(application)
    private val songRepository = LocalSongRepository(application)
    private val pitchDetector = LocalPitchDetector()
    private val _state = MutableStateFlow(PracticeState(song = DemoSong.song))
    val state: StateFlow<PracticeState> = _state.asStateFlow()

    private var playbackJob: Job? = null
    private var startedAtNanos = 0L
    private var startedAtBeat = 0.0
    private var completedRuns = 0
    private var loadedSongId: String? = null
    private var originalSong: Song = DemoSong.song
    private var lastMetronomeBeat = -1
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 32)
    private var backingFile: File? = null
    private var backingPlayer: MediaPlayer? = null

    fun loadSong(song: Song) {
        if (loadedSongId == song.id) return
        if (loadedSongId != null) saveProgress()
        releaseBacking()
        playbackJob?.cancel()
        stopListening()
        loadedSongId = song.id

        val cachedAnalysis = analysisRepository.load(song.id)
        val effectiveSong = cachedAnalysis?.toSong(song.id, song.title, song.lyrics) ?: song
        originalSong = effectiveSong
        backingFile = audioRepository.find(song.id)
        val saved = progressRepository.load(song.id)
        completedRuns = saved?.completedRuns ?: 0
        val difficulty = DifficultyLevel.entries.firstOrNull { it.level == saved?.difficultyLevel } ?: DifficultyLevel.ONE
        val endBeat = effectiveSong.events.maxOf { it.beat + it.durationBeats }
        val analyzed = cachedAnalysis != null
        val beginner = analyzed
        _state.value = PracticeState(
            song = if (beginner) simplifiedSong(effectiveSong) else effectiveSong,
            difficulty = difficulty,
            bpm = if (analyzed) effectiveSong.bpm else saved?.bpm?.coerceIn(40, 160) ?: effectiveSong.bpm,
            positionBeats = saved?.positionBeats?.coerceIn(0.0, endBeat) ?: 0.0,
            backingAvailable = backingFile != null,
            backingEnabled = analyzed && backingFile != null,
            beginnerMode = beginner,
            analysisAvailable = analyzed,
            analysisKey = cachedAnalysis?.key,
            analysisScale = cachedAnalysis?.scale,
            analysisSegments = cachedAnalysis?.chords?.size ?: 0,
            playAlongMode = if (analyzed) PlayAlongMode.LEARN else PlayAlongMode.PLAY,
            barHapticsEnabled = true,
            rhythmPattern = RhythmPattern.BASIC,
            playbackRate = 1.0f,
            loopLyricIndex = null
        )
    }

    fun exit(onExit: () -> Unit) { pause(true); stopListening(); releaseBacking(); onExit() }
    fun togglePlayback() { if (_state.value.isPlaying || _state.value.countdown != null) pause(true) else startWithCountIn() }
    fun toggleSound() { if (!_state.value.listeningEnabled) _state.value = _state.value.copy(soundEnabled = !_state.value.soundEnabled) }
    fun setPlayAlongMode(mode: PlayAlongMode) { _state.value = _state.value.copy(playAlongMode = mode) }
    fun toggleBarHaptics() { _state.value = _state.value.copy(barHapticsEnabled = !_state.value.barHapticsEnabled) }
    fun setRhythmPattern(pattern: RhythmPattern) { _state.value = _state.value.copy(rhythmPattern = pattern) }

    fun setPlaybackRate(rate: Float) {
        val next = rate.coerceIn(0.5f, 1.25f)
        if (_state.value.isPlaying && !_state.value.backingEnabled) {
            startedAtBeat = _state.value.positionBeats
            startedAtNanos = SystemClock.elapsedRealtimeNanos()
        }
        _state.value = _state.value.copy(playbackRate = next)
        backingPlayer?.let { player ->
            runCatching {
                player.playbackParams = player.playbackParams
                    .setSpeed(next)
                    .setPitch(1.0f)
            }
        }
    }

    fun seekToBeat(beat: Double) {
        val end = originalSong.events.maxOfOrNull { it.beat + it.durationBeats } ?: 0.0
        val target = beat.coerceIn(0.0, end)
        val wasPlaying = _state.value.isPlaying
        if (wasPlaying) {
            backingPlayer?.pause()
            startedAtBeat = target
            startedAtNanos = SystemClock.elapsedRealtimeNanos()
        }
        _state.value = _state.value.copy(positionBeats = target)
        if (_state.value.backingEnabled) {
            val p = ensureBacking()
            if (p != null) {
                val ms = (target * 60_000.0 / _state.value.bpm).toInt()
                    .coerceAtLeast(0)
                    .coerceAtMost((p.duration - 1).coerceAtLeast(0))
                runCatching {
                    p.seekTo(ms)
                    p.playbackParams = p.playbackParams
                        .setSpeed(_state.value.playbackRate)
                        .setPitch(1.0f)
                    if (wasPlaying) p.start()
                }
            }
        }
    }

    fun toggleLyricLoop(index: Int) {
        val lyrics = _state.value.song.lyrics
        if (index !in lyrics.indices) return
        val next = if (_state.value.loopLyricIndex == index) null else index
        _state.value = _state.value.copy(loopLyricIndex = next)
        if (next != null) {
            val lyric = lyrics[next]
            val leadIn = _state.value.song.beatsPerBar * 2.0
            seekToBeat((lyric.beat - leadIn).coerceAtLeast(0.0))
        }
    }

    fun clearLyricLoop() {
        _state.value = _state.value.copy(loopLyricIndex = null)
    }

    fun saveTappedLyrics(lines: List<String>, beats: List<Double>) {
        val clean = lines.map { it.trim() }.filter { it.isNotBlank() }
        if (clean.isEmpty() || beats.isEmpty()) return
        val count = minOf(clean.size, beats.size)
        val songEnd = originalSong.events.maxOfOrNull { it.beat + it.durationBeats } ?: 0.0
        val result = (0 until count).map { index ->
            val start = beats[index].coerceIn(0.0, songEnd)
            val end = if (index + 1 < count) beats[index + 1].coerceAtLeast(start + 0.05)
            else songEnd.coerceAtLeast(start + _state.value.song.beatsPerBar)
            LyricEvent(clean[index], start, (end - start).coerceAtLeast(0.05))
        }
        applyLyrics(result)
    }

    fun autoDistributeLyrics(raw: String) {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return
        val beats = suggestLyricBeats(raw)
        saveTappedLyrics(lines, beats)
    }

    fun suggestLyricBeats(raw: String): List<Double> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val songEnd = originalSong.events.maxOfOrNull { it.beat + it.durationBeats } ?: return emptyList()

        val weights = lines.map { line ->
            (line.count { !it.isWhitespace() }.coerceAtLeast(4) + line.count { it in ",.;:!?—-" } * 3).toDouble()
        }
        val totalWeight = weights.sum().coerceAtLeast(1.0)

        val bar = _state.value.song.beatsPerBar.toDouble().coerceAtLeast(1.0)
        val candidates = buildList {
            add(0.0)
            originalSong.events.forEach { add(it.beat) }
            var beat = 0.0
            while (beat <= songEnd) {
                add(beat)
                beat += bar
            }
        }.distinct().sorted()

        var cumulative = 0.0
        var previous = -0.01
        return lines.indices.map { index ->
            val ideal = if (index == 0) 0.0 else songEnd * cumulative / totalWeight
            val available = candidates.filter { it > previous + 0.2 }
            val snapped = available.minByOrNull { kotlin.math.abs(it - ideal) } ?: ideal
            previous = snapped
            cumulative += weights[index]
            snapped.coerceIn(0.0, songEnd)
        }
    }

    private fun applyLyrics(lyrics: List<LyricEvent>) {
        val updated = originalSong.copy(lyrics = lyrics.sortedBy { it.beat })
        songRepository.saveSong(updated)
        originalSong = updated
        val visible = if (_state.value.beginnerMode) simplifiedSong(updated) else updated
        _state.value = _state.value.copy(song = visible, loopLyricIndex = null)
    }

    fun toggleBeginner() {
        val next = !_state.value.beginnerMode
        _state.value = _state.value.copy(beginnerMode = next, song = if (next) simplifiedSong(originalSong) else originalSong)
    }

    private fun simplifiedSong(song: Song): Song = song.copy(events = song.events.map { it.copy(chord = simplifyUkuleleChord(it.chord)) })

    fun toggleBacking() {
        val s = _state.value
        if (!s.backingAvailable || s.listeningEnabled) return
        val enabled = !s.backingEnabled
        _state.value = s.copy(backingEnabled = enabled)
        if (!enabled) backingPlayer?.pause() else if (s.isPlaying) startBackingAt(s.positionBeats)
    }

    fun startListening() {
        if (_state.value.listeningEnabled) return
        tone.stopTone()
        backingPlayer?.pause()
        _state.value = _state.value.copy(listeningEnabled = true, soundEnabled = false, backingEnabled = false)
        pitchDetector.start { pitch ->
            val current = _state.value
            if (!current.listeningEnabled) return@start
            if (pitch == null) {
                _state.value = current.copy(detectedNote=null, detectedMidi=null, detectedFrequencyHz=null, detectedCents=null, pitchConfidence=0f)
            } else {
                val capture = current.isPlaying && pitch.confidence >= 0.70f
                val previous = current.melodyTrail.lastOrNull()
                val farEnough = previous == null || current.positionBeats - previous.beat >= 0.07
                val changed = previous?.midi != pitch.midi
                val trail = if (capture && (farEnough || changed))
                    (current.melodyTrail + MelodyPoint(current.positionBeats,pitch.note,pitch.midi,pitch.cents,pitch.confidence)).takeLast(320)
                else current.melodyTrail
                _state.value = current.copy(detectedNote=pitch.note,detectedMidi=pitch.midi,detectedFrequencyHz=pitch.frequencyHz,detectedCents=pitch.cents,pitchConfidence=pitch.confidence,melodyTrail=trail)
            }
        }
    }

    fun stopListening() {
        pitchDetector.stop()
        _state.value = _state.value.copy(listeningEnabled=false,detectedNote=null,detectedMidi=null,detectedFrequencyHz=null,detectedCents=null,pitchConfidence=0f)
    }
    fun clearMelodyTrail() { _state.value = _state.value.copy(melodyTrail = emptyList()) }

    private fun click(accent: Boolean) {
        val s = _state.value
        if (!s.soundEnabled || s.listeningEnabled || s.backingEnabled) return
        tone.startTone(if (accent) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, if (accent) 50 else 28)
    }
    private fun pulseBeat(accent: Boolean) { val s = _state.value; _state.value = s.copy(beatPulse = s.beatPulse + 1); click(accent) }

    private fun ensureBacking(): MediaPlayer? {
        val file = backingFile ?: return null
        if (backingPlayer == null) {
            backingPlayer = runCatching {
                MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setVolume(.85f,.85f)
                    playbackParams = playbackParams
                        .setSpeed(_state.value.playbackRate)
                        .setPitch(1.0f)
                }
            }.getOrNull()
        }
        return backingPlayer
    }

    private fun startBackingAt(beat: Double) {
        if (!_state.value.backingEnabled || _state.value.listeningEnabled) return
        val p = ensureBacking() ?: return
        val ms = (beat * 60_000.0 / _state.value.bpm).toInt().coerceAtLeast(0).coerceAtMost((p.duration - 1).coerceAtLeast(0))
        runCatching {
            p.seekTo(ms)
            p.playbackParams = p.playbackParams
                .setSpeed(_state.value.playbackRate)
                .setPitch(1.0f)
            p.start()
        }
    }

    private fun releaseBacking() { runCatching { backingPlayer?.stop() }; backingPlayer?.release(); backingPlayer = null }

    private fun startWithCountIn() {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        val endBeat = originalSong.events.maxOf { it.beat + it.durationBeats }
        if (_state.value.positionBeats >= endBeat - .001) _state.value = _state.value.copy(positionBeats=0.0,melodyTrail=emptyList())
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val beatDurationMs = (60_000.0 / (_state.value.bpm * _state.value.playbackRate)).toLong()
            for (count in 4 downTo 1) { _state.value = _state.value.copy(countdown=count); pulseBeat(count==1); delay(beatDurationMs) }
            startedAtBeat = _state.value.positionBeats
            startedAtNanos = SystemClock.elapsedRealtimeNanos()
            lastMetronomeBeat = floor(startedAtBeat).toInt() - 1
            _state.value = _state.value.copy(isPlaying=true,countdown=null)
            startBackingAt(startedAtBeat)
            while (_state.value.isPlaying) {
                val player = backingPlayer
                val playerIsClock = _state.value.backingEnabled && player != null && runCatching { player.isPlaying }.getOrDefault(false)
                val beat = if (playerIsClock) {
                    player!!.currentPosition * _state.value.bpm / 60_000.0
                } else {
                    val elapsed = SystemClock.elapsedRealtimeNanos() - startedAtNanos
                    startedAtBeat + elapsed / 60_000_000_000.0 * _state.value.bpm * _state.value.playbackRate
                }
                val songEnd = originalSong.events.maxOf { it.beat + it.durationBeats }
                val playerEnded = _state.value.backingEnabled && player != null && !runCatching { player.isPlaying }.getOrDefault(false) && player.currentPosition > 0
                if (beat >= songEnd || playerEnded) {
                    completedRuns++
                    backingPlayer?.pause()
                    _state.value = _state.value.copy(positionBeats=songEnd,isPlaying=false,countdown=null)
                    saveProgress(); break
                }
                val loopIndex = _state.value.loopLyricIndex
                val lyricLoop = loopIndex?.let { _state.value.song.lyrics.getOrNull(it) }
                val loopLead = _state.value.song.beatsPerBar * 2.0
                val loopTarget = lyricLoop?.let { (it.beat - loopLead).coerceAtLeast(0.0) }
                val loopEnd = lyricLoop?.let {
                    (it.beat + it.durationBeats + loopLead).coerceAtMost(songEnd)
                }
                if (lyricLoop != null && loopTarget != null && loopEnd != null && beat >= loopEnd) {
                    val target = loopTarget
                    startedAtBeat = target
                    startedAtNanos = SystemClock.elapsedRealtimeNanos()
                    if (_state.value.backingEnabled && player != null) {
                        val ms = (target * 60_000.0 / _state.value.bpm).toInt()
                            .coerceAtLeast(0)
                            .coerceAtMost((player.duration - 1).coerceAtLeast(0))
                        runCatching {
                            player.seekTo(ms)
                            player.playbackParams = player.playbackParams
                                .setSpeed(_state.value.playbackRate)
                                .setPitch(1.0f)
                            player.start()
                        }
                    }
                    _state.value = _state.value.copy(positionBeats = target)
                    lastMetronomeBeat = floor(target).toInt() - 1
                    delay(16)
                    continue
                }

                val integerBeat = floor(beat).toInt()
                if (integerBeat > lastMetronomeBeat) {
                    lastMetronomeBeat = integerBeat
                    pulseBeat(integerBeat % _state.value.song.beatsPerBar == 0)
                }
                _state.value = _state.value.copy(positionBeats = beat)
                delay(16)
            }
        }
    }

    private fun pause(save: Boolean) { playbackJob?.cancel(); playbackJob=null; backingPlayer?.pause(); _state.value=_state.value.copy(isPlaying=false,countdown=null); if(save)saveProgress() }
    fun restart() { playbackJob?.cancel(); playbackJob=null; runCatching { backingPlayer?.pause(); backingPlayer?.seekTo(0) }; _state.value=_state.value.copy(positionBeats=0.0,isPlaying=false,countdown=null,melodyTrail=emptyList()); saveProgress() }
    fun changeTempo(delta: Int) { if(_state.value.isPlaying||_state.value.countdown!=null||_state.value.backingEnabled)return; _state.value=_state.value.copy(bpm=(_state.value.bpm+delta).coerceIn(40,160)); saveProgress() }
    fun setDifficulty(level: DifficultyLevel) { if(_state.value.isPlaying||_state.value.countdown!=null)return; _state.value=_state.value.copy(difficulty=level); saveProgress() }
    private fun saveProgress() { val s=_state.value; val end=originalSong.events.maxOfOrNull{it.beat+it.durationBeats}?:return; progressRepository.save(s.song.id,ProgressSnapshot(s.positionBeats,end,s.bpm,s.difficulty.level,completedRuns)) }
    override fun onCleared() { playbackJob?.cancel(); stopListening(); releaseBacking(); tone.release(); saveProgress() }
}
