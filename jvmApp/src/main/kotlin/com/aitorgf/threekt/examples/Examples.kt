package com.aitorgf.threekt.examples

/** Examples available in the JVM app, in menu order. Add new ports here. */
object Examples {
    val all = listOf(
        ExampleEntry("webgl_buffergeometry_rawshader", "Geometría con shader propio", { context, _ -> WebglBuffergeometryRawshader(context) }),
        ExampleEntry("webgl_buffergeometry", "Triángulos con iluminación Phong", { context, _ -> WebglBuffergeometry(context) }),
        ExampleEntry("webgl_loader_obj", "Carga de un modelo Wavefront OBJ", ::webgl_loader_obj),
        ExampleEntry("webgl_interactive_voxelpainter", "Editor de vóxeles", ::webgl_interactive_voxelpainter),
    )

    /** Returns the example with the upstream [id], or null when it has not been ported to the JVM. */
    fun find(id: String): ExampleEntry? = all.firstOrNull { it.id == id }
}
