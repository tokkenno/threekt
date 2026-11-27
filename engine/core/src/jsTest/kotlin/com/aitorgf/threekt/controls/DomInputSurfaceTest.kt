package com.aitorgf.threekt.controls

import com.aitorgf.threekt.cameras.PerspectiveCamera
import kotlin.math.PI
import kotlin.math.pow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomInputSurfaceTest {
    private var element: dynamic = null

    @BeforeTest
    fun createElement() {
        element = js("document.createElement('div')")
        element.style.width = "200px"
        element.style.height = "100px"
        js("document.body").appendChild(element)
    }

    @AfterTest
    fun removeElement() {
        element.remove()
    }

    private fun wheel(deltaY: Int, deltaMode: Int = 0): dynamic {
        val options: dynamic = js("({ cancelable: true })")
        options.deltaY = deltaY
        options.deltaMode = deltaMode
        return js("new WheelEvent('wheel', options)")
    }

    private fun pointer(type: String, x: Int): dynamic {
        val options: dynamic = js("({ pointerId: 1, pointerType: 'mouse', button: 0, clientY: 50, bubbles: true })")
        options.clientX = x
        return js("new PointerEvent(type, options)")
    }

    @Test
    fun domEventsDriveOrbitControlsUntilDisposed() {
        val camera = PerspectiveCamera(40.0, 2.0, 1.0, 1000.0).apply { position.set(0.0, 0.0, 10.0) }
        val surface = DomInputSurface(element)
        assertEquals(200.0, surface.width)
        assertEquals(100.0, surface.height)
        val controls = OrbitControls(camera, surface)
        assertEquals("none", element.style.touchAction)

        val event = wheel(-100)
        element.dispatchEvent(event)
        assertTrue(event.defaultPrevented == true)
        assertEquals(9.5, controls.getDistance(), 1e-9)
        // Line-based deltas are converted to pixels: 6 lines are 96 px.
        element.dispatchEvent(wheel(-6, deltaMode = 1))
        assertEquals(9.5 * 0.95.pow(0.96), controls.getDistance(), 1e-9)

        element.dispatchEvent(pointer("pointerdown", 100))
        // Moves are tracked on the document so that drags can leave the element.
        js("document").dispatchEvent(pointer("pointermove", 125))
        js("document").dispatchEvent(pointer("pointerup", 125))
        assertEquals(-PI / 2, controls.getAzimuthalAngle(), 1e-9)

        controls.dispose()
        assertEquals("", element.style.touchAction)
        val ignored = wheel(-100)
        element.dispatchEvent(ignored)
        assertFalse(ignored.defaultPrevented == true)
    }

    @Test
    fun keySourceHandlesConfiguredKeysOnly() {
        val controls = OrbitControls(PerspectiveCamera().apply { position.z = 10.0 }, DomInputSurface(element))
        controls.listenToKeyEvents(DomInputSource(element))
        val arrow = js("new KeyboardEvent('keydown', { code: 'ArrowLeft', cancelable: true })")
        element.dispatchEvent(arrow)
        assertTrue(arrow.defaultPrevented == true)
        assertTrue(controls.target.x < 0.0)
        val other = js("new KeyboardEvent('keydown', { code: 'KeyA', cancelable: true })")
        element.dispatchEvent(other)
        assertFalse(other.defaultPrevented == true)
        controls.dispose()
    }
}
