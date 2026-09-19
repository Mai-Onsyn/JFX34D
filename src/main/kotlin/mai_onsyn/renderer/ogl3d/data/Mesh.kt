package mai_onsyn.renderer.ogl3d.data

import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniform4fv
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL30.*
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 三角形列表, 给"生成线程写 / 渲染线程读"用的。
 *
 * 渲染器只认 [snapshot] 发布出来的那份**不可变**数据:
 *   生成线程: add() / addAll() 往 built 里塞, 攒够 [CHUNK] 个或主动 [commit] 就发布一份;
 *   渲染线程: 只看得到已经发布的内容, 永远不会看到写一半的顶点数组。
 *
 * 所以渲染线程不用加锁也不会 ConcurrentModificationException,
 * 生成线程继续往同一个 mesh 里塞新的顶点也没问题 (老的那份已经发布出去了)。
 *
 * 两种用法都行:
 *   1. 每轮造一个新 Mesh: 建完直接丢给场景, 不用管 commit;
 *   2. 复用同一个 Mesh 持续改顶点: 改完调一次 [markDirty]。
 */
class TriangleList : AbstractMutableList<Triangle>() {
    /** 正在写的缓冲: 生成线程写, 渲染线程不碰 */
    private val built = ArrayList<Triangle>()
    /** 已发布的块; [publish] 时整块换新, 所以渲染线程读到的永远是完整的 */
    private val chunks = ArrayList<List<Triangle>>(4)
    private var chunksSize = 0
    /** 已发布数据拼成的只读视图, 每次 publish 增量往后接 */
    private val merged = ArrayList<Triangle>()

    private val published = AtomicReference<List<Triangle>>(emptyList())
    private val rev = AtomicReference(0L)

    private val lock = Any()

    /** 渲染线程读的版本号; 变了说明有新数据要重传 */
    val revision: Long get() = rev.get()

    /** 渲染线程拿到的稳定视图, 别人怎么改都不会动到它 */
    fun snapshot(): List<Triangle> = published.get()

    /** 把攒着的数据发出去; add 攒够 [CHUNK] 会自动调, 也可以手动调 */
    fun commit() = synchronized(lock) {
        if (built.isNotEmpty()) publish()
    }

    /** 这轮数据改完了 (或直接改了顶点), 让渲染器下次重传 */
    fun markDirty() {
        rev.incrementAndGet()
    }

    /** 调用前要持有 lock */
    private fun publish() {
        if (built.isEmpty()) return
        val chunk = ArrayList<Triangle>(built)
        built.clear()
        if (chunks.isEmpty() || chunks[chunks.size - 1].isNotEmpty()) {
            chunks.add(chunk)
        } else {
            chunks[chunks.size - 1] = chunk
        }
        chunksSize += chunk.size
        // 增量往后接, 每次只拷这一块, 不用把历史数据重拼一遍
        merged.addAll(chunk)
        published.set(ArrayList(merged))
        rev.incrementAndGet()
    }

    override val size: Int
        get() = synchronized(lock) { chunksSize + built.size }

    override fun get(index: Int): Triangle = synchronized(lock) {
        requireIndex(index, chunksSize + built.size)
        if (index < chunksSize) {
            var i = index
            for (c in chunks) {
                if (i < c.size) return@synchronized c[i]
                i -= c.size
            }
            throw IndexOutOfBoundsException("index=$index")
        }
        built[index - chunksSize]
    }

    override fun add(element: Triangle): Boolean = synchronized(lock) {
        built.add(element)
        if (built.size >= CHUNK) publish()
        true
    }

    override fun add(index: Int, element: Triangle) = synchronized(lock) {
        requireIndex(index, chunksSize + built.size + 1)
        if (index < chunksSize) {
            // 中间插入很少见, 把已发布的搬回缓冲统一处理, 免得块里下标算错
            pullBackPublished()
        }
        built.add(index - chunksSize, element)
        if (built.size >= CHUNK) publish()
    }

    override fun removeAt(index: Int): Triangle = synchronized(lock) {
        requireIndex(index, chunksSize + built.size)
        if (index < chunksSize) pullBackPublished()
        val removed = built.removeAt(index)
        rev.incrementAndGet()
        removed
    }

    override fun set(index: Int, element: Triangle): Triangle = synchronized(lock) {
        requireIndex(index, chunksSize + built.size)
        if (index < chunksSize) pullBackPublished()
        val old = built.set(index, element)
        rev.incrementAndGet()
        old
    }

    override fun clear(): Unit = synchronized(lock) {
        built.clear()
        chunks.clear()
        chunks.add(emptyList())
        chunksSize = 0
        merged.clear()
        published.set(emptyList())
        rev.incrementAndGet()
    }

    /** 把已发布的块全搬回 built, 之后就在 built 里改。调用前要持有 lock */
    private fun pullBackPublished() {
        if (chunksSize == 0) return
        val all = ArrayList<Triangle>(chunksSize + built.size)
        for (c in chunks) all.addAll(c)
        all.addAll(built)
        built.clear()
        built.addAll(all)
        chunks.clear()
        chunksSize = 0
        merged.clear()
    }

    private fun requireIndex(index: Int, size: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    private companion object {
        /** 一攒够这么多就自动发布一次 */
        const val CHUNK = 8192
    }
}

class Mesh(
    val triangles: TriangleList = TriangleList(),
    val transform: Transform = Transform.NONE
) {
    /** 同一材质的连续三角形: EBO 里 [start, end) 这段索引共用一个材质 */
    class TriangleGroup(
        val start: Int,
        val end: Int,
        val material: GLMaterial,
        val transparent: Boolean,
        /** 模型空间下这一组的中心, 画透明组时用来从远到近排序 */
        val centerMS: Vector3f,
    )

    class GLMeshData(
        val vboArray: FloatArray,
        val eboArray: IntArray,
        val groups: List<TriangleGroup> = emptyList(),
    )
    // ---- 下面这些只归渲染线程碰 ----
    /** 已经传到 GPU 的那版数据号; -1 表示还没传过 */
    private var uploadedRevision: Long = -1
    /** 正在用的那份三角形数据, 拿着引用防止被回收 */
    private var uploadedData: List<Triangle>? = null
    /** dispose 之后 {@link draw} / {@link upload} 直接返回 */
    private val disposed = AtomicBoolean(false)
    var vao = 0
        private set
    var vbo = 0
        private set
    var ebo = 0
        private set
    var indexCount = 0
        private set
    /** 按材质切出来的组; draw 会自己分不透明/透明两趟画, 外面不用管 */
    private var groups: List<TriangleGroup> = emptyList()

    private companion object {
        const val FLOATS_PER_VERTEX = 12
        /** 组中心的包围盒 */
        const val BIG = Float.MAX_VALUE
    }

    /** 内部构建器：把状态打包到一个类里, 避免 lambda/局部函数捕获导致的装箱。 */
    private class Builder(triangleCount: Int) {
        val map: HashMap<Vertex, Int> = HashMap(triangleCount * 3 * 2)
        val vbo: FloatArray = FloatArray(triangleCount * 3 * FLOATS_PER_VERTEX)
        val ebo: IntArray = IntArray(triangleCount * 3)
        var vboFloatIdx = 0
        var vertexCount = 0
        var eboIdx = 0

        fun add(v: Vertex) {
            val existing = map[v]
            if (existing != null) {
                ebo[eboIdx++] = existing
                return
            }
            val idx = vertexCount++
            map[v] = idx
            // pos
            vbo[vboFloatIdx++] = v.pos.x
            vbo[vboFloatIdx++] = v.pos.y
            vbo[vboFloatIdx++] = v.pos.z
            // color (RGBA float)
            vbo[vboFloatIdx++] = v.color.redF
            vbo[vboFloatIdx++] = v.color.greenF
            vbo[vboFloatIdx++] = v.color.blueF
            vbo[vboFloatIdx++] = v.color.alphaF
            // normal
            vbo[vboFloatIdx++] = v.normal.x
            vbo[vboFloatIdx++] = v.normal.y
            vbo[vboFloatIdx++] = v.normal.z
            // uv
            vbo[vboFloatIdx++] = v.uv.x
            vbo[vboFloatIdx++] = v.uv.y

            ebo[eboIdx++] = idx
        }
    }

    /** 边收集一组三角形, 边算这一组的包围盒中心 */
    private class GroupBuilder(val start: Int, val material: GLMaterial) {
        val min = Vector3f(BIG, BIG, BIG)
        val max = Vector3f(-BIG, -BIG, -BIG)

        fun expand(v: Vertex) {
            val p = v.pos
            if (p.x < min.x) min.x = p.x
            if (p.y < min.y) min.y = p.y
            if (p.z < min.z) min.z = p.z
            if (p.x > max.x) max.x = p.x
            if (p.y > max.y) max.y = p.y
            if (p.z > max.z) max.z = p.z
        }

        fun build(end: Int): TriangleGroup = TriangleGroup(
            start, end, material, material.checkTransparent(),
            Vector3f(
                (min.x + max.x) * 0.5f,
                (min.y + max.y) * 0.5f,
                (min.z + max.z) * 0.5f,
            )
        )
    }

    private fun buildGLData(snapshot: List<Triangle>): GLMeshData {
        val triCount = snapshot.size
        if (triCount == 0) {
            return GLMeshData(FloatArray(0), IntArray(0))
        }

        val b = Builder(triCount)
        val groups = ArrayList<TriangleGroup>()
        var current: GroupBuilder? = null
        // 手动展开三角形循环, 避免每次迭代创建临时对象
        for (i in 0 until triCount) {
            val t = snapshot[i]
            // 材质变化就切一组, 一组对应一次材质绑定 + 一次 drawElements
            val material = (t.texture ?: Texture.DEFAULT).gl
            if (material !== current?.material) {
                current?.let { groups.add(it.build(b.eboIdx)) }
                current = GroupBuilder(b.eboIdx, material)
            }
            current.expand(t.v0)
            current.expand(t.v1)
            current.expand(t.v2)
            b.add(t.v0)
            b.add(t.v1)
            b.add(t.v2)
        }
        current?.let { groups.add(it.build(b.eboIdx)) }

        // 如果去重后没填满, 裁剪到真实长度；否则原样返回, 避免多余拷贝
        val vbo = if (b.vboFloatIdx == b.vbo.size) b.vbo else b.vbo.copyOf(b.vboFloatIdx)
        val ebo = if (b.eboIdx == b.ebo.size) b.ebo else b.ebo.copyOf(b.eboIdx)
        return GLMeshData(vbo, ebo, groups).also {
//            println("Build GL Data, groups = ${groups.size}, transparent = ${groups.count { g -> g.transparent }}")
        }
    }

    /**
     * 把 CPU 数据传到 GPU。只能在渲染线程 (有 GL 上下文) 调。
     *
     * 靠数据版本号判断要不要重传: 生成线程改完数据后 [TriangleList.markDirty]
     * (或直接 commit) 一下, 下一帧就会自动整块重传。
     */
    fun upload() {
        if (disposed.get()) {
            // 已经释放了, 别再传回去
            uploadedRevision = Long.MAX_VALUE
            return
        }
        // 生成线程可能攒着没 commit, 这里兜一下 (commit 会 +1 revision)
        triangles.commit()
        if (triangles.revision == uploadedRevision) return

        // 先拿稳定快照, 后面整段都不再碰生成线程的数据
        val snapshot = triangles.snapshot()
        val glData = buildGLData(snapshot)

        if (uploadedRevision < 0) {
            // ---- 第一次：创建三件套 ----
            vao = glGenVertexArrays()
            vbo = glGenBuffers()
            ebo = glGenBuffers()

            glBindVertexArray(vao)

            // VBO
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, glData.vboArray, GL_STATIC_DRAW)

            // EBO
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, glData.eboArray, GL_STATIC_DRAW)

            // 属性配置（只配一次）
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 48, 0L)
            glEnableVertexAttribArray(0)
            glVertexAttribPointer(1, 4, GL_FLOAT, false, 48, 12L)
            glEnableVertexAttribArray(1)
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 48, 28L)
            glEnableVertexAttribArray(2)
            glVertexAttribPointer(3, 2, GL_FLOAT, false, 48, 40L)
            glEnableVertexAttribArray(3)

            glBindVertexArray(0)
        } else {
            // 顶点数变了, 直接整块重传
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, glData.vboArray, GL_STATIC_DRAW)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, glData.eboArray, GL_STATIC_DRAW)
        }

        uploadedData = snapshot
        uploadedRevision = triangles.revision
        indexCount = glData.eboArray.size
        groups = glData.groups
    }

    private val modelMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val materialColorBuffer = BufferUtils.createFloatBuffer(4)

    /**
     * 画这个 mesh —— 就这一个入口。
     *
     * 内部按材质组自己跑两趟, 外面不用关心:
     *   不透明组: 正常写深度, 不混合
     *   透明组:   关深度写入 + 开混合, 并按组中心从远到近排序
     * 画完恢复成"写深度 + 不混合"的干净状态。
     *
     * @param cameraPos 世界空间相机位置, 透明排序用
     */
    fun draw(modelLocation: Int, cameraPos: Vector3f) {
        if (disposed.get() || uploadedRevision < 0 || groups.isEmpty()) return

        bindMesh(modelLocation)

        val transparentCount = groups.count { it.transparent }
        if (transparentCount == 0) {
            // 绝大多数 mesh 全是实体材质, 直接一趟画完, 不碰任何状态
            for (g in groups) drawGroup(g)
            glBindVertexArray(0)
            return
        }

        // 第一趟: 实体
        glDepthMask(true)
        glDisable(GL_BLEND)
        for (g in groups) {
            if (!g.transparent) drawGroup(g)
        }

        // 第二趟: 透明, 从远到近
        glDepthMask(false)
        glEnable(GL_BLEND)
        for (g in groups.asSequence().filter { it.transparent }
            .sortedByDescending { it.centerMS.distanceSquared(cameraPos) }) {
            drawGroup(g)
        }

        // 还原成"干净"状态: 写深度 + 不混合 (透明组才临时开混合)
        // 不查 GL 拿旧状态, 免得每帧同步一次
        glDepthMask(true)
        glDisable(GL_BLEND)

        glBindVertexArray(0)
    }

    private fun bindMesh(modelLocation: Int) {
        // 设这个模型自己的变换
        transform.matrix.get(modelMatrixBuffer)
        glUniformMatrix4fv(modelLocation, false, modelMatrixBuffer)
        // bind 自己的 VAO（三件套一起恢复）
        glBindVertexArray(vao)
    }

    private fun drawGroup(g: TriangleGroup) {
        val m = g.material
        m.upload()
        m.bindTextures()
        bindMaterial(m)

        glDrawElements(
            GL_TRIANGLES,
            g.end - g.start,
            GL_UNSIGNED_INT,
            (g.start.toLong() * 4L)   // EBO 是 uint, 偏移要按字节
        )
    }

    /** 材质参数走 uniform; 贴图本身已经传过了, 这里只管数值和开关 */
    private fun bindMaterial(m: GLMaterial) {
        val tex = m.texture
        glUniform3f(GLMaterial.uKa, tex.ka.redF, tex.ka.greenF, tex.ka.blueF)
        glUniform4fv(GLMaterial.uKd, colorBuffer(tex.kd, 1f))
        glUniform3f(GLMaterial.uKs, tex.ks.redF, tex.ks.greenF, tex.ks.blueF)
        glUniform1f(GLMaterial.uNs, tex.ns)
        glUniform1f(GLMaterial.uD, if (tex.d > 0f) tex.d else 1f)

        // 没有主贴图就走顶点色
        glUniform1i(GLMaterial.uUseTexture, if (m.mapKdId != 0) 1 else 0)
    }

    private fun colorBuffer(c: ColorARGB, alpha: Float): FloatBuffer {
        materialColorBuffer.clear()
        materialColorBuffer.put(c.redF).put(c.greenF).put(c.blueF).put(alpha)
        materialColorBuffer.flip()
        return materialColorBuffer
    }

    /**
     * 释放 GPU 资源。只能在渲染线程 (有 GL 上下文) 调。
     * 外部线程删 mesh 请走 [SimpleScene3D.removeMesh], 由渲染线程代劳。
     * 重复调用是安全的。
     */
    fun dispose() {
        if (!disposed.compareAndSet(false, true)) return
        if (uploadedRevision >= 0) {
            glDeleteBuffers(vbo)
            glDeleteBuffers(ebo)
            glDeleteVertexArrays(vao)
            uploadedRevision = -1
        }
        // 贴图是共享的 (Texture.gl), 这里只清掉本 mesh 的 buffer
        uploadedData = null
        groups = emptyList()
    }
}

fun AtomicReference<Long>.incrementAndGet(): Long = this.getAndSet(this.get() + 1) + 1