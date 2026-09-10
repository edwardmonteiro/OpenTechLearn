package com.edward.ukuleleai.ui.practice

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edward.ukuleleai.data.song.LocalProgressRepository
import com.edward.ukuleleai.data.song.ProgressSnapshot
import com.edward.ukuleleai.domain.DemoSong
import com.edward.ukuleleai.domain.DifficultyLevel
import com.edward.ukuleleai.domain.PracticeState
import com.edward.ukuleleai.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepository = LocalProgressRepository(application)
    private val _state = MutableStateFlow(PracticeState(song = DemoSong.song))
    val state: StateFlow<PracticeState> = _state.asStateFlow()

    private var playbackJob: Job? = null
    private var startedAtNanos: Long = 0L
    private var startedAtBeat: Double = 0.0
    private var completedRuns: Int = 0
    private var loadedSongId: String? = null

    fun loadSong(song: Song) {
        if (loadedSongId == song.id) return
        if (loadedSongId != null) saveProgress()
        playbackJob?.cancel()
        playbackJob = null
        loadedSongId = song.id
        val saved = progressRepository.load(song.id)
        completedRuns = saved?.completedRuns ?: 0
        val savedDifficulty = DifficultyLevel.entries.firstOrNull { it.level == saved?.difficultyLevel }
            ?: DifficultyLevel.ONE
        val endBeat = song.events.maxOf { it.beat + it.durationBeats }
        val resumedPosition = saved?.positionBeats?.coerceIn(0.0, endBeat) ?: 0.0
        _state.value = PracticeState(
            song = song,
            difficulty = savedDifficulty,
            bpm = saved?.bpm?.coerceIn(40, 160) ?: song.bpm,
            positionBeats = resumedPosition
        )
    }

    fun exit(onExit: () -> Unit) {
        pause(save = true)
        onExit()
    }

    fun togglePlayback() {
        if (_state.value.isPlaying || _state.value.countdown != null) pause(save = true)
        else startWithCountIn()
    }

    private fun startWithCountIn() {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        val endBeat = _state.value.song.events.maxOf { it.beat + it.durationBeats }
        if (_state.value.positionBeats >= endBeat - 0.001) {
            _state.value = _state.value.copy(positionBeats = 0.0)
        }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val beatDurationMs = (60_000.0 / _state.value.bpm).toLong()
            for (count in 3 downTo 1) {
                _state.value = _state.value.copy(countdown = count)
                delay(beatDurationMs)
            }
            startedAtBeat = _state.value.positionBeats
            startedAtNanos = SystemClock.elapsedRealtimeNanos()
            _state.value = _state.value.copy(isPlaying = true, countdown = null)
            while (_state.value.isPlaying) {
                val elapsedNanos = SystemClock.elapsedRealtimeNanos() - startedAtNanos
                val elapsedMinutes = elapsedNanos / 60_000_000_000.0
                val beat = startedAtBeat + elapsedMinutes * _state.value.bpm
                val songEndBeat = _state.value.song.events.maxOf { it.beat + it.durationBeats }
                if (beat >= songEndBeat) {
                    completedRuns += 1
                    _state.value = _state.value.copy(positionBeats = songEndBeat, isPlaying = false, countdown = null)
                    saveProgress()
                    break
                }
                _state.value = _state.value.copy(positionBeats = beat)
                delay(16)
            }
        }
    }

    private fun pause(save: Boolean) {
        playbackJob?.cancel()
        playbackJob = null
        _state.value = _state.value.copy(isPlaying = false, countdown = null)
        if (save) saveProgress()
    }

    fun restart() {
        playbackJob?.cancel()
        playbackJob = null
        _state.value = _state.value.copy(positionBeats = 0.0, isPlaying = false, countdown = null)
        saveProgress()
    }

    fun changeTempo(delta: Int) {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        _state.value = _state.value.copy(bpm = (_state.value.bpm + delta).coerceIn(40, 160))
        saveProgress()
    }

    fun setDifficulty(level: DifficultyLevel) {
        if (_state.value.isPlaying || _state.value.countdown != null) return
        _state.value = _state.value.copy(difficulty = level)
        saveProgress()
    }

    private fun saveProgress() {
        val current = _state.value
        val endBeat = current.song.events.maxOfOrNull { it.beat + it.durationBeats } ?: return
        progressRepository.save(
            songId = current.song.id,
            snapshot = ProgressSnapshot(
                positionBeats = current.positionBeats,
                endBeat = endBeat,
                bpm = current.bpm,
                difficultyLevel = current.difficulty.level,
                completedRuns = completedRuns
            )
        )
    }

    override fun onCleared() {
        playbackJob?.cancel()
        saveProgress()
    }
}
