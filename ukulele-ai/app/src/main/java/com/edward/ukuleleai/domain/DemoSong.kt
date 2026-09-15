package com.edward.ukuleleai.domain

object DemoSong {
    val song = Song(
        id = "hoje-eu-vou-example",
        title = "Hoje Eu Vou",
        bpm = 117,
        events = buildList {
            var beat = 0.0
            fun bar(chord: String) { add(ChordEvent(chord, beat, 4.0, "↓")); beat += 4.0 }
            repeat(2) { listOf("C","Am","F","G").forEach(::bar) }
            listOf("F","C","Dm","G").forEach(::bar)
            listOf("C","G","Am","F","C","G","Am","F","C","G","F","C").forEach(::bar)
            repeat(2) { listOf("C","Am","F","G").forEach(::bar) }
            listOf("C","G","Am","F","C","G","Am","F","C","G","F","C").forEach(::bar)
            listOf("C","F","C","C").forEach(::bar)
        },
        lyrics = listOf(
            LyricEvent("Hoje eu vou", 0.0, 4.0),
            LyricEvent("seguir o som", 4.0, 4.0),
            LyricEvent("deixar o medo", 8.0, 4.0),
            LyricEvent("ficar pra trás", 12.0, 4.0),
            LyricEvent("Um acorde", 16.0, 4.0),
            LyricEvent("de cada vez", 20.0, 4.0),
            LyricEvent("eu encontro", 24.0, 4.0),
            LyricEvent("o meu lugar", 28.0, 4.0)
        )
    )
}
