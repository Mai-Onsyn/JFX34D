package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Mesh4D
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
        scene.targets(name).forEach {
            it.transform.matrix = Matrix5f(matrix.data.copyOf())
            it.dirty = true
        }
    }

    override fun transform(name: String, m: Matrix5f) {
        scene.targets(name).forEach {
            it.transform.matrix = m * it.transform.matrix
            it.dirty = true
        }
    }

    override fun move(name: String, v: Vector4f) {
        scene.targets(name).forEach {
            it.transform.move(v)
            it.dirty = true
        }
    }

    override fun scale(name: String, x: Float, y: Float, z: Float, w: Float) {
        scene.targets(name).forEach {
            it.transform.scale(x, y, z, w)
            it.dirty = true
        }
    }

    override fun rotate(name: String, axis: Direction.Plane, angle: Float) {
        scene.targets(name).forEach {
            it.transform.rotate(axis, angle)
            it.dirty = true
        }
    }

    override fun clip(
        name: String,
        src: Direction.Axis,
        dest: Direction.Axis,
        amount: Float
    ) {
        scene.targets(name).forEach {
            it.transform.clip(src, dest, amount)
            it.dirty = true
        }
    }

    override fun setCoordinate(name: String, origin: Vector4f, coordinate: Coordinate4D) {
        scene.targets(name).forEach {
            it.transform.setTransformCoordinate(origin, coordinate)
            it.dirty = true
        }
    }
}
