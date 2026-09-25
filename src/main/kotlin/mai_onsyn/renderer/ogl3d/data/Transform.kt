package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.times

class Transform(
    var matrix: Matrix4f
) {
    companion object {
        val NONE = Transform(Matrix4f(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        ))
    }

    fun move(v: Vector3f) {
        val new = Matrix4f().translate(v)
        matrix = new * matrix
    }
}