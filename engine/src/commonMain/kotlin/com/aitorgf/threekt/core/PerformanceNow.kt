package com.aitorgf.threekt.core

/**
 * Milliseconds on a monotonic clock, with sub-millisecond precision where the platform provides it.
 * In the browser this is `performance.now()`, the clock of `requestAnimationFrame` timestamps, which
 * Three.js reads in [Clock] and [Timer].
 */
internal expect fun performanceNow(): Double
