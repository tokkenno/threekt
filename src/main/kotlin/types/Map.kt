package types

@ExperimentalJsExport
@JsExport
class Map {
    private var entries: Array<MapEntry> = emptyArray()

    val size: Int
        get() = this.entries.size

    val keys: Array<String>
        get() = this.entries.map { it.key }.toTypedArray()

    val values: Array<dynamic>
        get() = this.entries.map { it.value }.toTypedArray()

    fun containsKey(key: String): Boolean {
        return this.entries.indexOfFirst { it.key == key } != -1
    }

    fun get(key: String): dynamic {
        val index = this.entries.indexOfFirst { it.key == key }
        if (index == -1) {
            return null
        } else {
            return this.entries[index].value
        }
    }

    fun getOrDefault(key: String, default: dynamic): dynamic {
        return this.get(key) ?: default
    }

    fun set(key: String, value: String) {
        val index = this.entries.indexOfFirst { it.key == key }
        if (index == -1) {
            val tmpSet = entries.toMutableSet()
            tmpSet.add(MapEntry(key, value))
            this.entries = tmpSet.toTypedArray()
        } else {
            this.entries[index].value = value
        }
    }
}
