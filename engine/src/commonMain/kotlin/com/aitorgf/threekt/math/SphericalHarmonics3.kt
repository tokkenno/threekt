package com.aitorgf.threekt.math

/**
 * Represents a third-order spherical harmonics (SH). Light probes use this class to encode lighting
 * information. - Primary reference: https://graphics.stanford.edu/papers/envmap/envmap.pdf - Secondary
 * reference: https://www.ppsloan.org/publications/StupidSH36.pdf
 */
class SphericalHarmonics3 {
    /** Nine mutable RGB coefficients, ordered by band. */
    val coefficients = List(9) { Vector3() }

    /** Sets the given SH coefficients to this instance by copying the values. Returns this instance. */
    fun set(coefficients: List<Vector3>): SphericalHarmonics3 = apply {
        for (index in this.coefficients.indices) this.coefficients[index].copy(coefficients[index])
    }

    /** Sets all SH coefficients to `0`. Returns this instance. */
    fun zero(): SphericalHarmonics3 = apply { coefficients.forEach { it.setScalar(0.0) } }

    /** Returns the radiance in the direction of the given normal. */
    fun getAt(normal: Vector3, target: Vector3): Vector3 = accumulate(
        getBasisAt(normal, DoubleArray(9)), target,
    )

    /**
     * Returns the irradiance (radiance convolved with cosine lobe) in the direction of the given normal.
     */
    fun getIrradianceAt(normal: Vector3, target: Vector3): Vector3 {
        val (x, y, z) = Triple(normal.x, normal.y, normal.z)
        return accumulate(doubleArrayOf(
            0.886227, 2.0 * 0.511664 * y, 2.0 * 0.511664 * z, 2.0 * 0.511664 * x,
            2.0 * 0.429043 * x * y, 2.0 * 0.429043 * y * z, 0.743125 * z * z - 0.247708,
            2.0 * 0.429043 * x * z, 0.429043 * (x * x - y * y),
        ), target)
    }

    private fun accumulate(weights: DoubleArray, target: Vector3): Vector3 {
        val value = Vector3()
        for (index in weights.indices) value.addScaledVector(coefficients[index], weights[index])
        return target.copy(value)
    }

    /** Adds the given SH to this instance. Returns this instance. */
    fun add(sh: SphericalHarmonics3): SphericalHarmonics3 = apply {
        for (index in coefficients.indices) coefficients[index].add(sh.coefficients[index])
    }

    /**
     * A convenience method for performing [SphericalHarmonics3.add] and [SphericalHarmonics3.scale] at
     * once. Returns this instance.
     */
    fun addScaledSH(sh: SphericalHarmonics3, scale: Double): SphericalHarmonics3 = apply {
        for (index in coefficients.indices) coefficients[index].addScaledVector(sh.coefficients[index], scale)
    }

    /** Scales this SH by the given scale factor. Returns this instance. */
    fun scale(scale: Double): SphericalHarmonics3 = apply { coefficients.forEach { it.multiplyScalar(scale) } }

    /**
     * Linear interpolates between the given SH and this instance by the given alpha factor. Returns this
     * instance.
     */
    fun lerp(sh: SphericalHarmonics3, alpha: Double): SphericalHarmonics3 = apply {
        for (index in coefficients.indices) coefficients[index].lerp(sh.coefficients[index], alpha)
    }

    /** Returns `true` if this spherical harmonics is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is SphericalHarmonics3 && coefficients == other.coefficients

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = coefficients.hashCode()

    /** Copies the supplied values into this instance and returns it. */
    fun copy(sh: SphericalHarmonics3): SphericalHarmonics3 = set(sh.coefficients)

    /** Returns a new instance with the same values. */
    fun copy(): SphericalHarmonics3 = SphericalHarmonics3().copy(this)

    /** Sets the SH coefficients of this instance from the given array. */
    fun fromArray(array: DoubleArray, offset: Int = 0): SphericalHarmonics3 = apply {
        for (index in coefficients.indices) coefficients[index].fromArray(array, offset + 3 * index)
    }

    /**
     * Returns an array with the SH coefficients, or copies them into the provided array. The coefficients
     * are represented as numbers.
     */
    fun toArray(array: DoubleArray = DoubleArray(27), offset: Int = 0): DoubleArray {
        for (index in coefficients.indices) coefficients[index].toArray(array, offset + 3 * index)
        return array
    }

    companion object {

        /** Computes the SH basis for the given normal vector. */
        fun getBasisAt(normal: Vector3, target: DoubleArray): DoubleArray {
            val x = normal.x
            val y = normal.y
            val z = normal.z
            target[0] = 0.282095
            target[1] = 0.488603 * y
            target[2] = 0.488603 * z
            target[3] = 0.488603 * x
            target[4] = 1.092548 * x * y
            target[5] = 1.092548 * y * z
            target[6] = 0.315392 * (3.0 * z * z - 1.0)
            target[7] = 1.092548 * x * z
            target[8] = 0.546274 * (x * x - y * y)
            return target
        }
    }
}
