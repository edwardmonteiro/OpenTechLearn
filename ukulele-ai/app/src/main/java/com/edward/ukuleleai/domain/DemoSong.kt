package com.edward.ukuleleai.domain

object DemoSong {
    val song = Song(
        id = "hoje-eu-vou-example",
        title = "Hoje Eu Vou",
        bpm = 117,
        events = buildList {
            var beat = 0.0
            fun bar(chord: String) { add(ChordEvent(chord, beat, 4.0, "↓")); beat += 4.0 }
            // Simplified beginner arrangement used as the editable example project.
            repeat(2) { listOf("C","Am","F","G").forEach(::bar) }
            listOf("F","C","Dm","G").forEach(::bar)
            listOf("C","G","Am","F","C","G","Am","F","C","G","F","C").forEach(::bar)
            repeat(2) { listOf("C","Am","F","G").forEach(::bar) }
            listOf("C","G","Am","F","C","G","Am","F","C","G","F","C").forEach(::bar)
            listOf("C","F","C","C").forEach(::bar)
        }
    )
}
