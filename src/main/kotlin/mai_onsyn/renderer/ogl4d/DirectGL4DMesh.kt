package mai_onsyn.renderer.ogl4d

import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL30.glEnableVertexAttribArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.opengl.GL30.glVertexAttribPointer
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER

/**
 * 一个 [Mesh4D] 在 GPU 上的那份资源。
 *
 * - [ssbo]: 输入, 每个四面体 36 个 float (与老路径交给 JNI 的那份数据逐字节一致)。
 * - [vbo]:  输出, compute shader 直接把变换后的顶点写进来, 布局和 `Mesh` 的顶点格式一模一样
 *           (pos3 | color4 | normal3 | uv2), 所以画的时候直接当顶点缓冲用, 中间不过 CPU。
 * - [vao]:  绑定 [vbo] 的四个顶点属性, 属性位置与 `Shader.basic` 的 basic-vertex.glsl 对齐。
 *
 * 生命周期由 [DirectGL4DEngine] 管, 只允许在渲染线程 (有 GL 上下文) 里碰。
 */
internal class DirectGL4DMesh {

    var vao = 0
        private set
    var vbo = 0
        private set
    var ssbo = 0
        private set

    /** 输入几何是否已经在 GPU 上 */
    var uploaded = false
        private set

    /** [vbo] 里当前有多少个四面体 (每个四面体 12 个顶点) */
    var tetCount = 0
        private set

    /** 顶点色里有没有非 1 的 alpha。和 `Mesh.draw` 的透明组一样: 有就关深度写 + 混合 */
    var transparent = false
        private set

    /** VBO 里总共有多少个顶点 */
    val vertexCount: Int get() = tetCount * OUT_VERTICES_PER_TET

    /**
     * 上次上传时的四面体对象引用。
     *
     * `Tetrahedron` / `Vertex4D` 的字段全是 val, 几何一旦被改动必然换成新对象,
     * 所以「引用没换」就等于「几何没变」—— 这样只改 transform 的时候不用重传顶点,
     * 而 transform 是每帧当 uniform 传上去的。
     */
    private var snapshot: Array<Tetrahedron> = emptyArray()
    private var capacityTets = 0

    /** 按当前四面体数把缓冲开够 (只在变大的时候重新分配, 缩小时保留) */
    fun ensureCapacity(tets: Int) {
        if (vao == 0) createObjects()
        if (tets <= capacityTets) return

        capacityTets = tets
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vboBytes(tets), GL_DYNAMIC_DRAW)
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, ssboBytes(tets), GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
    }

    /** 几何是不是真的变了 (对象引用比对; 别的时候只改了 transform 就不用重传) */
    fun geometryChanged(mesh: Mesh4D): Boolean {
        if (!uploaded) return true
        val tets = mesh.tetrahedrons
        if (snapshot.size != tets.size) return true
        for (i in snapshot.indices) {
            // getOrNull: 别的线程正在增删这个列表时当作几何变了, 而不是抛异常
            if (snapshot[i] !== tets.getOrNull(i)) return true
        }
        return false
    }

    /** 把 4D 顶点打包 (格式与 JNI 路径一致) 并传到 GPU */
    fun upload(mesh: Mesh4D) {
        val packed = pack(mesh)
        val tets = packed.size / IN_FLOATS_PER_TET

        if (tets == 0) {
            // 空模型: 一个字节都不用传, 也别去建 GL 对象
            tetCount = 0
            transparent = false
            snapshot = mesh.tetrahedrons.toTypedArray()
            uploaded = true
            return
        }

        ensureCapacity(tets)
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, packed)
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)

        tetCount = tets
        transparent = scanTransparent(packed)
        snapshot = mesh.tetrahedrons.toTypedArray()
        uploaded = true
    }

    fun dispose() {
        if (vao == 0) return
        glDeleteBuffers(vbo)
        glDeleteBuffers(ssbo)
        glDeleteVertexArrays(vao)
        vao = 0
        vbo = 0
        ssbo = 0
        uploaded = false
        tetCount = 0
        capacityTets = 0
        transparent = false
        snapshot = emptyArray()
    }

    private fun createObjects() {
        vao = glGenVertexArrays()
        vbo = glGenBuffers()
        ssbo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        // 与 basic-vertex.glsl 的 location 0..3 一一对应
        glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_STRIDE_BYTES, 0L)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, VERTEX_STRIDE_BYTES, 12L)
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, VERTEX_STRIDE_BYTES, 28L)
        glEnableVertexAttribArray(2)
        glVertexAttribPointer(3, 2, GL_FLOAT, false, VERTEX_STRIDE_BYTES, 40L)
        glEnableVertexAttribArray(3)
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    /** 扫一遍顶点色 (每 9 个 float 的第 5 个) 看有没有 alpha != 255 */
    private fun scanTransparent(packed: FloatArray): Boolean {
        var i = 4
        while (i < packed.size) {
            if (((packed[i].toRawBits() ushr 24) and 0xFF) != 0xFF) return true
            i += IN_FLOATS_PER_VERTEX
        }
        return false
    }

    /**
     * 打包成 compute shader 输入的那份布局: 每个顶点 9 个 float
     * `pos(x,y,z,w) | color(ARGB 的位模式当 float) | normal(x,y,z,w)`, 每个四面体 36 个。
     *
     * **格式必须和 [mai_onsyn.renderer.cpu4dkt.Tetrahedron.pack] 完全一致** —— 它是
     * Kotlin 侧与 shader/C++ 之间那份冻结的契约 (老路径靠 JNI 传同一份数据)。
     * 这里自己写一份是为了不再依赖原生库 (直接渲染这条路径本来就不需要 JNI)。
     */
    private fun pack(mesh: Mesh4D): FloatArray {
        val tets = mesh.tetrahedrons
        val out = FloatArray(tets.size * IN_FLOATS_PER_TET)
        var o = 0
        for (i in tets.indices) {
            // getOrNull: 别的线程正在改这个列表时能收多少收多少
            val t = tets.getOrNull(i) ?: break
            o = writeVertex(out, o, t.v0)
            o = writeVertex(out, o, t.v1)
            o = writeVertex(out, o, t.v2)
            o = writeVertex(out, o, t.v3)
        }
        return if (o == out.size) out else out.copyOf(o)
    }

    private fun writeVertex(dst: FloatArray, offset: Int, v: Vertex4D): Int {
        var o = offset
        dst[o++] = v.pos.x
        dst[o++] = v.pos.y
        dst[o++] = v.pos.z
        dst[o++] = v.pos.w
        dst[o++] = Float.fromBits(v.color.hex)
        dst[o++] = v.normal.x
        dst[o++] = v.normal.y
        dst[o++] = v.normal.z
        dst[o++] = v.normal.w
        return o
    }

    private fun vboBytes(tets: Int): Long =
        tets.toLong() * OUT_VERTICES_PER_TET * VERTEX_STRIDE_BYTES

    private fun ssboBytes(tets: Int): Long =
        tets.toLong() * IN_FLOATS_PER_TET * 4L

    companion object {
        /** 打包后一个四面体的 float 数: 4 个顶点 * (pos4 + color1 + normal4) */
        const val IN_FLOATS_PER_TET = 36
        /** 打包后一个顶点占的 float 数 */
        const val IN_FLOATS_PER_VERTEX = 9
        /** 一个四面体拆成 4 个三角形, 共 12 个顶点 */
        const val OUT_VERTICES_PER_TET = 12
        /** VBO 里一个顶点的 float 数, 与 Mesh 一致 */
        const val OUT_FLOATS_PER_VERTEX = 12
        /** 48 字节一个顶点 */
        private const val VERTEX_STRIDE_BYTES = OUT_FLOATS_PER_VERTEX * 4
    }
}
