package mai_onsyn.renderer.ogl3d.data

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs

/**
 * 3D 网格的便捷操作：三角形列表 ↔ [Mesh]，以及把变换**手动烘焙**进顶点。
 *
 * 为什么要手动烘焙：`cpu4dkt` 的 `constructPrism4` / `constructCone4`（服务于
 * `ShapeInterface.createPrism4` / `createCone4`）只读 `base.triangles` 里顶点的 `pos`，
 * **不会**去乘 `base.transform.matrix`。所以给 3D 底面做的变换必须在调用这两个 4D
 * 生成器之前先落到顶点上，也就是先
 *
 * ```
 * val base = createPrism(...).toMesh()          // 生成
 * val t = Transform(); t.rotate(Direction.Plane.XZ, 30f)   // 变换累加到 matrix 上
 * val baked = base.applyTransform(t)            // 手动烘焙进顶点（返回新 Mesh）
 * renderer.shape.createPrism4("Tower", "Body", baked, ws = -1f, we = 1f)
 * ```
 */

/** 把一组三角形包成 [Mesh]；[transform] 只挂载、不烘焙（渲染时由 GPU 应用）。 */
fun MutableList<Triangle>.toMesh(transform: Transform = Transform.NONE): Mesh =
    Mesh(this, transform)

/**
 * 手动把 [matrix] 烘焙进顶点，返回**新** Mesh（源 Mesh 不变），结果的 transform 为单位阵。
 * 颜色 / UV / 贴图原样保留。
 */
fun Mesh.applyTransform(matrix: Matrix4f): Mesh =
    Mesh(triangles.transformed(matrix), Transform.NONE, type)

/** 用 Mesh 自己挂着的 [Mesh.transform] 烘焙（便于"先在 Mesh 上累加变换，再烘焙"的写法）。 */
fun Mesh.applyTransform(transform: Transform = this.transform): Mesh =
    applyTransform(Matrix4f(transform.matrix))

/**
 * 把矩阵应用到每个顶点，返回新的三角形列表（源列表不变）：
 * - 位置：`p' = M · p`（含平移，w 按 1 处理，内部做透视除法，仿射矩阵下等价）；
 * - 法向：`n' = (M₃ₓ₃⁻¹)ᵀ · n` 再归一化 —— 非均匀缩放 / 切变下方向才正确；
 * - 颜色、UV、贴图直接带过去。
 */
fun MutableList<Triangle>.transformed(matrix: Matrix4f): MutableList<Triangle> {
    val normalMatrix = normalMatrixOf(matrix)
    val result = ArrayList<Triangle>(size)
    for (t in this) {
        result.add(
            Triangle(
                t.v0.transformed(matrix, normalMatrix),
                t.v1.transformed(matrix, normalMatrix),
                t.v2.transformed(matrix, normalMatrix),
                t.texture
            )
        )
    }
    return result
}

/** 法向矩阵 = 左上 3×3 的逆转置；矩阵奇异（行列式≈0）时退回原 3×3。 */
private fun normalMatrixOf(matrix: Matrix4f): Matrix3f {
    val nm = Matrix3f().set(matrix)
    if (abs(nm.determinant()) > 1e-12f) nm.invert().transpose()
    return nm
}

private fun Vertex.transformed(matrix: Matrix4f, normalMatrix: Matrix3f): Vertex {
    val n = Vector3f(normal).mul(normalMatrix)
    if (n.lengthSquared() > 1e-12f) n.normalize()
    return Vertex(
        matrix.transformPosition(Vector3f(pos)),
        color,
        n,
        Vector2f(uv)
    )
}
