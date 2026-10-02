package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.utils.Coordinate3D
import mai_onsyn.renderer.utils.Direction
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.times
import kotlin.math.cos
import kotlin.math.sin

/**
 * 三维模型变换，语义与 `cpu4dkt` 的 `Transform4D` 完全对齐：
 * 每个变换都按 `matrix ← T⁻¹ · new · T · matrix` 计算，即
 * **"在当前变换坐标系下解释，然后左乘到已有矩阵上"**。
 *
 * `T` / `T⁻¹` 由 [setTransformCoordinate] 设置，默认是世界坐标系（单位阵）。
 *
 * 约定（与文档 §0.2 / §4.2 / §5.3.3 一致）：
 * - 顶点是**列向量**，`p_out = matrix · p_in`；平移量写在矩阵的**第 4 列**；
 * - 旋转在二维平面内进行，平面 `(a, b)` 上 **a 轴转向 b 轴为正**：
 *   `a' = a·cos θ − b·sin θ`，`b' = a·sin θ + b·cos θ`；
 * - 角度一律是**角度制**，内部 `toRadians`。
 *
 * 注意 JOML 的 `Matrix4f` 是**列主序**（字段名 `m<列><行>`），和 `Matrix5f` 的行主序不同，
 * 下面构造旋转矩阵时 16 个参数依次是 col0、col1、col2、col3。
 */
class Transform(
    var matrix: Matrix4f = Matrix4f()
) {

    // T       世界 -> 当前坐标系
    private var tT = Matrix4f()
    // T^(-1)  当前坐标系 -> 世界
    private var tTn1 = Matrix4f()

    companion object {
        val NONE: Transform
            get() = Transform()

        /** 单位变换，语义与 [NONE] 相同，命名更直白时可用 */
        val IDENTITY: Transform
            get() = Transform()
    }

    /** 按三维向量平移（等价于各轴分别 move） */
    fun move(v: Vector3f) {
        val new = Matrix4f().translate(v.x, v.y, v.z)
        matrix = tTn1 * new * tT * matrix
    }

    /** 缩放；负值镜像。缺省的轴按与 x 相同的倍率 */
    fun scale(x: Float, y: Float = x, z: Float = x) {
        val new = Matrix4f(
            x, 0f, 0f, 0f,      // col0
            0f, y, 0f, 0f,      // col1
            0f, 0f, z, 0f,      // col2
            0f, 0f, 0f, 1f      // col3
        )
        matrix = tTn1 * new * tT * matrix
    }

    /**
     * 在二维平面内旋转，[angle] 为角度制。
     *
     * 三维只支持三个平面：[Direction.Plane.XY] / [Direction.Plane.XZ] / [Direction.Plane.YZ]，
     * 传 `XW` / `YW` / `ZW` 会抛 `IllegalArgumentException`。
     */
    fun rotate(plane: Direction.Plane, angle: Float) {
        val r = Math.toRadians(angle.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        val new = when (plane) {
            Direction.Plane.XY -> Matrix4f(
                c, s, 0f, 0f,       // col0
                -s, c, 0f, 0f,      // col1
                0f, 0f, 1f, 0f,     // col2
                0f, 0f, 0f, 1f      // col3
            )
            Direction.Plane.XZ -> Matrix4f(
                c, 0f, s, 0f,
                0f, 1f, 0f, 0f,
                -s, 0f, c, 0f,
                0f, 0f, 0f, 1f
            )
            Direction.Plane.YZ -> Matrix4f(
                1f, 0f, 0f, 0f,
                0f, c, s, 0f,
                0f, -s, c, 0f,
                0f, 0f, 0f, 1f
            )
            Direction.Plane.XW,
            Direction.Plane.YW,
            Direction.Plane.ZW ->
                throw IllegalArgumentException(
                    "3D transform only supports rotation planes XY/XZ/YZ, got $plane"
                )
        }
        matrix = tTn1 * new * tT * matrix
    }

    /**
     * 剪切（切变）：`p_dest ← p_dest + k · p_src`；`src == dest` 时是 `p ← (1 + k) · p`。
     *
     * 三维只支持 `X` / `Y` / `Z` 三轴，传 `W` 会抛 `IllegalArgumentException`。
     */
    fun clip(src: Direction.Axis, dest: Direction.Axis, k: Float) {
        require(src != Direction.Axis.W && dest != Direction.Axis.W) {
            "3D transform only supports axes X/Y/Z for clip, got $src -> $dest"
        }
        val new = Matrix4f()    // 单位阵
        // JOML 的 set(index, value) 按列主序，index = 列 * 4 + 行
        new.set(src.ordinal, dest.ordinal, if (src == dest) 1f + k else k)
        matrix = tTn1 * new * tT * matrix
    }

    /**
     * 设置"当前变换坐标系"：之后同一 Transform 上的 move/scale/rotate/clip 都以 [origin] 为原点、
     * 以 [coordinate] 的三个基向量为轴。默认是世界坐标系。
     */
    fun setTransformCoordinate(origin: Vector3f, coordinate: Coordinate3D) {
        // tT: 世界 -> 局部。三行分别是 vx、vy、vz，平移列是 -vi·origin
        tT = Matrix4f(
            coordinate.vx.x, coordinate.vy.x, coordinate.vz.x, 0f,
            coordinate.vx.y, coordinate.vy.y, coordinate.vz.y, 0f,
            coordinate.vx.z, coordinate.vy.z, coordinate.vz.z, 0f,
            -coordinate.vx.dot(origin), -coordinate.vy.dot(origin), -coordinate.vz.dot(origin), 1f
        )
        // tTn1: 局部 -> 世界，是 tT 的逆（基向量正交时就是转置 + 平移 origin）
        tTn1 = Matrix4f(
            coordinate.vx.x, coordinate.vx.y, coordinate.vx.z, 0f,
            coordinate.vy.x, coordinate.vy.y, coordinate.vy.z, 0f,
            coordinate.vz.x, coordinate.vz.y, coordinate.vz.z, 0f,
            origin.x, origin.y, origin.z, 1f
        )
    }

    /** `matrix ← m · matrix`（左乘），对应文档里 3D 变换的 `MATRIX` 方法 */
    fun applyMatrix(m: Matrix4f) {
        matrix = Matrix4f(m) * matrix
    }

    /** 深拷贝（连内部的坐标系基准一起带过去） */
    fun copy(): Transform = Transform(Matrix4f(matrix)).also {
        it.tT = Matrix4f(tT)
        it.tTn1 = Matrix4f(tTn1)
    }

    override fun toString(): String {
        val a = FloatArray(16)
        matrix.get(a)   // 列主序：a[col * 4 + row]
        return """
            %.2f, %.2f, %.2f, %.2f
            %.2f, %.2f, %.2f, %.2f
            %.2f, %.2f, %.2f, %.2f
            %.2f, %.2f, %.2f, %.2f
        """.trimIndent().format(
            a[0], a[4], a[8], a[12],
            a[1], a[5], a[9], a[13],
            a[2], a[6], a[10], a[14],
            a[3], a[7], a[11], a[15],
        )
    }
}
