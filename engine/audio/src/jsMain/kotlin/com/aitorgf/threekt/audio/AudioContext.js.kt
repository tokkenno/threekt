package com.aitorgf.threekt.audio

internal actual fun createDefaultAudioContext(): AudioGraphContext = WebAudioContext()
