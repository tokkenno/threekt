package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * A 32-bit mask of the layers, numbered from 0 to 31, that an [Object3D] belongs to. A camera only
 * renders, and a raycaster only picks, objects that share at least one layer with it. Objects start in
 * layer 0.
 *
 * Threekt stores [mask] as a signed Kotlin `Int`, so a mask that includes layer 31 is negative, whereas
 * Three.js returns it as an unsigned number from [set].
 */
@JsExport
class Layers {
    /** Bit mask of the enabled layers. */
    var mask: Int = 1

    /** Makes [layer] the only enabled layer. */
    fun set(layer: Int) {
        mask = 1 shl layer
    }

    /** Adds [layer] to the enabled layers. */
    fun enable(layer: Int) {
        mask = mask or (1 shl layer)
    }

    /** Enables all 32 layers. */
    fun enableAll() {
        mask = -1
    }

    /** Toggles [layer]. */
    fun toggle(layer: Int) {
        mask = mask xor (1 shl layer)
    }

    /** Removes [layer] from the enabled layers. */
    fun disable(layer: Int) {
        mask = mask and (1 shl layer).inv()
    }

    /** Disables all layers. */
    fun disableAll() {
        mask = 0
    }

    /** Returns whether this mask and [layers] share at least one enabled layer. */
    fun test(layers: Layers): Boolean = (mask and layers.mask) != 0

    /** Returns whether [layer] is enabled. */
    fun isEnabled(layer: Int): Boolean = (mask and (1 shl layer)) != 0
}
