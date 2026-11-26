package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Uint16BufferAttribute
import com.aitorgf.threekt.core.Uint32BufferAttribute

/**
 * Per-geometry renderer state, as Three.js `WebGLGeometries`. For now it builds the line indices used to
 * draw meshes as wireframes.
 */
internal class WebGLGeometries(private val attributes: WebGLAttributes) {
    private val wireframeAttributes = mutableMapOf<BufferGeometry, BufferAttribute>()

    private fun updateWireframeAttribute(geometry: BufferGeometry) {
        val geometryIndex = geometry.index
        val geometryPosition = geometry.getAttribute("position") ?: return
        val count = geometryIndex?.size ?: (geometryPosition.size / 3 - 1)
        val size = (count + 2) / 3 * 6
        val indices = IntArray(size)
        var version: Int
        var j = 0
        if (geometryIndex != null) {
            version = geometryIndex.version
            for (i in 0 until count step 3) {
                val a = geometryIndex.readRaw(i).toInt()
                val b = geometryIndex.readRaw(i + 1).toInt()
                val c = geometryIndex.readRaw(i + 2).toInt()
                indices[j++] = a; indices[j++] = b
                indices[j++] = b; indices[j++] = c
                indices[j++] = c; indices[j++] = a
            }
        } else {
            version = geometryPosition.version
            for (i in 0 until count step 3) {
                indices[j++] = i; indices[j++] = i + 1
                indices[j++] = i + 1; indices[j++] = i + 2
                indices[j++] = i + 2; indices[j++] = i
            }
        }
        val attribute = if (geometryPosition.count >= 65535) Uint32BufferAttribute(indices, 1)
        else Uint16BufferAttribute(ShortArray(size) { indices[it].toShort() }, 1)
        attribute.attributeVersion = version
        wireframeAttributes.put(geometry, attribute)?.let { attributes.remove(it) }
    }

    /**
     * Returns the line indices that draw the triangle edges of [geometry], rebuilding them when its index
     * changed, or null when it has no positions.
     */
    fun getWireframeAttribute(geometry: BufferGeometry): BufferAttribute? {
        val current = wireframeAttributes[geometry]
        if (current != null) {
            val geometryIndex = geometry.index
            if (geometryIndex != null && current.version < geometryIndex.version) updateWireframeAttribute(geometry)
        } else {
            updateWireframeAttribute(geometry)
        }
        return wireframeAttributes[geometry]
    }
}
