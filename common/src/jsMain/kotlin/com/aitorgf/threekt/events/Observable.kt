package com.aitorgf.threekt.events

@ExperimentalJsExport
@JsExport
actual external interface Observable {
    actual fun subscribe(listener: (Any?) -> Unit): Subscription
}
