package com.aitorgf.threekt.core

import com.aitorgf.threekt.types.UUID


@ExperimentalJsExport
@JsExport
external interface Entity {
    val id: UUID
    var name: String
}
