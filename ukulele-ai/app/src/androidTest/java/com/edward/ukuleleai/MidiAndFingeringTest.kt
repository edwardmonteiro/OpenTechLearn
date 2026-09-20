package com.edward.ukuleleai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edward.ukuleleai.data.song.LocalSongRepository
import com.edward.ukuleleai.data.midi.StandardMidiAnalyzer
import com.edward.ukuleleai.domain.UkuleleFingeringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class MidiAndFingeringTest {

    @Test
    fun type0Midi_infersChordTimeline_andFingeringsExist() {
        val song = StandardMidiAnalyzer.analyze(testMidi(), "midi-test", "Old MIDI")
        assertEquals("Old MIDI", song.title)
        assertEquals(120, song.bpm)
        assertTrue(song.events.size >= 2)
        assertTrue(song.events.map { it.chord }.distinct().size >= 2)
        assertTrue(song.events.first().chord.startsWith("C"))
        assertTrue(song.events.any { it.chord.startsWith("F") })
        assertEquals(2, song.lyrics.size)
        assertEquals("Hello from MIDI", song.lyrics[0].text)
        assertEquals(0.0, song.lyrics[0].beat, 0.001)
        assertEquals("Next line", song.lyrics[1].text)
        assertEquals(2.0, song.lyrics[1].beat, 0.001)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = LocalSongRepository(context)
        repository.saveSong(song)
        val restored = repository.get(song.id)
        assertEquals(2, restored?.lyrics?.size)
        assertEquals("Hello from MIDI", restored?.lyrics?.get(0)?.text)
        assertEquals(2.0, restored?.lyrics?.get(1)?.beat ?: -1.0, 0.001)

        val c = UkuleleFingeringEngine.forChord("C")
        val f = UkuleleFingeringEngine.forChord("F")
        val am = UkuleleFingeringEngine.forChord("Am")
        assertEquals(listOf(0,0,0,3), c?.frets)
        assertEquals(listOf(2,0,1,0), f?.frets)
        assertEquals(listOf(2,0,0,0), am?.frets)
    }

    private fun testMidi(): ByteArray {
        val track = ByteArrayOutputStream()

        meta(track, 0, 0x51, byteArrayOf(0x07, 0xA1.toByte(), 0x20)) // 120 BPM
        meta(track, 0, 0x58, byteArrayOf(4, 2, 24, 8))
        meta(track, 0, 0x05, "Hello from MIDI".toByteArray())

        noteOn(track,0,0,60,100); noteOn(track,0,0,64,96); noteOn(track,0,0,67,94)
        noteOff(track,960,0,60); noteOff(track,0,0,64); noteOff(track,0,0,67)
        meta(track, 0, 0x05, "Next line".toByteArray())

        noteOn(track,0,0,65,100); noteOn(track,0,0,69,96); noteOn(track,0,0,72,94)
        noteOff(track,960,0,65); noteOff(track,0,0,69); noteOff(track,0,0,72)

        meta(track,0,0x2F,byteArrayOf())

        val out=ByteArrayOutputStream()
        out.write("MThd".toByteArray())
        writeU32(out,6)
        writeU16(out,0)
        writeU16(out,1)
        writeU16(out,480)
        val bytes=track.toByteArray()
        out.write("MTrk".toByteArray())
        writeU32(out,bytes.size)
        out.write(bytes)
        return out.toByteArray()
    }

    private fun noteOn(out:ByteArrayOutputStream,delta:Int,ch:Int,note:Int,velocity:Int){
        writeVar(out,delta); out.write(0x90 or ch); out.write(note); out.write(velocity)
    }
    private fun noteOff(out:ByteArrayOutputStream,delta:Int,ch:Int,note:Int){
        writeVar(out,delta); out.write(0x80 or ch); out.write(note); out.write(0)
    }
    private fun meta(out:ByteArrayOutputStream,delta:Int,type:Int,data:ByteArray){
        writeVar(out,delta); out.write(0xFF); out.write(type); writeVar(out,data.size); out.write(data)
    }
    private fun writeU16(out:ByteArrayOutputStream,v:Int){out.write(v shr 8);out.write(v)}
    private fun writeU32(out:ByteArrayOutputStream,v:Int){out.write(v shr 24);out.write(v shr 16);out.write(v shr 8);out.write(v)}
    private fun writeVar(out:ByteArrayOutputStream,value:Int){
        var buffer=value and 0x7F
        var v=value shr 7
        while(v>0){buffer=(buffer shl 8) or ((v and 0x7F) or 0x80);v=v shr 7}
        while(true){out.write(buffer and 0xFF);if(buffer and 0x80!=0)buffer=buffer shr 8 else break}
    }
}
