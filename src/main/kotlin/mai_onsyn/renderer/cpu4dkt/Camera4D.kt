package mai_onsyn.renderer.cpu4dkt

import org.joml.Vector4f
import org.joml.plus
import org.joml.plusAssign
import org.joml.times
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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

    fun getCameraOrientation(): CameraOrientation {
        val x = vx.normalize()
        val y = vy.normalize()
        val z = vz.normalize()
        val w = vw.normalize()

        // 1. 3D 传统姿态提取 (基于 X-Y-W 构成的 3D 投影)
        // 视线在 3D 的水平偏航 (Yaw)
        val yawRad = atan2(w.x, w.w)
        // 视线在 3D 的垂直俯仰 (Pitch)
        val pitchRad = asin(w.y.coerceIn(-1f, 1f))
        // 上方向在 3D 的翻滚角 (Roll)
        val rollRad = atan2(y.x, y.y)

        // 2. 4D 各轴相对 Z 轴(第四维)的偏角 (Direct Angles to 4D Axis)
        // 视线 vw 偏向 Z 的角度 (0° 代表完全在 3D，90° 代表直视第四维)
        val fwdDepthRad = asin(w.z.coerceIn(-1f, 1f))
        // 上方向 vy 偏向 Z 的角度
        val upDepthRad = asin(y.z.coerceIn(-1f, 1f))
        // 右方向 vx 偏向 Z 的角度
        val rightDepthRad = asin(x.z.coerceIn(-1f, 1f))

        return CameraOrientation(
            yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat(),
            pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat(),
            rollDeg = Math.toDegrees(rollRad.toDouble()).toFloat(),
            fwdDepthDeg = Math.toDegrees(fwdDepthRad.toDouble()).toFloat(),
            upDepthDeg = Math.toDegrees(upDepthRad.toDouble()).toFloat(),
            rightDepthDeg = Math.toDegrees(rightDepthRad.toDouble()).toFloat()
        )
    }
}

data class CameraOrientation(
    // === 3D 姿态 (人脑最容易理解的部分) ===
    val yawDeg: Float,     // 左右偏航 (rotateXW 主要是它在动)
    val pitchDeg: Float,   // 上下俯仰 (rotateYW 主要是它在动)
    val rollDeg: Float,    // 镜头翻滚 (rotateXY 主要是它在动)

    // === 4D 侧倾 (相机各轴向第四维 Z 轴偏离的角度) ===
    val fwdDepthDeg: Float,  // 主视线 vw 偏向 Z 轴的角度 (rotateZW 主要是它在动)
    val upDepthDeg: Float,   // 上方向 vy 偏向 Z 轴的角度 (rotateYZ 主要是它在动)
    val rightDepthDeg: Float // 右方向 vx 偏向 Z 轴的角度 (rotateXZ 主要是它在动)
) {
    override fun toString(): String = "Yaw: %.2f | Pitch: %.2f | Roll: %.2f | Depth: %.2f | Up: %.2f | Right: %.2f".format(yawDeg, pitchDeg, rollDeg, fwdDepthDeg, upDepthDeg, rightDepthDeg)
}