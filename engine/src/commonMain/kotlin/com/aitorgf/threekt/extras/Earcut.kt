package com.aitorgf.threekt.extras

import kotlin.js.JsExport
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Polygon triangulation with the ear-clipping algorithm of mapbox/earcut 3.0.2, as Three.js `Earcut`.
 * The algorithm is ported line by line, so it produces the same triangles.
 */
@JsExport
object Earcut {
    /**
     * Triangulates the polygon whose flat coordinates are in [data], with [dim] coordinates per vertex.
     * [holeIndices] lists the first vertex of each hole. Returns vertex indices, three per triangle.
     */
    fun triangulate(data: DoubleArray, holeIndices: IntArray? = null, dim: Int = 2): IntArray =
        earcut(data, holeIndices, dim).toIntArray()
}

private class Node(val i: Int, val x: Double, val y: Double) {
    lateinit var prev: Node
    lateinit var next: Node
    var z = 0
    var prevZ: Node? = null
    var nextZ: Node? = null
    var steiner = false
}

private fun earcut(data: DoubleArray, holeIndices: IntArray?, dim: Int): MutableList<Int> {
    val hasHoles = holeIndices != null && holeIndices.isNotEmpty()
    val outerLen = if (hasHoles) holeIndices[0] * dim else data.size
    var outerNode = linkedList(data, 0, outerLen, dim, true)
    val triangles = mutableListOf<Int>()
    if (outerNode == null || outerNode.next === outerNode.prev) return triangles

    var minX = 0.0
    var minY = 0.0
    var invSize = 0.0
    if (hasHoles) outerNode = eliminateHoles(data, holeIndices, outerNode, dim)

    // If the shape is not too simple, use a z-order curve hash later; calculate the polygon bounding box.
    if (data.size > 80 * dim) {
        minX = data[0]
        minY = data[1]
        var maxX = minX
        var maxY = minY
        for (i in dim until outerLen step dim) {
            val x = data[i]
            val y = data[i + 1]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }
        // minX, minY and invSize transform coordinates into integers for the z-order calculation.
        invSize = max(maxX - minX, maxY - minY)
        invSize = if (invSize != 0.0) 32767 / invSize else 0.0
    }
    earcutLinked(outerNode, triangles, dim, minX, minY, invSize, 0)
    return triangles
}

/** Creates a circular doubly linked list from polygon points in the specified winding order. */
private fun linkedList(data: DoubleArray, start: Int, end: Int, dim: Int, clockwise: Boolean): Node? {
    var last: Node? = null
    if (clockwise == (signedArea(data, start, end, dim) > 0)) {
        var i = start
        while (i < end) {
            last = insertNode(i / dim, data[i], data[i + 1], last)
            i += dim
        }
    } else {
        var i = end - dim
        while (i >= start) {
            last = insertNode(i / dim, data[i], data[i + 1], last)
            i -= dim
        }
    }
    if (last != null && equals(last, last.next)) {
        removeNode(last)
        last = last.next
    }
    return last
}

/** Eliminates collinear or duplicate points. */
private fun filterPoints(start: Node?, endNode: Node? = null): Node? {
    if (start == null) return start
    var end = endNode ?: start
    var p: Node = start
    var again: Boolean
    do {
        again = false
        if (!p.steiner && (equals(p, p.next) || area(p.prev, p, p.next) == 0.0)) {
            removeNode(p)
            p = p.prev
            end = p
            if (p === p.next) break
            again = true
        } else {
            p = p.next
        }
    } while (again || p !== end)
    return end
}

/** Main ear-slicing loop, which triangulates a polygon given as a linked list. */
private fun earcutLinked(start: Node?, triangles: MutableList<Int>, dim: Int, minX: Double, minY: Double, invSize: Double, pass: Int) {
    var ear = start ?: return
    // Interlink polygon nodes in z-order.
    if (pass == 0 && invSize != 0.0) indexCurve(ear, minX, minY, invSize)
    var stop = ear
    // Iterate through ears, slicing them one by one.
    while (ear.prev !== ear.next) {
        val prev = ear.prev
        val next = ear.next
        if (if (invSize != 0.0) isEarHashed(ear, minX, minY, invSize) else isEar(ear)) {
            triangles.add(prev.i)
            triangles.add(ear.i)
            triangles.add(next.i)
            removeNode(ear)
            // Skipping the next vertex leads to fewer sliver triangles.
            ear = next.next
            stop = next.next
            continue
        }
        ear = next
        // If the whole remaining polygon was traversed without finding any more ears.
        if (ear === stop) {
            when (pass) {
                // Try filtering points and slicing again.
                0 -> earcutLinked(filterPoints(ear), triangles, dim, minX, minY, invSize, 1)
                // If this did not work, try curing all small self-intersections locally.
                1 -> {
                    val cured = cureLocalIntersections(filterPoints(ear)!!, triangles)
                    earcutLinked(cured, triangles, dim, minX, minY, invSize, 2)
                }
                // As a last resort, try splitting the remaining polygon into two.
                2 -> splitEarcut(ear, triangles, dim, minX, minY, invSize)
            }
            break
        }
    }
}

/** Checks whether a polygon node forms a valid ear with adjacent nodes. */
private fun isEar(ear: Node): Boolean {
    val a = ear.prev
    val b = ear
    val c = ear.next
    if (area(a, b, c) >= 0) return false // Reflex, cannot be an ear.
    // Make sure there are no other points inside the potential ear.
    val ax = a.x; val bx = b.x; val cx = c.x; val ay = a.y; val by = b.y; val cy = c.y
    val x0 = min(ax, min(bx, cx)); val y0 = min(ay, min(by, cy))
    val x1 = max(ax, max(bx, cx)); val y1 = max(ay, max(by, cy))
    var p = c.next
    while (p !== a) {
        if (p.x >= x0 && p.x <= x1 && p.y >= y0 && p.y <= y1 &&
            pointInTriangleExceptFirst(ax, ay, bx, by, cx, cy, p.x, p.y) &&
            area(p.prev, p, p.next) >= 0
        ) return false
        p = p.next
    }
    return true
}

private fun isEarHashed(ear: Node, minX: Double, minY: Double, invSize: Double): Boolean {
    val a = ear.prev
    val b = ear
    val c = ear.next
    if (area(a, b, c) >= 0) return false // Reflex, cannot be an ear.
    val ax = a.x; val bx = b.x; val cx = c.x; val ay = a.y; val by = b.y; val cy = c.y
    val x0 = min(ax, min(bx, cx)); val y0 = min(ay, min(by, cy))
    val x1 = max(ax, max(bx, cx)); val y1 = max(ay, max(by, cy))
    // Z-order range for the current triangle bounding box.
    val minZ = zOrder(x0, y0, minX, minY, invSize)
    val maxZ = zOrder(x1, y1, minX, minY, invSize)
    var p = ear.prevZ
    var n = ear.nextZ
    fun inside(q: Node) = q.x >= x0 && q.x <= x1 && q.y >= y0 && q.y <= y1 && q !== a && q !== c &&
        pointInTriangleExceptFirst(ax, ay, bx, by, cx, cy, q.x, q.y) && area(q.prev, q, q.next) >= 0
    // Look for points inside the triangle in both directions.
    while (p != null && p.z >= minZ && n != null && n.z <= maxZ) {
        if (inside(p)) return false
        p = p.prevZ
        if (inside(n)) return false
        n = n.nextZ
    }
    // Look for remaining points in decreasing z-order.
    while (p != null && p.z >= minZ) {
        if (inside(p)) return false
        p = p.prevZ
    }
    // Look for remaining points in increasing z-order.
    while (n != null && n.z <= maxZ) {
        if (inside(n)) return false
        n = n.nextZ
    }
    return true
}

/** Goes through all polygon nodes and cures small local self-intersections. */
private fun cureLocalIntersections(startNode: Node, triangles: MutableList<Int>): Node? {
    var start = startNode
    var p = start
    do {
        val a = p.prev
        val b = p.next.next
        if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(a, b) && locallyInside(b, a)) {
            triangles.add(a.i)
            triangles.add(p.i)
            triangles.add(b.i)
            // Remove the two nodes involved.
            removeNode(p)
            removeNode(p.next)
            p = b
            start = b
        }
        p = p.next
    } while (p !== start)
    return filterPoints(p)
}

/** Tries splitting the polygon into two and triangulating them independently. */
private fun splitEarcut(start: Node, triangles: MutableList<Int>, dim: Int, minX: Double, minY: Double, invSize: Double) {
    // Look for a valid diagonal that divides the polygon into two.
    var a = start
    do {
        var b = a.next.next
        while (b !== a.prev) {
            if (a.i != b.i && isValidDiagonal(a, b)) {
                // Split the polygon in two by the diagonal.
                var c: Node? = splitPolygon(a, b)
                // Filter collinear points around the cuts.
                val filteredA = filterPoints(a, a.next)
                c = filterPoints(c, c!!.next)
                // Run earcut on each half.
                earcutLinked(filteredA, triangles, dim, minX, minY, invSize, 0)
                earcutLinked(c, triangles, dim, minX, minY, invSize, 0)
                return
            }
            b = b.next
        }
        a = a.next
    } while (a !== start)
}

/** Links every hole into the outer loop, producing a single-ring polygon without holes. */
private fun eliminateHoles(data: DoubleArray, holeIndices: IntArray, outer: Node, dim: Int): Node {
    var outerNode = outer
    val queue = mutableListOf<Node>()
    for (i in holeIndices.indices) {
        val start = holeIndices[i] * dim
        val end = if (i < holeIndices.size - 1) holeIndices[i + 1] * dim else data.size
        val list = linkedList(data, start, end, dim, false) ?: continue
        if (list === list.next) list.steiner = true
        queue.add(getLeftmost(list))
    }
    queue.sortWith { a, b -> compareXYSlope(a, b).let { if (it < 0) -1 else if (it > 0) 1 else 0 } }
    // Process holes from left to right.
    for (hole in queue) outerNode = eliminateHole(hole, outerNode)
    return outerNode
}

private fun compareXYSlope(a: Node, b: Node): Double {
    var result = a.x - b.x
    // When the leftmost points of two holes meet at a vertex, sort the holes counterclockwise so that the
    // bridge to the outer shell is always the point where they meet.
    if (result == 0.0) {
        result = a.y - b.y
        if (result == 0.0) {
            val aSlope = (a.next.y - a.y) / (a.next.x - a.x)
            val bSlope = (b.next.y - b.y) / (b.next.x - b.x)
            result = aSlope - bSlope
        }
    }
    return result
}

/** Finds a bridge between vertices that connects [hole] with the outer ring and links it. */
private fun eliminateHole(hole: Node, outerNode: Node): Node {
    val bridge = findHoleBridge(hole, outerNode) ?: return outerNode
    val bridgeReverse = splitPolygon(bridge, hole)
    // Filter collinear points around the cuts.
    filterPoints(bridgeReverse, bridgeReverse.next)
    return filterPoints(bridge, bridge.next)!!
}

/** David Eberly's algorithm for finding a bridge between a hole and the outer polygon. */
private fun findHoleBridge(hole: Node, outerNode: Node): Node? {
    var p = outerNode
    val hx = hole.x
    val hy = hole.y
    var qx = Double.NEGATIVE_INFINITY
    var m: Node? = null
    // Find a segment intersected by a ray from the hole's leftmost point to the left. The segment's endpoint
    // with the lesser x is a potential connection point, unless they intersect at a vertex.
    if (equals(hole, p)) return p
    do {
        if (equals(hole, p.next)) {
            return p.next
        } else if (hy <= p.y && hy >= p.next.y && p.next.y != p.y) {
            val x = p.x + (hy - p.y) * (p.next.x - p.x) / (p.next.y - p.y)
            if (x <= hx && x > qx) {
                qx = x
                m = if (p.x < p.next.x) p else p.next
                if (x == hx) return m // The hole touches the outer segment; pick the leftmost endpoint.
            }
        }
        p = p.next
    } while (p !== outerNode)
    if (m == null) return null

    // Look for points inside the triangle of the hole point, segment intersection and endpoint. Without such
    // points the connection is valid; otherwise choose the point of minimum angle with the ray.
    val stop: Node = m
    val mx = m.x
    val my = m.y
    var tanMin = Double.POSITIVE_INFINITY
    p = m
    do {
        if (hx >= p.x && p.x >= mx && hx != p.x &&
            pointInTriangle(if (hy < my) hx else qx, hy, mx, my, if (hy < my) qx else hx, hy, p.x, p.y)
        ) {
            val tan = abs(hy - p.y) / (hx - p.x) // Tangential.
            val current = m!!
            if (locallyInside(p, hole) &&
                (tan < tanMin || (tan == tanMin && (p.x > current.x || (p.x == current.x && sectorContainsSector(current, p)))))
            ) {
                m = p
                tanMin = tan
            }
        }
        p = p.next
    } while (p !== stop)
    return m
}

/** Whether the sector in vertex [m] contains the sector in vertex [p] in the same coordinates. */
private fun sectorContainsSector(m: Node, p: Node): Boolean = area(m.prev, m, p.prev) < 0 && area(p.next, m, m.next) < 0

/** Interlinks polygon nodes in z-order. */
private fun indexCurve(start: Node, minX: Double, minY: Double, invSize: Double) {
    var p = start
    do {
        if (p.z == 0) p.z = zOrder(p.x, p.y, minX, minY, invSize)
        p.prevZ = p.prev
        p.nextZ = p.next
        p = p.next
    } while (p !== start)
    p.prevZ!!.nextZ = null
    p.prevZ = null
    sortLinked(p)
}

/** Simon Tatham's linked-list merge sort. */
private fun sortLinked(head: Node): Node? {
    var list: Node? = head
    var numMerges: Int
    var inSize = 1
    do {
        var p = list
        var e: Node
        list = null
        var tail: Node? = null
        numMerges = 0
        while (p != null) {
            numMerges++
            var q: Node? = p
            var pSize = 0
            for (i in 0 until inSize) {
                pSize++
                q = q!!.nextZ
                if (q == null) break
            }
            var qSize = inSize
            while (pSize > 0 || (qSize > 0 && q != null)) {
                if (pSize != 0 && (qSize == 0 || q == null || p!!.z <= q.z)) {
                    e = p!!
                    p = p.nextZ
                    pSize--
                } else {
                    e = q!!
                    q = q.nextZ
                    qSize--
                }
                if (tail != null) tail.nextZ = e else list = e
                e.prevZ = tail
                tail = e
            }
            p = q
        }
        tail!!.nextZ = null
        inSize *= 2
    } while (numMerges > 1)
    return list
}

/** Z-order of a point, given its coordinates and the inverse of the longer side of the bounding box. */
private fun zOrder(px: Double, py: Double, minX: Double, minY: Double, invSize: Double): Int {
    // Coordinates are transformed into a non-negative 15-bit integer range.
    var x = ((px - minX) * invSize).toInt()
    var y = ((py - minY) * invSize).toInt()
    x = (x or (x shl 8)) and 0x00FF00FF
    x = (x or (x shl 4)) and 0x0F0F0F0F
    x = (x or (x shl 2)) and 0x33333333
    x = (x or (x shl 1)) and 0x55555555
    y = (y or (y shl 8)) and 0x00FF00FF
    y = (y or (y shl 4)) and 0x0F0F0F0F
    y = (y or (y shl 2)) and 0x33333333
    y = (y or (y shl 1)) and 0x55555555
    return x or (y shl 1)
}

/** Finds the leftmost node of a polygon ring. */
private fun getLeftmost(start: Node): Node {
    var p = start
    var leftmost = start
    do {
        if (p.x < leftmost.x || (p.x == leftmost.x && p.y < leftmost.y)) leftmost = p
        p = p.next
    } while (p !== start)
    return leftmost
}

/** Checks whether a point lies within a convex triangle. */
private fun pointInTriangle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, px: Double, py: Double): Boolean =
    (cx - px) * (ay - py) >= (ax - px) * (cy - py) &&
        (ax - px) * (by - py) >= (bx - px) * (ay - py) &&
        (bx - px) * (cy - py) >= (cx - px) * (by - py)

/** Like [pointInTriangle], but false when the point equals the first vertex of the triangle. */
private fun pointInTriangleExceptFirst(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, px: Double, py: Double): Boolean =
    !(ax == px && ay == py) && pointInTriangle(ax, ay, bx, by, cx, cy, px, py)

/** Checks whether a diagonal between two polygon nodes is valid, that is, inside the polygon. */
private fun isValidDiagonal(a: Node, b: Node): Boolean =
    a.next.i != b.i && a.prev.i != b.i && !intersectsPolygon(a, b) && // Does not intersect other edges.
        ((locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b) && // Locally visible.
            (area(a.prev, a, b.prev) != 0.0 || area(a, b.prev, b) != 0.0)) || // Does not create opposite-facing sectors.
            (equals(a, b) && area(a.prev, a, a.next) > 0 && area(b.prev, b, b.next) > 0)) // Special zero-length case.

/** Signed area of a triangle. */
private fun area(p: Node, q: Node, r: Node): Double = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)

private fun equals(p1: Node, p2: Node): Boolean = p1.x == p2.x && p1.y == p2.y

/** Checks whether two segments intersect. */
private fun intersects(p1: Node, q1: Node, p2: Node, q2: Node): Boolean {
    val o1 = sign(area(p1, q1, p2))
    val o2 = sign(area(p1, q1, q2))
    val o3 = sign(area(p2, q2, p1))
    val o4 = sign(area(p2, q2, q1))
    if (o1 != o2 && o3 != o4) return true // General case.
    if (o1 == 0 && onSegment(p1, p2, q1)) return true // p1, q1 and p2 are collinear and p2 lies on p1q1.
    if (o2 == 0 && onSegment(p1, q2, q1)) return true // p1, q1 and q2 are collinear and q2 lies on p1q1.
    if (o3 == 0 && onSegment(p2, p1, q2)) return true // p2, q2 and p1 are collinear and p1 lies on p2q2.
    if (o4 == 0 && onSegment(p2, q1, q2)) return true // p2, q2 and q1 are collinear and q1 lies on p2q2.
    return false
}

/** For collinear points p, q and r, checks whether q lies on segment pr. */
private fun onSegment(p: Node, q: Node, r: Node): Boolean =
    q.x <= max(p.x, r.x) && q.x >= min(p.x, r.x) && q.y <= max(p.y, r.y) && q.y >= min(p.y, r.y)

private fun sign(num: Double): Int = if (num > 0) 1 else if (num < 0) -1 else 0

/** Checks whether a polygon diagonal intersects any polygon segment. */
private fun intersectsPolygon(a: Node, b: Node): Boolean {
    var p = a
    do {
        if (p.i != a.i && p.next.i != a.i && p.i != b.i && p.next.i != b.i && intersects(p, p.next, a, b)) return true
        p = p.next
    } while (p !== a)
    return false
}

/** Checks whether a polygon diagonal is locally inside the polygon. */
private fun locallyInside(a: Node, b: Node): Boolean =
    if (area(a.prev, a, a.next) < 0) area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0
    else area(a, b, a.prev) < 0 || area(a, a.next, b) < 0

/** Checks whether the middle point of a polygon diagonal is inside the polygon. */
private fun middleInside(a: Node, b: Node): Boolean {
    var p = a
    var inside = false
    val px = (a.x + b.x) / 2
    val py = (a.y + b.y) / 2
    do {
        if (((p.y > py) != (p.next.y > py)) && p.next.y != p.y &&
            (px < (p.next.x - p.x) * (py - p.y) / (p.next.y - p.y) + p.x)
        ) inside = !inside
        p = p.next
    } while (p !== a)
    return inside
}

/**
 * Links two polygon vertices with a bridge. If they belong to the same ring, the polygon splits in two; if
 * one belongs to the outer ring and the other to a hole, they merge into a single ring.
 */
private fun splitPolygon(a: Node, b: Node): Node {
    val a2 = Node(a.i, a.x, a.y)
    val b2 = Node(b.i, b.x, b.y)
    val an = a.next
    val bp = b.prev
    a.next = b
    b.prev = a
    a2.next = an
    an.prev = a2
    b2.next = a2
    a2.prev = b2
    bp.next = b2
    b2.prev = bp
    return b2
}

/** Creates a node and links it after [last] in a circular doubly linked list. */
private fun insertNode(i: Int, x: Double, y: Double, last: Node?): Node {
    val p = Node(i, x, y)
    if (last == null) {
        p.prev = p
        p.next = p
    } else {
        p.next = last.next
        p.prev = last
        last.next.prev = p
        last.next = p
    }
    return p
}

private fun removeNode(p: Node) {
    p.next.prev = p.prev
    p.prev.next = p.next
    p.prevZ?.nextZ = p.nextZ
    p.nextZ?.prevZ = p.prevZ
}

private fun signedArea(data: DoubleArray, start: Int, end: Int, dim: Int): Double {
    var sum = 0.0
    var j = end - dim
    var i = start
    while (i < end) {
        sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1])
        j = i
        i += dim
    }
    return sum
}
