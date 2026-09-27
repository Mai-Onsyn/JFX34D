package mai_onsyn.renderer.interfaces.impl

import com.alibaba.fastjson2.JSONObject
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.MeshKind
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.generator.ballSubdivisions
import mai_onsyn.renderer.cpu4dkt.generator.construct16Cell
import mai_onsyn.renderer.cpu4dkt.generator.construct5Cell
import mai_onsyn.renderer.cpu4dkt.generator.constructBall4
import mai_onsyn.renderer.cpu4dkt.generator.constructCone4
import mai_onsyn.renderer.cpu4dkt.generator.constructHypercubeWithCellColors
import mai_onsyn.renderer.cpu4dkt.generator.constructPrism4
import mai_onsyn.renderer.cpu4dkt.generator.constructTetrahedronCell
import mai_onsyn.renderer.interfaces.ShapeInterface
import mai_onsyn.renderer.ogl3d.data.Mesh
import org.joml.Vector4f
import java.text.DecimalFormat

/**
 * 形状创建：把参数化几何写进指定路径下的一个新模型，kind = SHAPE，params 记录参数。
 * 目标路径已存在时直接报错（避免两个形状挤进同一个 SHAPE 把参数弄脏）。
 */
class ShapeInterfaceImpl(
    val scene: SimpleScene4D
): ShapeInterface {
    private val decimalFormat = DecimalFormat("#.##")

    override fun createTetrahedron(target: String, name: String, center: Vector4f, radius: Float) {
        require(radius > 0f) { "radius must be > 0, got $radius" }
        addShape(
            target, name,
            constructTetrahedronCell(center, radius),
            JSONObject(
                mapOf(
                    "type" to "Tetrahedron",
                    "center" to center.toString(decimalFormat),
                    "radius" to radius.toString()
                )
            )
        )
    }

    override fun create5Cell(target: String, name: String, center: Vector4f, size: Float) {
        require(size > 0f) { "size (edge length) must be > 0, got $size" }
        addShape(
            target, name,
            construct5Cell(center, size),
            JSONObject(
                mapOf(
                    "type" to "5Cell",
                    "center" to center.toString(decimalFormat),
                    "size" to size.toString(),
                    "cells" to "5"
                )
            )
        )
    }

    override fun create16Cell(target: String, name: String, center: Vector4f, radius: Float) {
        require(radius > 0f) { "radius must be > 0, got $radius" }
        addShape(
            target, name,
            construct16Cell(center, radius),
            JSONObject(
                mapOf(
                    "type" to "16Cell",
                    "center" to center.toString(decimalFormat),
                    "radius" to radius.toString(),
                    "cells" to "16"
                )
            )
        )
    }

    override fun createTesseract(target: String, name: String, center: Vector4f, edgeLength: Float) {
        require(edgeLength > 0f) { "edge length must be > 0, got $edgeLength" }
        addShape(
            target, name,
            constructHypercubeWithCellColors(center, edgeLength),
            JSONObject(
                mapOf(
                    "type" to "Tesseract",
                    "center" to center.toString(decimalFormat),
                    "edge length" to edgeLength.toString(),
                    "cells" to "8"
                )
            )
        )
    }

    override fun createPrism4(target: String, name: String, base: Mesh, ws: Float, we: Float) {
        require(base.triangles.isNotEmpty()) { "Prism base mesh has no triangle" }
        require(ws != we) { "Prism w range must not be empty, got ws = we = $ws" }
        val low = minOf(ws, we)
        val high = maxOf(ws, we)
        addShape(
            target, name,
            constructPrism4(base, low, high),
            JSONObject(
                mapOf(
                    "type" to "Prism4",
                    "base triangles" to base.triangles.size.toString(),
                    "w range" to "($low, $high)"
                )
            )
        )
    }

    override fun createCone4(target: String, name: String, base: Mesh, apex: Vector4f) {
        require(base.triangles.isNotEmpty()) { "Cone base mesh has no triangle" }
        addShape(
            target, name,
            constructCone4(base, apex),
            JSONObject(
                mapOf(
                    "type" to "Cone4",
                    "base triangles" to base.triangles.size.toString(),
                    "apex" to apex.toString(decimalFormat)
                )
            )
        )
    }

    override fun createBall4(target: String, name: String, center: Vector4f, radius: Float, density: Float) {
        require(radius > 0f) { "radius must be > 0, got $radius" }
        require(density > 0f) { "density must be > 0, got $density" }
        addShape(
            target, name,
            constructBall4(center, radius, density),
            JSONObject(
                mapOf(
                    "type" to "Ball4",
                    "center" to center.toString(decimalFormat),
                    "radius" to radius.toString(),
                    "density" to density.toString(),
                    "subdivisions" to ballSubdivisions(density).toString()
                )
            )
        )
    }

    // ---------------------------------------------------------------- 内部

    private fun addShape(
        target: String,
        name: String,
        tetrahedrons: MutableList<Tetrahedron>,
        params: JSONObject
    ) {
        val path = shapePath(target, name)
        scene.requireNotContains(path)
        scene.meshList.add(
            Mesh4D(tetrahedrons = tetrahedrons, name = path).apply {
                this.kind = MeshKind.SHAPE
                this.params = params
                this.dirty = true
            }
        )
    }

    /**
     * target 是目标模型（可以带多级路径），name 是新部件的名字（也可以带子路径）。
     * 父级路径不需要真实存在，层级完全由名字前缀表达。
     */
    private fun shapePath(target: String, name: String): String {
        val t = target.trim().trim('/')
        val n = name.trim().trim('/')
        require(n.isNotEmpty()) { "Part name must not be empty" }
        require("//" !in n && "//" !in t) { "Path must not contain an empty segment: \"$target/$name\"" }
        require('#' !in n) { "Part name must not contain '#': \"$name\"" }
        val path = if (t.isEmpty()) n else "$t/$n"
        require(path.split('/').all { it.isNotEmpty() }) { "Invalid part path: \"$path\"" }
        return path
    }
}
