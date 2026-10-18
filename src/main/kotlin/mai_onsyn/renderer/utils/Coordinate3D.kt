package mai_onsyn.renderer.utils

import org.joml.Vector3f

data class Coordinate3D @JvmOverloads constructor(
    val vx: Vector3f = Vector3f(1f, 0f, 0f),
    val vy: Vector3f = Vector3f(0f, 1f, 0f),
    val vz: Vector3f = Vector3f(0f, 0f, 1f)
)
