package utils.events

@ExperimentalJsExport
@JsExport
class Subscription (
            private val emitter: EventEmitter,
            private val action: (Any?) -> Unit
    ) {
    internal fun doAction(value: Any?) {
        this.action(value)
    }

    fun unsubscribe() {
        this.emitter.unsubscribe(this)
    }
}
