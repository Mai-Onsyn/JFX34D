package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.utils.Coordinate4D
import org.joml.Vector4f
import kotlin.math.*

fun dot4(a: Vector4f, b: Vector4f): Float =
    a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w

fun length4(v: Vector4f): Float = sqrt(dot4(v, v))

fun normalize4(v: Vector4f): Vector4f {
    val len = length4(v)
    return if (len < 1e-8f) Vector4f(0f, 0f, 0f, 0f)
    else Vector4f(v.x / len, v.y / len, v.z / len, v.w / len)
}

// 返回 d，使得 (a, b, c, d) 构成右手系
fun cross4(a: Vector4f, b: Vector4f, c: Vector4f): Vector4f {
    val d0 = a.y * (b.z * c.w - b.w * c.z) -
            a.z * (b.y * c.w - b.w * c.y) +
            a.w * (b.y * c.z - b.z * c.y)
    val d1 = -a.x * (b.z * c.w - b.w * c.z) +
            a.z * (b.x * c.w - b.w * c.x) -
            a.w * (b.x * c.z - b.z * c.x)
    val d2 = a.x * (b.y * c.w - b.w * c.y) -
            a.y * (b.x * c.w - b.w * c.x) +
            a.w * (b.x * c.y - b.y * c.x)
    val d3 = -a.x * (b.y * c.z - b.z * c.y) +
            a.y * (b.x * c.z - b.z * c.x) -
            a.z * (b.x * c.y - b.y * c.x)
    return Vector4f(d0, d1, d2, d3)
}

@JvmOverloads
fun lookAt4D(
    eye: Vector4f,
    target: Vector4f,
    up: Vector4f = Vector4f(0f, 1f, 0f, 0f),
    anaRef: Vector4f = Vector4f(0f, 0f, 1f, 0f)
): Coordinate4D {
    // 1. 前方向 w
    val w = normalize4(Vector4f(target).sub(eye))

    // 2. 上方向 y
    var y = Vector4f(up).sub(Vector4f(w).mul(dot4(up, w)))
    if (length4(y) < 1e-6f) {
        // up 与 w 平行，换备用方向
        val alt = if (abs(dot4(Vector4f(0f, 0f, 1f, 0f), w)) < 0.9f)
            Vector4f(0f, 0f, 1f, 0f) else Vector4f(1f, 0f, 0f, 0f)
        y = Vector4f(alt).sub(Vector4f(w).mul(dot4(alt, w)))
    }
    y = normalize4(y)

    // 3. ana 方向 z
    var z = Vector4f(anaRef)
        .sub(Vector4f(w).mul(dot4(anaRef, w)))
        .sub(Vector4f(y).mul(dot4(anaRef, y)))
    if (length4(z) < 1e-6f) {
        // anaRef 退化，换备用方向
        val alt = Vector4f(0f, 0f, 0f, 1f)
        z = Vector4f(alt)
            .sub(Vector4f(w).mul(dot4(alt, w)))
            .sub(Vector4f(y).mul(dot4(alt, y)))
    }
    z = normalize4(z)

    // 4. 右方向 x：补齐正交基。
    //    这里**不能**再乘 -1：摄像机自身的基（默认单位阵，以及所有 rotate* 的结果）行列式恒为 +1，
    //    而 Coordinate4D.interpolate 把姿态拆成四元数对 (a, b)（SO(4) 的 p ↦ a·p·b⁻¹）时，
    //    要求端点必须是正常正交基（det = +1）。乘 -1 会得到 det = -1 的镜像基，
    //    拆解会静默失效（重建出的 vx/vy/vz 与端点不符），表现就是视线动画首尾跳变。
    val x = normalize4(cross4(y, z, w))

    return Coordinate4D(vx = x, vy = y, vz = z, vw = w)
}