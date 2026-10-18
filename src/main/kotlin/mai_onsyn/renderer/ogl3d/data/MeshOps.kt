package mai_onsyn.renderer.ogl3d.data

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs

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
