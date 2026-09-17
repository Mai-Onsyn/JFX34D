package mai_onsyn.renderer.ogl3d.data

import org.joml.Matrix4f

class Transform(
    val matrix: Matrix4f
) {
    companion object {
        val NONE = Transform(Matrix4f(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        ))
    }
}