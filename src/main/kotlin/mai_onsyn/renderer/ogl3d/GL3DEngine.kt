package mai_onsyn.renderer.ogl3d

import com.huskerdev.openglfx.canvas.events.GLInitializeEvent
import com.huskerdev.openglfx.canvas.events.GLRenderEvent
import com.huskerdev.openglfx.canvas.events.GLReshapeEvent
import javafx.scene.paint.Color
import mai_onsyn.renderer.ogl3d.data.GLMaterial
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
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform3f
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
    private var lightPosPtr = 0
    private var lightColorPtr = 0
    private var lightIntensityPtr = 0
    private var lightRangePtr = 0
    private var attnAPtr = 0
    private var attnBPtr = 0

    /** 全局环境光 */
    private val ambient = Vector3f(0.3f, 0.3f, 0.3f)
    /** 灯光先写死 (世界空间), 之后要做多光源/可调时改这里 */
    private val lightPos = Vector3f(-5f, 35f, 0f)
    private val lightColor = Vector3f(1.0f, 1.0f, 1.0f)
    private var lightIntensity = 0.6f
    private var lightRange = 600.0f
    private var attnA = 0.00007f
    private var attnB = 0.00003f
    /** 世界空间的相机位置 = -view 矩阵的平移列 */
    private val viewPos = Vector3f()

    private var aspect = 1.0f
    var useOutlineRendering: Boolean = false
    var bgColor: Color = Color(0.5294, 0.8078, 0.9216, 1.0)

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
        lightPosPtr = glGetUniformLocation(program, "lightPos")
        lightColorPtr = glGetUniformLocation(program, "lightColor")
        lightIntensityPtr = glGetUniformLocation(program, "lightIntensity")
        lightRangePtr = glGetUniformLocation(program, "lightRange")
        attnAPtr = glGetUniformLocation(program, "attnA")
        attnBPtr = glGetUniformLocation(program, "attnB")

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
        glUniform3f(ambientPtr, ambient.x, ambient.y, ambient.z)
        glUniform3f(lightPosPtr, lightPos.x, lightPos.y, lightPos.z)
        glUniform3f(lightColorPtr, lightColor.x, lightColor.y, lightColor.z)
        glUniform1f(lightIntensityPtr, lightIntensity)
        glUniform1f(lightRangePtr, lightRange)
        glUniform1f(attnAPtr, attnA)
        glUniform1f(attnBPtr, attnB)

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
}
