package com.edward.ukuleleai.data.midi

import android.content.Context
import android.net.Uri
import com.edward.ukuleleai.data.song.LocalSongRepository
import com.edward.ukuleleai.domain.ChordEvent
import com.edward.ukuleleai.domain.Song
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class MidiSongImporter(
    private val context: Context,
    private val songs: LocalSongRepository
) {
    private val sourceDir = File(context.filesDir, "midi_sources").apply { mkdirs() }

    fun import(uri: Uri, title: String): Song {
        val bytes = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open MIDI file." }
            input.readBytes()
        }
        require(bytes.isNotEmpty()) { "MIDI file is empty." }

        val id = UUID.randomUUID().toString()
        val song = StandardMidiAnalyzer.analyze(bytes, id, title)
        songs.saveSong(song)

        File(sourceDir, "$id.mid").writeBytes(bytes)
        return song
    }
}

object StandardMidiAnalyzer {
    private data class MidiNote(
        val startTick: Long,
        val endTick: Long,
        val note: Int,
        val velocity: Int,
        val channel: Int
    )

    private data class Tempo(val tick: Long, val microsPerQuarter: Int)
    private data class TimeSignature(val tick: Long, val numerator: Int, val denominator: Int)

    fun analyze(bytes: ByteArray, id: String, requestedTitle: String = ""): Song {
        val cursor = Cursor(bytes)
        require(cursor.readAscii(4) == "MThd") { "Not a Standard MIDI file." }
        val headerLength = cursor.readU32().toInt()
        require(headerLength >= 6) { "Invalid MIDI header." }

        val format = cursor.readU16()
        val trackCount = cursor.readU16()
        val division = cursor.readU16()
        require(format in 0..1) { "MIDI format $format is not supported yet." }
        require(trackCount > 0) { "MIDI contains no tracks." }
        require(division and 0x8000 == 0) { "SMPTE-timed MIDI is not supported yet." }
        val ppq = division.coerceAtLeast(1)
        if (headerLength > 6) cursor.skip(headerLength - 6)

        val notes = mutableListOf<MidiNote>()
        val tempos = mutableListOf<Tempo>()
        val signatures = mutableListOf<TimeSignature>()
        val names = mutableListOf<String>()

        repeat(trackCount) {
            require(cursor.remaining >= 8) { "Truncated MIDI track." }
            require(cursor.readAscii(4) == "MTrk") { "Invalid MIDI track header." }
            val length = cursor.readU32().toInt()
            val end = (cursor.pos + length).coerceAtMost(bytes.size)
            parseTrack(cursor, end, notes, tempos, signatures, names)
            cursor.pos = end
        }

        val musicalNotes = notes.filter { it.channel != 9 && it.endTick > it.startTick }
        require(musicalNotes.isNotEmpty()) { "No pitched MIDI notes found." }

        val tempo = tempos.minByOrNull { it.tick }?.microsPerQuarter ?: 500_000
        val bpm = (60_000_000.0 / tempo).roundToInt().coerceIn(40, 220)
        val signature = signatures.minByOrNull { it.tick }
        val beatsPerBar = signature?.numerator?.coerceIn(2, 12) ?: 4
        val title = requestedTitle.trim().ifBlank {
            names.firstOrNull { it.isNotBlank() }?.trim() ?: "Imported MIDI"
        }

        val events = inferChords(musicalNotes, ppq)
        require(events.isNotEmpty()) { "Could not infer chords from MIDI notes." }

        return Song(
            id = id,
            title = title,
            bpm = bpm,
            beatsPerBar = beatsPerBar,
            events = events
        )
    }

    private fun parseTrack(
        cursor: Cursor,
        end: Int,
        notes: MutableList<MidiNote>,
        tempos: MutableList<Tempo>,
        signatures: MutableList<TimeSignature>,
        names: MutableList<String>
    ) {
        var tick = 0L
        var runningStatus = -1
        val active = mutableMapOf<Int, MutableList<Pair<Long, Int>>>()

        fun noteOff(channel: Int, note: Int) {
            val key = channel * 128 + note
            val list = active[key] ?: return
            if (list.isEmpty()) return
            val (start, velocity) = list.removeAt(0)
            notes += MidiNote(start, tick, note, velocity, channel)
            if (list.isEmpty()) active.remove(key)
        }

        while (cursor.pos < end) {
            tick += cursor.readVarLen()
            if (cursor.pos >= end) break

            val peek = cursor.peekU8()
            val status = if (peek >= 0x80) {
                val value = cursor.readU8()
                if (value < 0xF0) runningStatus = value
                value
            } else {
                require(runningStatus >= 0) { "Invalid MIDI running status." }
                runningStatus
            }

            when {
                status in 0x80..0x8F -> {
                    val channel = status and 0x0F
                    val note = cursor.readU8()
                    cursor.readU8()
                    noteOff(channel, note)
                }
                status in 0x90..0x9F -> {
                    val channel = status and 0x0F
                    val note = cursor.readU8()
                    val velocity = cursor.readU8()
                    if (velocity == 0) noteOff(channel, note)
                    else active.getOrPut(channel * 128 + note) { mutableListOf() }.add(tick to velocity)
                }
                status in 0xA0..0xBF || status in 0xE0..0xEF -> {
                    cursor.readU8()
                    cursor.readU8()
                }
                status in 0xC0..0xDF -> cursor.readU8()
                status == 0xFF -> {
                    val type = cursor.readU8()
                    val length = cursor.readVarLen().toInt()
                    val start = cursor.pos
                    when (type) {
                        0x03 -> names += cursor.readBytes(length).toString(Charsets.ISO_8859_1)
                        0x51 -> if (length >= 3) {
                            val a = cursor.readU8()
                            val b = cursor.readU8()
                            val c = cursor.readU8()
                            tempos += Tempo(tick, (a shl 16) or (b shl 8) or c)
                        }
                        0x58 -> if (length >= 2) {
                            val numerator = cursor.readU8()
                            val denominatorPow = cursor.readU8().coerceIn(0, 6)
                            signatures += TimeSignature(tick, numerator, 1 shl denominatorPow)
                        }
                    }
                    cursor.pos = (start + length).coerceAtMost(end)
                    runningStatus = -1
                }
                status == 0xF0 || status == 0xF7 -> {
                    val length = cursor.readVarLen().toInt()
                    cursor.skip(length)
                    runningStatus = -1
                }
                status == 0xF1 || status == 0xF3 -> cursor.skip(1)
                status == 0xF2 -> cursor.skip(2)
                else -> Unit
            }
        }

        active.forEach { (key, starts) ->
            val channel = key / 128
            val note = key % 128
            starts.forEach { (start, velocity) ->
                if (tick > start) notes += MidiNote(start, tick, note, velocity, channel)
            }
        }
    }

    private fun inferChords(notes: List<MidiNote>, ppq: Int): List<ChordEvent> {
        val songEnd = notes.maxOf { it.endTick }
        val window = max(1L, ppq.toLong() / 2L)
        val rootNames = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
        val labels = mutableListOf<String>()
        var start = 0L
        var previous: String? = null

        while (start < songEnd) {
            val end = minOf(songEnd, start + window)
            val histogram = DoubleArray(12)
            var lowestPitch = 128
            var total = 0.0

            notes.forEach { note ->
                val overlap = minOf(end, note.endTick) - maxOf(start, note.startTick)
                if (overlap > 0) {
                    val weight = (overlap.toDouble() / window) * (0.55 + 0.45 * note.velocity / 127.0)
                    histogram[note.note % 12] += weight
                    total += weight
                    if (note.note < lowestPitch) lowestPitch = note.note
                }
            }

            val label = if (total <= 0.0001) {
                previous ?: "C"
            } else {
                var bestLabel = previous ?: "C"
                var bestScore = Double.NEGATIVE_INFINITY

                for (root in 0 until 12) {
                    for (minor in listOf(false, true)) {
                        val third = (root + if (minor) 3 else 4) % 12
                        val fifth = (root + 7) % 12
                        val chordTones = setOf(root, third, fifth)
                        val chordWeight = histogram[root] * 1.35 + histogram[third] * 1.2 + histogram[fifth]
                        val nonChord = histogram.indices.filterNot { it in chordTones }.sumOf { histogram[it] }
                        val present = chordTones.count { histogram[it] > total * 0.04 }
                        val bassBonus = if (lowestPitch < 128 && lowestPitch % 12 == root) 0.35 else 0.0
                        val completeness = when (present) { 3 -> 0.7; 2 -> 0.15; else -> -1.2 }
                        val score = chordWeight - nonChord * 0.55 + bassBonus + completeness
                        if (score > bestScore) {
                            bestScore = score
                            bestLabel = rootNames[root] + if (minor) "m" else ""
                        }
                    }
                }
                bestLabel
            }

            labels += label
            previous = label
            start = end
        }

        if (labels.size >= 3) {
            for (i in 1 until labels.lastIndex) {
                if (labels[i - 1] == labels[i + 1] && labels[i] != labels[i - 1]) {
                    labels[i] = labels[i - 1]
                }
            }
        }

        data class Segment(var label: String, var windows: Int)
        val segments = mutableListOf<Segment>()
        labels.forEach { label ->
            if (segments.lastOrNull()?.label == label) segments.last().windows++
            else segments += Segment(label, 1)
        }

        var i = 1
        while (i < segments.size) {
            if (segments[i].windows == 1) {
                segments[i - 1].windows += segments[i].windows
                segments.removeAt(i)
            } else i++
        }

        val beatPerWindow = window.toDouble() / ppq
        var beat = 0.0
        return segments.map { segment ->
            val duration = segment.windows * beatPerWindow
            ChordEvent(segment.label, beat, duration).also { beat += duration }
        }
    }

    private class Cursor(private val data: ByteArray) {
        var pos: Int = 0
        val remaining: Int get() = data.size - pos

        fun peekU8(): Int {
            require(pos < data.size) { "Unexpected end of MIDI." }
            return data[pos].toInt() and 0xFF
        }

        fun readU8(): Int {
            val value = peekU8()
            pos++
            return value
        }

        fun readU16(): Int = (readU8() shl 8) or readU8()

        fun readU32(): Long =
            (readU8().toLong() shl 24) or
                (readU8().toLong() shl 16) or
                (readU8().toLong() shl 8) or
                readU8().toLong()

        fun readAscii(length: Int): String = readBytes(length).toString(Charsets.US_ASCII)

        fun readBytes(length: Int): ByteArray {
            require(length >= 0 && pos + length <= data.size) { "Unexpected end of MIDI." }
            return data.copyOfRange(pos, pos + length).also { pos += length }
        }

        fun skip(length: Int) {
            require(length >= 0 && pos + length <= data.size) { "Unexpected end of MIDI." }
            pos += length
        }

        fun readVarLen(): Long {
            var value = 0L
            repeat(4) {
                val b = readU8()
                value = (value shl 7) or (b and 0x7F).toLong()
                if (b and 0x80 == 0) return value
            }
            return value
        }
    }
}
