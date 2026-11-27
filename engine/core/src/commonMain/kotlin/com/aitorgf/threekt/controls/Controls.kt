package com.aitorgf.threekt.controls

import com.aitorgf.threekt.core.EventDispatcher
import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Base class for controls that drive [obj] from user input on an [InputSurface]. Controls dispatch
 * `change` when they move the object, and `start` and `end` around user interactions. Platforms supply
 * the surface, for example `DomInputSurface` in the browser.
 */
@JsExport
abstract class Controls<T : Object3D>(
    /** Object moved by these controls, usually a camera. Named `object` in JavaScript, as in Three.js. */
    @JsName("object")
    val obj: T,
) : EventDispatcher(), InputListener {
    /** Surface that delivers input, or null while the controls are disconnected. */
    var surface: InputSurface? = null
        private set

    /** Whether input events are handled. Disabled controls still respond to direct method calls. */
    var enabled = true

    /** Starts receiving input from [surface], disconnecting from any previous surface first. */
    fun connect(surface: InputSurface) {
        disconnect()
        this.surface = surface
        surface.addInputListener(this)
        onConnect(surface)
    }

    /** Stops receiving input. Has no effect when the controls are not connected. */
    fun disconnect() {
        val surface = surface ?: return
        onDisconnect(surface)
        surface.removeInputListener(this)
        this.surface = null
    }

    /** Releases input listeners and other resources held by these controls. */
    open fun dispose() {
        disconnect()
    }

    /** Called after [surface] starts delivering input to these controls. */
    protected open fun onConnect(surface: InputSurface) = Unit

    /** Called before [surface] stops delivering input. Resets any in-progress interaction. */
    protected open fun onDisconnect(surface: InputSurface) = Unit

    /**
     * Updates [obj] from the accumulated input. [deltaTime] is the time since the previous update in
     * seconds, or null when the update is not tied to a frame clock. Returns whether the object moved.
     */
    open fun update(deltaTime: Double? = null): Boolean = false

    override fun onPointerDown(input: PointerInput) = Unit
    override fun onPointerMove(input: PointerInput) = Unit
    override fun onPointerUp(input: PointerInput) = Unit
    override fun onWheel(input: WheelInput): Boolean = false
    override fun onContextMenu(): Boolean = false
    override fun onKeyDown(input: KeyInput): Boolean = false
}
