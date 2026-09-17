package mai_onsyn.renderer.data

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL30.*

class Mesh(
    val triangles: MutableList<Triangle> = mutableListOf(),
    val transform: Transform = Transform.NONE
) {
    class GLMeshData(
        val vboArray: FloatArray,
        val eboArray: IntArray,
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

    fun createGLData(): GLMeshData {
        if (dirty) {
            glData = GLMeshData(floatArrayOf(), intArrayOf())
        }
        return glData!!
    }

    private companion object {
        const val FLOATS_PER_VERTEX = 12
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

    private fun buildGLData(): GLMeshData {
        val triCount = triangles.size
        if (triCount == 0) {
            return GLMeshData(FloatArray(0), IntArray(0))
        }

        val b = Builder(triCount)
        // 手动展开三角形循环, 避免每次迭代创建临时对象
        for (i in 0 until triCount) {
            val t = triangles[i]
            b.add(t.v0)
            b.add(t.v1)
            b.add(t.v2)
        }

        // 如果去重后没填满, 裁剪到真实长度；否则原样返回, 避免多余拷贝
        val vbo = if (b.vboFloatIdx == b.vbo.size) b.vbo else b.vbo.copyOf(b.vboFloatIdx)
        val ebo = if (b.eboIdx == b.ebo.size) b.ebo else b.ebo.copyOf(b.eboIdx)
        return GLMeshData(vbo, ebo).also { println("Build GL Data") }
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

        } else if (dirty) {
            val glData = buildGLData()
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, glData.vboArray, GL_STATIC_DRAW)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, glData.eboArray, GL_STATIC_DRAW)
            indexCount = glData.eboArray.size
        }
        dirty = false
    }

    private val modelMatrixBuffer = BufferUtils.createFloatBuffer(16)
    fun draw(modelLocation: Int) {
        if (!uploaded) return

        // 设这个模型自己的变换
        transform.matrix.get(modelMatrixBuffer)
        glUniformMatrix4fv(modelLocation, false, modelMatrixBuffer)

        // bind 自己的 VAO（三件套一起恢复）
        glBindVertexArray(vao)

        // draw
        glDrawElements(
            GL_TRIANGLES,
            indexCount,
            GL_UNSIGNED_INT,
            0L
        )

        glBindVertexArray(0)
    }

    fun dispose() {
        if (uploaded) {
            glDeleteBuffers(vbo)
            glDeleteBuffers(ebo)
            glDeleteVertexArrays(vao)
            uploaded = false
        }
    }
}