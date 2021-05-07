package com.aitorgf.threekt.core

import com.aitorgf.threekt.events.Observable

@ExperimentalJsExport
@JsExport
external interface Disposable {
    val onDispose: Observable;
    fun dispose(): Unit
}
