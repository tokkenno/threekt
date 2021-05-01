package core

import types.UUID

@ExperimentalJsExport
@JsExport
class Texture {
    val id: UUID = UUID.randomUUID()

    fun clone(): Texture {
        return Texture()
    }
}
