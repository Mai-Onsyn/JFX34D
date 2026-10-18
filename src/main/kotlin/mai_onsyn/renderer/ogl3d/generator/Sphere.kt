package mai_onsyn.renderer.ogl3d.generator

import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.ogl3d.data.Vertex
import mai_onsyn.renderer.utils.ColorARGB
import mai_onsyn.renderer.utils.DEFAULT_MAX_SUBDIVISIONS
import mai_onsyn.renderer.utils.densityToSubdivisions
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

fun createSphere(
    radius: Float = 1f,
    density: Float = 8f,
    center: Vector3f = Vector3f(),
    color: ColorARGB = DEFAULT_MESH_COLOR,
    texture: Texture? = null,
    maxSubdivisions: Int = DEFAULT_MAX_SUBDIVISIONS
): MutableList<Triangle> {
    require(radius > 0f) { "radius must be > 0, got $radius" }
    val k = densityToSubdivisions(density, maxSubdivisions)

    val result = ArrayList<Triangle>(6 * k * k * 2)
    val step = 2f / k

    /** 立方体局部坐标：某个轴固定为 ±1，另两个自由轴在 [-1, 1] 上均匀取值 */
    fun local(axis: Int, fixed: Float, u: Int, v: Int, ui: Int, vi: Int): Vector3f {
        val p = FloatArray(3)
        p[axis] = fixed
        p[u] = -1f + ui * step
        p[v] = -1f + vi * step
        return Vector3f(p[0], p[1], p[2])
    }

    /** 方向 -> 球面顶点：位置沿径向投影，法向就是该方向，UV 用等距圆柱投影 */
    fun vertex(dir: Vector3f): Vertex = Vertex(
        Vector3f(dir).mul(radius).add(center),
        color,
        Vector3f(dir),
        Vector2f(
            0.5f + atan2(dir.z, dir.x) / (2f * PI.toFloat()),
            0.5f - asin(dir.y.coerceIn(-1f, 1f)) / PI.toFloat()
        )
    )

    /** 按 [faceDir]（该小格朝外的方向）决定绕向，保证三角形朝外 */
    fun emit(v0: Vertex, v1: Vertex, v2: Vertex, faceDir: Vector3f) {
        val cross = Vector3f(v1.pos).sub(v0.pos).cross(Vector3f(v2.pos).sub(v0.pos))
        if (cross.dot(faceDir) >= 0f) result.add(Triangle(v0, v1, v2, texture))
        else result.add(Triangle(v0, v2, v1, texture))
    }

    for (axis in 0..2) {
        // 正负面各用剩下两个轴当自由轴
        val u = (axis + 1) % 3
        val v = (axis + 2) % 3
        for (sign in intArrayOf(1, -1)) {
            val fixed = if (sign > 0) 1f else -1f
            for (i in 0 until k) {
                for (j in 0 until k) {
                    val d00 = local(axis, fixed, u, v, i, j).normalize()
                    val d10 = local(axis, fixed, u, v, i + 1, j).normalize()
                    val d11 = local(axis, fixed, u, v, i + 1, j + 1).normalize()
                    val d01 = local(axis, fixed, u, v, i, j + 1).normalize()

                    // 该小格朝外的方向：四角方向的平均（未归一化也不影响 dot 判号）
                    val faceDir = Vector3f(d00).add(d10).add(d11).add(d01)

                    val a = vertex(d00)
                    val b = vertex(d10)
                    val c = vertex(d11)
                    val d = vertex(d01)
                    emit(a, b, c, faceDir)
                    emit(a, c, d, faceDir)
                }
            }
        }
    }
    return result
}
