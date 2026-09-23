package mai_onsyn.renderer.ogl3d.data

import java.util.concurrent.CopyOnWriteArrayList

/**
 * 场景: 就一个并发安全的 mesh 列表。
 *
 * 用 CopyOnWriteArrayList, 增删查都可以并发:
 *   渲染线程每帧遍历不会 ConcurrentModificationException;
 *   外部线程 add / remove 也不用自己加锁。
 *
 * mesh 的 GPU 资源 (Mesh.dispose / GLMaterial.dispose) 由调用方自己负责,
 * 这里不管生命周期。
 */
class SimpleScene3D(
    val meshList: MutableList<Mesh> = CopyOnWriteArrayList(),
    val mainCamera: Camera = Camera()
) : Scene3D {

    override fun getMeshes(): List<Mesh> = meshList

    override fun getCamera(): Camera = mainCamera

    fun addMesh(mesh: Mesh) {
        meshList.add(mesh)
    }
}
