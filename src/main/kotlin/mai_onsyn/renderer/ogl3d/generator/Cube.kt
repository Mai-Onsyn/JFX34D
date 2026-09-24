package mai_onsyn.renderer.ogl3d.generator

import mai_onsyn.renderer.ogl3d.data.ColorARGB
import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.ogl3d.data.Vertex
import org.joml.Vector2f
import org.joml.Vector3f

enum class CubeFace(val normal: Vector3f, val corners: List<Vector3f>) {
    FRONT (Vector3f(0f,0f,1f),  listOf(Vector3f(-1f,-1f, 1f), Vector3f( 1f,-1f, 1f), Vector3f( 1f, 1f, 1f), Vector3f(-1f, 1f, 1f))),
    BACK  (Vector3f(0f,0f,-1f), listOf(Vector3f( 1f,-1f,-1f), Vector3f(-1f,-1f,-1f), Vector3f(-1f, 1f,-1f), Vector3f( 1f, 1f,-1f))),
    RIGHT (Vector3f(1f,0f,0f),  listOf(Vector3f( 1f,-1f, 1f), Vector3f( 1f,-1f,-1f), Vector3f( 1f, 1f,-1f), Vector3f( 1f, 1f, 1f))),
    LEFT  (Vector3f(-1f,0f,0f), listOf(Vector3f(-1f,-1f,-1f), Vector3f(-1f,-1f, 1f), Vector3f(-1f, 1f, 1f), Vector3f(-1f, 1f,-1f))),
    TOP   (Vector3f(0f,1f,0f),  listOf(Vector3f(-1f, 1f, 1f), Vector3f( 1f, 1f, 1f), Vector3f( 1f, 1f,-1f), Vector3f(-1f, 1f,-1f))),
    BOTTOM(Vector3f(0f,-1f,0f), listOf(Vector3f(-1f,-1f,-1f), Vector3f( 1f,-1f,-1f), Vector3f( 1f,-1f, 1f), Vector3f(-1f,-1f, 1f)));
}

fun createCube(
    size: Float = 2f,
    center: Vector3f = Vector3f(),
    texture: Texture? = null,
    colorOf: (CubeFace) -> ColorARGB = { face ->
        when (face) {
            CubeFace.TOP    -> ColorARGB(1f, 0f, 0f)
            CubeFace.BOTTOM -> ColorARGB(0f, 1f, 0f)
            CubeFace.LEFT   -> ColorARGB(0f, 0f, 1f)
            CubeFace.RIGHT  -> ColorARGB(1f, 1f, 0f)
            CubeFace.FRONT  -> ColorARGB(0f, 1f, 1f)
            CubeFace.BACK   -> ColorARGB(1f, 0f, 1f)
        }
    }
): MutableList<Triangle> {
    val h = size / 2f
    val uv = listOf(Vector2f(0f, 0f), Vector2f(1f, 0f), Vector2f(1f, 1f), Vector2f(0f, 1f))
    return CubeFace.entries.flatMap { f ->
        val color = colorOf(f)
        val v = f.corners.mapIndexed { i, p ->
            Vertex(
                Vector3f(center.x + p.x * h, center.y + p.y * h, center.z + p.z * h),
                color,
                Vector3f(f.normal),
                Vector2f(uv[i])
            )
        }
        listOf(Triangle(v[0], v[1], v[2], texture), Triangle(v[0], v[2], v[3], texture))
    } as MutableList<Triangle>
}