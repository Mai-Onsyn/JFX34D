package mai_onsyn.renderer.ogl3d

import com.huskerdev.openglfx.canvas.events.GLInitializeEvent
import com.huskerdev.openglfx.canvas.events.GLRenderEvent
import com.huskerdev.openglfx.canvas.events.GLReshapeEvent
import javafx.scene.paint.Color
import mai_onsyn.renderer.ogl3d.data.GLMaterial
import mai_onsyn.renderer.ogl3d.data.Light
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.data.MeshSourceType
import mai_onsyn.renderer.ogl3d.data.Scene3D
import mai_onsyn.renderer.ogl3d.data.Shader
import mai_onsyn.renderer.utils.FrequencyCounter
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.glBlendFuncSeparate
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniform3fv
import org.lwjgl.opengl.GL20.glUniform4fv
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL20.glUseProgram

class GL3DEngine(
    var scene: Scene3D
) {
    val fpsCounter = FrequencyCounter()

    private var modelPtr = 0
    private var viewPtr = 0
    private var projectionPtr = 0
    private var viewPosPtr = 0
    private var ambientPtr = 0
    private var lightingPtr = 0
    private var lightCountPtr = 0
    private var lightPosPtr = 0
    private var lightColorPtr = 0
    private var lightDirPtr = 0
    private var lightParamPtr = 0

    /** 世界空间的相机位置 = -view 矩阵的平移列 */
    private val viewPos = Vector3f()

    private var aspect = 1.0f
    var useOutlineRendering: Boolean = false
    var bgColor: Color = Color(0.5294, 0.8078, 0.9216, 1.0)

    /**
     * 光照总开关。false = 材质原样输出, 光源和环境光都不参与, 但光源列表原封不动留着,
     * 打开就立刻恢复。
     */
    @Volatile
    var lightingEnabled: Boolean = true

    private companion object {
        /**
         * shader 里光源数组的长度, 必须和 basic-fragment.glsl 的 MAX_LIGHTS 一致。
         * 场景里多出来的光源会被丢掉。
         */
        const val MAX_LIGHTS = 16
    }

    // 每帧要提交的光源数据, 预分配好避免每帧建对象
    private val lightPosBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 3)
    private val lightColorBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 3)
    private val lightDirBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 3)
    /** 每个光源 4 个: intensity / range / a / b */
    private val lightParamBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * 4)

    fun init(event: GLInitializeEvent) {
        val program = Shader.basic.program
        glUseProgram(program)
//        glClearColor(0.5294f, 0.8078f, 0.9216f, 1.0f)
//        glClearColor(0.1215686f, 0.12549019f, 0.13333333f, 1.0f)

        modelPtr = glGetUniformLocation(program, "model")
        viewPtr = glGetUniformLocation(program, "view")
        projectionPtr = glGetUniformLocation(program, "projection")
        // phone 光照需要用到的
        viewPosPtr = glGetUniformLocation(program, "viewPos")
        ambientPtr = glGetUniformLocation(program, "ambient")
        lightingPtr = glGetUniformLocation(program, "uLighting")
        lightCountPtr = glGetUniformLocation(program, "lightCount")
        lightPosPtr = glGetUniformLocation(program, "lightPositions")
        lightColorPtr = glGetUniformLocation(program, "lightColors")
        lightDirPtr = glGetUniformLocation(program, "lightDirs")
        lightParamPtr = glGetUniformLocation(program, "lightParams")

        // 材质 uniform 位置, 只取一次
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
        // 混合全局开着, 别再按材质临时切: 切来切去容易和别处的状态打架。
        //   rgb:   src*a + dst*(1-a)   普通的按 alpha 覆盖
        //   alpha: dst*(1-a) + src     这一项很关键 —— 这块纹理是要交给 JavaFX 合成的,
        //          用它 alpha 只会朝 1 靠, 不会因为透明面重叠而累加/饱和。
        //          (之前用 GL_ONE, GL_ONE 累加, 重叠面一多 alpha 就飘, 画面会闪)
        glEnable(GL_BLEND)
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
    }

    fun reshape(event: GLReshapeEvent) {
        aspect = event.width.toFloat() / event.height.toFloat()
        glViewport(0, 0, event.width, event.height)
    }

    private val viewMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val projectionMatrixBuffer = BufferUtils.createFloatBuffer(16)

    private val uploadedMeshes = mutableListOf<Mesh>()
    fun render(event: GLRenderEvent) {
        val program = Shader.basic.program
        glClearColor(bgColor.red.toFloat(), bgColor.green.toFloat(), bgColor.blue.toFloat(), bgColor.opacity.toFloat())
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // getMeshes() 是并发安全列表, 别的线程同时增删也不影响这次遍历
        val meshes = scene.getMeshes()
        for (m in meshes) {
            m.upload()
            uploadedMeshes.add(m)
        }
        glUseProgram(program)
        // 光照开关: 每帧提交一次, 外部改了立刻生效
        glUniform1i(lightingPtr, if (lightingEnabled) 1 else 0)

        val view = scene.getCamera().viewMatrix
        view.get(viewMatrixBuffer)
        scene.getCamera().projectionMatrix(aspect).get(projectionMatrixBuffer)
        glUniformMatrix4fv(viewPtr, false, viewMatrixBuffer)
        glUniformMatrix4fv(projectionPtr, false, projectionMatrixBuffer)

        // 相机 = 世界空间的相机位置, 传给 fragment 算 phone
        //
        // view 矩阵的行是相机的基向量 (vx/vy/vz), col3 存的是 -(基向量·相机位置),
        // 所以要把 -col3 沿三个基向量摊回去才是相机位置。
        // 直接取 -col3 只有在相机正好朝 +z 时才对 (那时基向量就是世界轴):
        //     pos = (-m30)*vx + (-m31)*vy + (-m32)*vz
        val c0 = -view.m30()
        val c1 = -view.m31()
        val c2 = -view.m32()
        viewPos.set(
            view.m00() * c0 + view.m01() * c1 + view.m02() * c2,   // vx
            view.m10() * c0 + view.m11() * c1 + view.m12() * c2,   // vy
            view.m20() * c0 + view.m21() * c1 + view.m22() * c2,   // vz
        )
        glUniform3f(viewPosPtr, viewPos.x, viewPos.y, viewPos.z)

        // 光照: 每帧从场景取一次快照提交给 uniform 数组
        uploadLights(scene.getLights())

        // Mesh.draw 自己按材质组跑两趟 (实体先, 透明后), 外面只管调
        for (m in meshes) {
            when (m.type) {
                MeshSourceType.D3 -> {
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
                }
                MeshSourceType.D4 -> {
                    if (useOutlineRendering) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                    else glPolygonMode(GL_FRONT, GL_FILL)
                }
            }
            m.draw(modelPtr, viewPos)
        }

        val uploadedButRemoved = uploadedMeshes.filter { !meshes.contains(it) }
        uploadedButRemoved.forEach {
            it.dispose()
            uploadedMeshes.remove(it)
        }

        fpsCounter.tick()
    }

    /**
     * 把场景里的光源打包成 uniform 数组。
     *
     * 环境光来自场景 (scene.getAmbient()), 不是光源的属性;
     * 点光源的方向写成 0 向量 (shader 里 0 向量 → 方向衰减恒为 1);
     * 面光源的方向直接写它的朝向; 颜色是 ColorARGB, 这里转成 0..1 的浮点。
     */
    private fun uploadLights(lights: List<Light>) {
        val count = minOf(lights.size, MAX_LIGHTS)

        lightPosBuffer.clear()
        lightColorBuffer.clear()
        lightDirBuffer.clear()
        lightParamBuffer.clear()

        for (i in 0 until count) {
            // 列表是并发安全的, 但外部可能正在增删, 序号访问会越界
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

        // 用实际填进去的光源数, 别用上面算的 count
        val uploaded = lightPosBuffer.position() / 3
        lightPosBuffer.flip()
        lightColorBuffer.flip()
        lightDirBuffer.flip()
        lightParamBuffer.flip()

        val ambient = scene.getAmbient()
        glUniform3f(ambientPtr, ambient.redF, ambient.greenF, ambient.blueF)
        glUniform1i(lightCountPtr, uploaded)
        if (uploaded > 0) {
            glUniform3fv(lightPosPtr, lightPosBuffer)
            glUniform3fv(lightColorPtr, lightColorBuffer)
            glUniform3fv(lightDirPtr, lightDirBuffer)
            glUniform4fv(lightParamPtr, lightParamBuffer)
        }
    }
}
