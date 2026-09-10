package com.edward.ukuleleai.data.song

import com.edward.ukuleleai.domain.ChordEvent
import com.edward.ukuleleai.domain.Song
import java.util.UUID

object SongChartParser {
    private val chordRegex = Regex("^[A-G](?:#|b)?[A-Za-z0-9+#b()°/-]*$")

    fun parse(
        raw: String,
        id: String = UUID.randomUUID().toString(),
        defaultBpm: Int = 80,
        beatsPerBar: Int = 4
    ): Result<Song> = runCatching {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "Paste at least one chart line." }

        val title = lines
            .firstOrNull { it.startsWith("Title:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.takeIf { it.isNotBlank() }
            ?: "Imported Song"

        val bpm = lines
            .firstOrNull { it.startsWith("BPM:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.toIntOrNull()?.coerceIn(40, 160)
            ?: defaultBpm

        val chartLines = lines.filterNot {
            it.startsWith("Title:", ignoreCase = true) ||
                it.startsWith("BPM:", ignoreCase = true) ||
                it.startsWith("#")
        }

        val bars = chartLines.flatMap { line ->
            line.split('|').map { it.trim() }.filter { it.isNotBlank() }
        }
        require(bars.isNotEmpty()) {
            "Use bars separated by |. Example: | C | G | Am | F |"
        }

        val events = mutableListOf<ChordEvent>()
        var beat = 0.0
        bars.forEachIndexed { barIndex, bar ->
            val chords = bar.split(Regex("\\s+"))
                .map { it.trim().removeSuffix(",") }
                .filter { it.isNotBlank() }
            require(chords.isNotEmpty()) { "Bar ${barIndex + 1} has no chord." }
            chords.forEach { chord ->
                require(chordRegex.matches(chord)) {
                    "Unsupported chord '$chord' in bar ${barIndex + 1}."
                }
            }
            val duration = beatsPerBar.toDouble() / chords.size
            chords.forEach { chord ->
                events += ChordEvent(chord = chord, beat = beat, durationBeats = duration)
                beat += duration
            }
        }

        Song(id = id, title = title, bpm = bpm, beatsPerBar = beatsPerBar, events = events)
    }
}
