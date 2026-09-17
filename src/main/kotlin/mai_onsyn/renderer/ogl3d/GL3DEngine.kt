package mai_onsyn.renderer.ogl3d

import com.huskerdev.openglfx.canvas.events.GLInitializeEvent
import com.huskerdev.openglfx.canvas.events.GLRenderEvent
import com.huskerdev.openglfx.canvas.events.GLReshapeEvent
import mai_onsyn.renderer.data.Scene3D
import mai_onsyn.renderer.data.Shader
import org.joml.times
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.glBlendFuncSeparate
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL20.glUseProgram
import java.nio.FloatBuffer

class GL3DEngine(
    var scene: Scene3D
) {
    private var modelPtr = 0
    private var viewPtr = 0
    private var projectionPtr = 0

    private var aspect = 1.0f
    
    fun init(event: GLInitializeEvent) {
        val program = Shader.basic.program
        glUseProgram(program)
        glClearColor(0.5294f, 0.8078f, 0.9216f, 1.0f)

        modelPtr = glGetUniformLocation(program, "model")
        viewPtr = glGetUniformLocation(program, "view")
        projectionPtr = glGetUniformLocation(program, "projection")

        glEnable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
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

        val meshes = scene.getMeshes()
        for (m in meshes) {
            m.upload()
        }
        glUseProgram(program)

        scene.getCamera().viewProjection.get(viewMatrixBuffer)
        scene.getCamera().projectionMatrix(aspect).get(projectionMatrixBuffer)
        glUniformMatrix4fv(viewPtr, false, viewMatrixBuffer)
        glUniformMatrix4fv(projectionPtr, false, projectionMatrixBuffer)

        for (m in meshes) {
            m.draw(modelPtr)
        }
    }
}