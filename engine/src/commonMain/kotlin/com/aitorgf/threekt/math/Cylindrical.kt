package com.aitorgf.threekt.math

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * This class can be used to represent points in 3D space as [Cylindrical
 * coordinates](https://en.wikipedia.org/wiki/Cylindrical_coordinate_system).
 */
data class Cylindrical(var radius: Double = 1.0, var theta: Double = 0.0, var y: Double = 0.0) {

    /** Sets the cylindrical components by copying the given values. Returns this instance. */
    fun set(radius: Double, theta: Double, y: Double): Cylindrical {
        this.radius = radius
        this.theta = theta
        this.y = y
        return this
    }

    /** Copies the supplied values into this instance and returns it. */
    fun copy(other: Cylindrical): Cylindrical = set(other.radius, other.theta, other.y)

    /**
     * Sets the cylindrical components from the given vector which is assumed to hold Cartesian
     * coordinates. Returns this instance.
     */
    fun setFromVector3(vector: Vector3): Cylindrical = setFromCartesianCoords(vector.x, vector.y, vector.z)

    /** Sets the cylindrical components from the given Cartesian coordinates. Returns this instance. */
    fun setFromCartesianCoords(x: Double, y: Double, z: Double): Cylindrical =
        set(sqrt(x * x + z * z), atan2(x, z), y)
}
