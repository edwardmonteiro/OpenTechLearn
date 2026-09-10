package com.edward.ukuleleai.data.song

import android.content.Context
import com.edward.ukuleleai.domain.Song
import java.io.File
import java.util.UUID

class LocalSongRepository(context: Context) {
    private val songsDir = File(context.filesDir, "songs").apply { mkdirs() }

    fun listSongs(): List<Song> = songsDir
        .listFiles { file -> file.isFile && file.extension == EXTENSION }
        .orEmpty()
        .sortedByDescending { it.lastModified() }
        .mapNotNull { file ->
            SongChartParser.parse(
                raw = file.readText(),
                id = file.nameWithoutExtension
            ).getOrNull()
        }

    fun getRawChart(songId: String): String? =
        File(songsDir, "$songId.$EXTENSION").takeIf { it.exists() }?.readText()

    fun saveChart(raw: String): Result<Song> {
        val id = UUID.randomUUID().toString()
        return saveChart(raw = raw, id = id)
    }

    fun updateChart(songId: String, raw: String): Result<Song> = saveChart(raw = raw, id = songId)

    private fun saveChart(raw: String, id: String): Result<Song> {
        return SongChartParser.parse(raw = raw, id = id).mapCatching { song ->
            File(songsDir, "$id.$EXTENSION").writeText(raw.trim() + "\n")
            song
        }
    }

    companion object {
        private const val EXTENSION = "uke"
    }
}
