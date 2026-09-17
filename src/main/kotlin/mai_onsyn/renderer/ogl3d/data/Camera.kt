package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.utils.rotate
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.plusAssign
import org.joml.times
import kotlin.math.tan

class Camera(
    var up: Vector3f = Vector3f(0f, 1f, 0f),
    var pos: Vector3f = Vector3f(0f, 0f, 0f),
    var vx: Vector3f = Vector3f(1f, 0f, 0f),
    var vy: Vector3f = Vector3f(0f, 1f, 0f),
    var vz: Vector3f = Vector3f(0f, 0f, 1f),
    var fov: Float = 70f,
    val near: Float = 0.1f
) {
    fun moveX(distance: Float) {
        pos += vx * distance
    }

    fun moveY(distance: Float) {
        pos += up * distance
    }

    fun moveZ(distance: Float) {
        pos += vx.cross(up, Vector3f()) * distance
    }

    fun rotateY(angle: Float) {
        vy = vy.rotate(angle, vx)
        vz = vz.rotate(angle, vx)
    }

    fun rotateX(angle: Float) {
        vx = vx.rotate(angle, up)
        vy = vy.rotate(angle, up)
        vz = vz.rotate(angle, up)
    }

    val viewMatrix: Matrix4f
        get() = Matrix4f(
            vx.x, vy.x, vz.x, 0f,                              // col0
            vx.y, vy.y, vz.y, 0f,                              // col1
            vx.z, vy.z, vz.z, 0f,                              // col2
            -(vx.x*pos.x + vx.y*pos.y + vx.z*pos.z),           // col3
            -(vy.x*pos.x + vy.y*pos.y + vy.z*pos.z),
            -(vz.x*pos.x + vz.y*pos.y + vz.z*pos.z),
            1f
        )

    fun projectionMatrix(aspect: Float): Matrix4f {
        val halfFov = fov * 3.1415925f / 360.0f
        val f = 1f / tan(halfFov)
        val far = 1000f
        val a = (far + near) / (far - near)
        val b = -2f * far * near / (far - near)
        return Matrix4f(
            f / aspect, 0f, 0f, 0f,   // col0
            0f, f, 0f, 0f,            // col1
            0f, 0f, a, 1f,            // col2
            0f, 0f, b, 0f             // col3
        )
    }
}