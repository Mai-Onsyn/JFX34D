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
                Thread.sleep(10)
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
            val alive = IdentityHashMap<Mesh4D, Boolean>()
            for (m4 in scene.getMeshes()) alive[m4] = true

            val it = projected.entries.iterator()
            while (it.hasNext()) {
                val (m4, m3) = it.next()
                if (!alive.containsKey(m4)) {
                    bindingSpace.meshList.remove(m3)
                    it.remove()
                }
            }

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
        }
    }
}