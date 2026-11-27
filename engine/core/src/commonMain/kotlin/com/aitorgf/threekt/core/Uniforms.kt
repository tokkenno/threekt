package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Named [Uniform]s of a shader material. Three.js uses a plain object, as in `uniforms.time.value`; in
 * Threekt the same uniform is `uniforms.get("time").value`.
 */
@JsExport
class Uniforms {
    private val uniforms = linkedMapOf<String, Uniform>()

    /** Registers [uniform] under [name], replacing any previous uniform. Returns this collection. */
    fun set(name: String, uniform: Uniform): Uniforms {
        uniforms[name] = uniform
        return this
    }

    /** Returns the uniform registered under [name], or null when there is none. */
    fun get(name: String): Uniform? = uniforms[name]

    /** Removes the uniform registered under [name], if any. Returns this collection. */
    fun remove(name: String): Uniforms {
        uniforms.remove(name)
        return this
    }

    /** Names of the registered uniforms in insertion order. */
    val names: Array<String> get() = uniforms.keys.toTypedArray()
}
