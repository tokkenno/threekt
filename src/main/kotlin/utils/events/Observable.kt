package utils.events

interface Observable<T> {
    fun subscribe(listener: (T) -> Unit): Subscription<T>
}