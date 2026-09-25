package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.utils.ColorARGB

interface Scene3D {
    fun getMeshes(): List<Mesh>
    fun getCamera(): Camera

    /** 场景光源; 默认没有光源, 实现类自己决定怎么存 */
    fun getLights(): MutableList<Light> = mutableListOf()

    /** 全局环境光 */
    fun getAmbient(): ColorARGB = ColorARGB(0.3f, 0.3f, 0.3f, 1f)

    /** 设置全局环境光; 不支持的话就别实现 */
    fun setAmbient(color: ColorARGB) {
        throw UnsupportedOperationException("${this::class.simpleName} 不支持设置环境光")
    }
}
