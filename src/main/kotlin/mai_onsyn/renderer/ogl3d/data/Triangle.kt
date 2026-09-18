package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

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
    this.forEach {
        mesh.triangles.add(Triangle(it.v0, it.v1, it.v2))
        mesh.triangles.add(Triangle(it.v0, it.v1, it.v3))
        mesh.triangles.add(Triangle(it.v0, it.v2, it.v3))
        mesh.triangles.add(Triangle(it.v1, it.v2, it.v3))
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

@JvmInline
value class ColorARGB(val hex: Int) {
    companion object {
        operator fun invoke(r: Float = 0f, g: Float = 0f, b: Float = 0f, a: Float = 1f): ColorARGB {
            val ai = (a.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val ri = (r.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val gi = (g.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val bi = (b.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            return ColorARGB((ai shl 24) or (ri shl 16) or (gi shl 8) or bi)
        }
    }

    val alpha: Int
        get() = ((hex ushr 24) and 0xff)

    val red: Int
        get() = ((hex ushr 16) and 0xff)

    val green: Int
        get() = ((hex ushr 8) and 0xff)

    val blue: Int
        get() = ((hex ushr 0) and 0xff)

    val alphaF: Float
        get() = ((hex ushr 24) and 0xff).toFloat() * 0.003921569f

    val redF: Float
        get() = ((hex ushr 16) and 0xff).toFloat() * 0.003921569f

    val greenF: Float
        get() = ((hex ushr 8) and 0xff).toFloat() * 0.003921569f

    val blueF: Float
        get() = ((hex ushr 0) and 0xff).toFloat() * 0.003921569f

    override fun toString(): String {
        return "#%02X%02X%02X%02X".format(alpha, red, green, blue)
    }
}