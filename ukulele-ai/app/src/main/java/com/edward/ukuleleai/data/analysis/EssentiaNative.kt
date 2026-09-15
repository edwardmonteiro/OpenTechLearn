package com.edward.ukuleleai.data.analysis

internal object EssentiaNative {
    init { System.loadLibrary("ukulele_analysis") }
    external fun analyze(samples: FloatArray, sampleRate: Int): String
}
