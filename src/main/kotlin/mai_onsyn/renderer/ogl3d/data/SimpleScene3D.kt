package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.utils.ColorARGB
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 场景: 并发安全的 mesh 列表 + 光源列表 + 全局环境光。
 *
 * 用 CopyOnWriteArrayList, 增删查都可以并发:
 *   渲染线程每帧遍历不会 ConcurrentModificationException;
 *   外部线程 add / remove 也不用自己加锁。
 *
 * mesh 的 GPU 资源 (Mesh.dispose / GLMaterial.dispose) 由调用方自己负责,
 * 这里不管生命周期。光源是纯数据, 也没有生命周期问题。
 */
class SimpleScene3D(
    val meshList: MutableList<Mesh> = CopyOnWriteArrayList(),
    /** 光源。默认放一盏点光源, 不然场景只有环境光 */
    val lightList: MutableList<Light> = CopyOnWriteArrayList(listOf(Light())),
    private val camera: Camera = Camera()
) : Scene3D {

    /** 全局环境光, 整个场景一份 (不是单个光源的属性) */
    private var ambient: ColorARGB = ColorARGB(0.3f, 0.3f, 0.3f, 1f)

    override fun getMeshes(): List<Mesh> = meshList

    override fun getLights(): MutableList<Light> = lightList

    override fun getAmbient(): ColorARGB = ambient

    override fun setAmbient(color: ColorARGB) {
        ambient = color
    }

    override fun getCamera(): Camera = camera

    fun addMesh(mesh: Mesh) {
        meshList.add(mesh)
    }

    fun addLight(light: Light) {
        lightList.add(light)
    }

    /** 清掉所有光源, 场景变成只有环境光 */
    fun removeLights() {
        lightList.clear()
    }

    fun removeLight(light: Light) {
        lightList.remove(light)
    }
}
