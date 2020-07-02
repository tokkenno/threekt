package core

import math.Euler
import math.Quaternion
import math.Radian
import math.Vector3

class VirtualObject {
    companion object {
        private var idGenerator: Long = 0
    }

    val id: Long by lazy { idGenerator++ }

    var name: String = ""

    var position: Vector3 = Vector3()
    var rotation: Euler = Euler()
    var scale: Vector3 = Vector3(1.0, 1.0, 1.0)
    private var quaternion: Quaternion = Quaternion()

    var parent: VirtualObject? = null
    var children: Collection<VirtualObject> = ArrayList()

    fun rotateOnAxis(axis: Vector3, angle: Radian): VirtualObject {
        this.quaternion.premultiply(Quaternion().setFromAxisAngle(axis, angle))
        return this
    }

    fun rotateX(angle: Radian): VirtualObject {
        return this.rotateOnAxis(Vector3.xAxis, angle)
    }

    fun rotateY(angle: Radian): VirtualObject {
        return this.rotateOnAxis(Vector3.yAxis, angle)
    }

    fun rotateZ(angle: Radian): VirtualObject {
        return this.rotateOnAxis(Vector3.zAxis, angle)
    }
}