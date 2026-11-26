package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.extras.ShapeUtils
import com.aitorgf.threekt.extras.core.Shape
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Extrudes [shapes] along +Z by [Options.depth], with optional bevels, as Three.js `ExtrudeGeometry`.
 * The geometry is non-indexed and its normals come from [computeVertexNormals]. Three.js takes a
 * parameters object and also accepts a single shape; Threekt takes an array and an [Options] instance.
 * Extrusion along a 3D path (`extrudePath`) and draw groups are not supported yet.
 */
@JsExport
class ExtrudeGeometry(
    shapes: Array<Shape> = arrayOf(
        Shape(arrayOf(Vector2(0.5, 0.5), Vector2(-0.5, 0.5), Vector2(-0.5, -0.5), Vector2(0.5, -0.5))),
    ),
    options: Options = Options(),
) : BufferGeometry() {
    /** Extrusion settings with the Three.js defaults. */
    class Options {
        /** Number of points sampled on each curve of the shapes. */
        var curveSegments = 12
        /** Number of segments along the depth. */
        var steps = 1
        /** Depth of the extrusion. */
        var depth = 1.0
        /** Whether the caps are beveled. */
        var bevelEnabled = true
        /** How deep the bevel goes into the shape. */
        var bevelThickness = 0.2
        /** Distance from the outline that the bevel extends, or null for `bevelThickness - 0.1`. */
        var bevelSize: Double? = null
        /** Distance from the outline at which the bevel starts. */
        var bevelOffset = 0.0
        /** Number of bevel layers. */
        var bevelSegments = 3
        /** Generates the UVs of the caps and side walls. */
        var uvGenerator: UVGenerator = WorldUVGenerator
    }

    /** Computes UVs for extruded faces, as the Three.js `UVGenerator` option. */
    interface UVGenerator {
        /** Returns the UVs of the cap triangle whose vertices start at the given vertex indices. */
        fun generateTopUV(geometry: ExtrudeGeometry, vertices: List<Double>, indexA: Int, indexB: Int, indexC: Int): Array<Vector2>

        /** Returns the UVs of the side-wall quad whose vertices start at the given vertex indices. */
        fun generateSideWallUV(
            geometry: ExtrudeGeometry, vertices: List<Double>, indexA: Int, indexB: Int, indexC: Int, indexD: Int,
        ): Array<Vector2>
    }

    /** Uses the X and Y coordinates for the caps and the dominant axis and depth for the side walls. */
    object WorldUVGenerator : UVGenerator {
        override fun generateTopUV(geometry: ExtrudeGeometry, vertices: List<Double>, indexA: Int, indexB: Int, indexC: Int) = arrayOf(
            Vector2(vertices[indexA * 3], vertices[indexA * 3 + 1]),
            Vector2(vertices[indexB * 3], vertices[indexB * 3 + 1]),
            Vector2(vertices[indexC * 3], vertices[indexC * 3 + 1]),
        )

        override fun generateSideWallUV(
            geometry: ExtrudeGeometry, vertices: List<Double>, indexA: Int, indexB: Int, indexC: Int, indexD: Int,
        ): Array<Vector2> {
            val indices = intArrayOf(indexA, indexB, indexC, indexD)
            val useX = abs(vertices[indexA * 3 + 1] - vertices[indexB * 3 + 1]) < abs(vertices[indexA * 3] - vertices[indexB * 3])
            return Array(4) {
                val index = indices[it]
                Vector2(if (useX) vertices[index * 3] else vertices[index * 3 + 1], 1 - vertices[index * 3 + 2])
            }
        }
    }

    init {
        val verticesArray = mutableListOf<Double>()
        val uvArray = mutableListOf<Double>()
        for (shape in shapes) addShape(shape, options, verticesArray, uvArray)
        setAttribute("position", Float32BufferAttribute(FloatArray(verticesArray.size) { verticesArray[it].toFloat() }, 3))
        setAttribute("uv", Float32BufferAttribute(FloatArray(uvArray.size) { uvArray[it].toFloat() }, 2))
        computeVertexNormals()
    }

    private fun addShape(shape: Shape, options: Options, verticesArray: MutableList<Double>, uvArray: MutableList<Double>) {
        val placeholder = mutableListOf<Double>()
        val curveSegments = options.curveSegments
        val steps = options.steps
        val depth = options.depth
        val bevelEnabled = options.bevelEnabled
        var bevelThickness = options.bevelThickness
        var bevelSize = options.bevelSize ?: (bevelThickness - 0.1)
        var bevelOffset = options.bevelOffset
        var bevelSegments = options.bevelSegments
        val uvgen = options.uvGenerator

        // Safeguards if bevels are not enabled.
        if (!bevelEnabled) {
            bevelSegments = 0
            bevelThickness = 0.0
            bevelSize = 0.0
            bevelOffset = 0.0
        }

        val shapePoints = shape.extractPoints(curveSegments)
        var vertices = shapePoints.shape.toMutableList()
        val holes = shapePoints.holes.map { it.toMutableList() }.toMutableList()
        val reverse = !ShapeUtils.isClockWise(vertices)
        if (reverse) {
            vertices.reverse()
            // Check whether the holes are in the opposite direction.
            for (h in holes.indices) if (ShapeUtils.isClockWise(holes[h])) holes[h].reverse()
        }

        fun mergeOverlappingPoints(points: MutableList<Vector2>) {
            val thresholdSq = 1e-10 * 1e-10
            var prevPos = points[0]
            var i = 1
            while (i <= points.size) {
                val currentIndex = i % points.size
                val currentPos = points[currentIndex]
                val dx = currentPos.x - prevPos.x
                val dy = currentPos.y - prevPos.y
                val distSq = dx * dx + dy * dy
                val scalingFactorSqrt = max(max(abs(currentPos.x), abs(currentPos.y)), max(abs(prevPos.x), abs(prevPos.y)))
                if (distSq <= thresholdSq * scalingFactorSqrt * scalingFactorSqrt) {
                    points.removeAt(currentIndex)
                    continue
                }
                prevPos = currentPos
                i++
            }
        }
        mergeOverlappingPoints(vertices)
        holes.forEach(::mergeOverlappingPoints)

        val numHoles = holes.size
        // `vertices` gets all points; `contour` keeps only the outline.
        val contour = vertices
        for (hole in holes) vertices = (vertices + hole).toMutableList()

        fun scalePt2(pt: Vector2, vec: Vector2, size: Double): Vector2 = pt.copy().addScaledVector(vec, size)

        val vlen = vertices.size

        // Finds the direction in which a point moves when the contour is offset by one unit to its left, which
        // is outward when walking the contour clockwise. Sharp corners are not normalized, but long spikes are
        // shortened.
        fun getBevelVec(inPt: Vector2, inPrev: Vector2, inNext: Vector2): Vector2 {
            val vTransX: Double
            val vTransY: Double
            val shrinkBy: Double
            val vPrevX = inPt.x - inPrev.x
            val vPrevY = inPt.y - inPrev.y
            val vNextX = inNext.x - inPt.x
            val vNextY = inNext.y - inPt.y
            val vPrevLensq = vPrevX * vPrevX + vPrevY * vPrevY
            // Check for collinear edges.
            val collinear0 = vPrevX * vNextY - vPrevY * vNextX
            if (abs(collinear0) > EPSILON) {
                // Not collinear. Shift the adjacent points by unit vectors to the left.
                val vPrevLen = sqrt(vPrevLensq)
                val vNextLen = sqrt(vNextX * vNextX + vNextY * vNextY)
                val ptPrevShiftX = inPrev.x - vPrevY / vPrevLen
                val ptPrevShiftY = inPrev.y + vPrevX / vPrevLen
                val ptNextShiftX = inNext.x - vNextY / vNextLen
                val ptNextShiftY = inNext.y + vNextX / vNextLen
                // Scaling factor for vPrev to the intersection point.
                val sf = ((ptNextShiftX - ptPrevShiftX) * vNextY - (ptNextShiftY - ptPrevShiftY) * vNextX) /
                    (vPrevX * vNextY - vPrevY * vNextX)
                // Vector from inPt to the intersection point.
                vTransX = ptPrevShiftX + vPrevX * sf - inPt.x
                vTransY = ptPrevShiftY + vPrevY * sf - inPt.y
                val vTransLensq = vTransX * vTransX + vTransY * vTransY
                if (vTransLensq <= 2) return Vector2(vTransX, vTransY)
                shrinkBy = sqrt(vTransLensq / 2)
            } else {
                // Collinear edges: either a straight sequence or a spike.
                var directionEq = false
                if (vPrevX > EPSILON) {
                    if (vNextX > EPSILON) directionEq = true
                } else if (vPrevX < -EPSILON) {
                    if (vNextX < -EPSILON) directionEq = true
                } else if (sign(vPrevY) == sign(vNextY)) {
                    directionEq = true
                }
                if (directionEq) {
                    vTransX = -vPrevY
                    vTransY = vPrevX
                    shrinkBy = sqrt(vPrevLensq)
                } else {
                    vTransX = vPrevX
                    vTransY = vPrevY
                    shrinkBy = sqrt(vPrevLensq / 2)
                }
            }
            return Vector2(vTransX / shrinkBy, vTransY / shrinkBy)
        }

        fun movements(points: List<Vector2>): List<Vector2> = List(points.size) { i ->
            val j = if (i == 0) points.size - 1 else i - 1
            val k = if (i == points.size - 1) 0 else i + 1
            getBevelVec(points[i], points[j], points[k])
        }
        val contourMovements = movements(contour)
        val holesMovements = holes.map(::movements)
        val verticesMovements = contourMovements + holesMovements.flatten()

        fun v(x: Double, y: Double, z: Double) {
            placeholder.add(x)
            placeholder.add(y)
            placeholder.add(z)
        }

        val faces: List<IntArray>
        if (bevelSegments == 0) {
            faces = ShapeUtils.triangulateShape(contour, holes)
        } else {
            val contractedContourVertices = mutableListOf<Vector2>()
            val expandedHoleVertices = mutableListOf<MutableList<Vector2>>()
            // One loop of bevel segments for the front; the back is added below.
            for (b in 0 until bevelSegments) {
                val t = b.toDouble() / bevelSegments
                val z = bevelThickness * cos(t * PI / 2)
                val bs = bevelSize * sin(t * PI / 2) + bevelOffset
                // Contract the shape.
                for (i in contour.indices) {
                    val vert = scalePt2(contour[i], contourMovements[i], bs)
                    v(vert.x, vert.y, -z)
                    if (t == 0.0) contractedContourVertices.add(vert)
                }
                // Expand the holes.
                for (h in 0 until numHoles) {
                    val ahole = holes[h]
                    val oneHoleVertices = mutableListOf<Vector2>()
                    for (i in ahole.indices) {
                        val vert = scalePt2(ahole[i], holesMovements[h][i], bs)
                        v(vert.x, vert.y, -z)
                        if (t == 0.0) oneHoleVertices.add(vert)
                    }
                    if (t == 0.0) expandedHoleVertices.add(oneHoleVertices)
                }
            }
            faces = ShapeUtils.triangulateShape(contractedContourVertices, expandedHoleVertices)
        }

        val bs = bevelSize + bevelOffset
        // Back-facing vertices.
        for (i in 0 until vlen) {
            val vert = if (bevelEnabled) scalePt2(vertices[i], verticesMovements[i], bs) else vertices[i]
            v(vert.x, vert.y, 0.0)
        }
        // Stepped vertices, including the front-facing ones.
        for (s in 1..steps) {
            for (i in 0 until vlen) {
                val vert = if (bevelEnabled) scalePt2(vertices[i], verticesMovements[i], bs) else vertices[i]
                v(vert.x, vert.y, depth / steps * s)
            }
        }
        // Bevel segment planes.
        for (b in bevelSegments - 1 downTo 0) {
            val t = b.toDouble() / bevelSegments
            val z = bevelThickness * cos(t * PI / 2)
            val bevel = bevelSize * sin(t * PI / 2) + bevelOffset
            for (i in contour.indices) {
                val vert = scalePt2(contour[i], contourMovements[i], bevel)
                v(vert.x, vert.y, depth + z)
            }
            for (h in holes.indices) {
                val ahole = holes[h]
                for (i in ahole.indices) {
                    val vert = scalePt2(ahole[i], holesMovements[h][i], bevel)
                    v(vert.x, vert.y, depth + z)
                }
            }
        }

        fun addVertex(index: Int) {
            verticesArray.add(placeholder[index * 3])
            verticesArray.add(placeholder[index * 3 + 1])
            verticesArray.add(placeholder[index * 3 + 2])
        }

        fun addUV(vector: Vector2) {
            uvArray.add(vector.x)
            uvArray.add(vector.y)
        }

        fun f3(a: Int, b: Int, c: Int) {
            addVertex(a)
            addVertex(b)
            addVertex(c)
            val nextIndex = verticesArray.size / 3
            val uvs = uvgen.generateTopUV(this, verticesArray, nextIndex - 3, nextIndex - 2, nextIndex - 1)
            addUV(uvs[0])
            addUV(uvs[1])
            addUV(uvs[2])
        }

        fun f4(a: Int, b: Int, c: Int, d: Int) {
            addVertex(a)
            addVertex(b)
            addVertex(d)
            addVertex(b)
            addVertex(c)
            addVertex(d)
            val nextIndex = verticesArray.size / 3
            val uvs = uvgen.generateSideWallUV(this, verticesArray, nextIndex - 6, nextIndex - 3, nextIndex - 2, nextIndex - 1)
            addUV(uvs[0])
            addUV(uvs[1])
            addUV(uvs[3])
            addUV(uvs[1])
            addUV(uvs[2])
            addUV(uvs[3])
        }

        // Top and bottom faces.
        if (bevelEnabled) {
            var offset = 0
            for (face in faces) f3(face[2] + offset, face[1] + offset, face[0] + offset) // Bottom.
            offset = vlen * (steps + bevelSegments * 2)
            for (face in faces) f3(face[0] + offset, face[1] + offset, face[2] + offset) // Top.
        } else {
            for (face in faces) f3(face[2], face[1], face[0]) // Bottom.
            for (face in faces) f3(face[0] + vlen * steps, face[1] + vlen * steps, face[2] + vlen * steps) // Top.
        }

        // Side faces along the outline and each hole.
        fun sidewalls(points: List<Vector2>, layeroffset: Int) {
            var i = points.size
            while (--i >= 0) {
                val j = i
                var k = i - 1
                if (k < 0) k = points.size - 1
                for (s in 0 until steps + bevelSegments * 2) {
                    val slen1 = vlen * s
                    val slen2 = vlen * (s + 1)
                    f4(layeroffset + j + slen1, layeroffset + k + slen1, layeroffset + k + slen2, layeroffset + j + slen2)
                }
            }
        }
        var layeroffset = 0
        sidewalls(contour, layeroffset)
        layeroffset += contour.size
        for (hole in holes) {
            sidewalls(hole, layeroffset)
            layeroffset += hole.size
        }
    }

    private companion object {
        /** JavaScript `Number.EPSILON`. */
        const val EPSILON = 2.220446049250313e-16
    }
}
