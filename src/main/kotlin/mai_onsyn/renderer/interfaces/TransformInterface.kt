package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector4f

interface TransformInterface {

    /** 获取模型变化矩阵 */
    fun getModelMatrix(name: String): Matrix5f

    /** 设置模型变换矩阵 */
    fun setModelMatrix(name: String, matrix: Matrix5f)

    /** 矩阵变换 */
    fun transform(name: String, m: Matrix5f)

    /** 移动 */
    fun move(name: String, v: Vector4f)

    /** 缩放 */
    fun scale(name: String, x: Float, y: Float, z: Float, w: Float)

    /** 旋转 */
    fun rotate(name: String, axis: Direction.Plane, angle: Float)

    /**
     * 裁剪
     * @param src 源轴，不改变
     * @param dest 目标轴，改变的轴
     */
    fun clip(name: String, src: Direction.Axis, dest: Direction.Axis, amount: Float)

    /** 设置变换坐标系 */
    fun setCoordinate(name: String, origin: Vector4f, coordinate: Coordinate4D)
}