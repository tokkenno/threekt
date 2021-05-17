package com.aitorgf.threekt.events

expect interface Observable {
    fun subscribe(listener: (Any?) -> Unit): Subscription
}
