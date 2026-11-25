package com.aitorgf.threekt.controls

/** In-memory [InputSurface] that tests drive by calling the listener methods directly. */
class FakeInputSurface(
    override val width: Double = 200.0,
    override val height: Double = 100.0,
) : InputSurface {
    override val left = 0.0
    override val top = 0.0
    override var cursor = Cursor.Auto
    val listeners = mutableListOf<InputListener>()

    override fun addInputListener(listener: InputListener) {
        if (listener !in listeners) listeners.add(listener)
    }

    override fun removeInputListener(listener: InputListener) {
        listeners.remove(listener)
    }

    fun down(id: Int, x: Double, y: Double, type: PointerType = PointerType.Mouse, button: Int = 0, shift: Boolean = false) =
        listeners.toList().forEach { it.onPointerDown(PointerInput(id, type, x, y, button, shiftKey = shift)) }

    fun move(id: Int, x: Double, y: Double, type: PointerType = PointerType.Mouse) =
        listeners.toList().forEach { it.onPointerMove(PointerInput(id, type, x, y)) }

    fun up(id: Int, x: Double, y: Double, type: PointerType = PointerType.Mouse) =
        listeners.toList().forEach { it.onPointerUp(PointerInput(id, type, x, y)) }

    fun wheel(deltaY: Double, pinch: Boolean = false): Boolean =
        listeners.toList().map { it.onWheel(WheelInput(100.0, 50.0, deltaY, pinch)) }.any { it }

    fun key(code: String, shift: Boolean = false): Boolean =
        listeners.toList().map { it.onKeyDown(KeyInput(code, shiftKey = shift)) }.any { it }
}
