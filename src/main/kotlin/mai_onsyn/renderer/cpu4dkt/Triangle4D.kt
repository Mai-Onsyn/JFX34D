package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.ogl3d.data.ColorARGB
import org.joml.Vector4f

data class Triangle4D(
    val v0: Vertex4D,
    val v1: Vertex4D,
    val v2: Vertex4D
)

data class Vertex4D(
    val pos: Vector4f,
    val color: ColorARGB
)