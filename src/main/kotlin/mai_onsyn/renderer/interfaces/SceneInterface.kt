package mai_onsyn.renderer.interfaces

import javafx.scene.paint.Color
import mai_onsyn.renderer.ogl3d.data.Light
import mai_onsyn.renderer.utils.ColorARGB

interface SceneInterface {
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

    /** 获取4D渲染器1% Low帧 */
    fun get4D1PercentLowFPS(): Float

    /** 开启仅线框渲染 */
    fun enableTriangleLineRendering(enable: Boolean)

    /** 调整渲染窗口的背景色 */
    fun setBackgroundColor(color: Color)

    /** 开启光照渲染 */
    fun enableLightRendering(enable: Boolean)

    /** 列出光源(AI用) */
    fun listLight(): String

    /** 列出光源(UI用) */
    fun listLights(): List<Light>

    /** 添加光源 */
    fun addLights(light: Light)

    /** 删除光源 */
    fun removeLights(names: List<String>)

    /** 设置环境光 */
    fun setAmbientLight(ambientLight: ColorARGB)

    /** 设置视口矩阵的边长，也就是四维超平面屏幕的尺寸 */
    fun setDisplaySize(edgeLength: Float)

    /** 当键盘切换输入键按下时或手动调用[setEnable4DInput]时触发 */
    fun setOnEnable4DInputChanged(eventHandler: (Boolean) -> Unit)

    /** 设置是否开启4D输入 如果关闭，即为开启3D输入，两者互斥，初始应调用一次，通过[setOnEnable4DInputChanged]回调初始化UI */
    fun setEnable4DInput(enable: Boolean)
}