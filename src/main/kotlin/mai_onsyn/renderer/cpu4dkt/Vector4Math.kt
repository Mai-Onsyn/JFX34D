package mai_onsyn.renderer.cpu4dkt

import org.joml.Vector4f

/** 3×3 行列式 */
internal fun det3(
    a1: Float, a2: Float, a3: Float,
    b1: Float, b2: Float, b3: Float,
    c1: Float, c2: Float, c3: Float
): Float = a1 * (b2 * c3 - b3 * c2) - a2 * (b1 * c3 - b3 * c1) + a3 * (b1 * c2 - b2 * c1)

/**
 * 四维广义叉积：返回与 u、v、w 都正交的向量（未归一化）。
 * 三个向量线性相关时返回零向量。
 */
fun perpendicular4(u: Vector4f, v: Vector4f, w: Vector4f): Vector4f = Vector4f(
    det3(u.y, u.z, u.w, v.y, v.z, v.w, w.y, w.z, w.w),
    -det3(u.x, u.z, u.w, v.x, v.z, v.w, w.x, w.z, w.w),
    det3(u.x, u.y, u.w, v.x, v.y, v.w, w.x, w.y, w.w),
    -det3(u.x, u.y, u.z, v.x, v.y, v.z, w.x, w.y, w.z)
)

/**
 * 由四个仿射无关的点确定超平面，返回单位法向量。
 * 四点仿射相关（共超平面）时抛 [IllegalArgumentException]。
 */
fun hyperplaneNormal(a: Vector4f, b: Vector4f, c: Vector4f, d: Vector4f): Vector4f {
    val n = perpendicular4(Vector4f(b).sub(a), Vector4f(c).sub(a), Vector4f(d).sub(a))
    require(n.length() > 1e-8f) {
        "The four points are affinely dependent, cannot determine a hyperplane normal"
    }
    return n.normalize()
}

/** 四个四维点的重心 */
fun centroid4(vararg points: Vector4f): Vector4f {
    // 注意：JOML 的 Vector4f() 是 (0,0,0,1)（齐次坐标约定），这里必须显式全零
    val sum = Vector4f(0f, 0f, 0f, 0f)
    points.forEach { sum.add(it) }
    return sum.div(points.size.toFloat())
}
