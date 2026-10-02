package mai_onsyn.renderer.ogl3d.generator

import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.ogl3d.data.Vertex
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D 基本几何体生成器的共享工具（对应文档 §5.3 的 `base` 几何体）。
 *
 * 所有生成器统一约定：
 * - 返回 `MutableList<Triangle>`（与 [createCube] 一致），可再 `.toMesh()` 包成 Mesh；
 * - **轴线沿 +Y**（球除外，球心在原点），位置以 [center] 为参照；
 * - 结果**封闭**、每个平面片法向**朝外**（`createPrism4` / `createCone4` 建议封闭底面）；
 * - 颜色缺省 `#FFB0B0B0`（文档 §5.3.1）。
 */

/** 文档 §5.3.1 的默认顶点色 `#FFB0B0B0` */
internal val DEFAULT_MESH_COLOR: ColorARGB = ColorARGB(1f, 176 / 255f, 176 / 255f, 1f)

/** 平面片缺省 UV（三角形三点） */
internal val FACE_UV: List<Vector2f> = listOf(
    Vector2f(0f, 0f), Vector2f(1f, 0f), Vector2f(0.5f, 1f)
)

/**
 * 正 n 边形顶点列表，位于 `y = [y]` 的水平面，外接圆半径 [radius]，
 * **第一个顶点在 +X 方向**（与文档 §5.3.2 的 PRISM / PYRAMID 约定一致）。
 */
internal fun regularPolygon(sides: Int, radius: Float, y: Float): List<Vector3f> =
    (0 until sides).map { i ->
        val a = 2.0 * PI * i / sides
        Vector3f((radius * cos(a)).toFloat(), y, (radius * sin(a)).toFloat())
    }

/** 生成一个平面三角形，法向为 [normal]（会归一化），绕向自动调整为与该法向一致。 */
internal fun flatTriangle(
    a: Vector3f,
    b: Vector3f,
    c: Vector3f,
    normal: Vector3f,
    color: ColorARGB,
    texture: Texture?,
    uv: List<Vector2f> = FACE_UV
): Triangle {
    val n = Vector3f(normal).normalize()
    val cross = Vector3f(b).sub(a).cross(Vector3f(c).sub(a))
    val flip = cross.dot(n) < 0f
    val p1 = if (flip) c else b
    val p2 = if (flip) b else c
    val u1 = if (flip) uv[2] else uv[1]
    val u2 = if (flip) uv[1] else uv[2]
    return Triangle(
        Vertex(Vector3f(a), color, Vector3f(n), Vector2f(uv[0])),
        Vertex(Vector3f(p1), color, Vector3f(n), Vector2f(u1)),
        Vertex(Vector3f(p2), color, Vector3f(n), Vector2f(u2)),
        texture
    )
}

/**
 * 生成一个"朝外"的平面三角形：用 [inside]（形体内部一点）判定几何法向的朝向，
 * 再交给 [flatTriangle] 统一绕向。凸形体上传形体中心即可。
 */
internal fun tri(
    a: Vector3f,
    b: Vector3f,
    c: Vector3f,
    inside: Vector3f,
    color: ColorARGB,
    texture: Texture?,
    uv: List<Vector2f> = FACE_UV
): Triangle = flatTriangle(a, b, c, outwardNormal(a, b, c, inside), color, texture, uv)

/** 三角形 (a,b,c) 的几何法向，方向取"背离 [inside]"的一侧；退化时退回 +Y。 */
private fun outwardNormal(a: Vector3f, b: Vector3f, c: Vector3f, inside: Vector3f): Vector3f {
    val n = Vector3f(b).sub(a).cross(Vector3f(c).sub(a))
    if (n.lengthSquared() < 1e-16f) return Vector3f(0f, 1f, 0f)
    n.normalize()
    val centroid = Vector3f(a).add(b).add(c).mul(1f / 3f)
    if (n.dot(Vector3f(centroid).sub(inside)) < 0f) n.negate()
    return n
}
