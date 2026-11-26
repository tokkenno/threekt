package com.aitorgf.threekt.core

internal actual fun performanceNow(): Double = js("performance.now()").unsafeCast<Double>()
