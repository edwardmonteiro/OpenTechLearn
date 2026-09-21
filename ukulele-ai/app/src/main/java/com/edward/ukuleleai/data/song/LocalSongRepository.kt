package com.edward.ukuleleai.data.song

import android.content.Context
import com.edward.ukuleleai.data.analysis.LocalAnalysisRepository
import com.edward.ukuleleai.domain.LyricEvent
import com.edward.ukuleleai.domain.Song
import java.io.File
import java.util.UUID

class LocalSongRepository(context: Context) {
    private val songsDir = File(context.filesDir, "songs").apply { mkdirs() }
    private val analysis = LocalAnalysisRepository(context)

    fun listSongs(): List<Song> = songsDir
        .listFiles { file -> file.isFile && file.extension == EXTENSION }
        .orEmpty()
        .sortedByDescending { it.lastModified() }
        .mapNotNull { file ->
            val base = SongChartParser.parse(file.readText(), file.nameWithoutExtension).getOrNull() ?: return@mapNotNull null
            analysis.load(base.id)?.toSong(base.id, base.title, base.lyrics) ?: base
        }

    fun get(songId: String): Song? = listSongs().firstOrNull { it.id == songId }

    fun getRawChart(songId: String): String? =
        File(songsDir, "$songId.$EXTENSION").takeIf { it.exists() }?.readText()

    fun saveChart(raw: String): Result<Song> {
        val id = UUID.randomUUID().toString()
        return saveChart(raw = raw, id = id)
    }

    fun createAudioSong(title: String): Song {
        val id = UUID.randomUUID().toString()
        val safeTitle = title.trim().ifBlank { "Imported MP3" }
        val raw = "Title: $safeTitle\nBPM: 80\n| C |\n"
        return saveChart(raw, id).getOrThrow()
    }

    fun updateChart(songId: String, raw: String): Result<Song> = saveChart(raw = raw, id = songId)

    fun saveSong(song: Song): Song {
        val raw = buildString {
            appendLine("Title: ${song.title}")
            appendLine("BPM: ${song.bpm}")
            song.events.forEach { event ->
                appendLine("@event ${event.beat} ${event.durationBeats} ${event.chord}")
            }
            if (song.lyrics.isNotEmpty()) {
                appendLine("[Lyrics]")
                song.lyrics.sortedBy { it.beat }.forEach { lyric ->
                    appendLine("@beat ${lyric.beat} ${lyric.durationBeats} ${lyric.text.replace("\n", " ").trim()}")
                }
            }
        }
        File(songsDir, "${song.id}.$EXTENSION").writeText(raw)
        return song
    }

    fun updateLyrics(songId: String, lyrics: List<LyricEvent>): Song {
        val current = get(songId) ?: error("Song not found.")
        return saveSong(current.copy(lyrics = lyrics.sortedBy { it.beat }))
    }

    private fun saveChart(raw: String, id: String): Result<Song> {
        return SongChartParser.parse(raw = raw, id = id).mapCatching { song ->
            File(songsDir, "$id.$EXTENSION").writeText(raw.trim() + "\n")
            song
        }
    }

    companion object { private const val EXTENSION = "uke" }
}
