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

class Mesh(
    /**
     * 顶点数据。外部线程往里面塞, 塞完把 [dirty] 置 true 通知渲染器。
     * 只加不改的话不用担心并发: 列表结构建好之后渲染器只是读。
     */
    val triangles: MutableList<Triangle> = mutableListOf(),
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

    /**
     * 数据变了, 下次 upload 重传。
     * 外部线程改完 triangles 之后置 true 即可, 初值 true 保证第一次会上传。
     */
    @Volatile
    var dirty: Boolean = true
    /** 已经传到 GPU 的三角形数; -1 表示还没传过 */
    private var uploadedCount: Int = -1
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

    private fun buildGLData(): GLMeshData {
        val triCount = triangles.size
        if (triCount == 0) {
            return GLMeshData(FloatArray(0), IntArray(0))
        }

        val b = Builder(triCount)
        val groups = ArrayList<TriangleGroup>()
        var current: GroupBuilder? = null
        // 手动展开三角形循环, 避免每次迭代创建临时对象
        for (i in 0 until triCount) {
            val t = triangles[i]
            // 材质变化就切一组, 一组对应一次材质绑定 + 一次 drawElements
            val material = (t.texture ?: Texture.DEFAULT).gl
            if (material !== current?.material) {
                current?.let { groups.add(it.build(b.eboIdx)) }
                current = GroupBuilder(b.eboIdx, material)
            }
            current!!.expand(t.v0)
            current!!.expand(t.v1)
            current!!.expand(t.v2)
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
     * 数据变了就重传。只能在渲染线程 (有 GL 上下文) 调。
     * 检查 [dirty] 标记, 没变就直接返回, 所以每帧调没有开销。
     */
    fun upload() {
        if (!dirty) return
        // 顶点数变了也整块重传, 所以不用管是新加还是全换
        val glData = buildGLData()

        if (uploadedCount < 0) {
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

        uploadedCount = triangles.size
        indexCount = glData.eboArray.size
        groups = glData.groups
        dirty = false
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
        if (dirty || uploadedCount < 0 || groups.isEmpty()) return

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
     * 释放本 mesh 的 GPU 资源 (VAO/VBO/EBO)。
     * 必须在渲染线程调 (要有 GL 上下文)。调用方自己负责什么时候调。
     */
    fun dispose() {
        if (uploadedCount < 0) return
        glDeleteBuffers(vbo)
        glDeleteBuffers(ebo)
        glDeleteVertexArrays(vao)
        uploadedCount = -1
        // 贴图是共享的 (Texture.gl), 这里只清掉本 mesh 的 buffer
        groups = emptyList()
    }
}
