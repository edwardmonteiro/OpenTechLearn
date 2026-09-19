package com.edward.ukuleleai.data.song

import com.edward.ukuleleai.domain.ChordEvent
import com.edward.ukuleleai.domain.LyricEvent
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
        val allLines = raw.lines().map { it.trimEnd() }
        val nonBlank = allLines.map { it.trim() }.filter { it.isNotBlank() }
        require(nonBlank.isNotEmpty()) { "Paste at least one chart line." }

        val title = nonBlank
            .firstOrNull { it.startsWith("Title:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.takeIf { it.isNotBlank() }
            ?: "Imported Song"

        val bpm = nonBlank
            .firstOrNull { it.startsWith("BPM:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.toIntOrNull()?.coerceIn(40, 160)
            ?: defaultBpm

        val lyricHeader = allLines.indexOfFirst { it.trim().equals("[Lyrics]", ignoreCase = true) }
        val chartSource = if (lyricHeader >= 0) allLines.take(lyricHeader) else allLines
        val lyricSource = if (lyricHeader >= 0) allLines.drop(lyricHeader + 1) else emptyList()

        val chartLines = chartSource.map { it.trim() }.filter { it.isNotBlank() }.filterNot {
            it.startsWith("Title:", ignoreCase = true) ||
                it.startsWith("BPM:", ignoreCase = true) ||
                it.startsWith("#")
        }

        val explicitEvents = chartLines.mapNotNull { line ->
            val m = Regex("^@event\\s+([0-9.]+)\\s+([0-9.]+)\\s+([^\\s]+)$").matchEntire(line)
            m?.let {
                val eventBeat = it.groupValues[1].toDouble()
                val duration = it.groupValues[2].toDouble()
                val chord = it.groupValues[3]
                require(chordRegex.matches(chord)) { "Unsupported chord '$chord'." }
                ChordEvent(chord, eventBeat, duration)
            }
        }

        val events = mutableListOf<ChordEvent>()
        var beat = 0.0

        if (explicitEvents.isNotEmpty()) {
            events += explicitEvents.sortedBy { it.beat }
            beat = events.maxOf { it.beat + it.durationBeats }
        } else {
            val bars = chartLines.flatMap { line ->
                line.split('|').map { it.trim() }.filter { it.isNotBlank() }
            }
            require(bars.isNotEmpty()) {
                "Use bars separated by |. Example: | C | G | Am | F |"
            }

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
        }

        val lyrics = lyricSource.map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapIndexedNotNull { index, line ->
                val explicit = Regex("^@(\\d+)\\s+(.+)$").matchEntire(line)
                val barNumber = explicit?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)
                val text = explicit?.groupValues?.get(2)?.trim() ?: line
                val startBeat = (barNumber - 1).coerceAtLeast(0) * beatsPerBar.toDouble()
                if (startBeat < beat && text.isNotBlank()) LyricEvent(text, startBeat, beatsPerBar.toDouble()) else null
            }

        Song(id = id, title = title, bpm = bpm, beatsPerBar = beatsPerBar, events = events, lyrics = lyrics)
    }
}
