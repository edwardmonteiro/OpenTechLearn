package com.edward.ukuleleai.data.analysis

import android.content.Context
import com.edward.ukuleleai.data.song.LocalAudioRepository
import com.edward.ukuleleai.domain.AnalyzedChord
import com.edward.ukuleleai.domain.AudioAnalysis
import org.json.JSONObject

class OfflineChordAnalyzer(context: Context) {
    private val audio = LocalAudioRepository(context)
    private val cache = LocalAnalysisRepository(context)

    fun cached(songId: String): AudioAnalysis? = cache.load(songId)

    fun analyze(songId: String, force: Boolean = false): AudioAnalysis {
        if (!force) cache.load(songId)?.let { return it }
        val file = requireNotNull(audio.find(songId)) { "Attach an MP3, WAV or M4A first." }
        val decoded = AndroidAudioDecoder.decodeMono(file)
        val json = EssentiaNative.analyze(decoded.samples, decoded.sampleRate)
        val o = JSONObject(json)
        val arr = o.getJSONArray("chords")
        val chords = buildList {
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                add(AnalyzedChord(c.getLong("startMs"), c.getLong("endMs"), c.getString("chord"), c.optDouble("confidence", 0.0).toFloat()))
            }
        }
        val result = AudioAnalysis(
            schemaVersion = o.optInt("schemaVersion", 1),
            source = o.optString("source", "essentia-offline"),
            durationMs = o.optLong("durationMs", decoded.durationMs),
            bpm = o.optDouble("bpm", 80.0).toFloat(),
            key = o.optString("key", "C"),
            scale = o.optString("scale", "major"),
            keyConfidence = o.optDouble("keyConfidence", 0.0).toFloat(),
            chords = chords
        )
        require(result.chords.isNotEmpty()) { "No stable chords were detected." }
        cache.save(songId, result)
        return result
    }

    fun editChord(songId: String, index: Int, chord: String): AudioAnalysis = cache.updateChord(songId, index, chord)
}
