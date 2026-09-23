package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.generator.constructHypercubeWithCellColors
import mai_onsyn.renderer.interfaces.ShapeInterface
import mai_onsyn.renderer.ogl3d.data.Mesh
import org.joml.Vector4f

class ShapeInterfaceImpl(
    val scene: SimpleScene4D
): ShapeInterface {
    override fun createTetrahedron(target: String, center: Vector4f, radius: Float) {
        TODO("Not yet implemented")
    }

    override fun create5Cell(target: String, center: Vector4f, size: Float) {
        TODO("Not yet implemented")
    }

    override fun create16Cell(target: String, center: Vector4f, radius: Float) {
        TODO("Not yet implemented")
    }

    override fun createTesseract(target: String, center: Vector4f, edgeLength: Float) {
        val find = scene.meshList.find { it.name == target }
        if (find == null) throw IllegalArgumentException("Model \"$target\" does not exists")
        find.tetrahedrons.addAll(constructHypercubeWithCellColors(center, edgeLength))
    }

    override fun createPrism4(target: String, base: Mesh, ws: Float, we: Float) {
        TODO("Not yet implemented")
    }

    override fun createCone4(target: String, base: Mesh, apex: Vector4f) {
        TODO("Not yet implemented")
    }

    override fun createBall4(target: String, center: Vector4f, radius: Float, density: Float) {
        TODO("Not yet implemented")
    }
}