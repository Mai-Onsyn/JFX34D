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
