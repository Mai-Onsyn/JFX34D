package mai_onsyn.renderer.utils

import org.joml.Vector4f

data class Coordinate4D @JvmOverloads constructor(
    val vx: Vector4f = Vector4f(1f, 0f, 0f, 0f),
    val vy: Vector4f = Vector4f(0f, 1f, 0f, 0f),
    val vz: Vector4f = Vector4f(0f, 0f, 1f, 0f),
    val vw: Vector4f = Vector4f(0f, 0f, 0f, 1f)
)