package com.aitorgf.threekt.core

import com.aitorgf.threekt.types.UUID

@ExperimentalJsExport
@JsExport
class Texture {
    val id: UUID = UUID.randomUUID()

    fun clone(): Texture {
        return Texture()
    }
}
