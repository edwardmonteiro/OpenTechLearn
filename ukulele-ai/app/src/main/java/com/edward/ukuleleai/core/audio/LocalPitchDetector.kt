package com.edward.ukuleleai.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Lightweight, local-only monophonic pitch detector for the M0 prototype.
 * It is intentionally tuned for the useful ukulele range and does not upload audio.
 */
class LocalPitchDetector {
    data class Pitch(
        val note: String,
        val frequencyHz: Float,
        val cents: Int,
        val confidence: Float
    )

    private val sampleRate = 44_100
    private val analysisSize = 4_096
    @Volatile private var running = false
    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun start(onPitch: (Pitch?) -> Unit) {
        if (running) return
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(analysisSize * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            onPitch(null)
            return
        }

        record = recorder
        running = true
        recorder.startRecording()

        thread(name = "UkulelePitchDetector", isDaemon = true) {
            val buffer = ShortArray(analysisSize)
            try {
                while (running) {
                    var offset = 0
                    while (offset < buffer.size && running) {
                        val read = recorder.read(buffer, offset, buffer.size - offset)
                        if (read <= 0) break
                        offset += read
                    }
                    if (offset >= analysisSize / 2) onPitch(detect(buffer, offset))
                }
            } finally {
                try { recorder.stop() } catch (_: Throwable) { }
                recorder.release()
                if (record === recorder) record = null
            }
        }
    }

    fun stop() {
        running = false
        try { record?.stop() } catch (_: Throwable) { }
        record = null
    }

    private fun detect(samples: ShortArray, count: Int): Pitch? {
        var sumSq = 0.0
        var mean = 0.0
        for (i in 0 until count) mean += samples[i]
        mean /= count
        for (i in 0 until count) {
            val v = samples[i] - mean
            sumSq += v * v
        }
        val rms = sqrt(sumSq / count)
        if (rms < 450.0) return null

        // Roughly 170–1000 Hz: broad enough for useful ukulele melody playing.
        val minLag = (sampleRate / 1000.0).roundToInt().coerceAtLeast(2)
        val maxLag = (sampleRate / 170.0).roundToInt().coerceAtMost(count / 2)
        var bestLag = -1
        var bestScore = 0.0

        for (lag in minLag..maxLag) {
            var cross = 0.0
            var energyA = 0.0
            var energyB = 0.0
            var i = 0
            while (i + lag < count) {
                val a = samples[i] - mean
                val b = samples[i + lag] - mean
                cross += a * b
                energyA += a * a
                energyB += b * b
                i += 2 // Enough resolution for the prototype, cuts CPU substantially.
            }
            val denom = sqrt(energyA * energyB)
            if (denom <= 0.0) continue
            val score = cross / denom
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (bestLag <= 0 || bestScore < 0.62) return null
        val frequency = sampleRate.toDouble() / bestLag
        if (frequency !in 170.0..1000.0) return null

        val midiFloat = 69.0 + 12.0 * log2(frequency / 440.0)
        val midi = midiFloat.roundToInt()
        val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val noteName = names[((midi % 12) + 12) % 12] + (midi / 12 - 1)
        val nearestHz = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
        val cents = (1200.0 * (ln(frequency / nearestHz) / ln(2.0))).roundToInt().coerceIn(-99, 99)

        return Pitch(
            note = noteName,
            frequencyHz = frequency.toFloat(),
            cents = cents,
            confidence = bestScore.toFloat().coerceIn(0f, 1f)
        )
    }
}
