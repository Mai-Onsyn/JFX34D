package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.cpu4dkt.hyperplaneNormal
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.data.toMesh
import mai_onsyn.renderer.ogl3d.generator.createCone
import mai_onsyn.renderer.ogl3d.generator.createCube
import mai_onsyn.renderer.ogl3d.generator.createPrism
import mai_onsyn.renderer.ogl3d.generator.createPyramid
import mai_onsyn.renderer.ogl3d.generator.createSphere
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f

/**
 * Agent(Java) 侧的桥接层。
 *
 * ### 为什么需要这个文件
 *
 * Agent 模块是 Java，渲染模块是 Kotlin，中间有三个跨语言障碍，导致 Java **在源码层面**
 * 根本写不出对应的调用（不是"没实现"，是编译器不允许）：
 *
 * 1. `ColorARGB` 是 `@JvmInline value class`。它在字节码层面是
 *    `private ColorARGB(int)` 加上两个**名字里带 `-`** 的静态方法
 *    （`constructor-impl` / `box-impl`），而 `-` 在 Java 里是非法标识符，
 *    所以 Java 既不能 `new` 也不能调静态工厂 —— 完全无法构造。
 * 2. `Vertex4D` / 3D `Vertex` 的构造函数是 `private`；编译器为带默认值的参数另外生成的
 *    那个 public 构造函数带 `DefaultConstructorMarker` 且被标记为 synthetic，
 *    `javac` 会直接忽略它。实测报错：`需要: Vector4f,int,Vector4f`。
 * 3. 只要函数签名里出现 value class，Kotlin 就会给方法名加哈希后缀做**名称修饰**
 *    （如 `GeometryInterface.setVertex-dnx2Rjw`），Java 同样写不出来。
 *
 * ### 本文件的约定
 *
 * **所有函数签名里都不出现 value class**，颜色一律用 `Int`（ARGB）传递。
 * 这样编译出的方法名是干净的，Java 可以直接调 `AgentBridge.xxx(...)`。
 *
 * 本文件只做"翻译"，不含业务逻辑：参数校验、消息拼装都在 Java 侧完成。
 * 没有改动渲染包里的任何既有文件。
 */
object AgentBridge {

    private val renderer: RendererInterface
        get() = RendererInterface.INSTANCE

    // ------------------------------------------------------------------ 几何编辑

    /**
     * `GeometryInterface.setVertex` 的 Java 可用版本。
     *
     * 原方法因为签名里有 `ColorARGB?` 被名称修饰，Java 调不到，这里代为转发。
     *
     * @param hasColor `false` 时保持顶点原色（对应原接口的 `color = null`）
     * @param hasNormal `false` 时保持顶点原法向（对应原接口的 `normal = null`）
     */
    @JvmStatic
    fun setVertex(
        name: String,
        tetIndex: Int,
        vertexNum: Int,
        pos: Vector4f,
        hasColor: Boolean,
        argb: Int,
        hasNormal: Boolean,
        normal: Vector4f
    ) {
        renderer.geometry.setVertex(
            name,
            tetIndex,
            vertexNum,
            Vector4f(pos),
            if (hasColor) ColorARGB(argb) else null,
            if (hasNormal) Vector4f(normal) else null
        )
    }

    /**
     * `GeometryInterface.addTetrahedron` 的 Java 可用版本。
     *
     * Java 侧无法构造 `Vertex4D`（构造函数 private），所以由这里代建。
     *
     * @param positions 16 个 float：4 个顶点各 `(x, y, z, w)`
     * @param colors 4 个 ARGB 颜色，每个顶点一个
     * @param normals 16 个 float：4 个顶点各 `(x, y, z, w)`；**全为 0 时**改用
     *   由这四个点算出的超平面单位法向量（`hyperplaneNormal`），符号按该函数的定义
     * @return 新四面体在该模型内的下标
     */
    @JvmStatic
    fun addTetrahedron(
        name: String,
        positions: FloatArray,
        colors: IntArray,
        normals: FloatArray
    ): Int {
        require(positions.size == 16) { "positions must contain 16 floats, got ${positions.size}" }
        require(colors.size == 4) { "colors must contain 4 values, got ${colors.size}" }
        require(normals.size == 16) { "normals must contain 16 floats, got ${normals.size}" }

        val p = arrayOf(
            Vector4f(positions[0], positions[1], positions[2], positions[3]),
            Vector4f(positions[4], positions[5], positions[6], positions[7]),
            Vector4f(positions[8], positions[9], positions[10], positions[11]),
            Vector4f(positions[12], positions[13], positions[14], positions[15])
        )
        val given = arrayOf(
            Vector4f(normals[0], normals[1], normals[2], normals[3]),
            Vector4f(normals[4], normals[5], normals[6], normals[7]),
            Vector4f(normals[8], normals[9], normals[10], normals[11]),
            Vector4f(normals[12], normals[13], normals[14], normals[15])
        )
        val auto = given.all { it.lengthSquared() < 1e-12f }
        val fallback =
            if (auto) hyperplaneNormal(p[0], p[1], p[2], p[3]) else Vector4f(0f, 0f, 0f, 0f)
        val n = if (auto) arrayOf(fallback, fallback, fallback, fallback) else given

        val tet = Tetrahedron(
            Vertex4D(p[0], ColorARGB(colors[0]), n[0]),
            Vertex4D(p[1], ColorARGB(colors[1]), n[1]),
            Vertex4D(p[2], ColorARGB(colors[2]), n[2]),
            Vertex4D(p[3], ColorARGB(colors[3]), n[3])
        )
        return renderer.geometry.addTetrahedron(name, tet)
    }

    /**
     * `GeometryInterface.sliceModel` 的 Java 可用版本。
     *
     * 切片超平面用一个 `Tetrahedron` 表示，同样因为 `Vertex4D` 无法在 Java 构造而需要代建。
     * 文档 §6.7 明确说该四面体的颜色/法向量对切片无意义，所以这里统一给透明黑 + 零法向。
     *
     * @param planePositions 16 个 float：超平面四个仿射无关的点各 `(x, y, z, w)`
     */
    @JvmStatic
    fun sliceModel(
        name: String,
        planePositions: FloatArray,
        pathA: String,
        pathB: String
    ) {
        require(planePositions.size == 16) {
            "planePositions must contain 16 floats, got ${planePositions.size}"
        }
        val zero = ColorARGB(0)
        val noNormal = Vector4f(0f, 0f, 0f, 0f)
        val plane = Tetrahedron(
            Vertex4D(Vector4f(planePositions[0], planePositions[1], planePositions[2], planePositions[3]), zero, noNormal),
            Vertex4D(Vector4f(planePositions[4], planePositions[5], planePositions[6], planePositions[7]), zero, noNormal),
            Vertex4D(Vector4f(planePositions[8], planePositions[9], planePositions[10], planePositions[11]), zero, noNormal),
            Vertex4D(Vector4f(planePositions[12], planePositions[13], planePositions[14], planePositions[15]), zero, noNormal)
        )
        renderer.geometry.sliceModel(name, plane, pathA, pathB)
    }

    // ------------------------------------------------------------------ 3D 底面网格

    /**
     * 生成 3D 基础网格，供 `CREATE_PRISM4` / `CREATE_CONE4` 当底面。
     *
     * 渲染包里的生成器（`ogl3d/generator/`）都带 `color: ColorARGB` 参数，因此同样被名称修饰，
     * Java 调不到；这里按形状转发一次。返回的 `Mesh` 是 Java 可用的普通类。
     *
     * @param shape `SPHERE` / `CUBE` / `PRISM` / `PYRAMID` / `CONE`（大小写不敏感）
     */
    @JvmStatic
    fun baseMesh(
        shape: String,
        sides: Int,
        radius: Float,
        height: Float,
        edge: Float,
        density: Float,
        segments: Int,
        argb: Int
    ): Mesh {
        val color = ColorARGB(argb)
        return when (shape.trim().uppercase()) {
            "SPHERE" -> createSphere(radius = radius, density = density, color = color).toMesh()
            "CUBE" -> createCube(size = edge, colorOf = { color }).toMesh()
            "PRISM" -> createPrism(sides = sides, radius = radius, height = height, color = color).toMesh()
            "PYRAMID" -> createPyramid(sides = sides, radius = radius, height = height, color = color).toMesh()
            "CONE" -> createCone(radius = radius, height = height, segments = segments, color = color).toMesh()
            else -> throw IllegalArgumentException(
                "Unknown base shape: \"$shape\", expected one of SPHERE, CUBE, PRISM, PYRAMID, CONE"
            )
        }
    }
}
