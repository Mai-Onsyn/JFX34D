package mai_onsyn.renderer.data

import org.joml.Vector2f
import org.joml.Vector3f

data class Triangle(
    val v0: Vertex,
    val v1: Vertex,
    val v2: Vertex,
    val texture: Texture? = null
)

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
value class ColorARGB(val hex: UInt) {
    val alpha: Int
        get() = ((hex shr 24) and 0xffu).toInt()

    val red: Int
        get() = ((hex shr 16) and 0xffu).toInt()

    val green: Int
        get() = ((hex shr 8) and 0xffu).toInt()

    val blue: Int
        get() = ((hex shr 0) and 0xffu).toInt()

    val alphaF: Float
        get() = ((hex shr 24) and 0xffu).toFloat() * 0.003921569f

    val redF: Float
        get() = ((hex shr 16) and 0xffu).toFloat() * 0.003921569f

    val greenF: Float
        get() = ((hex shr 8) and 0xffu).toFloat() * 0.003921569f

    val blueF: Float
        get() = ((hex shr 0) and 0xffu).toFloat() * 0.003921569f
}