import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class SimpleTriangleApp extends Application {

    private int shaderProgram;
    private int vao;
    private int vbo;

    @Override
    public void start(Stage primaryStage) {
        // 1. 创建 GLCanvas，使用 LWJGL 模块，并指定使用 Core Profile
        GLCanvas canvas = new GLCanvas(
                new GLCanvas.Builder().setExecutor(LWJGLExecutor.LWJGL_MODULE)
        );

        // 2. 添加渲染事件
        canvas.addOnInitEvent(event -> {
            // 当 OpenGL 上下文初始化时，只执行一次
            GL.createCapabilities(); // 初始化 LWJGL 的 OpenGL 绑定
            GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f); // 设置深灰色背景
            initShadersAndBuffers(); // 初始化着色器和顶点数据
        });

        canvas.addOnRenderEvent(event -> {
            // 每一帧都会执行
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT); // 清除颜色缓冲区

            // 使用我们编译好的着色器程序
            GL20.glUseProgram(shaderProgram);
            // 绑定顶点数组对象 (VAO)
            GL30.glBindVertexArray(vao);
            // 绘制三角形：从第0个顶点开始，总共3个顶点
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            // 解绑 VAO（好习惯）
            GL30.glBindVertexArray(0);
        });

        canvas.addOnReshapeEvent(event -> {
            // 当 Canvas 大小改变时，更新视口
            GL11.glViewport(0, 0, event.width, event.height);
        });

        // 3. 将 GLCanvas 添加到 JavaFX 场景中
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("OpenGLFX Triangle");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * 初始化着色器和顶点数据
     */
    private void initShadersAndBuffers() {
        // --- 1. 编译顶点着色器 ---
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;   // 顶点位置，来自我们的顶点数据
            layout (location = 1) in vec3 aColor; // 顶点颜色，来自我们的顶点数据
            out vec3 ourColor; // 将颜色传递给片段着色器
            void main() {
                gl_Position = vec4(aPos, 1.0); // 直接使用位置（已经在NDC坐标中）
                ourColor = aColor;
            }
            """;
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);
        checkShaderCompileError(vertexShader, "VERTEX");

        // --- 2. 编译片段着色器 ---
        String fragmentShaderSource = """
            #version 330 core
            in vec3 ourColor; // 从顶点着色器接收颜色
            out vec4 FragColor; // 最终输出的像素颜色
            void main() {
                FragColor = vec4(ourColor, 1.0); // 使用顶点颜色
            }
            """;
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentShaderSource);
        GL20.glCompileShader(fragmentShader);
        checkShaderCompileError(fragmentShader, "FRAGMENT");

        // --- 3. 链接着色器程序 ---
        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        checkProgramLinkError(shaderProgram);

        // 链接完成后可以删除着色器对象
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        // --- 4. 准备顶点数据 ---
        // 一个三角形，包含位置 (x, y, z) 和颜色 (r, g, b)
        // 顶点坐标范围在 -1.0 到 1.0 之间，这被称为标准化设备坐标 (NDC)
        float[] vertices = {
                // 位置                // 颜色
                0.0f,  0.5f, 0.0f,   1.0f, 0.0f, 0.0f, // 顶部顶点，红色
                -0.5f, -0.5f, 0.0f,   0.0f, 1.0f, 0.0f, // 左下顶点，绿色
                0.5f, -0.5f, 0.0f,   0.0f, 0.0f, 1.0f  // 右下顶点，蓝色
        };

        // 将数组转换为 OpenGL 所需的 FloatBuffer
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        // --- 5. 创建 VAO 和 VBO ---
        vao = GL30.glGenVertexArrays(); // 创建顶点数组对象 (VAO)
        vbo = GL15.glGenBuffers();       // 创建顶点缓冲对象 (VBO)

        GL30.glBindVertexArray(vao); // 绑定 VAO

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo); // 绑定 VBO
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW); // 将数据上传到 GPU

        // 设置顶点属性指针
        // 位置属性 (location = 0)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        // 颜色属性 (location = 1)
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // 解绑
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // 释放本地内存
        MemoryUtil.memFree(vertexBuffer);
    }

    // 简单的错误检查辅助方法
    private void checkShaderCompileError(int shader, String type) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println(type + " Shader compile error: " + GL20.glGetShaderInfoLog(shader));
        }
    }
    private void checkProgramLinkError(int program) {
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("Shader program link error: " + GL20.glGetProgramInfoLog(program));
        }
    }
}