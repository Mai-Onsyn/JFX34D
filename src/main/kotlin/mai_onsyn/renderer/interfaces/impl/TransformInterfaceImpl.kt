package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.TransformInterface
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector4f

class TransformInterfaceImpl(
    val scene: SimpleScene4D
): TransformInterface {
    override fun getModelMatrix(name: String): Matrix5f =
        scene.requireContains(name).transform.matrix

    override fun setModelMatrix(name: String, matrix: Matrix5f) {
        scene.requireContains(name).transform.matrix = matrix
    }

    override fun transform(name: String, m: Matrix5f) {
        scene.requireContains(name).transform.matrix * m
    }

    override fun move(name: String, v: Vector4f) {
        scene.requireContains(name).transform.move(v)
    }

    override fun scale(name: String, x: Float, y: Float, z: Float, w: Float) {
        scene.requireContains(name).transform.scale(x, y, z, w)
    }

    override fun rotate(name: String, axis: Direction.Plane, angle: Float) {
        scene.requireContains(name).transform.rotate(axis, angle)
    }

    override fun clip(
        name: String,
        src: Direction.Axis,
        dest: Direction.Axis,
        amount: Float
    ) {
        scene.requireContains(name).transform.clip(src, dest, amount)
    }

    override fun setCoordinate(name: String, origin: Vector4f, coordinate: Coordinate4D) {
        scene.requireContains(name).transform.setTransformCoordinate(origin, coordinate)
    }
}