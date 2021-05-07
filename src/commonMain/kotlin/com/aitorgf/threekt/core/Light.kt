package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.events.EventEmitter
import com.aitorgf.threekt.events.Observable

@ExperimentalJsExport
@JsExport
open class Light(
    var color: Color,
    var intensity: Double = 0.0
) : Object3(), Disposable {
    open val type: LightType = LightType.Light

    protected val onDisposeEmitter = EventEmitter()

    override val onDispose: Observable
        get() = onDisposeEmitter

    override fun dispose() {}

    override fun copy(source: Object3, recursive: Boolean): Light {
        super.copy(source)

        if (source is Light) {
            this.color = source.color.clone()
            this.intensity = source.intensity
        }

        return this
    }
}
