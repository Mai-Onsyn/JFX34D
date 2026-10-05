package mai_onsyn.renderer.ogl3d.generator

import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector3f

/**
 * 圆锥，对应文档 §5.3 的 `CONE`。
 *
 * - 轴线沿 **+Y**；底面圆在 `y = center.y - height/2`，锥顶在 `y = center.y + height/2`；
 * - [segments] 为圆周分段数（≥3），**第一个底面点在 +X 方向**；
 * - 底面 + 侧面都生成，法向朝外，结果**封闭**。
 */
fun createCone(
    radius: Float = 1f,
    height: Float = 1f,
    segments: Int = 16,
    center: Vector3f = Vector3f(),
    color: ColorARGB = DEFAULT_MESH_COLOR,
    texture: Texture? = null
): MutableList<Triangle> {
    require(radius > 0f) { "radius must be > 0, got $radius" }
    require(height > 0f) { "height must be > 0, got $height" }
    require(segments >= 3) { "segments must be >= 3, got $segments" }

    val half = height / 2f
    val base = regularPolygon(segments, radius, -half).map { Vector3f(it).add(center) }
    val apex = Vector3f(0f, half, 0f).add(center)

    val result = ArrayList<Triangle>(segments * 2)

    // 底面：扇形三角化
    for (i in 1 until segments - 1) {
        result.add(tri(base[0], base[i], base[i + 1], center, color, texture))
    }

    // 侧面：每条底边与锥顶组成一个三角形
    for (i in 0 until segments) {
        val m = (i + 1) % segments
        result.add(tri(base[i], apex, base[m], center, color, texture))
    }
    return result
}
