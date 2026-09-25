package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.minus

data class Triangle(
    val v0: Vertex,
    val v1: Vertex,
    val v2: Vertex,
    val texture: Texture? = null
)

data class Tetrahedron3D(
    val v0: Vertex,
    val v1: Vertex,
    val v2: Vertex,
    val v3: Vertex
) {
    companion object {
        fun extract(arr: FloatArray, offset: Int): Tetrahedron3D {
            var offset = offset
            fun extractVertex(): Vertex = Vertex(
                Vector3f(
                    arr[offset++],
                    arr[offset++],
                    arr[offset++]
                ), ColorARGB(arr[offset++].toRawBits()),
                Vector3f(
                    arr[offset++],
                    arr[offset++],
                    arr[offset++]
                ), Vector2f()
            )
            return Tetrahedron3D(
                extractVertex(),
                extractVertex(),
                extractVertex(),
                extractVertex()
            )
        }
    }
}

fun List<Tetrahedron3D>.toMesh(): Mesh {
    val mesh = Mesh()

    fun calcNormal(a: Vector3f, b: Vector3f, c: Vector3f, d: Vector3f): Vector3f {
        val n = (b - a).cross(c - a)
        val s = n.dot(d - a)
        return if (s > 0) n / (-n.length())
        else n.normalize()
    }
    this.forEach {
        val n0 = calcNormal(it.v1.pos, it.v2.pos, it.v3.pos, it.v0.pos)
        val n1 = calcNormal(it.v2.pos, it.v3.pos, it.v0.pos, it.v1.pos)
        val n2 = calcNormal(it.v3.pos, it.v0.pos, it.v1.pos, it.v2.pos)
        val n3 = calcNormal(it.v0.pos, it.v1.pos, it.v2.pos, it.v3.pos)
        mesh.triangles.add(Triangle(it.v0.copy(normal = n3), it.v1.copy(normal = n3), it.v2.copy(normal = n3)))
        mesh.triangles.add(Triangle(it.v0.copy(normal = n2), it.v1.copy(normal = n2), it.v3.copy(normal = n2)))
        mesh.triangles.add(Triangle(it.v0.copy(normal = n1), it.v2.copy(normal = n1), it.v3.copy(normal = n1)))
        mesh.triangles.add(Triangle(it.v1.copy(normal = n0), it.v2.copy(normal = n0), it.v3.copy(normal = n0)))
    }
    return mesh
}

data class Vertex(
    val pos: Vector3f,
    val color: ColorARGB,
    val normal: Vector3f,
    val uv: Vector2f,
) {
    fun copy(): Vertex {
        return Vertex(
            Vector3f(pos.x, pos.y, pos.z),
            color,
            Vector3f(normal.x, normal.y, normal.z),
            Vector2f(uv.x, uv.y)
        )
    }
}