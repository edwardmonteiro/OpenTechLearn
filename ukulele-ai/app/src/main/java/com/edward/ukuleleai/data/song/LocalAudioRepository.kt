package com.edward.ukuleleai.data.song

import android.content.Context
import android.net.Uri
import java.io.File

class LocalAudioRepository(private val context: Context) {
    private val dir = File(context.filesDir, "backing_tracks").apply { mkdirs() }
    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
    fun import(songId: String, uri: Uri): Result<File> = runCatching {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("wav") -> "wav"
            mime.contains("mp4") || mime.contains("aac") || mime.contains("m4a") -> "m4a"
            else -> "mp3"
        }
        dir.listFiles()?.filter { it.name.startsWith(safe(songId) + ".") }?.forEach { it.delete() }
        val target = File(dir, "${safe(songId)}.$ext")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open audio file." }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        require(target.length() > 0) { "Audio file is empty." }
        target
    }
    fun find(songId: String): File? = dir.listFiles()?.firstOrNull { it.name.startsWith(safe(songId) + ".") && it.length() > 0 }
    fun remove(songId: String) { dir.listFiles()?.filter { it.name.startsWith(safe(songId) + ".") }?.forEach { it.delete() } }
}
