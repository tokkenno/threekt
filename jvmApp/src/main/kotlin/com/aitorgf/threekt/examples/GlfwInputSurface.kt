package com.aitorgf.threekt.examples

import com.aitorgf.threekt.controls.Cursor
import com.aitorgf.threekt.controls.InputListener
import com.aitorgf.threekt.controls.InputSurface
import com.aitorgf.threekt.controls.PointerInput
import com.aitorgf.threekt.controls.PointerType
import com.aitorgf.threekt.controls.WheelInput
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW

/** Adapts GLFW mouse callbacks to the shared controls API in logical window coordinates. */
internal class GlfwInputSurface(private val window: Long) : InputSurface, AutoCloseable {
    override val left = 0.0
    override val top = 0.0
    override var width = 0.0
        private set
    override var height = 0.0
        private set

    private val listeners = linkedSetOf<InputListener>()
    private val x = DoubleArray(1)
    private val y = DoubleArray(1)
    private var pressedButton: Int? = null
    private var handCursor = 0L

    override var cursor = Cursor.Auto
        set(value) {
            if (field == value) return
            field = value
            if (value != Cursor.Auto && handCursor == 0L) {
                handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR)
            }
            GLFW.glfwSetCursor(window, if (value == Cursor.Auto) 0L else handCursor)
        }

    init {
        GLFW.glfwSetCursorPosCallback(window) { _, mouseX, mouseY ->
            x[0] = mouseX
            y[0] = mouseY
            if (pressedButton != null) {
                val input = pointer()
                listeners.toList().forEach { it.onPointerMove(input) }
            }
        }
        GLFW.glfwSetMouseButtonCallback(window) { _, button, action, mods ->
            if (action == GLFW.GLFW_PRESS && pressedButton == null && button in GLFW.GLFW_MOUSE_BUTTON_LEFT..GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                pressedButton = button
                GLFW.glfwGetCursorPos(window, x, y)
                val input = pointer(button, mods)
                listeners.toList().forEach { it.onPointerDown(input) }
            } else if (action == GLFW.GLFW_RELEASE && pressedButton == button) {
                releasePointer()
            }
        }
        GLFW.glfwSetScrollCallback(window) { _, _, vertical ->
            GLFW.glfwGetCursorPos(window, x, y)
            val input = WheelInput(x[0], y[0], deltaY = -vertical * 100)
            listeners.toList().forEach { it.onWheel(input) }
        }
        GLFW.glfwSetWindowFocusCallback(window) { _, focused -> if (!focused) releasePointer() }
    }

    fun resize(width: Int, height: Int) {
        this.width = width.toDouble()
        this.height = height.toDouble()
    }

    override fun addInputListener(listener: InputListener) {
        listeners.add(listener)
    }

    override fun removeInputListener(listener: InputListener) {
        listeners.remove(listener)
    }

    private fun pointer(button: Int = GLFW.GLFW_MOUSE_BUTTON_LEFT, mods: Int = 0): PointerInput = PointerInput(
        0, PointerType.Mouse, x[0], y[0],
        button = when (button) {
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 1
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 2
            else -> 0
        },
        ctrlKey = mods and GLFW.GLFW_MOD_CONTROL != 0,
        metaKey = mods and GLFW.GLFW_MOD_SUPER != 0,
        shiftKey = mods and GLFW.GLFW_MOD_SHIFT != 0,
    )

    private fun releasePointer() {
        if (pressedButton == null) return
        pressedButton = null
        val input = pointer()
        listeners.toList().forEach { it.onPointerUp(input) }
    }

    override fun close() {
        Callbacks.glfwFreeCallbacks(window)
        cursor = Cursor.Auto
        if (handCursor != 0L) GLFW.glfwDestroyCursor(handCursor)
    }
}
