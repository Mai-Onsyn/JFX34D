package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Camera4D
import mai_onsyn.renderer.interfaces.CameraInterface
import mai_onsyn.renderer.utils.Coordinate4D
import org.joml.Vector4f

class CameraInterfaceImpl(val camera: Camera4D): CameraInterface {
    override fun getPosition(): Vector4f = camera.pos

    override fun getView(): Coordinate4D = Coordinate4D(
        camera.vx,
        camera.vy,
        camera.vz,
        camera.vw
    )

    override fun setPosition(pos: Vector4f) {
        camera.pos = pos
    }

    override fun setView(v: Coordinate4D) {
        TODO("Not yet implemented")
    }

    override fun moveRight(distance: Float) = camera.moveRight(distance)

    override fun moveUp(distance: Float) = camera.moveUp(distance)

    override fun moveAna(distance: Float) = camera.moveAna(distance)

    override fun moveForward(distance: Float) = camera.moveForward(distance)

    override fun rotateXY(angle: Float) {
        TODO("Not yet implemented")
    }

    override fun rotateXZ(angle: Float) {
        TODO("Not yet implemented")
    }

    override fun rotateXW(angle: Float) {
        TODO("Not yet implemented")
    }

    override fun rotateYZ(angle: Float) {
        TODO("Not yet implemented")
    }

    override fun rotateYW(angle: Float) {
        TODO("Not yet implemented")
    }

    override fun rotateZW(angle: Float) {
        TODO("Not yet implemented")
    }
}