package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.interfaces.TransformInterface
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector4f

class TransformInterfaceImpl: TransformInterface {
    override fun getModelMatrix(name: String) {
        TODO("Not yet implemented")
    }

    override fun transform(m: Matrix5f) {
        TODO("Not yet implemented")
    }

    override fun move(v: Vector4f) {
        TODO("Not yet implemented")
    }

    override fun scale(x: Float, y: Float, z: Float, w: Float) {
        TODO("Not yet implemented")
    }

    override fun rotate(axis: Direction.Plane, angle: Float) {
        TODO("Not yet implemented")
    }

    override fun clip(
        src: Direction.Axis,
        dest: Direction.Axis,
        amount: Float
    ) {
        TODO("Not yet implemented")
    }

    override fun setCoordinate(origin: Vector4f, coordinate: Coordinate4D) {
        TODO("Not yet implemented")
    }
}