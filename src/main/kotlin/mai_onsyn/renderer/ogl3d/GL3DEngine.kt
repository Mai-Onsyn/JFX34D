package mai_onsyn.renderer.ogl3d

import com.huskerdev.openglfx.canvas.events.GLInitializeEvent
import com.huskerdev.openglfx.canvas.events.GLRenderEvent
import com.huskerdev.openglfx.canvas.events.GLReshapeEvent
import mai_onsyn.renderer.ogl3d.data.GLMaterial
import mai_onsyn.renderer.ogl3d.data.Scene3D
import mai_onsyn.renderer.ogl3d.data.Shader
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import mai_onsyn.renderer.utils.FrequencyCounter
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
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
    private val lightPos = Vector3f(-5f, 15f, 0f)
    private val lightColor = Vector3f(1.0f, 1.0f, 1.0f)
    private var lightIntensity = 1.4f
    private var lightRange = 600.0f
    private var attnA = 0.00007f
    private var attnB = 0.00003f
    /** 世界空间的相机位置 = -view 矩阵的平移列 */
    private val viewPos = Vector3f()

    private var aspect = 1.0f

    fun init(event: GLInitializeEvent) {
        val program = Shader.basic.program
        glUseProgram(program)
        glClearColor(0.5294f, 0.8078f, 0.9216f, 1.0f)

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
        // 当前像素按自己的 alpha 覆盖, 剩下的显示后面的像素: rgb = src*a + dst*(1-a)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        // 混合只在有透明材质时由 Mesh.draw 临时打开; 这里定好默认状态
        glDepthMask(true)
        glDisable(GL_BLEND)
    }

    fun reshape(event: GLReshapeEvent) {
        aspect = event.width.toFloat() / event.height.toFloat()
        glViewport(0, 0, event.width, event.height)
    }

    private val viewMatrixBuffer = BufferUtils.createFloatBuffer(16)
    private val projectionMatrixBuffer = BufferUtils.createFloatBuffer(16)
    fun render(event: GLRenderEvent) {
        val program = Shader.basic.program
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // 外部线程删掉的 mesh, 到这里才真正 glDelete (GL 只能在渲染线程调)
        (scene as? SimpleScene3D)?.drainPendingDispose()

        // getMeshes() 拿到的是当前快照, 别的线程同时增删也不影响这次遍历
        val meshes = scene.getMeshes()
        for (m in meshes) {
            m.upload()
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

//        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        // Mesh.draw 自己处理深度写入/混合 (透明材质分两趟), 外面只管调
        for (m in meshes) {
            m.draw(modelPtr, viewPos)
        }

        fpsCounter.tick()
    }
}
