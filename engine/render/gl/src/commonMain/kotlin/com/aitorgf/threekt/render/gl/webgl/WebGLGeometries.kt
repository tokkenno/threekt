package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.DispatchedEvent
import com.aitorgf.threekt.core.Uint16BufferAttribute
import com.aitorgf.threekt.core.Uint32BufferAttribute

/**
 * Per-geometry renderer state, as Three.js `WebGLGeometries`. It tracks the geometries in use, so that
 * [BufferGeometry.dispose] releases their GPU buffers, and builds the line indices that draw meshes as
 * wireframes.
 */
internal class WebGLGeometries(private val attributes: WebGLAttributes) {
    private val geometries = mutableSetOf<BufferGeometry>()
    private val wireframeAttributes = mutableMapOf<BufferGeometry, BufferAttribute>()

    /**
     * Version of the index or positions each wireframe attribute was built from. Three.js stores it in the
     * attribute's own version.
     */
    private val wireframeVersions = mutableMapOf<BufferGeometry, Int>()

    private val onGeometryDispose: (DispatchedEvent) -> Unit = { event ->
        val geometry = event.target as BufferGeometry
        geometry.index?.let { attributes.remove(it) }
        for (name in geometry.attributeNames) attributes.remove(geometry.getAttribute(name)!!)
        for (targets in geometry.morphAttributes.values) for (target in targets) attributes.remove(target)
        geometry.removeEventListener(BufferGeometry.DISPOSE, onGeometryDisposeListener)
        geometries.remove(geometry)
        wireframeAttributes.remove(geometry)?.let { attributes.remove(it) }
        wireframeVersions.remove(geometry)
    }
    private val onGeometryDisposeListener: (DispatchedEvent) -> Unit get() = onGeometryDispose

    /** Starts tracking [geometry] so that its `dispose` event releases its buffers. Returns [geometry]. */
    fun get(geometry: BufferGeometry): BufferGeometry {
        if (geometries.add(geometry)) geometry.addEventListener(BufferGeometry.DISPOSE, onGeometryDispose)
        return geometry
    }

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
                val a = geometryIndex.getX(i).toInt()
                val b = geometryIndex.getX(i + 1).toInt()
                val c = geometryIndex.getX(i + 2).toInt()
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
        wireframeVersions[geometry] = version
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
            if (geometryIndex != null && wireframeVersions.getValue(geometry) < geometryIndex.version) {
                updateWireframeAttribute(geometry)
            }
        } else {
            updateWireframeAttribute(geometry)
        }
        return wireframeAttributes[geometry]
    }

    /** Stops tracking every geometry. Their buffers are released by [WebGLAttributes.dispose]. */
    fun dispose() {
        for (geometry in geometries) geometry.removeEventListener(BufferGeometry.DISPOSE, onGeometryDispose)
        geometries.clear()
        wireframeAttributes.clear()
        wireframeVersions.clear()
    }
}
