package mai_onsyn.renderer.ogl3d.generator

import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector3f

/**
 * 正 n 棱柱，对应文档 §5.3 的 `PRISM`。
 *
 * - 轴线沿 **+Y**，Y 方向以 [center] 为参照居中（`-height/2 ~ +height/2`）；
 * - 底面为正 [sides] 边形，外接圆半径 [radius]，**一个顶点在 +X 方向**；
 * - 端盖 + 侧面都生成，法向朝外，结果**封闭**。
 */
fun createPrism(
    sides: Int,
    radius: Float = 1f,
    height: Float = 1f,
    center: Vector3f = Vector3f(),
    color: ColorARGB = DEFAULT_MESH_COLOR,
    texture: Texture? = null
): MutableList<Triangle> {
    require(sides >= 3) { "sides must be >= 3, got $sides" }
    require(radius > 0f) { "radius must be > 0, got $radius" }
    require(height > 0f) { "height must be > 0, got $height" }

    val half = height / 2f
    val bottom = regularPolygon(sides, radius, -half).map { Vector3f(it).add(center) }
    val top = regularPolygon(sides, radius, half).map { Vector3f(it).add(center) }

    val result = ArrayList<Triangle>(sides * 4)

    // 两个端盖：扇形三角化（法向由 tri 依据 center 自动朝外）
    for (i in 1 until sides - 1) {
        result.add(tri(top[0], top[i], top[i + 1], center, color, texture))
        result.add(tri(bottom[0], bottom[i], bottom[i + 1], center, color, texture))
    }

    // 侧面：每对相邻棱拉出一个竖直四边形，拆成两个三角形
    for (i in 0 until sides) {
        val m = (i + 1) % sides
        result.add(tri(bottom[i], top[i], top[m], center, color, texture))
        result.add(tri(bottom[i], top[m], bottom[m], center, color, texture))
    }
    return result
}
