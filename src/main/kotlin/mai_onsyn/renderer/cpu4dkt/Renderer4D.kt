package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import mai_onsyn.renderer.ogl3d.data.toMesh
import mai_onsyn.renderer.utils.toRowMajorFloatArray
import org.joml.Matrix4f
import java.util.*

class Renderer4D(
    val scene: SimpleScene4D,
    val bindingSpace: SimpleScene3D
) {
    private val lock = Any()
    private val projected = IdentityHashMap<Mesh4D, Mesh>()

    init {
        Thread.ofVirtual().start {
            while (!Thread.interrupted()) {
                render(true)
                Thread.sleep(50)
            }
        }
    }

    private fun projectMesh(mesh: Mesh4D): Mesh {
        val flattened = JNIRasterizer.packMesh4D(mesh)
        val I = Matrix5f.IDENTITY.data
        val outArray = JNIRasterizer.project(
            flattened,
            mesh.tetrahedrons.size,
            I,
            scene.getCamera().viewMatrix.data,
            scene.getCamera().projectionMatrix().data,
            Matrix4f().scale(5f).toRowMajorFloatArray()
        )
        val tetrahedrons = JNIRasterizer.extractTetrahedrons(outArray)
        return tetrahedrons.toMesh()
    }

    fun render(force: Boolean = false) {
        synchronized(lock) {
            // 1) 移除：缓存里有、但 scene 里已不存在的（与是否强制无关）
            val alive = IdentityHashMap<Mesh4D, Boolean>()
            for (m4 in scene.getMeshes()) alive[m4] = true

            val it = projected.entries.iterator()
            while (it.hasNext()) {
                val (m4, m3) = it.next()
                if (!alive.containsKey(m4)) {
                    bindingSpace.meshList.remove(m3)   // 只删自己加进去的
                    it.remove()
                }
            }

            // 2) 新增 / 更新：逐个处理，原地替换保持位置
            for (m4 in scene.getMeshes()) {
                val cached = projected[m4]
                when {
                    cached == null -> {
                        val m3 = projectMesh(m4)
                        projected[m4] = m3
                        bindingSpace.meshList.add(m3)          // 新增追加到末尾
                        m4.dirty = false
                    }
                    m4.dirty || force -> {                     // ← 强制时走这里
                        val m3 = projectMesh(m4)
                        val idx = bindingSpace.meshList.indexOf(cached)
                        if (idx >= 0) {
                            bindingSpace.meshList[idx] = m3    // 原地替换，位置不变
                        } else {
                            bindingSpace.meshList.add(m3)      // 被外部误删了，补回来
                        }
                        projected[m4] = m3
                        m4.dirty = false
                    }
                    else -> {
                        if (!bindingSpace.meshList.contains(cached))
                            bindingSpace.meshList.add(cached)
                    }
                }
            }

            // 3) 强制模式下，把所有未标记 dirty 的 Mesh4D 也顺手标一遍？
            //    不需要：force 只作用于当帧，dirty 语义保持不变。
        }
    }
}