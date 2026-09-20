package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.utils.Coordinate4D
import org.joml.Vector4f

interface CameraInterface {

    /** 获取摄像机坐标 */
    fun getPosition(): Vector4f

    /** 获取摄像机视线 */
    fun getView(): Coordinate4D

    /** 设置摄像机位置 */
    fun setPosition(pos: Vector4f)

    /** 设置摄像机视线 */
    fun setView(v: Coordinate4D)

    /** 正数向x+移动，负数向x-移动 */
    fun moveRight(distance: Float)

    /** 正数向y+移动，负数向y-移动 */
    fun moveUp(distance: Float)

    /** 正数向z+移动，负数向z-移动 */
    fun moveAna(distance: Float)

    /** 正数向w+移动，负数向w-移动 */
    fun moveForward(distance: Float)

    /** XY 平面：屏幕内旋转（等于 3D 的 roll） */
    fun rotateXY(angle: Float)

    /** XZ 平面：右方向卷入/卷出第四维 */
    fun rotateXZ(angle: Float)

    /** XW 平面：偏航 yaw（左右环顾，不动 vy、vz） */
    fun rotateXW(angle: Float)

    /** YZ 平面：上方向卷入/卷出第四维 */
    fun rotateYZ(angle: Float)

    /** YW 平面：俯仰 pitch（抬头低头，不动 vx、vz） */
    fun rotateYW(angle: Float)

    /** ZW 平面：前方卷入/卷出第四维（4D 里最有"另类"感的旋转） */
    fun rotateZW(angle: Float)

}