package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.ogl3d.data.ColorARGB
import org.joml.Vector4f

data class Tetrahedron(
    val v0: Vertex4D,
    val v1: Vertex4D,
    val v2: Vertex4D,
    val v3: Vertex4D
) {
    fun pack(dest: FloatArray, offset: Int) {
        var offset = offset
        fun packVertex(v: Vertex4D) {
            dest[offset++] = v.pos.x
            dest[offset++] = v.pos.y
            dest[offset++] = v.pos.z
            dest[offset++] = v.pos.w
            dest[offset++] = Float.fromBits(v.color.hex)
        }
        packVertex(v0)
        packVertex(v1)
        packVertex(v2)
        packVertex(v3)
    }

    companion object {
        fun extract(arr: FloatArray, offset: Int): Tetrahedron {
            var offset = offset
            fun extractVertex(): Vertex4D = Vertex4D(
                Vector4f(
                    arr[offset++],
                    arr[offset++],
                    arr[offset++],
                    arr[offset++]
                ), ColorARGB(arr[offset++].toRawBits())
            )
            return Tetrahedron(
                extractVertex(),
                extractVertex(),
                extractVertex(),
                extractVertex()
            )
        }
    }
}

data class Vertex4D(
    val pos: Vector4f,
    val color: ColorARGB
)

fun constructHypercube(
    center: Vector4f = Vector4f(0f, 0f, 0f, 0f),
    edgeLength: Float = 2f,
    colorizer: (Vector4f) -> ColorARGB = { p ->
        // 默认颜色映射：以 [-1,1] 为参考，将坐标归一化到 [0,1]
        val r = (p.x + 1f) / 2f
        val g = (p.y + 1f) / 2f
        val b = (p.z + 1f) / 2f
        ColorARGB(r, g, b)
    }
): MutableList<Tetrahedron> {
    val half = edgeLength / 2f

    // 1. 生成 16 个顶点
    // 顶点索引 i 的二进制位分别对应 x, y, z, w 坐标（0 -> -half, 1 -> +half），再平移 center
    val vertices = Array(16) { i ->
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
        // 用于颜色映射的“归一化局部坐标”（相对中心的偏移 / half），范围 [-1,1]
        val normalized = Vector4f(sx / half, sy / half, sz / half, sw / half)
        Vertex4D(pos, colorizer(normalized))
    }

    // 辅助函数：根据固定的轴和值，以及局部坐标 (a,b,c) 获取全局顶点索引
    // ax: 固定轴 (0=x, 1=y, 2=z, 3=w)
    // valFixed: 固定值 (0 或 1)
    // a,b,c: 三个可变轴的局部坐标（按升序排列的可变轴顺序）
    fun globalIndex(ax: Int, valFixed: Int, a: Int, b: Int, c: Int): Int {
        val axes = (0..3).filter { it != ax }   // 三个可变轴，按升序
        var index = 0
        if (valFixed == 1) index = index or (1 shl ax)
        if (a == 1) index = index or (1 shl axes[0])
        if (b == 1) index = index or (1 shl axes[1])
        if (c == 1) index = index or (1 shl axes[2])
        return index
    }

    // 局部索引 0..7 对应的顶点为 (a,b,c) ∈ {0,1}^3，其中 index = a + 2*b + 4*c
    // 立方体的 6 个四面体分解（Kuhn 分解，以主对角线 0-7 为中心）
    val localTets = listOf(
        intArrayOf(0, 7, 1, 3),
        intArrayOf(0, 7, 3, 2),
        intArrayOf(0, 7, 2, 6),
        intArrayOf(0, 7, 6, 4),
        intArrayOf(0, 7, 4, 5),
        intArrayOf(0, 7, 5, 1)
    )

    val result = mutableListOf<Tetrahedron>()

    // 2. 遍历 8 个立方体单元：4 个固定轴 × 2 个固定值
    for (ax in 0..3) {
        for (valFixed in 0..1) {
            // 获取当前立方体的 8 个顶点，按局部索引 0..7 排列
            val cellVertices = Array(8) { localIdx ->
                val a = localIdx and 1
                val b = (localIdx shr 1) and 1
                val c = (localIdx shr 2) and 1
                val gIdx = globalIndex(ax, valFixed, a, b, c)
                vertices[gIdx]
            }

            // 为每个立方体生成 6 个四面体
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