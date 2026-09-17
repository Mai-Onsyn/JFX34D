package mai_onsyn.renderer.data

import org.lwjgl.opengl.GL30
import java.io.IOException

class Shader(vertexSource: String, fragmentSource: String) {
    val program: Int

    init {
        val vertexShader = GL30.glCreateShader(GL30.GL_VERTEX_SHADER)
        GL30.glShaderSource(vertexShader, vertexSource)
        GL30.glCompileShader(vertexShader)

        val fragmentShader = GL30.glCreateShader(GL30.GL_FRAGMENT_SHADER)
        GL30.glShaderSource(fragmentShader, fragmentSource)
        GL30.glCompileShader(fragmentShader)

        program = GL30.glCreateProgram()
        GL30.glAttachShader(program, vertexShader)
        GL30.glAttachShader(program, fragmentShader)
        GL30.glLinkProgram(program)
        GL30.glDeleteShader(vertexShader)
        GL30.glDeleteShader(fragmentShader)
    }

    companion object {
        val basic = Shader(
            String(this.javaClass.getResourceAsStream("/shaders/basic-vertex.glsl")?.readBytes() ?: throw IOException("Cannot load shaders/basic-vertex.glsl")).trimIndent(),
            String(this.javaClass.getResourceAsStream("/shaders/basic-fragment.glsl")?.readBytes() ?: throw IOException("Cannot load shaders/basic-fragment.glsl")).trimIndent(),
        )
    }
}