package mai_onsyn.renderer.ogl3d.data

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 线程安全的场景。
 *
 * 用法约定:
 *   外部/生成线程 —— 只管 [addMesh] / [removeMesh] 和往 Mesh 里塞数据, **绝不碰 GL**;
 *   渲染线程     —— 每帧 [getMeshes] 拿快照遍历, 并在帧首调 [drainPendingDispose] 释放 GPU 资源。
 *
 * 用 CopyOnWriteArrayList 是因为读多写少 (每帧读 N 次, 增删偶尔才发生),
 * 遍历时不需要加锁也不会 ConcurrentModificationException。
 */
class SimpleScene3D(
    val meshList: MutableList<Mesh> = CopyOnWriteArrayList(),
    val mainCamera: Camera = Camera()
) : Scene3D {

    /** 外部线程删掉的 mesh 先排队, 等渲染线程有空了再 dispose (GL 只能在渲染线程调) */
    private val pendingDispose = ConcurrentLinkedQueue<Mesh>()

    override fun getMeshes(): List<Mesh> = meshList

    override fun getCamera(): Camera = mainCamera

    /** 加一个 mesh。外部线程随便调 */
    fun addMesh(mesh: Mesh) {
        meshList.add(mesh)
    }

    /**
     * 删一个 mesh。外部线程随便调 —— 这里只把它从渲染列表拿掉并排队,
     * 真正的 glDelete* 留给渲染线程, 所以外部线程不需要 GL 上下文。
     */
    fun removeMesh(mesh: Mesh): Boolean {
        val removed = meshList.remove(mesh)
        if (removed) pendingDispose.add(mesh)
        return removed
    }

    fun clearMeshes() {
        val all = meshList.toList()
        meshList.clear()
        pendingDispose.addAll(all)
    }

    /** 渲染线程每帧开头调: 把这一帧要删的 mesh 释放掉 */
    fun drainPendingDispose() {
        while (true) {
            val mesh = pendingDispose.poll() ?: return
            mesh.dispose()
        }
    }
}
