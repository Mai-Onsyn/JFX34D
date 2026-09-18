package mai_onsyn.renderer.cpu4dkt

import org.joml.Vector4f
import org.joml.plus
import org.joml.plusAssign
import org.joml.times
import kotlin.math.cos
import kotlin.math.sin
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

    // 通用私有工具：把 (a, b) 这一对基向量在它们的平面内旋转
    private inline fun rotatePlane(
        a: Vector4f, b: Vector4f, angle: Float,
        assignA: (Vector4f) -> Unit, assignB: (Vector4f) -> Unit,
    ) {
        val c = cos(angle); val s = sin(angle)
        assignA(a * c + b * s)
        assignB(a * (-s) + b * c)
    }

    /** XY 平面：屏幕内旋转（等于 3D 的 roll） */
    fun rotateXY(a: Float) = rotatePlane(vx, vy, a, { vx = it }, { vy = it })

    /** XZ 平面：右方向卷入/卷出第四维 */
    fun rotateXZ(a: Float) = rotatePlane(vx, vz, a, { vx = it }, { vz = it })

    /** XW 平面：偏航 yaw（左右环顾，不动 vy、vz） */
    fun rotateXW(a: Float) = rotatePlane(vx, vw, a, { vx = it }, { vw = it })

    /** YZ 平面：上方向卷入/卷出第四维 */
    fun rotateYZ(a: Float) = rotatePlane(vy, vz, a, { vy = it }, { vz = it })

    /** YW 平面：俯仰 pitch（抬头低头，不动 vx、vz） */
    fun rotateYW(a: Float) = rotatePlane(vy, vw, a, { vy = it }, { vw = it })

    /** ZW 平面：前方卷入/卷出第四维（4D 里最有"另类"感的旋转） */
    fun rotateZW(a: Float) = rotatePlane(vz, vw, a, { vz = it }, { vw = it })

    // ---- 移动 ----
    fun moveRight(d: Float)  { pos.add(vx * d) }
    fun moveUp(d: Float)     { pos.add(vy * d) }
    fun moveAna(d: Float)    { pos.add(vz * d) }
    fun moveForward(d: Float){ pos.add(vw * d) }

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