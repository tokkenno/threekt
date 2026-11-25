package com.aitorgf.threekt.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EventDispatcherTest {
    @Test
    fun listenersReceiveTheirTypeOnceAndCanBeRemoved() {
        val dispatcher = EventDispatcher()
        val received = mutableListOf<String>()
        val listener: (DispatchedEvent) -> Unit = { event ->
            assertSame(dispatcher, event.target)
            received.add(event.type)
        }
        dispatcher.addEventListener("change", listener)
        dispatcher.addEventListener("change", listener)
        assertTrue(dispatcher.hasEventListener("change", listener))
        assertFalse(dispatcher.hasEventListener("end", listener))

        dispatcher.dispatchEvent("change")
        dispatcher.dispatchEvent("end")
        assertEquals(listOf("change"), received)

        dispatcher.removeEventListener("change", listener)
        dispatcher.dispatchEvent("change")
        assertEquals(listOf("change"), received)
    }

    @Test
    fun listenersRemovedDuringDispatchStillReceiveTheCurrentEvent() {
        val dispatcher = EventDispatcher()
        val calls = mutableListOf<Int>()
        lateinit var first: (DispatchedEvent) -> Unit
        val second: (DispatchedEvent) -> Unit = { calls.add(2) }
        first = {
            calls.add(1)
            dispatcher.removeEventListener("change", second)
        }
        dispatcher.addEventListener("change", first)
        dispatcher.addEventListener("change", second)
        dispatcher.dispatchEvent("change")
        dispatcher.dispatchEvent("change")
        assertEquals(listOf(1, 2, 1), calls)
    }
}
