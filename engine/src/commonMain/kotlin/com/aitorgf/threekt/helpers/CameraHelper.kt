package com.aitorgf.threekt.helpers

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Usage
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.js.JsExport

/** Displays a camera's frustum in world space. Add it to the scene root and call [update] after camera changes. */
@JsExport
class CameraHelper(val camera: Camera) : LineSegments(
    BufferGeometry().setAttribute("position", Float32BufferAttribute(FloatArray(18 * 6), 3).setUsage(Usage.DynamicDraw)),
    LineBasicMaterial(Color(0xffaa00)).apply { fog = false },
) {
    private val positions = geometry.getAttribute("position") as Float32BufferAttribute
    private val points = Array(9) { Vector3() }

    init {
        update()
    }

    /** Updates the near and far rectangles, connecting edges and the camera's viewing lines. */
    fun update() {
        camera.updateWorldMatrix(true, false)
        val zeroToOne = camera.coordinateSystem == CoordinateSystem.WebGPU || camera.reversedDepth
        val lower = if (zeroToOne) 0.0 else -1.0
        val near = if (camera.reversedDepth) 1.0 else lower
        val far = if (camera.reversedDepth) lower else 1.0
        for (i in 0 until 8) {
            points[i].set(if (i % 2 == 0) -1.0 else 1.0, if (i % 4 < 2) -1.0 else 1.0, if (i < 4) near else far)
                .applyMatrix4(camera.projectionMatrixInverse).applyMatrix4(camera.matrixWorld)
        }
        points[8].setFromMatrixPosition(camera.matrixWorld)
        var offset = 0
        fun line(a: Int, b: Int) {
            for (i in listOf(a, b)) {
                val point = points[i]
                positions.array[offset++] = point.x.toFloat()
                positions.array[offset++] = point.y.toFloat()
                positions.array[offset++] = point.z.toFloat()
            }
        }
        for (i in 0..1) {
            val j = i * 4
            line(j, j + 1)
            line(j + 1, j + 3)
            line(j + 3, j + 2)
            line(j + 2, j)
            line(j, j + 3)
        }
        for (i in 0..3) {
            line(i, i + 4)
            line(8, i)
        }
        positions.markUpdated()
    }
}
