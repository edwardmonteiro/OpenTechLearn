package com.edward.ukuleleai.domain

data class UkuleleFingering(
    val chord: String,
    val frets: List<Int>,
    val fingers: List<Int>
)

fun ukuleleChordDisplayName(rawChord: String): String {
    val chord = simplifyUkuleleChord(rawChord)
    val names = mapOf(
        "C" to "Dó", "C#" to "Dó♯", "Db" to "Ré♭",
        "D" to "Ré", "D#" to "Ré♯", "Eb" to "Mi♭",
        "E" to "Mi", "F" to "Fá", "F#" to "Fá♯", "Gb" to "Sol♭",
        "G" to "Sol", "G#" to "Sol♯", "Ab" to "Lá♭",
        "A" to "Lá", "A#" to "Lá♯", "Bb" to "Si♭",
        "B" to "Si"
    )
    val match = Regex("^([A-G](?:#|b)?)(m?)$").matchEntire(chord) ?: return chord
    val root = match.groupValues[1]
    val minor = match.groupValues[2] == "m"
    val latin = names[root] ?: return chord
    return if (minor) "$chord · $latin menor" else "$chord · $latin"
}

object UkuleleFingeringEngine {
    private val tuning = intArrayOf(7, 0, 4, 9) // G C E A

    // First choice: familiar beginner shapes, not merely mathematically valid voicings.
    private val canonical = mapOf(
        "C"  to UkuleleFingering("C",  listOf(0,0,0,3), listOf(0,0,0,3)),
        "Cm" to UkuleleFingering("Cm", listOf(0,3,3,3), listOf(0,1,1,1)),
        "D"  to UkuleleFingering("D",  listOf(2,2,2,0), listOf(1,1,1,0)),
        "Dm" to UkuleleFingering("Dm", listOf(2,2,1,0), listOf(2,3,1,0)),
        "E"  to UkuleleFingering("E",  listOf(1,4,0,2), listOf(1,4,0,2)),
        "Em" to UkuleleFingering("Em", listOf(0,4,3,2), listOf(0,3,2,1)),
        "F"  to UkuleleFingering("F",  listOf(2,0,1,0), listOf(2,0,1,0)),
        "Fm" to UkuleleFingering("Fm", listOf(1,0,1,3), listOf(1,0,2,4)),
        "G"  to UkuleleFingering("G",  listOf(0,2,3,2), listOf(0,1,3,2)),
        "Gm" to UkuleleFingering("Gm", listOf(0,2,3,1), listOf(0,2,3,1)),
        "A"  to UkuleleFingering("A",  listOf(2,1,0,0), listOf(2,1,0,0)),
        "Am" to UkuleleFingering("Am", listOf(2,0,0,0), listOf(2,0,0,0)),
        "Bb" to UkuleleFingering("Bb", listOf(3,2,1,1), listOf(4,3,1,1)),
        "B"  to UkuleleFingering("B",  listOf(4,3,2,2), listOf(4,3,1,1)),
        "Bm" to UkuleleFingering("Bm", listOf(4,2,2,2), listOf(3,1,1,1))
    )

    fun forChord(rawChord: String): UkuleleFingering? {
        val chord = simplifyUkuleleChord(rawChord)
        canonical[chord]?.let { return it }

        val match = Regex("^([A-G])([#b]?)(m?)$").matchEntire(chord) ?: return null
        val natural = when (match.groupValues[1]) {
            "C" -> 0; "D" -> 2; "E" -> 4; "F" -> 5
            "G" -> 7; "A" -> 9; "B" -> 11
            else -> return null
        }
        val accidental = when (match.groupValues[2]) {
            "#" -> 1
            "b" -> -1
            else -> 0
        }
        val root = (natural + accidental + 12) % 12
        val minor = match.groupValues[3] == "m"
        val tones = setOf(root, (root + if (minor) 3 else 4) % 12, (root + 7) % 12)

        var best: List<Int>? = null
        var bestScore = Double.POSITIVE_INFINITY

        for (g in 0..4) for (c in 0..4) for (e in 0..4) for (a in 0..4) {
            val frets = listOf(g, c, e, a)
            val pcs = frets.indices.map { (tuning[it] + frets[it]) % 12 }
            if (pcs.any { it !in tones }) continue
            if (!tones.all { it in pcs }) continue

            val fretted = frets.filter { it > 0 }
            val span = if (fretted.isEmpty()) 0 else (fretted.max() - fretted.min())
            var score = fretted.sum() * 0.5 + fretted.size * 1.2 + span * 1.5 + frets.max() * 0.4
            score += (4 - pcs.distinct().size) * 0.2

            if (score < bestScore) {
                bestScore = score
                best = frets
            }
        }

        val frets = best ?: return null
        val fingers = MutableList(4) { 0 }
        frets.indices.filter { frets[it] > 0 }
            .sortedWith(compareBy<Int> { frets[it] }.thenBy { it })
            .forEachIndexed { index, stringIndex ->
                fingers[stringIndex] = (index + 1).coerceAtMost(4)
            }

        return UkuleleFingering(chord, frets, fingers)
    }
}
