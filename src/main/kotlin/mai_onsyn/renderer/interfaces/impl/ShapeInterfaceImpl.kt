package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.interfaces.ShapeInterface
import mai_onsyn.renderer.ogl3d.data.Mesh
import org.joml.Vector4f

class ShapeInterfaceImpl: ShapeInterface {
    override fun createTetrahedron(center: Vector4f, radius: Float) {
        TODO("Not yet implemented")
    }

    override fun create5Cell(center: Vector4f, size: Float) {
        TODO("Not yet implemented")
    }

    override fun create16Cell(center: Vector4f, radius: Float) {
        TODO("Not yet implemented")
    }

    override fun createTesseract(center: Vector4f, edgeLength: Float) {
        TODO("Not yet implemented")
    }

    override fun createPrism4(base: Mesh, ws: Float, we: Float) {
        TODO("Not yet implemented")
    }

    override fun createCone4(base: Mesh, apex: Vector4f) {
        TODO("Not yet implemented")
    }

    override fun createBall4(center: Vector4f, radius: Float) {
        TODO("Not yet implemented")
    }
}