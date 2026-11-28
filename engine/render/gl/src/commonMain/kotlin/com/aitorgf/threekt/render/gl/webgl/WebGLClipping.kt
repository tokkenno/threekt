package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Plane

/** Projects global and local clipping planes into view space, reusing the upload buffer. */
internal class WebGLClipping(private val gl: GLContext) {
    private val plane = Plane()
    private val normalMatrix = Matrix3()
    private var values = FloatArray(0)

    fun setState(location: Any?, global: Array<Plane>, local: Array<Plane>?, camera: Camera) {
        val size = (global.size + (local?.size ?: 0)) * 4
        if (values.size != size) values = FloatArray(size)
        val view = camera.matrixWorldInverse
        normalMatrix.getNormalMatrix(view)
        projectPlanes(global, 0, view)
        if (local != null) projectPlanes(local, global.size * 4, view)
        gl.uniform4fv(location, values)
    }

    private fun projectPlanes(planes: Array<Plane>, offset: Int, view: Matrix4) {
        for ((index, source) in planes.withIndex()) {
            plane.copy(source).applyMatrix4(view, normalMatrix)
            val slot = offset + index * 4
            values[slot] = plane.normal.x.toFloat()
            values[slot + 1] = plane.normal.y.toFloat()
            values[slot + 2] = plane.normal.z.toFloat()
            values[slot + 3] = plane.constant.toFloat()
        }
    }
}
