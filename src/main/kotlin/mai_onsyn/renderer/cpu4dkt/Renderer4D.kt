package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.data.MeshSourceType
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import mai_onsyn.renderer.ogl3d.data.toMesh
import mai_onsyn.renderer.utils.FrequencyCounter
import mai_onsyn.renderer.utils.dynamicFrame
import mai_onsyn.renderer.utils.fixedFrame
import mai_onsyn.renderer.utils.toRowMajorFloatArray
import org.joml.Matrix4f
import java.util.*
import kotlin.concurrent.thread

class Renderer4D(
    val scene: SimpleScene4D,
    val bindingSpace: SimpleScene3D
) {
    private val lock = Any()
    private val projected = IdentityHashMap<Mesh4D, Mesh>()

    var maxFPS: Int = 100

    val fpsCounter: FrequencyCounter = FrequencyCounter()

//    init {
//        Thread.ofVirtual().start {
//            while (!Thread.interrupted()) {
//                render(true)
//                Thread.sleep(10)
//            }
//        }
//    }

    private var thread: Thread? = null

    fun start() {
        thread = thread(isDaemon = true) {
            dynamicFrame(
                fps = { maxFPS },
                condition = { !Thread.currentThread().isInterrupted }
            ) {
                render(true)
                fpsCounter.tick()
            }
        }
    }

    fun stop() {
        thread?.interrupt()
    }

    private fun projectMesh(mesh: Mesh4D): Mesh {
        val flattened = JNIRasterizer.packMesh4D(mesh)
        val transform = Transform4D()
        transform.scale(2f)
        val outArray = JNIRasterizer.project(
            flattened,
            mesh.tetrahedrons.size,
            transform.matrix.data,
            scene.getCamera().viewMatrix.data,
            scene.getCamera().projectionMatrix().data,
            Matrix4f().scale(5f).toRowMajorFloatArray()
        )
        val tetrahedrons = JNIRasterizer.extractTetrahedrons(outArray)
        return tetrahedrons.toMesh().apply { this.type = MeshSourceType.D4 }
    }

    fun render(force: Boolean = false) {
        synchronized(lock) {
            val currentMeshes = scene.getMeshes()
            val alive = IdentityHashMap<Mesh4D, Boolean>(currentMeshes.size)
            for (m4 in currentMeshes) alive[m4] = true

            // 1. 先处理当前场景内的所有 Mesh4D（更新或新增）
            for (m4 in currentMeshes) {
                val cachedM3 = projected[m4]

                if (cachedM3 == null) {
                    // 【新增模型】：投影并追加到末尾
                    val newM3 = projectMesh(m4)
                    projected[m4] = newM3
                    m4.dirty = false
                    bindingSpace.meshList.add(newM3)
                } else if (m4.dirty || force) {
                    // 【需更新模型】：重新投影，并在 meshList 中直接原位替换（不经过 remove，无闪烁中间态）
                    val newM3 = projectMesh(m4)
                    projected[m4] = newM3
                    m4.dirty = false

                    val idx = bindingSpace.meshList.indexOf(cachedM3)
                    if (idx >= 0) {
                        bindingSpace.meshList[idx] = newM3 // 👈 原位原子替换：渲染线程读到的要么是旧 m3，要么是新 m3，绝不会是 null 或缺失
                    } else {
                        bindingSpace.meshList.add(newM3)   // 防御性补回
                    }
                } else {
                    // 【无变化模型】：若因异常不在列表中，则补回
                    if (!bindingSpace.meshList.contains(cachedM3)) {
                        bindingSpace.meshList.add(cachedM3)
                    }
                }
            }

            // 2. 清理已被销毁/移除的 Mesh4D（只有真正死亡的模型才删）
            val it = projected.entries.iterator()
            while (it.hasNext()) {
                val (m4, m3) = it.next()
                if (!alive.containsKey(m4)) {
                    bindingSpace.meshList.remove(m3) // 仅当模型从 Scene 中销毁时剔除
                    it.remove()
                }
            }
        }
    }
}