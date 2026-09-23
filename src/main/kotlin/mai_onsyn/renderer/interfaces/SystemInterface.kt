package mai_onsyn.renderer.interfaces

interface SystemInterface {
    /** 调整3D渲染器最大FPS */
    fun set3DMaxFPS(fps: Float)

    /** 获取3D渲染器FPS */
    fun get3DFPS(): Float

    /** 获取3D渲染器1% Low帧 */
    fun get3D1PercentLowFPS(): Float

    /** 调整4D渲染器最大FPS */
    fun set4DMaxFPS(fps: Float)

    /** 获取4D渲染器FPS */
    fun get4DFPS(): Float

    /** 开启仅线框渲染 */
    fun enableTriangleLineRendering(enable: Boolean)

    /** 开启光照渲染 */
    fun enableLightRendering(enable: Boolean)
}