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
        val opaqueGroups: List<TriangleGroup> = emptyList(),
        val transparentGroups: List<TriangleGroup> = emptyList(),
    )
    private var dirty: Boolean = true
    private var glData: GLMeshData? = null
    var vao = 0
        private set
    var vbo = 0
        private set
    var ebo = 0
        private set
    var indexCount = 0
        private set
    /** 不透明组: 正常写深度, 不用混合 */
    var opaqueGroups: List<TriangleGroup> = emptyList()
        private set
    /** 透明组: 关掉深度写入 + 开混合, 从远到近画 */
    var transparentGroups: List<TriangleGroup> = emptyList()
        private set

    fun createGLData(): GLMeshData {
        if (dirty) {
            glData = GLMeshData(floatArrayOf(), intArrayOf())
        }
        return glData!!
    }

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

        // 透明和不透明分成两批, 渲染时分开处理
        val opaque = ArrayList<TriangleGroup>()
        val transparent = ArrayList<TriangleGroup>()
        for (g in groups) {
            if (g.transparent) transparent.add(g) else opaque.add(g)
        }

        // 如果去重后没填满, 裁剪到真实长度；否则原样返回, 避免多余拷贝
        val vbo = if (b.vboFloatIdx == b.vbo.size) b.vbo else b.vbo.copyOf(b.vboFloatIdx)
        val ebo = if (b.eboIdx == b.ebo.size) b.ebo else b.ebo.copyOf(b.eboIdx)
        return GLMeshData(vbo, ebo, opaque, transparent).also {
            println("Build GL Data, opaque = ${opaque.size}, transparent = ${transparent.size}")
        }
    }

    private var uploaded = false
    fun upload() {
        if (!uploaded) {
            val glData = buildGLData()
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
            uploaded = true
            indexCount = glData.eboArray.size
            opaqueGroups = glData.opaqueGroups
            transparentGroups = glData.transparentGroups

        } else if (dirty) {
            val glData = buildGLData()
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, glData.vboArray, GL_STATIC_DRAW)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, glData.eboArray, GL_STATIC_DRAW)
            indexCount = glData.eboArray.size
            opaqueGroups = glData.opaqueGroups
            transparentGroups = glData.transparentGroups
        }
        dirty = false
    }

    private val modelMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val materialColorBuffer = BufferUtils.createFloatBuffer(4)

    /** 画不透明组 (调用者负责开深度写入, 关混合) */
    fun drawOpaque(modelLocation: Int) {
        if (!uploaded || opaqueGroups.isEmpty()) return
        bindMesh(modelLocation)
        for (g in opaqueGroups) drawGroup(g)
        glBindVertexArray(0)
    }

    /** 画透明组: 按材质组中心从远到近排序, 从远往近画才能正确叠在后面像素上 */
    fun drawTransparent(modelLocation: Int, cameraPos: Vector3f) {
        if (!uploaded || transparentGroups.isEmpty()) return
        bindMesh(modelLocation)
        transparentGroups.sortedByDescending { it.centerMS.distanceSquared(cameraPos) }
            .forEach { drawGroup(it) }
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

    fun dispose() {
        if (uploaded) {
            glDeleteBuffers(vbo)
            glDeleteBuffers(ebo)
            glDeleteVertexArrays(vao)
            uploaded = false
        }
        // 贴图是共享的 (Texture.gl), 这里只清掉本 mesh 的 buffer
        opaqueGroups = emptyList()
        transparentGroups = emptyList()
    }
}
