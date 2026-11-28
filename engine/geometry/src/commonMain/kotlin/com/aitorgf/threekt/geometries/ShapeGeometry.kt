package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.extras.ShapeUtils
import com.aitorgf.threekt.extras.core.Shape
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/**
 * Flat triangulation of [shapes] in the XY plane with a +Z normal and world-space UVs, as Three.js
 * `ShapeGeometry`. Curves are sampled with [curveSegments] segments, and each shape gets a draw group
 * with its index as material index. Three.js also accepts a single shape, without groups; Threekt always
 * takes an array.
 */
@JsExport
class ShapeGeometry(
    shapes: Array<Shape> = arrayOf(Shape(arrayOf(Vector2(0.0, 0.5), Vector2(-0.5, -0.5), Vector2(0.5, -0.5)))),
    curveSegments: Int = 12,
) : BufferGeometry() {
    init {
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()

        var groupStart = 0
        for ((shapeIndex, shape) in shapes.withIndex()) {
            val indexOffset = vertices.size / 3
            val groupCount = indices.size
            val points = shape.extractPoints(curveSegments)
            var shapeVertices = points.shape.toMutableList()
            val shapeHoles = points.holes.map { it.toMutableList() }.toMutableList()
            // Check the direction of the vertices.
            if (!ShapeUtils.isClockWise(shapeVertices)) shapeVertices.reverse()
            for (i in shapeHoles.indices) {
                if (ShapeUtils.isClockWise(shapeHoles[i])) shapeHoles[i].reverse()
            }
            val faces = ShapeUtils.triangulateShape(shapeVertices, shapeHoles)
            // Join the vertices of the inner and outer paths into a single list.
            for (hole in shapeHoles) shapeVertices = (shapeVertices + hole).toMutableList()
            for (vertex in shapeVertices) {
                vertices.add(vertex.x.toFloat()); vertices.add(vertex.y.toFloat()); vertices.add(0f)
                normals.add(0f); normals.add(0f); normals.add(1f)
                uvs.add(vertex.x.toFloat()); uvs.add(vertex.y.toFloat()) // World UVs.
            }
            for (face in faces) {
                indices.add(face[0] + indexOffset)
                indices.add(face[1] + indexOffset)
                indices.add(face[2] + indexOffset)
            }
            // One group per shape enables a material per shape.
            addGroup(groupStart, indices.size - groupCount, shapeIndex)
            groupStart = indices.size
        }

        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
