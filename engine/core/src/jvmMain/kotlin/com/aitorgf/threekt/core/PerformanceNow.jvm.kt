package com.aitorgf.threekt.core

internal actual fun performanceNow(): Double = System.nanoTime() / 1_000_000.0
