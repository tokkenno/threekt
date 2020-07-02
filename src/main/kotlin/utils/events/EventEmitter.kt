package utils.events

class EventEmitter<T> : Observable<T> {
    private val subscriptions = ArrayList<Subscription<T>>()

    fun emit(value: T) {
        for (subscription in this.subscriptions) {
            subscription.doAction(value)
        }
    }

    override fun subscribe(listener: (T) -> Unit): Subscription<T> {
        val subs = Subscription(this, listener)
        this.subscriptions.add(subs)
        return subs
    }

    internal fun unsubscribe(subscription: Subscription<T>) {
        this.subscriptions.remove(subscription)
    }
}