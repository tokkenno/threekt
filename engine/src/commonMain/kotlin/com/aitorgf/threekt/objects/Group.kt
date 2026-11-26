package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport

/** An object that only groups its children, for example to transform them together. */
@JsExport
class Group : Object3D() {
    override fun clone(recursive: Boolean): Group = Group().copy(this, recursive) as Group
}
