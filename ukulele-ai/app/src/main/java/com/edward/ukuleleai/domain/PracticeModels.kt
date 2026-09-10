package com.edward.ukuleleai.domain

data class Song(
    val id: String,
    val title: String,
    val bpm: Int,
    val beatsPerBar: Int = 4,
    val events: List<ChordEvent>
)

data class ChordEvent(
    val chord: String,
    val beat: Double,
    val durationBeats: Double,
    val strumHint: String = "↓"
)

enum class DifficultyLevel(val level: Int) {
    ONE(1), TWO(2), THREE(3), FOUR(4)
}

data class PracticeState(
    val song: Song,
    val difficulty: DifficultyLevel = DifficultyLevel.ONE,
    val bpm: Int = song.bpm,
    val positionBeats: Double = 0.0,
    val isPlaying: Boolean = false,
    val countdown: Int? = null,
    val soundEnabled: Boolean = true
)
