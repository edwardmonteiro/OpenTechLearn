package com.edward.ukuleleai.domain

import kotlin.math.roundToInt

data class AnalyzedChord(
    val startMs: Long,
    val endMs: Long,
    val chord: String,
    val confidence: Float
)

data class AudioAnalysis(
    val schemaVersion: Int = 1,
    val source: String = "essentia-offline",
    val analyzedAtEpochMs: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val bpm: Float,
    val key: String,
    val scale: String,
    val keyConfidence: Float,
    val chords: List<AnalyzedChord>
) {
    fun toSong(id: String, title: String, lyrics: List<LyricEvent> = emptyList()): Song {
        val songBpm = bpm.roundToInt().coerceIn(40, 200)
        val events = chords
            .filter { it.endMs > it.startMs && it.chord.isNotBlank() }
            .map {
                val startBeat = it.startMs * songBpm / 60_000.0
                val durationBeats = ((it.endMs - it.startMs) * songBpm / 60_000.0).coerceAtLeast(0.05)
                ChordEvent(it.chord, startBeat, durationBeats)
            }
        return Song(
            id = id,
            title = title,
            bpm = songBpm,
            beatsPerBar = 4,
            events = events.ifEmpty { listOf(ChordEvent(key.ifBlank { "C" }, 0.0, 4.0)) },
            lyrics = lyrics
        )
    }
}

fun simplifyUkuleleChord(chord: String): String {
    val match = Regex("^([A-G](?:#|b)?)(.*)$").matchEntire(chord.trim()) ?: return chord
    val root = match.groupValues[1]
    val suffix = match.groupValues[2].lowercase()
    val minor = suffix.startsWith("m") && !suffix.startsWith("maj")
    return root + if (minor) "m" else ""
}

fun displayChordAtBeat(song: Song, beat: Double, beginnerMode: Boolean): String? {
    val chord = song.events.lastOrNull { beat >= it.beat && beat < it.beat + it.durationBeats }?.chord
        ?: song.events.firstOrNull()?.chord
    return chord?.let { if (beginnerMode) simplifyUkuleleChord(it) else it }
}
