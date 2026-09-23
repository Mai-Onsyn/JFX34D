package mai_onsyn.renderer.cpu4dkt

import org.joml.Vector4f

data class Vector5f(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var w: Float = 0f,
    var u: Float = 0f
) {
    constructor(v4f: Vector4f) : this(v4f.x, v4f.y, v4f.z, v4f.w, 1f)

    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        4 -> u
        else -> throw IndexOutOfBoundsException()
    }

    operator fun set(i: Int, v: Float) = when (i) {
        0 -> x = v
        1 -> y = v
        2 -> z = v
        3 -> w = v
        4 -> u = v
        else -> throw IndexOutOfBoundsException()
    }

    fun toVector4f(): Vector4f {
        return Vector4f(x, y, z, w)
    }
}