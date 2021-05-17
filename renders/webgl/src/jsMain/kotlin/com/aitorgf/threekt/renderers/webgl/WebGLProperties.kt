package com.aitorgf.threekt.renderers.webgl

@ExperimentalJsExport
@JsExport
class WebGLProperties {
    private val properties: MutableMap<Any, MutableMap<Any, Any>> = HashMap()

    fun get(obj: Any): Any {
        var value = this.properties[obj]
        if (value == null) {
            value = HashMap<Any, Any>()
            properties.set(obj, value)
        }
        return value
    }

    fun remove(obj: Any) {
        this.properties.remove(obj)
    }

    fun update(obj: Any, key: Any, value: Any) {
        var objEntry = properties[obj]
        if (objEntry == null) {
            objEntry = HashMap<Any, Any>()
            properties.set(obj, objEntry)
        }
        objEntry[key] = value
    }

    fun dispose() {
        properties.clear()
    }
}
