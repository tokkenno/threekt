package com.aitorgf.threekt.events

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
class EventEmitter : Observable {
    private val subscriptions = ArrayList<Subscription>()

    fun emit(value: Any? = null) {
        for (subscription in this.subscriptions) {
            subscription.doAction(value)
        }
    }

    override fun subscribe(listener: (Any?) -> Unit): Subscription {
        val subs = Subscription(this, listener)
        this.subscriptions.add(subs)
        return subs
    }

    internal fun unsubscribe(subscription: Subscription) {
        this.subscriptions.remove(subscription)
    }
}
