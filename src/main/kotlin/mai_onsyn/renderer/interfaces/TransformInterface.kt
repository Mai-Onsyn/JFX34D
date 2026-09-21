package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector4f

interface TransformInterface {

    /** 获取模型变化矩阵 */
    fun getModelMatrix(name: String)

    /** 矩阵变换 */
    fun transform(m: Matrix5f)

    /** 移动 */
    fun move(v: Vector4f)

    /** 缩放 */
    fun scale(x: Float, y: Float, z: Float, w: Float)

    /** 旋转 */
    fun rotate(axis: Direction.Plane, angle: Float)

    /** 裁剪 */
    fun clip(src: Direction.Axis, dest: Direction.Axis, amount: Float)

    /** 设置变换坐标系 */
    fun setCoordinate(origin: Vector4f, coordinate: Coordinate4D)
}