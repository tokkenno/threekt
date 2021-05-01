package core

@ExperimentalJsExport
@JsExport
class Layers {
    private var mask: Short = 0

    fun disable(layer: Int): Unit {

    }

    fun enable(layer: Int): Unit {

    }

    fun set(layer: Int): Unit {

    }

    fun setLayers(layers: Layers): Unit {
        this.mask = layers.mask
    }

    fun test(layer: Int): Boolean {
        return true
    }

    fun toggle(layer: Int): Unit {

    }

    fun enableAll(): Unit {

    }

    fun disableAll(): Unit {

    }
}
