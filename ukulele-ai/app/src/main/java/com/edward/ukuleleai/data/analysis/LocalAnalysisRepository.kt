package com.edward.ukuleleai.data.analysis

import android.content.Context
import com.edward.ukuleleai.domain.AnalyzedChord
import com.edward.ukuleleai.domain.AudioAnalysis
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocalAnalysisRepository(context: Context) {
    private val dir = File(context.filesDir, "backing_tracks").apply { mkdirs() }
    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun file(songId: String) = File(dir, "${safe(songId)}.analysis.json")

    fun load(songId: String): AudioAnalysis? = runCatching {
        val f = file(songId)
        if (!f.exists()) return null
        fromJson(JSONObject(f.readText()))
    }.getOrNull()

    fun save(songId: String, analysis: AudioAnalysis) {
        val target = file(songId)
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(toJson(analysis).toString(2))
        if (target.exists()) target.delete()
        check(tmp.renameTo(target)) { "Could not persist analysis." }
    }

    fun remove(songId: String) { file(songId).delete() }

    fun updateChord(songId: String, index: Int, chord: String): AudioAnalysis {
        val current = requireNotNull(load(songId)) { "No analysis found." }
        require(index in current.chords.indices) { "Invalid chord segment." }
        val edited = current.chords.toMutableList()
        edited[index] = edited[index].copy(chord = chord.trim(), confidence = 1f)
        return current.copy(chords = edited).also { save(songId, it) }
    }

    private fun toJson(a: AudioAnalysis): JSONObject = JSONObject().apply {
        put("schemaVersion", a.schemaVersion)
        put("source", a.source)
        put("analyzedAtEpochMs", a.analyzedAtEpochMs)
        put("durationMs", a.durationMs)
        put("bpm", a.bpm.toDouble())
        put("key", a.key)
        put("scale", a.scale)
        put("keyConfidence", a.keyConfidence.toDouble())
        put("chords", JSONArray().apply {
            a.chords.forEach { c -> put(JSONObject().apply {
                put("startMs", c.startMs); put("endMs", c.endMs)
                put("chord", c.chord); put("confidence", c.confidence.toDouble())
            }) }
        })
    }

    private fun fromJson(o: JSONObject): AudioAnalysis {
        val arr = o.getJSONArray("chords")
        val chords = buildList {
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                add(AnalyzedChord(c.getLong("startMs"), c.getLong("endMs"), c.getString("chord"), c.optDouble("confidence", 0.0).toFloat()))
            }
        }
        return AudioAnalysis(
            schemaVersion = o.optInt("schemaVersion", 1),
            source = o.optString("source", "essentia-offline"),
            analyzedAtEpochMs = o.optLong("analyzedAtEpochMs", 0L),
            durationMs = o.getLong("durationMs"),
            bpm = o.optDouble("bpm", 80.0).toFloat(),
            key = o.optString("key", "C"),
            scale = o.optString("scale", "major"),
            keyConfidence = o.optDouble("keyConfidence", 0.0).toFloat(),
            chords = chords
        )
    }
}
