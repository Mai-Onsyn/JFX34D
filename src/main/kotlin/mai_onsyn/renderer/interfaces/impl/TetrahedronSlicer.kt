package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.cpu4dkt.hyperplaneNormal
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f
import kotlin.math.abs

/**
 * 用四维超平面精确切割四面体。
 *
 * 超平面由四个仿射无关的点定义，法向量用四维广义叉积求出，f(p) = n·p - d。
 *
 * 被跨越的四面体只有两种切法：
 * - 1 : 3  一侧是一个四面体，另一侧是一个三棱柱；
 * - 2 : 2  两侧各是一个三棱柱。
 * 三棱柱按标准方式分解为 3 个四面体。
 *
 * 落在超平面上的顶点统一按"非负"一侧参与分类（符号扰动）。这样顶点共面、棱共面
 * 这些边界情形不需要单独分支：退化出来的子四面体（重复顶点）会被丢弃，
 * 既不会重复计入，也不会漏掉体积。
 */
internal object TetrahedronSlicer {

    private const val EPS = 1e-6f

    /** f(p) = n·p - d；f < 0 归 A，f >= 0 归 B */
    class Plane internal constructor(private val n: Vector4f, private val d: Float) {
        fun f(p: Vector4f): Float = n.x * p.x + n.y * p.y + n.z * p.z + n.w * p.w - d
        val normal: Vector4f get() = Vector4f(n)
    }

    /** 由四个点求出超平面；四点仿射相关时抛异常 */
    fun planeOf(a: Vector4f, b: Vector4f, c: Vector4f, d: Vector4f): Plane {
        val n = hyperplaneNormal(a, b, c, d)
        return Plane(n, n.dot(a))
    }

    /** @return A 侧（f < 0）与 B 侧（f > 0）的四面体 */
    fun split(tets: List<Tetrahedron>, plane: Plane): Pair<List<Tetrahedron>, List<Tetrahedron>> {
        val a = mutableListOf<Tetrahedron>()
        val b = mutableListOf<Tetrahedron>()
        tets.forEach { splitOne(it, plane, a, b) }
        return a to b
    }

    private fun splitOne(
        tet: Tetrahedron,
        plane: Plane,
        a: MutableList<Tetrahedron>,
        b: MutableList<Tetrahedron>
    ) {
        val vs = tet.vertices
        val f = FloatArray(4) { plane.f(vs[it].pos) }
        val neg = (0..3).filter { f[it] < 0f }
        val pos = (0..3).filter { f[it] >= 0f }

        if (neg.isEmpty()) { b.add(tet); return }   // 整体在非负侧（含共面）
        if (pos.isEmpty()) { a.add(tet); return }   // 整体在负侧

        /** 棱 (i, j) 与超平面的交点；端点恰在平面上时就是该端点 */
        fun cut(i: Int, j: Int): Vertex4D {
            if (f[i] == 0f) return vs[i]
            if (f[j] == 0f) return vs[j]
            return lerp(vs[i], vs[j], f[i] / (f[i] - f[j]))
        }

        when (neg.size) {
            1 -> {
                // A 侧：单个负顶点 + 三条棱上的交点；B 侧：三棱柱
                val n = neg[0]
                addTet(a, vs[n], cut(n, pos[0]), cut(n, pos[1]), cut(n, pos[2]))
                prism(
                    b,
                    vs[pos[0]], vs[pos[1]], vs[pos[2]],
                    cut(n, pos[0]), cut(n, pos[1]), cut(n, pos[2])
                )
            }

            3 -> {
                // 与上面对称
                val p = pos[0]
                addTet(b, vs[p], cut(p, neg[0]), cut(p, neg[1]), cut(p, neg[2]))
                prism(
                    a,
                    vs[neg[0]], vs[neg[1]], vs[neg[2]],
                    cut(p, neg[0]), cut(p, neg[1]), cut(p, neg[2])
                )
            }

            else -> {
                // 2 : 2，两侧各是一个三棱柱
                val n0 = neg[0]; val n1 = neg[1]
                val p0 = pos[0]; val p1 = pos[1]
                prism(
                    a,
                    vs[n0], cut(n0, p0), cut(n0, p1),
                    vs[n1], cut(n1, p0), cut(n1, p1)
                )
                prism(
                    b,
                    vs[p0], cut(p0, n0), cut(p0, n1),
                    vs[p1], cut(p1, n0), cut(p1, n1)
                )
            }
        }
    }

    /**
     * 三棱柱 (A1,A2,A3)-(B1,B2,B3) 分解为 3 个四面体。
     * Ai-Bi 是侧棱：A1-A2-A3 与 B1-B2-B3 是两个底面三角形。
     */
    private fun prism(
        dst: MutableList<Tetrahedron>,
        a1: Vertex4D, a2: Vertex4D, a3: Vertex4D,
        b1: Vertex4D, b2: Vertex4D, b3: Vertex4D
    ) {
        addTet(dst, a1, a2, a3, b1)
        addTet(dst, a2, a3, b1, b2)
        addTet(dst, a3, b1, b2, b3)
    }

    private fun addTet(dst: MutableList<Tetrahedron>, vararg v: Vertex4D) {
        if (isDegenerate(v)) return
        dst.add(Tetrahedron(v[0], v[1], v[2], v[3]))
    }

    /** 有重复顶点就是退化结果（体积为 0），直接丢弃 */
    private fun isDegenerate(v: Array<out Vertex4D>): Boolean {
        for (i in v.indices) {
            for (j in i + 1 until v.size) {
                val p = v[i].pos
                val q = v[j].pos
                if (abs(p.x - q.x) < EPS && abs(p.y - q.y) < EPS &&
                    abs(p.z - q.z) < EPS && abs(p.w - q.w) < EPS
                ) return true
            }
        }
        return false
    }

    /** 位置线性插值，颜色与法向量一并插值 */
    private fun lerp(a: Vertex4D, b: Vertex4D, t: Float): Vertex4D = Vertex4D(
        Vector4f(a.pos).lerp(b.pos, t),
        ColorARGB(
            r = a.color.redF + (b.color.redF - a.color.redF) * t,
            g = a.color.greenF + (b.color.greenF - a.color.greenF) * t,
            b = a.color.blueF + (b.color.blueF - a.color.blueF) * t,
            a = a.color.alphaF + (b.color.alphaF - a.color.alphaF) * t
        ),
        Vector4f(a.normal).lerp(b.normal, t).also {
            if (it.lengthSquared() > 1e-12f) it.normalize()
        }
    )
}
