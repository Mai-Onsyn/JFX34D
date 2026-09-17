package mai_onsyn.renderer.cpu4dkt

import org.joml.Vector4f
import kotlin.math.tan

class Camera4D(
    var pos: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    var vx: Vector4f = Vector4f(1f, 0f, 0f, 0f),
    var vy: Vector4f = Vector4f(0f, 1f, 0f, 0f),
    var vz: Vector4f = Vector4f(0f, 0f, 1f, 0f),
    var vw: Vector4f = Vector4f(0f, 0f, 0f, 1f),
    var fov: Float = 80f,
    var near: Float = 0.1f,
    var far: Float = 100f,
) {
    val viewMatrix: Matrix5f
        get() = Matrix5f(
            vx.x, vx.y, vx.z, vx.w, -(vx.dot(pos)),
            vy.x, vy.y, vy.z, vy.w, -(vy.dot(pos)),
            vz.x, vz.y, vz.z, vz.w, -(vz.dot(pos)),
            vw.x, vw.y, vw.z, vw.w, -(vw.dot(pos)),
            0f, 0f, 0f, 0f, 1f
        )

    fun projectionMatrix(aspectX: Float = 1f, aspectZ: Float = 1f): Matrix5f {
        val sy = 1f / tan(fov / 2)
        return Matrix5f(
            sy / aspectX, 0f, 0f, 0f, 0f,
            0f, sy, 0f, 0f, 0f,
            0f, 0f, sy / aspectZ, 0f, 0f,
            0f, 0f, 0f, far / (far - near), near * far / (near - far),
            0f, 0f, 0f, 1f, 0f
        )
    }
}