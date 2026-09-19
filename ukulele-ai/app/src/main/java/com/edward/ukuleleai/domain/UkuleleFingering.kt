package com.edward.ukuleleai.domain

data class UkuleleFingering(
    val chord: String,
    val frets: List<Int>,
    val fingers: List<Int>
)

object UkuleleFingeringEngine {
    private val tuning = intArrayOf(7, 0, 4, 9) // G C E A

    fun forChord(rawChord: String): UkuleleFingering? {
        val chord = simplifyUkuleleChord(rawChord)
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
        val frettedStrings = frets.indices.filter { frets[it] > 0 }
            .sortedWith(compareBy<Int> { frets[it] }.thenBy { it })

        frettedStrings.forEachIndexed { index, stringIndex ->
            fingers[stringIndex] = (index + 1).coerceAtMost(4)
        }

        return UkuleleFingering(chord, frets, fingers)
    }
}
