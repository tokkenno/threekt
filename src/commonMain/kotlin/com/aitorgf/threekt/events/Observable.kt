package com.aitorgf.threekt.events


@ExperimentalJsExport
@JsExport
external interface Observable {
    fun subscribe(listener: (Any?) -> Unit): Subscription
}
