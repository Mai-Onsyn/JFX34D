package mai_onsyn.renderer.ogl3d.generator

import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector3f

fun createPyramid(
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
    val base = regularPolygon(sides, radius, -half).map { Vector3f(it).add(center) }
    val apex = Vector3f(0f, half, 0f).add(center)

    val result = ArrayList<Triangle>(sides * 2)

    // 底面：扇形三角化
    for (i in 1 until sides - 1) {
        result.add(tri(base[0], base[i], base[i + 1], center, color, texture))
    }

    // 侧面：每条底边与锥顶组成一个三角形
    for (i in 0 until sides) {
        val m = (i + 1) % sides
        result.add(tri(base[i], apex, base[m], center, color, texture))
    }
    return result
}
