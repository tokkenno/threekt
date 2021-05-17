package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class Line3(
    var start: MutableVector3 = MutableVector3(),
    var end: MutableVector3 = MutableVector3()
) {
    fun set(start: Vector3, end: Vector3): Line3 {

        this.start.set(start)
        this.end.set(end)

        return this

    }

    @JsName("setLine")
    fun set(line: Line3): Line3 {
        this.start.set(line.start)
        this.end.set(line.end)

        return this
    }

    fun getCenter(): Vector3 {
        return MutableVector3().add(this.start, this.end).multiply(0.5)
    }

    fun delta(): MutableVector3 {
        return MutableVector3().sub(this.end, this.start)
    }

    fun distanceSq(): Double {
        return this.start.distanceToSquared(this.end)
    }

    fun distance(): Double {
        return this.start.distanceTo(this.end)
    }

    fun at(t: Double): MutableVector3 {
        return this.delta().multiply(t).add(this.start)
    }

    fun closestPointToPointParameter(point: Vector3, clampToLine: Boolean = false): Double {
        val startP = MutableVector3().sub(point, this.start)
        val startEnd = MutableVector3().sub(this.end, this.start)

        val startEnd2 = startEnd.dot(startEnd)
        val startEnd_startP = startEnd.dot(startP)

        var t = startEnd_startP / startEnd2

        if (clampToLine) {
            t = Math.clamp(t, 0.0, 1.0)
        }

        return t
    }

    fun closestPointToPoint(point: Vector3, clampToLine: Boolean): MutableVector3 {
        val t = this.closestPointToPointParameter(point, clampToLine)
        return this.delta().multiply(t).add(this.start)

    }

    fun applyMatrix4(matrix: Matrix4): Line3 {
        this.start.apply(matrix)
        this.end.apply(matrix)

        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Line3) return false
        return other.start.equals(this.start) && other.end.equals(this.end)
    }

    fun clone(): Line3 {
        return Line3().set(this)
    }
}
