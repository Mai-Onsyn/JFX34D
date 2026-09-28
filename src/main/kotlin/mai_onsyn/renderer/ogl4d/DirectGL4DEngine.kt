package mai_onsyn.renderer.ogl4d

import com.huskerdev.openglfx.canvas.events.GLDisposeEvent
import com.huskerdev.openglfx.canvas.events.GLInitializeEvent
import com.huskerdev.openglfx.canvas.events.GLRenderEvent
import com.huskerdev.openglfx.canvas.events.GLReshapeEvent
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.ogl3d.data.GLMaterial
import mai_onsyn.renderer.ogl3d.data.Light
import mai_onsyn.renderer.ogl3d.data.Scene3D
import mai_onsyn.renderer.ogl3d.data.Shader
import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.utils.FrequencyCounter
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_FILL
import org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL11.GL_LINE
import org.lwjgl.opengl.GL11.GL_ONE
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glGetString
import org.lwjgl.opengl.GL11.glPolygonMode
import org.lwjgl.opengl.GL14.glBlendFuncSeparate
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform1fv
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniform3fv
import org.lwjgl.opengl.GL20.glUniform4fv
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
import org.lwjgl.opengl.GL42.glMemoryBarrier
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.opengl.GL43.glDispatchCompute
import java.util.IdentityHashMap

/**
 * 直接用 compute shader 变换 4D 顶点、并把结果写进 GPU 的 VBO 的渲染引擎。
 *
 * 和老路径 ([mai_onsyn.renderer.cpu4dkt.Renderer4D] + JNI) 的关系:
 *   - 输入完全一样: 同一个 [Mesh4D] 列表, 同一份 `packMesh4D` 打包格式, 同一个 mvp。
 *   - 变换从「CPU 串行 + JNI 回调 + 重建 Mesh」变成「GPU 并行, 一个四面体一个 invocation」。
 *   - 结果不再回 CPU: compute shader 直接把 12 个顶点写进 VBO, 画的时候直接当顶点缓冲用。
 *   - 画面语义与老路径严格对齐: 屏幕空间面法线 (与 toMesh() 的 calcNormal 同式)、uv=0、
 *     顶点色原样、兜底材质、沿用同一个 basic shader 的光照/材质/透明规则。
 *
 * 线程与顺序: 所有方法都必须在 GL 渲染线程里调 (由 [mai_onsyn.renderer.core.GL4DRegion] 挂在 openglfx 的
 * init / reshape / render 事件上)。本引擎不清屏、也不碰 3D 场景的绘制, 它假定 3D 引擎
 * ([mai_onsyn.renderer.ogl3d.GL3DEngine]) 已经在同一帧里画完, 所以本引擎必须在它之后运行。
 */
class DirectGL4DEngine(
    private val scene4D: SimpleScene4D,
    private val scene3D: Scene3D
) {

    /** 4D 侧 (compute + 上传) 的频率; 不是画布帧率 */
    val fpsCounter = FrequencyCounter()

    /**
     * compute 的帧率上限, <= 0 表示不限制。
     * 和 [mai_onsyn.renderer.cpu4dkt.Renderer4D.maxFPS] 同一个含义: 到点了才重算一次变换,
     * 中间那些画布帧直接用上次算好的 VBO 画。
     */
    var maxFPS: Int = 10000

    /** 与 Renderer4D.viewPortLength 同一个含义: viewPort = scale(viewPortLength * 0.5) */
    var viewPortLength: Float = 8f

    /**
     * 4D 网格是否画线框。
     *
     * 注意: GL3DRegion.setOutlineRendering 是 final 的, 而且只改得到 3D 引擎,
     * 所以 4D 这条线框开关是本引擎自己的, 由 DirectGL4DRegion.set4DOutlineRendering 转发过来。
     */
    var useOutlineRendering: Boolean = false

    /** 当前上下文能不能跑 compute (需要 OpenGL 4.3+); false 时整个引擎空转 */
    var isSupported: Boolean = false
        private set

    private val gpuMeshes = IdentityHashMap<Mesh4D, DirectGL4DMesh>()

    private var computeProgram = 0
    private var uMvpPtr = -1
    private var uViewScalePtr = -1
    private var uTetCountPtr = -1

    private var modelPtr = -1
    private var viewPtr = -1
    private var projectionPtr = -1
    private var viewPosPtr = -1
    private var ambientPtr = -1
    private var lightCountPtr = -1
    private var lightPosPtr = -1
    private var lightColorPtr = -1
    private var lightDirPtr = -1
    private var lightParamPtr = -1

    private var aspect = 1.0f
    private var lastComputeNanos = 0L

    /** 世界空间的相机位置 = -view 矩阵的平移列摊回基向量 (和 GL3DEngine 一致) */
    private val viewPos = Vector3f()

    private val mvpArray = FloatArray(25)
    private val viewProjArray = FloatArray(25)

    private val viewMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val projectionMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val modelMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val materialColorBuffer = BufferUtils.createFloatBuffer(4)
    private val identityModel = Matrix4f()

    // 每帧要提交的光源数据, 预分配好避免每帧建对象
    private val lightPosBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 3)
    private val lightColorBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 3)
    private val lightDirBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 3)
    /** 每个光源 4 个: intensity / range / a / b */
    private val lightParamBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 4)

    private companion object {
        /** 必须和 basic-fragment.glsl 的 MAX_LIGHTS 一致 */
        const val MAX_LIGHTS = 16
        /** 必须和 direct4d-transform.comp 的 local_size_x 一致 */
        const val LOCAL_SIZE_X = 64
    }

    fun init(event: GLInitializeEvent) {
        // ---- 基础 shader 的 uniform 位置 / GL 状态 ----
        // 和 GL3DEngine.init 做的是同一件事 (同一个 program), 谁先生效都幂等。
        val program = Shader.basic.program
        glUseProgram(program)
        modelPtr = glGetUniformLocation(program, "model")
        viewPtr = glGetUniformLocation(program, "view")
        projectionPtr = glGetUniformLocation(program, "projection")
        viewPosPtr = glGetUniformLocation(program, "viewPos")
        ambientPtr = glGetUniformLocation(program, "ambient")
        lightCountPtr = glGetUniformLocation(program, "lightCount")
        lightPosPtr = glGetUniformLocation(program, "lightPositions")
        lightColorPtr = glGetUniformLocation(program, "lightColors")
        lightDirPtr = glGetUniformLocation(program, "lightDirs")
        lightParamPtr = glGetUniformLocation(program, "lightParams")

        GLMaterial.uKa = glGetUniformLocation(program, "material.ka")
        GLMaterial.uKd = glGetUniformLocation(program, "material.kd")
        GLMaterial.uKs = glGetUniformLocation(program, "material.ks")
        GLMaterial.uNs = glGetUniformLocation(program, "material.ns")
        GLMaterial.uD = glGetUniformLocation(program, "material.d")
        GLMaterial.uUseTexture = glGetUniformLocation(program, "uUseTexture")
        GLMaterial.uMapKd = glGetUniformLocation(program, "material.mapKd")
        GLMaterial.uMapKs = glGetUniformLocation(program, "material.mapKs")
        GLMaterial.uMapD = glGetUniformLocation(program, "material.mapD")
        GLMaterial.uMapBump = glGetUniformLocation(program, "material.mapBump")

        glEnable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)

        // ---- compute ----
        val caps = GL.getCapabilities()
        if (!caps.OpenGL43) {
            System.err.println(
                "[DirectGL4D] 当前 OpenGL 上下文不支持 compute shader (需要 4.3+), " +
                        "上下文版本: ${glGetString(GL_VERSION)}, DirectGL4DEngine 停用。"
            )
            return
        }

        computeProgram = ComputeShader.direct4D.program
        // 数组 uniform 用 "uMvp" 查不到时退回第一个元素的名字
        uMvpPtr = glGetUniformLocation(computeProgram, "uMvp")
            .takeIf { it != -1 } ?: glGetUniformLocation(computeProgram, "uMvp[0]")
        uViewScalePtr = glGetUniformLocation(computeProgram, "uViewScale")
        uTetCountPtr = glGetUniformLocation(computeProgram, "uTetCount")
        isSupported = true
    }

    fun reshape(event: GLReshapeEvent) {
        aspect = event.width.toFloat() / event.height.toFloat()
    }

    fun render(event: GLRenderEvent) {
        if (!isSupported) return

        // 和老路径一样: 只处理可见的 (含父级隐藏时整棵子树一起隐藏)
        val visible = scene4D.filterVisible()

        val now = System.nanoTime()
        val interval = if (maxFPS > 0) 1_000_000_000L / maxFPS else 0L
        if (interval <= 0L || now - lastComputeNanos >= interval) {
            computePass(visible)
            lastComputeNanos = now
            fpsCounter.tick()
        }

        drawPass(visible)
        collectGarbage()
    }

    /** 释放所有 GPU 资源 (渲染线程, 上下文还在的时候调) */
    fun dispose(event: GLDisposeEvent) {
        for (gpu in gpuMeshes.values) gpu.dispose()
        gpuMeshes.clear()
    }

    // ------------------------------------------------------------------
    //  第一趟: compute, 把 4D 顶点变换成屏幕空间顶点写进各自的 VBO
    // ------------------------------------------------------------------

    private fun computePass(visible: List<Mesh4D>) {
        val camera = scene4D.getCamera()
        // 与 C++ 侧一致: viewProj = projection * view, 再乘 model
        val viewProj = camera.projectionMatrix() * camera.viewMatrix
        System.arraycopy(viewProj.data, 0, viewProjArray, 0, 25)

        glUseProgram(computeProgram)
        glUniform1f(uViewScalePtr, viewPortLength * 0.5f)

        for (m4 in visible) {
            val gpu = gpuMeshes.getOrPut(m4) { DirectGL4DMesh() }

            // dirty 只说明「有事发生」(改 transform 也会置 dirty), 所以再看一眼几何对象有没有换;
            // 数量对不上说明 VBO 布局都过期了, 必须重传。
            val liveTets = m4.tetrahedrons.size
            if (m4.dirty || !gpu.uploaded || gpu.tetCount != liveTets) {
                if (gpu.geometryChanged(m4)) gpu.upload(m4)
                m4.dirty = false
            }

            if (gpu.tetCount == 0) continue

            mul5(mvpArray, viewProjArray, m4.transform.matrix.data)
            glUniform1fv(uMvpPtr, mvpArray)
            glUniform1i(uTetCountPtr, gpu.tetCount)

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, gpu.ssbo)
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, gpu.vbo)
            glDispatchCompute((gpu.tetCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X, 1, 1)
        }

        // 让 compute 写进 VBO 的内容对后面的顶点读取可见
        glMemoryBarrier(GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT or GL_SHADER_STORAGE_BARRIER_BIT)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0)
        glUseProgram(0)
    }

    // ------------------------------------------------------------------
    //  第二趟: 用一个顶点缓冲直接画 (顶点已经在 VBO 里, 中间不过 CPU)
    // ------------------------------------------------------------------

    private fun drawPass(visible: List<Mesh4D>) {
        glUseProgram(Shader.basic.program)

        // 3D 场景的相机 / 光照 / 材质: 与 GL3DEngine 提交的内容一致,
        // 这样 4D 网格和 3D 场景里的东西处在同一套视角和光照下。
        val camera = scene3D.getCamera()
        val view = camera.viewMatrix
        view.get(viewMatrixBuffer)
        camera.projectionMatrix(aspect).get(projectionMatrixBuffer)
        glUniformMatrix4fv(viewPtr, false, viewMatrixBuffer)
        glUniformMatrix4fv(projectionPtr, false, projectionMatrixBuffer)

        val c0 = -view.m30()
        val c1 = -view.m31()
        val c2 = -view.m32()
        viewPos.set(
            view.m00() * c0 + view.m01() * c1 + view.m02() * c2,
            view.m10() * c0 + view.m11() * c1 + view.m12() * c2,
            view.m20() * c0 + view.m21() * c1 + view.m22() * c2,
        )
        glUniform3f(viewPosPtr, viewPos.x, viewPos.y, viewPos.z)

        uploadLights(scene3D.getLights())

        // 顶点已经在屏幕空间里了, 这里必须是单位阵
        identityModel.get(modelMatrixBuffer)
        glUniformMatrix4fv(modelPtr, false, modelMatrixBuffer)

        // 4D 网格只有顶点色, 用兜底材质 + 顶点色通道 (uUseTexture = 0)
        val material = Texture.DEFAULT.gl
        material.upload()
        material.bindTextures()
        bindDefaultMaterial(material)

        // 注意: 这里故意不提交 uLighting —— 它是 program 级状态, GL3DEngine 每帧开头已经
        // 按 region.enableLightRendering() 设过一次, 本趟绘制在它之后, 直接复用同一个值,
        // 于是 4D / 3D 的光照开关天然一致。

        for (m4 in visible) {
            val gpu = gpuMeshes[m4] ?: continue
            if (!gpu.uploaded || gpu.tetCount == 0) continue

            glPolygonMode(GL_FRONT_AND_BACK, if (useOutlineRendering) GL_LINE else GL_FILL)
            // 和 Mesh.draw 的单组行为一致: 顶点色里有非 1 的 alpha 就按透明组处理 (关深度写)
            glDepthMask(!gpu.transparent)

            glBindVertexArray(gpu.vao)
            glDrawArrays(GL_TRIANGLES, 0, gpu.vertexCount)
        }

        glBindVertexArray(0)
        glDepthMask(true)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        glUseProgram(0)
    }

    /** 材质参数走 uniform (与 Mesh.bindMaterial 一样, 只是这里没有贴图) */
    private fun bindDefaultMaterial(m: GLMaterial) {
        val tex = m.texture
        glUniform3f(GLMaterial.uKa, tex.ka.redF, tex.ka.greenF, tex.ka.blueF)

        materialColorBuffer.clear()
        materialColorBuffer.put(tex.kd.redF).put(tex.kd.greenF).put(tex.kd.blueF).put(1f)
        materialColorBuffer.flip()
        glUniform4fv(GLMaterial.uKd, materialColorBuffer)

        glUniform3f(GLMaterial.uKs, tex.ks.redF, tex.ks.greenF, tex.ks.blueF)
        glUniform1f(GLMaterial.uNs, tex.ns)
        glUniform1f(GLMaterial.uD, if (tex.d > 0f) tex.d else 1f)
        glUniform1i(GLMaterial.uUseTexture, 0)
    }

    /** 和 GL3DEngine.uploadLights 同一套打包方式 */
    private fun uploadLights(lights: List<Light>) {
        val count = minOf(lights.size, MAX_LIGHTS)

        lightPosBuffer.clear()
        lightColorBuffer.clear()
        lightDirBuffer.clear()
        lightParamBuffer.clear()

        for (i in 0 until count) {
            val light = lights.getOrNull(i) ?: break
            val pos = light.pos
            val color = light.color
            val dir = light.direction

            lightPosBuffer.put(pos.x).put(pos.y).put(pos.z)
            lightColorBuffer.put(color.redF).put(color.greenF).put(color.blueF)
            if (dir != null) lightDirBuffer.put(dir.x).put(dir.y).put(dir.z)
            else lightDirBuffer.put(0f).put(0f).put(0f)
            lightParamBuffer
                .put(light.intensity).put(light.range)
                .put(light.attenuationA).put(light.attenuationB)
        }

        val uploaded = lightPosBuffer.position() / 3
        lightPosBuffer.flip()
        lightColorBuffer.flip()
        lightDirBuffer.flip()
        lightParamBuffer.flip()

        val ambient = scene3D.getAmbient()
        glUniform3f(ambientPtr, ambient.redF, ambient.greenF, ambient.blueF)
        glUniform1i(lightCountPtr, uploaded)
        if (uploaded > 0) {
            glUniform3fv(lightPosPtr, lightPosBuffer)
            glUniform3fv(lightColorPtr, lightColorBuffer)
            glUniform3fv(lightDirPtr, lightDirBuffer)
            glUniform4fv(lightParamPtr, lightParamBuffer)
        }
    }

    /** 场景里已经没有的 Mesh4D, 把它的 GPU 资源删掉 */
    private fun collectGarbage() {
        if (gpuMeshes.isEmpty()) return
        val live = scene4D.meshList
        val it = gpuMeshes.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            // Mesh4D 没有重写 equals, 所以这里就是引用比较
            if (!live.contains(entry.key)) {
                entry.value.dispose()
                it.remove()
            }
        }
    }

    /** out = a * b, 5x5 行主序; 累加顺序与 Matrix5f.times 一致 */
    private fun mul5(out: FloatArray, a: FloatArray, b: FloatArray) {
        for (r in 0 until 5) {
            val ro = r * 5
            for (c in 0 until 5) {
                var sum = 0f
                for (k in 0 until 5) sum += a[ro + k] * b[k * 5 + c]
                out[ro + c] = sum
            }
        }
    }
}
