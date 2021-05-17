package com.aitorgf.threekt.types

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
data class MapEntry(
    val key: String,
    var value: Any
)
