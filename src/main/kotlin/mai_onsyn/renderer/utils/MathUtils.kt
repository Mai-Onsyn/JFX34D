package mai_onsyn.renderer.utils

import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

fun Vector3f.rotate(angle: Float, axis: Vector3f): Vector3f {
    val kr = Vector3f(axis).normalize()

    val dp = this.dot(kr)

    val cp = Vector3f()
    kr.cross(this, cp)

    val cosTheta = cos(angle)
    val sinTheta = sin(angle)

    val vxT = x * cosTheta + (1 - cosTheta) * dp * kr.x + sinTheta * cp.x
    val vyT = y * cosTheta + (1 - cosTheta) * dp * kr.y + sinTheta * cp.y
    val vzT = z * cosTheta + (1 - cosTheta) * dp * kr.z + sinTheta * cp.z

    return Vector3f(vxT, vyT, vzT)
}