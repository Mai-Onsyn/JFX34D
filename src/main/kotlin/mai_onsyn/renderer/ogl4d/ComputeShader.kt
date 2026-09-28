package mai_onsyn.renderer.ogl4d

import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_FALSE
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glDeleteProgram
import org.lwjgl.opengl.GL20.glDeleteShader
import org.lwjgl.opengl.GL20.glGetProgramInfoLog
import org.lwjgl.opengl.GL20.glGetProgrami
import org.lwjgl.opengl.GL20.glGetShaderInfoLog
import org.lwjgl.opengl.GL20.glGetShaderi
import org.lwjgl.opengl.GL20.glLinkProgram
import org.lwjgl.opengl.GL20.glShaderSource
import org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER
import java.io.IOException

/**
 * compute program 的最小封装。
 *
 * 比 [mai_onsyn.renderer.ogl3d.data.Shader] 多了一件事: **编译/链接失败会把 info log 打出来**。
 * compute shader 写错了如果只是静默失败, 画面上什么都看不到, 根本没法查。
 */
class ComputeShader private constructor(val program: Int) {

    companion object {

        /** 4D 顶点变换用的 compute program; 第一次访问时创建 (必须在有 GL 上下文的渲染线程里) */
        val direct4D: ComputeShader by lazy { fromResource("/shaders/direct4d-transform.comp") }

        fun fromResource(path: String): ComputeShader {
            val source = ComputeShader::class.java.getResourceAsStream(path)
                ?.use { it.readBytes().decodeToString() }
                ?: throw IOException("Cannot load compute shader: $path")
            return compile(source, path)
        }

        fun compile(source: String, name: String = "compute"): ComputeShader {
            val shader = glCreateShader(GL_COMPUTE_SHADER)
            glShaderSource(shader, source)
            glCompileShader(shader)
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                val log = glGetShaderInfoLog(shader)
                glDeleteShader(shader)
                throw IllegalStateException("Compute shader compile failed [$name]:\n$log")
            }

            val program = glCreateProgram()
            glAttachShader(program, shader)
            glLinkProgram(program)
            glDeleteShader(shader)
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                val log = glGetProgramInfoLog(program)
                glDeleteProgram(program)
                throw IllegalStateException("Compute shader link failed [$name]:\n$log")
            }
            return ComputeShader(program)
        }
    }
}
