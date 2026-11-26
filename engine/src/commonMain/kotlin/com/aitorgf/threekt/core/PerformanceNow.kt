package com.aitorgf.threekt.core

/**
 * Milliseconds on a monotonic clock, with sub-millisecond precision where the platform provides it.
 * In the browser this is `performance.now()`, the clock of `requestAnimationFrame` timestamps, which
 * [Timer] reads.
 */
internal expect fun performanceNow(): Double
