package mai_onsyn.renderer.utils

import mai_onsyn.renderer.animation.Interpolator
import org.joml.Vector4f
import kotlin.math.*

data class Coordinate4D @JvmOverloads constructor(
    val vx: Vector4f = Vector4f(1f, 0f, 0f, 0f),
    val vy: Vector4f = Vector4f(0f, 1f, 0f, 0f),
    val vz: Vector4f = Vector4f(0f, 0f, 1f, 0f),
    val vw: Vector4f = Vector4f(0f, 0f, 0f, 1f)
) {
    companion object {
        fun interpolate(frac: Float, ca: Coordinate4D, cb: Coordinate4D): Coordinate4D {
            val (a0, b0) = extractQuaternionPair(ca)
            var (a1, b1) = extractQuaternionPair(cb)

            // 统一符号：使 (a0,b0) 到 (a1,b1) 在 SU(2)xSU(2) 中距离最近
            val d = a0.dot(a1) + b0.dot(b1)
            if (d < 0) {
                a1 = Quaternion(-a1.w, -a1.x, -a1.y, -a1.z)
                b1 = Quaternion(-b1.w, -b1.x, -b1.y, -b1.z)
            }

            val a = slerpNoFlip(a0, a1, frac)
            val b = slerpNoFlip(b0, b1, frac)

            val bConj = b.conjugate()
            // 新基：1 -> a * 1 * b_conj = a * b_conj
            //       i -> a * i * b_conj
            //       j -> a * j * b_conj
            //       k -> a * k * b_conj
            val newVw = a * bConj
            val newVx = a * Quaternion(0f, 1f, 0f, 0f) * bConj
            val newVy = a * Quaternion(0f, 0f, 1f, 0f) * bConj
            val newVz = a * Quaternion(0f, 0f, 0f, 1f) * bConj

            return Coordinate4D(
                vx = Quaternion.toVector4f(newVx),
                vy = Quaternion.toVector4f(newVy),
                vz = Quaternion.toVector4f(newVz),
                vw = Quaternion.toVector4f(newVw)
            )
        }
    }
}

fun Coordinate4D.Companion.interpolator(): Interpolator<Coordinate4D> {
    return object : Interpolator<Coordinate4D> {
        override fun interpolate(
            frac: Float,
            start: Coordinate4D,
            end: Coordinate4D
        ): Coordinate4D = Coordinate4D.interpolate(frac, start, end)
    }
}

// 四元数类
data class Quaternion(val w: Float, val x: Float, val y: Float, val z: Float) {
    operator fun times(q: Quaternion): Quaternion {
        return Quaternion(
            w * q.w - x * q.x - y * q.y - z * q.z,
            w * q.x + x * q.w + y * q.z - z * q.y,
            w * q.y - x * q.z + y * q.w + z * q.x,
            w * q.z + x * q.y - y * q.x + z * q.w
        )
    }

    fun conjugate(): Quaternion = Quaternion(w, -x, -y, -z)

    fun dot(q: Quaternion): Float = w * q.w + x * q.x + y * q.y + z * q.z

    fun normalize(): Quaternion {
        val len = sqrt(w * w + x * x + y * y + z * z)
        if (len < 1e-8f) return Quaternion(1f, 0f, 0f, 0f)
        return Quaternion(w / len, x / len, y / len, z / len)
    }

    companion object {
        // Vector4f 分量顺序 (x, y, z, w) -> 四元数 (w, x, y, z)
        fun fromVector4f(v: Vector4f): Quaternion = Quaternion(v.w, v.x, v.y, v.z)
        fun toVector4f(q: Quaternion): Vector4f = Vector4f(q.x, q.y, q.z, q.w)
    }
}

// 从 Coordinate4D 提取四元数对 (a, b)
// 假设 vw 对应四元数基 1，vx 对应 i，vy 对应 j，vz 对应 k
fun extractQuaternionPair(coord: Coordinate4D): Pair<Quaternion, Quaternion> {
    val v0 = Quaternion.fromVector4f(coord.vw) // 1
    val v1 = Quaternion.fromVector4f(coord.vx) // i
    val v2 = Quaternion.fromVector4f(coord.vy) // j
    val v3 = Quaternion.fromVector4f(coord.vz) // k

    val v0c = v0.conjugate()
    val q = v0c * v1
    val r = v0c * v2
    val s = v0c * v3

    // 构造 3x3 旋转矩阵 M（列向量为 q, r, s 的虚部）
    val m00 = q.x; val m10 = q.y; val m20 = q.z
    val m01 = r.x; val m11 = r.y; val m21 = r.z
    val m02 = s.x; val m12 = s.y; val m22 = s.z

    // 从 3x3 旋转矩阵提取四元数 b
    val trace = m00 + m11 + m22
    val b: Quaternion = when {
        trace > 0f -> {
            val S = sqrt(trace + 1f) * 2f
            Quaternion(0.25f * S, (m21 - m12) / S, (m02 - m20) / S, (m10 - m01) / S)
        }
        m00 > m11 && m00 > m22 -> {
            val S = sqrt(1f + m00 - m11 - m22) * 2f
            Quaternion((m21 - m12) / S, 0.25f * S, (m01 + m10) / S, (m02 + m20) / S)
        }
        m11 > m22 -> {
            val S = sqrt(1f + m11 - m00 - m22) * 2f
            Quaternion((m02 - m20) / S, (m01 + m10) / S, 0.25f * S, (m12 + m21) / S)
        }
        else -> {
            val S = sqrt(1f + m22 - m00 - m11) * 2f
            Quaternion((m10 - m01) / S, (m02 + m20) / S, (m12 + m21) / S, 0.25f * S)
        }
    }.normalize()

    // a = v0 * b
    val a = (v0 * b).normalize()
    return Pair(a, b)
}

// 不翻转符号的 SLERP，直接沿测地线插值
fun slerpNoFlip(q0: Quaternion, q1: Quaternion, t: Float): Quaternion {
    val dot = q0.dot(q1).coerceIn(-1f, 1f)
    val theta = acos(dot)
    if (theta < 1e-6f) return q0
    val sinTheta = sin(theta)
    if (sinTheta < 1e-6f) {
        // 几乎相反，线性插值后归一化
        return Quaternion(
            q0.w + t * (q1.w - q0.w),
            q0.x + t * (q1.x - q0.x),
            q0.y + t * (q1.y - q0.y),
            q0.z + t * (q1.z - q0.z)
        ).normalize()
    }
    val s0 = sin((1 - t) * theta) / sinTheta
    val s1 = sin(t * theta) / sinTheta
    return Quaternion(
        s0 * q0.w + s1 * q1.w,
        s0 * q0.x + s1 * q1.x,
        s0 * q0.y + s1 * q1.y,
        s0 * q0.z + s1 * q1.z
    ).normalize()
}