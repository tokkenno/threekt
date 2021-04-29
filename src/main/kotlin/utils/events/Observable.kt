package utils.events


@ExperimentalJsExport
@JsExport
external interface Observable {
    fun subscribe(listener: (Any?) -> Unit): Subscription
}
