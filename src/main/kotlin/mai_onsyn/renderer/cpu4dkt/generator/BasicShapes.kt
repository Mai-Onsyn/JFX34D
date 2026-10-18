package mai_onsyn.renderer.cpu4dkt.generator

import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.cpu4dkt.centroid4
import mai_onsyn.renderer.cpu4dkt.hyperplaneNormal
import mai_onsyn.renderer.cpu4dkt.perpendicular4
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.data.Vertex
import mai_onsyn.renderer.utils.ColorARGB
import mai_onsyn.renderer.utils.DEFAULT_MAX_SUBDIVISIONS
import mai_onsyn.renderer.utils.densityToSubdivisions
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sqrt

/** 立方体沿主对角线 0-7 的 6 个 Kuhn 四面体（局部角点索引，位 0/1/2 对应三个自由轴） */
private val KUHN_6 = listOf(
    intArrayOf(0, 7, 1, 3),
    intArrayOf(0, 7, 3, 2),
    intArrayOf(0, 7, 2, 6),
    intArrayOf(0, 7, 6, 4),
    intArrayOf(0, 7, 4, 5),
    intArrayOf(0, 7, 5, 1)
)

/** HSV 转 ARGB，给胞上色用 */
fun hsvColor(hue: Float, saturation: Float = 0.85f, value: Float = 1f): ColorARGB {
    val h = ((hue % 360f) + 360f) % 360f
    val c = value * saturation
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = value - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return ColorARGB(r + m, g + m, b + m, 1f)
}

/** 色相均匀铺开的胞调色板，count = 胞数量 */
fun cellPalette(count: Int): List<ColorARGB> = List(count) { hsvColor(it * 360f / count) }

private fun axisVector(axis: Int, value: Float): Vector4f = when (axis) {
    0 -> Vector4f(value, 0f, 0f, 0f)
    1 -> Vector4f(0f, value, 0f, 0f)
    2 -> Vector4f(0f, 0f, value, 0f)
    else -> Vector4f(0f, 0f, 0f, value)
}

/** 四个顶点共用一个法向量的胞 */
private fun cell(
    v0: Vector4f, v1: Vector4f, v2: Vector4f, v3: Vector4f,
    color: ColorARGB, normal: Vector4f
): Tetrahedron = Tetrahedron(
    Vertex4D(Vector4f(v0), color, Vector4f(normal)),
    Vertex4D(Vector4f(v1), color, Vector4f(normal)),
    Vertex4D(Vector4f(v2), color, Vector4f(normal)),
    Vertex4D(Vector4f(v3), color, Vector4f(normal))
)

/**
 * 正四面超平面：四个顶点在四条坐标轴上距中心 radius 的位置。
 * 四个点落在超平面 x+y+z+w = radius 上，是一个 3-胞（四维体积为 0，三维体积正常）。
 */
fun constructTetrahedronCell(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    radius: Float = 1f,
    color: ColorARGB = hsvColor(200f)
): MutableList<Tetrahedron> {
    val points = (0..3).map { Vector4f(axisVector(it, radius)).add(center) }
    val normal = hyperplaneNormal(points[0], points[1], points[2], points[3])
    return mutableListOf(cell(points[0], points[1], points[2], points[3], color, normal))
}

/**
 * 四维单纯形（5-cell / 超四面体）：棱长 size，5 个胞。
 * 每个胞是"去掉一个顶点"后的四面体，外法向沿 (顶点 - 重心)。
 */
fun construct5Cell(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    size: Float = 1f,
    cellColors: List<ColorARGB> = cellPalette(5)
): MutableList<Tetrahedron> {
    // 标准 4-单纯形，棱长 2√2，缩放到 size
    val k = 1f / sqrt(5f)
    val scale = size / (2f * sqrt(2f))
    val raw = listOf(
        Vector4f(1f, 1f, 1f, -k),
        Vector4f(1f, -1f, -1f, -k),
        Vector4f(-1f, 1f, -1f, -k),
        Vector4f(-1f, -1f, 1f, -k),
        Vector4f(0f, 0f, 0f, 4f * k)
    )
    val points = raw.map { Vector4f(it).mul(scale).add(center) }
    val center4 = centroid4(*points.toTypedArray())

    val result = mutableListOf<Tetrahedron>()
    for (missing in 0..4) {
        val idx = (0..4).filter { it != missing }
        val normal = Vector4f(points[missing]).sub(center4)
        require(normal.length() > 1e-8f) { "Degenerate 5-cell" }
        normal.normalize()
        result.add(
            cell(
                points[idx[0]], points[idx[1]], points[idx[2]], points[idx[3]],
                cellColors[missing % cellColors.size], normal
            )
        )
    }
    return result
}

/**
 * 超八面体（16-cell）：8 个顶点在四条坐标轴的 ±radius 处，16 个胞。
 * 每个胞从 4 条轴各取一个顶点（符号组合共 16 种），外法向 = 符号组合方向。
 */
fun construct16Cell(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    radius: Float = 1f,
    cellColors: List<ColorARGB> = cellPalette(16)
): MutableList<Tetrahedron> {
    val result = mutableListOf<Tetrahedron>()
    for (signs in 0 until 16) {
        val points = (0..3).map { axis ->
            val sign = if ((signs shr axis) and 1 == 1) 1f else -1f
            Vector4f(axisVector(axis, sign * radius)).add(center)
        }
        // 四个顶点的重心方向就是外法向
        val normal = Vector4f(points[0]).add(points[1]).add(points[2]).add(points[3])
            .sub(Vector4f(center).mul(4f))
        require(normal.length() > 1e-8f) { "Degenerate 16-cell" }
        normal.normalize()
        result.add(
            cell(points[0], points[1], points[2], points[3], cellColors[signs % cellColors.size], normal)
        )
    }
    return result
}

private fun lift(v: Vertex, w: Float, normal: Vector4f? = null): Vertex4D = Vertex4D(
    Vector4f(v.pos.x, v.pos.y, v.pos.z, w),
    v.color,
    normal ?: Vector4f(v.normal.x, v.normal.y, v.normal.z, 0f)
)

private fun samePosition(a: Vertex, b: Vertex): Boolean =
    abs(a.pos.x - b.pos.x) < 1e-6f && abs(a.pos.y - b.pos.y) < 1e-6f && abs(a.pos.z - b.pos.z) < 1e-6f

/** 3D 位置的规范化 key，用来给底面三角形的顶点做与绕向无关的排序 */
private fun positionKey(v: Vertex): String =
    "%08.4f|%08.4f|%08.4f".format(v.pos.x + 0f, v.pos.y + 0f, v.pos.z + 0f)

fun constructPrism4(base: Mesh, ws: Float, we: Float): MutableList<Tetrahedron> {
    val result = mutableListOf<Tetrahedron>()

    for (t in base.triangles) {
        if (samePosition(t.v0, t.v1) || samePosition(t.v1, t.v2) || samePosition(t.v0, t.v2)) continue

        // 规范化排序：让公共棱两侧算出同一条对角线
        val ordered = listOf(t.v0, t.v1, t.v2).sortedBy { positionKey(it) }
        val a = lift(ordered[0], ws); val b = lift(ordered[1], ws); val c = lift(ordered[2], ws)
        val d = lift(ordered[0], we); val e = lift(ordered[1], we); val f = lift(ordered[2], we)

        // 三棱柱 (A,B,C)-(D,E,F)，Ai-Bi 是侧棱
        result.add(Tetrahedron(a, b, c, f))
        result.add(Tetrahedron(a, b, f, e))
        result.add(Tetrahedron(a, e, f, d))
    }
    return result
}

/**
 * 超锥：以 3D 网格为底面（提升到 w = 0 超平面），以四维点为顶点。
 * 每个底面三角形与 apex 组成一个四面体，外法向由广义叉积求出后朝外翻转。
 */
fun constructCone4(
    base: Mesh,
    apex: Vector4f,
    apexColor: ColorARGB = ColorARGB(1f, 1f, 1f, 1f)
): MutableList<Tetrahedron> {
    val result = mutableListOf<Tetrahedron>()
    val apex4 = Vector4f(apex)
    val basePoints = base.triangles.flatMap { listOf(lift(it.v0, 0f), lift(it.v1, 0f), lift(it.v2, 0f)) }
    if (basePoints.isEmpty()) return result
    val baseCenter = centroid4(*basePoints.map { it.pos }.toTypedArray())
    val inner = Vector4f(baseCenter).add(apex4).mul(0.5f)

    for (t in base.triangles) {
        if (samePosition(t.v0, t.v1) || samePosition(t.v1, t.v2) || samePosition(t.v0, t.v2)) continue
        val a = lift(t.v0, 0f); val b = lift(t.v1, 0f); val c = lift(t.v2, 0f)

        var normal = perpendicular4(
            Vector4f(b.pos).sub(a.pos),
            Vector4f(c.pos).sub(a.pos),
            Vector4f(apex4).sub(a.pos)
        )
        if (normal.length() < 1e-8f) {
            // 退化胞：退回底面自身的法向
            normal = Vector4f(a.normal)
        } else {
            normal.normalize()
            val cellCenter = centroid4(a.pos, b.pos, c.pos, apex4)
            if (normal.dot(Vector4f(cellCenter).sub(inner)) < 0f) normal.negate()
        }

        result.add(
            Tetrahedron(
                Vertex4D(Vector4f(a.pos), a.color, Vector4f(normal)),
                Vertex4D(Vector4f(b.pos), b.color, Vector4f(normal)),
                Vertex4D(Vector4f(c.pos), c.color, Vector4f(normal)),
                Vertex4D(Vector4f(apex4), apexColor, Vector4f(normal))
            )
        )
    }
    return result
}

fun ballSubdivisions(density: Float, max: Int = DEFAULT_MAX_SUBDIVISIONS): Int =
    densityToSubdivisions(density, max)

fun constructBall4(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    radius: Float = 1f,
    density: Float = 1f,
    cellColors: List<ColorARGB> = cellPalette(8),
    maxSubdivisions: Int = DEFAULT_MAX_SUBDIVISIONS
): MutableList<Tetrahedron> {
    val k = ballSubdivisions(density, maxSubdivisions)
    val result = mutableListOf<Tetrahedron>()
    var cellIndex = 0

    for (axis in 0..3) {
        for (fixedSign in 1 downTo 0) {
            val color = cellColors[cellIndex % cellColors.size]
            val fixedValue = if (fixedSign == 1) 1f else -1f
            val free = (0..3).filter { it != axis }

            for (i in 0 until k) {
                for (j in 0 until k) {
                    for (l in 0 until k) {
                        // 小立方体的 8 个角点（角点位 0/1/2 对应 free[0]/free[1]/free[2]）
                        val gridPoints = Array(8) { corner ->
                            val coords = FloatArray(4)
                            coords[axis] = fixedValue
                            coords[free[0]] = (i + (corner and 1)) * 2f / k - 1f
                            coords[free[1]] = (j + ((corner shr 1) and 1)) * 2f / k - 1f
                            coords[free[2]] = (l + ((corner shr 2) and 1)) * 2f / k - 1f
                            Vector4f(coords[0], coords[1], coords[2], coords[3])
                        }
                        // 径向投影到球面
                        val verts = gridPoints.map { p ->
                            val dir = Vector4f(p).normalize()
                            Vertex4D(
                                Vector4f(dir).mul(radius).add(center),
                                color,
                                Vector4f(dir)
                            )
                        }
                        for (tet in KUHN_6) {
                            result.add(
                                Tetrahedron(
                                    verts[tet[0]], verts[tet[1]], verts[tet[2]], verts[tet[3]]
                                )
                            )
                        }
                    }
                }
            }
            cellIndex++
        }
    }
    return result
}
