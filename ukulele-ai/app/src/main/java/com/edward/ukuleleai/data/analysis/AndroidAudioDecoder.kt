package com.edward.ukuleleai.data.analysis

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecList
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
        try {
            extractor.setDataSource(file.absolutePath)
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(i)
                if (candidate.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i
                    format = candidate
                    break
                }
            }
            require(track >= 0 && format != null) { "No audio track found." }
            extractor.selectTrack(track)
            val inputFormat = requireNotNull(format)
            val mime = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME))
            val decoder = createStartedDecoder(mime, inputFormat)

            val output = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var outputRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            try {
                while (!outputDone) {
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = requireNotNull(decoder.getInputBuffer(index))
                            buffer.clear()
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
                            val out = decoder.outputFormat
                            outputRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                            if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                pcmEncoding = out.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (index >= 0) {
                            decoder.getOutputBuffer(index)?.let { buffer ->
                                if (info.size > 0) {
                                    buffer.position(info.offset)
                                    buffer.limit(info.offset + info.size)
                                    val bytes = ByteArray(info.size)
                                    buffer.get(bytes)
                                    output.write(bytes)
                                }
                            }
                            decoder.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
            } finally {
                runCatching { decoder.stop() }
                runCatching { decoder.release() }
            }

            val mono = pcmToMono(output.toByteArray(), channels, pcmEncoding)
            require(mono.isNotEmpty()) { "Android decoder produced no PCM audio." }
            val samples = if (outputRate > maxSampleRate) resampleLinear(mono, outputRate, maxSampleRate) else mono
            val rate = if (outputRate > maxSampleRate) maxSampleRate else outputRate
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) inputFormat.getLong(MediaFormat.KEY_DURATION) else 0L
            val duration = if (durationUs > 0) durationUs / 1000L else samples.size * 1000L / rate.coerceAtLeast(1)
            return Decoded(samples, rate, duration)
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * Some Android images expose a codec that can be created but is already released when
     * configure() is called. Try every matching decoder explicitly instead of trusting
     * createDecoderByType() to select a usable implementation.
     */
    private fun createStartedDecoder(mime: String, inputFormat: MediaFormat): MediaCodec {
        val infos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { info -> !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) } }
            .sortedBy { info ->
                when {
                    info.name.startsWith("c2.android.") -> 0
                    info.name.startsWith("OMX.google.") -> 1
                    else -> 2
                }
            }

        var lastError: Throwable? = null
        for (info in infos) {
            var codec: MediaCodec? = null
            try {
                codec = MediaCodec.createByCodecName(info.name)
                codec.configure(inputFormat, null, null, 0)
                codec.start()
                return codec
            } catch (t: Throwable) {
                lastError = t
                runCatching { codec?.release() }
            }
        }

        throw IllegalStateException(
            "No usable Android decoder for $mime (${infos.joinToString { it.name }}).",
            lastError
        )
    }

    private fun pcmToMono(bytes: ByteArray, channels: Int, encoding: Int): FloatArray {
        if (bytes.isEmpty()) return FloatArray(0)
        val ch = channels.coerceAtLeast(1)
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val frameCount = bytes.size / (bytesPerSample * ch)
        val mono = FloatArray(frameCount)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        fun readSample(): Float = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> ((buffer.get().toInt() and 0xFF) - 128) / 128f
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                val b0 = buffer.get().toInt() and 0xFF
                val b1 = buffer.get().toInt() and 0xFF
                val b2 = buffer.get().toInt()
                val value = b0 or (b1 shl 8) or (b2 shl 16)
                value / 8388608f
            }
            AudioFormat.ENCODING_PCM_32BIT -> buffer.int / 2147483648f
            AudioFormat.ENCODING_PCM_FLOAT -> buffer.float.coerceIn(-1f, 1f)
            else -> buffer.short / 32768f
        }

        for (frame in 0 until frameCount) {
            var sum = 0f
            repeat(ch) { sum += readSample() }
            mono[frame] = sum / ch
        }
        return mono
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
