package com.aitorgf.threekt.helpers

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.math.Box3
import com.aitorgf.threekt.objects.LineSegments
import kotlin.js.JsExport

/**
 * Draws the edges of the world-space bounding box of [object] as line segments. The box is measured on
 * construction and by [update]; call it after the object moves. The helper's own transform is not
 * updated automatically, so add it to the scene root or to an untransformed parent.
 */
@JsExport
class BoxHelper(`object`: Object3D? = null, color: Int = 0xffff00) :
    LineSegments(createGeometry(), LineBasicMaterial(color)) {
    /** Object whose bounds are drawn, or null for none. */
    var `object`: Object3D? = `object`
        private set

    private val box = Box3()

    init {
        matrixAutoUpdate = false
        update()
    }

    /** Measures [object] again and moves the box corners. Has no effect when the bounds are empty. */
    fun update() {
        `object`?.let { box.setFromObject(it) }
        if (box.isEmpty()) return
        val min = box.min
        val max = box.max
        val position = geometry.getAttribute("position") as Float32BufferAttribute
        val corners = doubleArrayOf(
            max.x, max.y, max.z, min.x, max.y, max.z, min.x, min.y, max.z, max.x, min.y, max.z,
            max.x, max.y, min.z, min.x, max.y, min.z, min.x, min.y, min.z, max.x, min.y, min.z,
        )
        for (index in corners.indices) position.array[index] = corners[index].toFloat()
        position.markUpdated()
        geometry.computeBoundingSphere()
    }

    /** Draws the bounds of [object] instead and returns this helper. */
    fun setFromObject(`object`: Object3D): BoxHelper {
        this.`object` = `object`
        update()
        return this
    }

    private companion object {
        /*
         *   5____4
         * 1/___0/|
         * | 6__|_7
         * 2/___3/
         */
        fun createGeometry(): BufferGeometry = BufferGeometry()
            .setIndex(intArrayOf(0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7))
            .setAttribute("position", Float32BufferAttribute(FloatArray(8 * 3), 3))
    }
}
