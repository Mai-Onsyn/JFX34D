package mai_onsyn.renderer.cpu4dkt.generator

import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f


fun constructHypercube(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    edgeLength: Float = 2f,
    colorizer: (Vector4f) -> ColorARGB = { p ->
        // 1. 将 4D 坐标从 [-1, 1] 映射到 0~15 的唯一索引
        val ix = if (p.x > 0f) 1 else 0
        val iy = if (p.y > 0f) 2 else 0
        val iz = if (p.z > 0f) 4 else 0
        val iw = if (p.w > 0f) 8 else 0
        val index = ix or iy or iz or iw // 0 到 15 的唯一整数

        // 2. 利用 HSV 颜色空间均衡分布 Hue（色相），保证 16 个顶点颜色最大化区分
        val hue = (index * 360f / 16f) % 360f
        val saturation = 0.85f
        val value = 1.0f

        // HSV 转 RGB
        val c = value * saturation
        val x = c * (1f - Math.abs((hue / 60f) % 2f - 1f))
        val m = value - c

        val (r1, g1, b1) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        // 不透明度 alpha 设为 1f
        ColorARGB(r = r1 + m, g = g1 + m, b = b1 + m, a = 1f)
    }
): MutableList<Tetrahedron> {
    val half = edgeLength / 2f

    // 1. 生成 16 个顶点（先不设置全局法向量，因为法向量依赖于它所在的胞/Cell）
    val rawPositions = Array(16) { i ->
        val sx = if (i and 1 != 0) half else -half
        val sy = if (i and 2 != 0) half else -half
        val sz = if (i and 4 != 0) half else -half
        val sw = if (i and 8 != 0) half else -half

        val pos = Vector4f(
            center.x + sx,
            center.y + sy,
            center.z + sz,
            center.w + sw
        )
        val normalized = Vector4f(sx / half, sy / half, sz / half, sw / half)
        Pair(pos, colorizer(normalized))
    }

    fun globalIndex(ax: Int, valFixed: Int, a: Int, b: Int, c: Int): Int {
        val axes = (0..3).filter { it != ax }
        var index = 0
        if (valFixed == 1) index = index or (1 shl ax)
        if (a == 1) index = index or (1 shl axes[0])
        if (b == 1) index = index or (1 shl axes[1])
        if (c == 1) index = index or (1 shl axes[2])
        return index
    }

    val localTets = listOf(
        intArrayOf(0, 7, 1, 3),
        intArrayOf(0, 7, 3, 2),
        intArrayOf(0, 7, 2, 6),
        intArrayOf(0, 7, 6, 4),
        intArrayOf(0, 7, 4, 5),
        intArrayOf(0, 7, 5, 1)
    )

    val result = mutableListOf<Tetrahedron>()

    // 2. 遍历 8 个 3D 胞 (Cell)
    for (ax in 0..3) {
        for (valFixed in 0..1) {
            // 计算当前胞的外法向量 (4D 法向量)
            val dir = if (valFixed == 1) 1f else -1f
            val normal = Vector4f(
                if (ax == 0) dir else 0f,
                if (ax == 1) dir else 0f,
                if (ax == 2) dir else 0f,
                if (ax == 3) dir else 0f
            )

            // 为当前胞构建包含正确 4D 法向量的 Vertex4D
            val cellVertices = Array(8) { localIdx ->
                val a = localIdx and 1
                val b = (localIdx shr 1) and 1
                val c = (localIdx shr 2) and 1
                val gIdx = globalIndex(ax, valFixed, a, b, c)
                val (pos, color) = rawPositions[gIdx]

                Vertex4D(pos, color, normal)
            }

            // 分解为四面体
            for (tet in localTets) {
                result.add(
                    Tetrahedron(
                        cellVertices[tet[0]],
                        cellVertices[tet[1]],
                        cellVertices[tet[2]],
                        cellVertices[tet[3]]
                    )
                )
            }
        }
    }

    return result
}

@JvmOverloads
fun constructHypercubeWithCellColors(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    edgeLength: Float = 2f,
    // 8 个胞（Cell）的颜色调色板，按顺序对应 8 个超面：
    // [+X, -X, +Y, -Y, +Z, -Z, +W, -W]
    cellColors: List<ColorARGB> = listOf(
        ColorARGB(1f, 0.4f, 0.4f, 1f), // +X
        ColorARGB(0.6f, 0f, 0.4f, 1f), // -X
        ColorARGB(0.4f, 1f, 0.4f, 1f), // +Y
        ColorARGB(0f, 0.6f, 0.4f, 1f), // -Y
        ColorARGB(0.4f, 0.4f, 1f, 1f), // +Z
        ColorARGB(0f, 0.4f, 0.6f, 1f), // -Z
        ColorARGB(1f, 1f, 0.4f, 1f),   // +W
        ColorARGB(1f, 0.5f, 0f, 1f)    // -W
    )
): MutableList<Tetrahedron> {
    val half = edgeLength / 2f

    // 辅助函数：计算 4D 局部坐标点在特定胞内的全局 4D 坐标
    fun computePosition(ax: Int, valFixed: Int, a: Int, b: Int, c: Int): Vector4f {
        val axes = (0..3).filter { it != ax } // 升序排列的 3 个可变轴
        val coords = FloatArray(4)

        // 固定轴的坐标
        coords[ax] = if (valFixed == 1) half else -half

        // 可变轴的坐标 (0 -> -half, 1 -> +half)
        coords[axes[0]] = if (a == 1) half else -half
        coords[axes[1]] = if (b == 1) half else -half
        coords[axes[2]] = if (c == 1) half else -half

        return Vector4f(
            center.x + coords[0],
            center.y + coords[1],
            center.z + coords[2],
            center.w + coords[3]
        )
    }

    // 胞内部 Kuhn 四面体分解索引表
    val localTets = listOf(
        intArrayOf(0, 7, 1, 3),
        intArrayOf(0, 7, 3, 2),
        intArrayOf(0, 7, 2, 6),
        intArrayOf(0, 7, 6, 4),
        intArrayOf(0, 7, 4, 5),
        intArrayOf(0, 7, 5, 1)
    )

    val result = mutableListOf<Tetrahedron>()
    var cellIndex = 0

    // 遍历 8 个超面 (Cell)
    for (ax in 0..3) {
        for (valFixed in 1 downTo 0) { // 先生成 + 轴，再生成 - 轴
            // 1. 获取当前胞的统一颜色与 4D 法向量
            val currentColor = cellColors[cellIndex % cellColors.size]
            val dir = if (valFixed == 1) 1f else -1f
            val normal = Vector4f(
                if (ax == 0) dir else 0f,
                if (ax == 1) dir else 0f,
                if (ax == 2) dir else 0f,
                if (ax == 3) dir else 0f
            )

            // 2. 为当前胞独立创建 8 个顶点（完全不与其他胞复用 Vertex4D 实例）
            val cellVertices = Array(8) { localIdx ->
                val a = localIdx and 1
                val b = (localIdx shr 1) and 1
                val c = (localIdx shr 2) and 1

                val pos = computePosition(ax, valFixed, a, b, c)
                // 该胞内的所有顶点分配完全相同的颜色和法向
                Vertex4D(pos, currentColor, normal)
            }

            // 3. 将该胞分解出的 6 个四面体加入结果
            for (tet in localTets) {
                result.add(
                    Tetrahedron(
                        cellVertices[tet[0]],
                        cellVertices[tet[1]],
                        cellVertices[tet[2]],
                        cellVertices[tet[3]]
                    )
                )
            }

            cellIndex++
        }
    }

    return result
}