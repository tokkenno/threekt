package com.aitorgf.threekt.events

actual interface Observable {
    actual fun subscribe(listener: (Any?) -> Unit): Subscription
}
