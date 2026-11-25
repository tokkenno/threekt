package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/**
 * Adapts pointer, wheel, and context-menu events of a DOM [element], usually a canvas, to an
 * [InputSurface]. Coordinates are client (viewport) pixels. DOM listeners are attached while the surface
 * has at least one [InputListener]; during that time touch scrolling is disabled on the element and each
 * pressed pointer is captured so that moves outside the element are still delivered.
 */
@JsExport
class DomInputSurface(private val element: dynamic) : InputSurface {
    private val listeners = InputListenerList()
    private val pressed = mutableListOf<Int>()
    private val document: dynamic = element.ownerDocument
    private var controlActive = false
    private var previousTouchAction = ""

    override val left: Double
        get() = element.getBoundingClientRect().left.unsafeCast<Double>() + element.clientLeft.unsafeCast<Double>()
    override val top: Double
        get() = element.getBoundingClientRect().top.unsafeCast<Double>() + element.clientTop.unsafeCast<Double>()
    override val width: Double get() = element.clientWidth.unsafeCast<Double>()
    override val height: Double get() = element.clientHeight.unsafeCast<Double>()

    override var cursor = Cursor.Auto
        set(value) {
            field = value
            element.style.cursor = when (value) {
                Cursor.Auto -> "auto"
                Cursor.Grab -> "grab"
                Cursor.Grabbing -> "grabbing"
            }
        }

    override fun addInputListener(listener: InputListener) {
        val wasEmpty = listeners.isEmpty
        if (listeners.add(listener) && wasEmpty) attach()
    }

    override fun removeInputListener(listener: InputListener) {
        if (listeners.remove(listener) && listeners.isEmpty) detach()
    }

    private fun attach() {
        element.addEventListener("pointerdown", onPointerDown)
        element.addEventListener("pointercancel", onPointerUp)
        element.addEventListener("contextmenu", onContextMenu)
        element.addEventListener("wheel", onWheel, js("({ passive: false })"))
        document.addEventListener("keydown", onControlKey, js("({ passive: true, capture: true })"))
        document.addEventListener("keyup", onControlKey, js("({ passive: true, capture: true })"))
        previousTouchAction = element.style.touchAction.unsafeCast<String>()
        element.style.touchAction = "none"
    }

    private fun detach() {
        element.removeEventListener("pointerdown", onPointerDown)
        element.removeEventListener("pointercancel", onPointerUp)
        element.removeEventListener("contextmenu", onContextMenu)
        element.removeEventListener("wheel", onWheel)
        document.removeEventListener("keydown", onControlKey, js("({ capture: true })"))
        document.removeEventListener("keyup", onControlKey, js("({ capture: true })"))
        stopTracking()
        pressed.clear()
        controlActive = false
        element.style.touchAction = previousTouchAction
    }

    private fun stopTracking() {
        document.removeEventListener("pointermove", onPointerMove)
        document.removeEventListener("pointerup", onPointerUp)
    }

    private fun pointerInput(event: dynamic) = PointerInput(
        pointerId = event.pointerId.unsafeCast<Int>(),
        pointerType = when (event.pointerType) {
            "touch" -> PointerType.Touch
            "pen" -> PointerType.Pen
            else -> PointerType.Mouse
        },
        x = event.clientX.unsafeCast<Double>(),
        y = event.clientY.unsafeCast<Double>(),
        button = event.button.unsafeCast<Int>(),
        ctrlKey = event.ctrlKey == true,
        metaKey = event.metaKey == true,
        shiftKey = event.shiftKey == true,
    )

    private val onPointerDown: (dynamic) -> Unit = { event ->
        val input = pointerInput(event)
        if (input.pointerId !in pressed) {
            if (pressed.isEmpty()) {
                element.setPointerCapture(input.pointerId)
                document.addEventListener("pointermove", onPointerMove)
                document.addEventListener("pointerup", onPointerUp)
            }
            pressed.add(input.pointerId)
            listeners.forEach { it.onPointerDown(input) }
        }
    }

    private val onPointerMove: (dynamic) -> Unit = { event ->
        val input = pointerInput(event)
        if (input.pointerId in pressed) listeners.forEach { it.onPointerMove(input) }
    }

    private val onPointerUp: (dynamic) -> Unit = { event ->
        val input = pointerInput(event)
        if (pressed.remove(input.pointerId)) {
            if (pressed.isEmpty()) {
                if (element.hasPointerCapture(input.pointerId) == true) element.releasePointerCapture(input.pointerId)
                stopTracking()
            }
            listeners.forEach { it.onPointerUp(input) }
        }
    }

    private val onWheel: (dynamic) -> Unit = { event ->
        val scale = when (event.deltaMode.unsafeCast<Int>()) {
            1 -> 16.0 // Lines.
            2 -> 100.0 // Pages.
            else -> 1.0
        }
        // Browsers report a trackpad pinch as a wheel event with Ctrl set but no Control key held.
        val input = WheelInput(
            x = event.clientX.unsafeCast<Double>(),
            y = event.clientY.unsafeCast<Double>(),
            deltaY = event.deltaY.unsafeCast<Double>() * scale,
            pinch = event.ctrlKey == true && !controlActive,
        )
        if (listeners.anyHandled { it.onWheel(input) }) event.preventDefault()
    }

    private val onContextMenu: (dynamic) -> Unit = { event ->
        if (listeners.anyHandled { it.onContextMenu() }) event.preventDefault()
    }

    private val onControlKey: (dynamic) -> Unit = { event ->
        if (event.key == "Control") controlActive = event.type == "keydown"
    }
}
