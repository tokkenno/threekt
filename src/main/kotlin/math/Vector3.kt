package math

class Vector3 (
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0
) : Vector {
    constructor(x: Int, y: Int, z: Int): this(x.toDouble(), y.toDouble(), z.toDouble())
    constructor(x: Int, y: Double, z: Int): this(x.toDouble(), y, z.toDouble())
    constructor(x: Int, y: Double, z: Double): this(x.toDouble(), y, z)
    constructor(x: Double, y: Int, z: Int): this(x, y.toDouble(), z.toDouble())
    constructor(x: Double, y: Double, z: Int): this(x, y, z.toDouble())
    constructor(x: Double, y: Int, z: Double): this(x, y.toDouble(), z)
    constructor(x: Int, y: Int, z: Double): this(x.toDouble(), y.toDouble(), z)

    companion object {
        val xAxis: Vector3 by lazy { Vector3(1.0, 0.0, 0.0) }
        val yAxis: Vector3 by lazy { Vector3(0.0, 1.0, 0.0) }
        val zAxis: Vector3 by lazy { Vector3(0.0, 0.0, 1.0) }
    }
}