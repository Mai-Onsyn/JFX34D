package mai_onsyn.renderer.cpu4dkt

import org.joml.Vector4f

class Transform4D @JvmOverloads constructor (
    var matrix: Matrix5f = Matrix5f.IDENTITY
) {
    fun move(v: Vector4f) {
        val new = Matrix5f(
            1f, 0f, 0f, 0f, v.x,
            0f, 1f, 0f, 0f, v.y,
            0f, 0f, 1f, 0f, v.z,
            0f, 0f, 0f, 1f, v.w,
            0f, 0f, 0f, 0f, 1f
        )
        this.matrix *= new
    }
}