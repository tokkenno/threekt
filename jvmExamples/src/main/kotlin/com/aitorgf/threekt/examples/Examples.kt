package com.aitorgf.threekt.examples

/** Examples available in the JVM app, in menu order. Add new ports here. */
object Examples {
    val all = listOf(
        ExampleEntry("webgl_buffergeometry_rawshader", "Geometría con shader propio", ::WebglBuffergeometryRawshader),
        ExampleEntry("webgl_buffergeometry", "Triángulos con iluminación Phong", ::WebglBuffergeometry),
    )

    /** Returns the example with the upstream [id], or null when it has not been ported to the JVM. */
    fun find(id: String): ExampleEntry? = all.firstOrNull { it.id == id }
}
