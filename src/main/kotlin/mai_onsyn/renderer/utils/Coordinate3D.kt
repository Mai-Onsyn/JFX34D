package mai_onsyn.renderer.utils

import org.joml.Vector3f

/**
 * 三维基坐标系，与 [Coordinate4D] 对称。
 *
 * 三个基向量默认是标准正交基 `(x, y, z)`。
 * 只用来给 [mai_onsyn.renderer.ogl3d.data.Transform.setTransformCoordinate] 提供
 * "当前变换坐标系"的取向，本身不做任何校验（正交/单位性由调用方保证）。
 */
data class Coordinate3D @JvmOverloads constructor(
    val vx: Vector3f = Vector3f(1f, 0f, 0f),
    val vy: Vector3f = Vector3f(0f, 1f, 0f),
    val vz: Vector3f = Vector3f(0f, 0f, 1f)
)
