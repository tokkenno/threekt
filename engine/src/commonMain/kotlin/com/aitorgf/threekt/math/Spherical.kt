package com.aitorgf.threekt.math

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * This class can be used to represent points in 3D space as [Spherical
 * coordinates](https://en.wikipedia.org/wiki/Spherical_coordinate_system).
 */
data class Spherical(var radius: Double = 1.0, var phi: Double = 0.0, var theta: Double = 0.0) {

    /** Sets the spherical components by copying the given values. Returns this instance. */
    fun set(radius: Double, phi: Double, theta: Double): Spherical {
        this.radius = radius
        this.phi = phi
        this.theta = theta
        return this
    }

    /** Copies the supplied values into this instance and returns it. */
    fun copy(other: Spherical): Spherical = set(other.radius, other.phi, other.theta)

    /**
     * Restricts the polar angle [page:.phi phi] to be between `0.000001` and pi - `0.000001`. Returns this
     * instance.
     */
    fun makeSafe(): Spherical = apply { phi = MathUtils.clamp(phi, 1e-6, PI - 1e-6) }

    /**
     * Sets the spherical components from the given vector which is assumed to hold Cartesian coordinates.
     * Returns this instance.
     */
    fun setFromVector3(vector: Vector3): Spherical = setFromCartesianCoords(vector.x, vector.y, vector.z)

    /** Sets the spherical components from the given Cartesian coordinates. Returns this instance. */
    fun setFromCartesianCoords(x: Double, y: Double, z: Double): Spherical {
        val radius = sqrt(x * x + y * y + z * z)
        if (radius == 0.0) return set(0.0, 0.0, 0.0)
        return set(radius, acos(MathUtils.clamp(y / radius, -1.0, 1.0)), atan2(x, z))
    }
}
