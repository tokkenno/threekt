package utils.events

class Subscription<T> (
            private val emitter: EventEmitter<T>,
            private val action: (T) -> Unit
    ) {
    internal fun doAction(value: T) {
        this.action(value)
    }

    fun unsubscribe() {
        this.emitter.unsubscribe(this)
    }
}