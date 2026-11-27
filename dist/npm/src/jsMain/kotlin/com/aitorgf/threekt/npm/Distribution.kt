package com.aitorgf.threekt.npm

/**
 * Entry point of the npm distribution build.
 *
 * Kotlin/JS only links a compilation that has sources of its own. This file gives `:dist:npm` one, so that
 * linking it emits one ES module per engine module, plus the Kotlin runtime modules, with per-module
 * granularity.
 */
internal val distribution = Unit
