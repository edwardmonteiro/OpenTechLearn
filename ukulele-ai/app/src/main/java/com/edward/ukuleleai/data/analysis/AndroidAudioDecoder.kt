package com.edward.ukuleleai.data.analysis

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decodes compressed audio with Android platform codecs. No network or FFmpeg. */
object AndroidAudioDecoder {
    data class Decoded(val samples: FloatArray, val sampleRate: Int, val durationMs: Long)

    fun decodeMono(file: File, maxSampleRate: Int = 44100): Decoded {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        var track = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                track = i; format = f; break
            }
        }
        require(track >= 0 && format != null) { "No audio track found." }
        extractor.selectTrack(track)
        val inputFormat = format!!
        val mime = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME))
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        val output = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var outputRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val index = decoder.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = decoder.getInputBuffer(index)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val index = decoder.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = decoder.outputFormat
                        outputRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (index >= 0) {
                        decoder.getOutputBuffer(index)?.let { b ->
                            if (info.size > 0) {
                                b.position(info.offset); b.limit(info.offset + info.size)
                                val bytes = ByteArray(info.size); b.get(bytes); output.write(bytes)
                            }
                        }
                        decoder.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            runCatching { decoder.stop() }; decoder.release(); extractor.release()
        }

        val pcm = output.toByteArray()
        val shorts = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val frameCount = shorts.remaining() / channels
        val mono = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            var sum = 0f
            for (ch in 0 until channels) sum += shorts.get().toFloat() / 32768f
            mono[frame] = sum / channels
        }
        val samples = if (outputRate > maxSampleRate) resampleLinear(mono, outputRate, maxSampleRate) else mono
        val rate = if (outputRate > maxSampleRate) maxSampleRate else outputRate
        val duration = inputFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L) / 1000L
        return Decoded(samples, rate, duration)
    }

    private fun resampleLinear(input: FloatArray, from: Int, to: Int): FloatArray {
        if (input.isEmpty() || from == to) return input
        val size = (input.size.toLong() * to / from).toInt().coerceAtLeast(1)
        return FloatArray(size) { i ->
            val p = i.toDouble() * from / to
            val a = p.toInt().coerceIn(0, input.lastIndex)
            val b = (a + 1).coerceAtMost(input.lastIndex)
            val t = (p - a).toFloat()
            input[a] * (1f - t) + input[b] * t
        }
    }
}
