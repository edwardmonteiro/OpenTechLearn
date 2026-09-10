package com.edward.ukuleleai.domain

object DemoSong {
    val song = Song(
        id = "demo-001",
        title = "First Practice",
        bpm = 80,
        events = listOf(
            ChordEvent("C", 0.0, 4.0, "↓"),
            ChordEvent("G", 4.0, 4.0, "↓"),
            ChordEvent("Am", 8.0, 4.0, "↓"),
            ChordEvent("F", 12.0, 4.0, "↓"),
            ChordEvent("C", 16.0, 4.0, "↓ ↓"),
            ChordEvent("G", 20.0, 4.0, "↓ ↓"),
            ChordEvent("Am", 24.0, 4.0, "↓ ↓"),
            ChordEvent("F", 28.0, 4.0, "↓ ↓")
        )
    )
}
