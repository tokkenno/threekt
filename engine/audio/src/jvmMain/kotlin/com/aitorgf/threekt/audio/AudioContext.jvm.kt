package com.aitorgf.threekt.audio

internal actual fun createDefaultAudioContext(): AudioGraphContext =
    throw UnsupportedOperationException(
        "Threekt has no JVM audio backend yet. Pass an AudioGraphContext implementation to AudioContext.setContext().",
    )
