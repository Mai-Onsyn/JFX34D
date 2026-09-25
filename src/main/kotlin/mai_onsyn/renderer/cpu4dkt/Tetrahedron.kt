package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f

data class Tetrahedron(
    val v0: Vertex4D,
    val v1: Vertex4D,
    val v2: Vertex4D,
    val v3: Vertex4D,
    val id: Int = -1
) {
    fun pack(dest: FloatArray, offset: Int) {
        var offset = offset
        fun packVertex(v: Vertex4D) {
            dest[offset++] = v.pos.x
            dest[offset++] = v.pos.y
            dest[offset++] = v.pos.z
            dest[offset++] = v.pos.w
            dest[offset++] = Float.fromBits(v.color.hex)
            dest[offset++] = v.normal.x
            dest[offset++] = v.normal.y
            dest[offset++] = v.normal.z
            dest[offset++] = v.normal.w
        }
        packVertex(v0)
        packVertex(v1)
        packVertex(v2)
        packVertex(v3)
    }

    fun transform(matrix5f: Matrix5f): Tetrahedron {
        return Tetrahedron(
            this.v0.copy(pos = (matrix5f * Vector5f(this.v0.pos)).toVector4f()),
            this.v1.copy(pos = (matrix5f * Vector5f(this.v1.pos)).toVector4f()),
            this.v2.copy(pos = (matrix5f * Vector5f(this.v2.pos)).toVector4f()),
            this.v3.copy(pos = (matrix5f * Vector5f(this.v3.pos)).toVector4f()),
            this.id
        )
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
                ), ColorARGB(arr[offset++].toRawBits()),
                Vector4f(
                    arr[offset++],
                    arr[offset++],
                    arr[offset++],
                    arr[offset++]
                )
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
    val color: ColorARGB,
    val normal: Vector4f
)