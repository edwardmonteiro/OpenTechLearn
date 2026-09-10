package com.edward.ukuleleai.data.song

import android.content.Context
import com.edward.ukuleleai.domain.DifficultyLevel

data class ProgressSnapshot(
    val positionBeats: Double,
    val endBeat: Double,
    val bpm: Int,
    val difficultyLevel: Int,
    val completedRuns: Int
) {
    val completionPercent: Int
        get() = if (endBeat <= 0.0) 0 else ((positionBeats / endBeat) * 100.0)
            .toInt()
            .coerceIn(0, 100)
}

class LocalProgressRepository(context: Context) {
    private val prefs = context.getSharedPreferences("practice_progress", Context.MODE_PRIVATE)

    fun load(songId: String): ProgressSnapshot? {
        if (!prefs.contains(key(songId, "position"))) return null
        return ProgressSnapshot(
            positionBeats = prefs.getFloat(key(songId, "position"), 0f).toDouble(),
            endBeat = prefs.getFloat(key(songId, "end"), 0f).toDouble(),
            bpm = prefs.getInt(key(songId, "bpm"), 80),
            difficultyLevel = prefs.getInt(key(songId, "difficulty"), DifficultyLevel.ONE.level),
            completedRuns = prefs.getInt(key(songId, "completed"), 0)
        )
    }

    fun save(songId: String, snapshot: ProgressSnapshot) {
        prefs.edit()
            .putFloat(key(songId, "position"), snapshot.positionBeats.toFloat())
            .putFloat(key(songId, "end"), snapshot.endBeat.toFloat())
            .putInt(key(songId, "bpm"), snapshot.bpm)
            .putInt(key(songId, "difficulty"), snapshot.difficultyLevel)
            .putInt(key(songId, "completed"), snapshot.completedRuns)
            .apply()
    }

    private fun key(songId: String, field: String) = "$songId.$field"
}
