package mai_onsyn.renderer.cpu4dkt

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Math.toDegrees
import org.joml.minus
import org.joml.plus
import org.joml.times
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
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

    fun getCameraOrientation(): CameraOrientation {
        // 1. XZ 平面：计算 vx 与 vz 在 (X, Z) 子空间构成的旋转角
        // 使用 vx.z 和 vz.z (或 vx.x 与 vx.z 的组合)，直接抓取 XZ 平面的旋转相位
        val xzRad = atan2(vx.z, vx.x)

        // 2. XY 平面：vx 在 (X, Y) 平面上的相位
        val xyRad = atan2(vx.y, vx.x)

        // 3. XW 平面：vx 在 (X, W) 平面上的相位
        val xwRad = atan2(vx.w, vx.x)

        // 4. YZ 平面：vy 在 (Y, Z) 平面上的相位
        val yzRad = atan2(vy.z, vy.y)

        // 5. YW 平面：vy 在 (Y, W) 平面上的相位
        val ywRad = atan2(vy.w, vy.y)

        // 6. ZW 平面：vz 在 (Z, W) 平面上的相位 (或 vw.z 与 vw.w)
        val zwRad = atan2(vz.w, vz.z)

        return CameraOrientation(
            Math.toDegrees(xyRad.toDouble()).toFloat(),
            Math.toDegrees(xzRad.toDouble()).toFloat(),
            Math.toDegrees(xwRad.toDouble()).toFloat(),
            Math.toDegrees(yzRad.toDouble()).toFloat(),
            Math.toDegrees(ywRad.toDouble()).toFloat(),
            Math.toDegrees(zwRad.toDouble()).toFloat()
        )
    }

    fun getCameraOrientation2(): CameraOrientation {
        val theta1 = acos(vw.w)
        val theta2: Float
        val theta3: Float
        if (sin(theta1) > 1e-6f) {
            theta2 = acos(vw.z / sin(theta1))
            theta3 = atan2(vw.y, vw.x)
        } else {
            theta2 = 0f
            theta3 = 0f
        }

        val e4 = Vector4f(0f, 0f, 0f, 1f)
        val rLook = if ((e4 - vw).length() < 1e-6f) {
            Matrix4f()
        } else {
            val v = e4 - vw
            val h = Matrix4f() - 2f * squareBy(v) / v.dot(v)
            val d = Matrix4f().identity().scale(-1f, 1f, 1f)
            h * d
        }

        val r0 = (rLook * Vector4f(1f, 0f, 0f, 0f)).toVec3()
        val u0 = (rLook * Vector4f(0f, 1f, 0f, 0f)).toVec3()
        val k0 = (rLook * Vector4f(0f, 0f, 1f, 0f)).toVec3()

        val rRoll = Matrix3f(r0, u0, k0).transpose() * Matrix3f(vx.toVec3(), vy.toVec3(), vz.toVec3())

        val beta = asin(-rRoll.get(0, 2))
        val alpha = if (abs(cos(beta)) < 1e-6) 0f else atan2(rRoll.get(1, 2), rRoll.get(2, 2))
        val gamma = atan2(rRoll.get(0, 1), rRoll.get(0, 0))

        return CameraOrientation(
            toDegrees(theta1), toDegrees(theta2), toDegrees(theta3),
            toDegrees(beta), toDegrees(alpha), toDegrees(gamma)
        )
    }

    private fun squareBy(v: Vector4f): Matrix4f {
        return Matrix4f(
            v.x * v.x, v.x * v.y, v.x * v.z, v.x * v.w,
            v.y * v.x, v.y * v.y, v.y * v.z, v.y * v.w,
            v.z * v.x, v.z * v.y, v.z * v.z, v.z * v.w,
            v.w * v.x, v.w * v.y, v.w * v.z, v.w * v.w
        )
    }

    private operator fun Float.times(m: Matrix4f): Matrix4f = m.scale(this)
    private operator fun Matrix4f.div(f: Float): Matrix4f = this.scale(1 / f)
    private fun Vector4f.toVec3(): Vector3f = Vector3f(x, y, z)
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
    override fun toString(): String = "XY: %.2f | XZ: %.2f | XW: %.2f | YZ: %.2f | YW: %.2f | ZW: %.2f".format(yawDeg, pitchDeg, rollDeg, fwdDepthDeg, upDepthDeg, rightDepthDeg)
}