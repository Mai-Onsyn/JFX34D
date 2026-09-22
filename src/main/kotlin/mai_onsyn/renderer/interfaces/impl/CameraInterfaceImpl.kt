package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Camera4D
import mai_onsyn.renderer.interfaces.CameraInterface
import mai_onsyn.renderer.utils.Coordinate4D
import org.joml.Math.toRadians
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
        camera.vx = v.vx
        camera.vy = v.vy
        camera.vz = v.vz
        camera.vw = v.vw
    }

    override fun moveRight(distance: Float) = camera.moveRight(distance)

    override fun moveUp(distance: Float) = camera.moveUp(distance)

    override fun moveAna(distance: Float) = camera.moveAna(distance)

    override fun moveForward(distance: Float) = camera.moveForward(distance)

    override fun rotateXY(angle: Float) = camera.rotateXY(toRadians(angle))

    override fun rotateXZ(angle: Float) = camera.rotateXZ(toRadians(angle))

    override fun rotateXW(angle: Float) = camera.rotateXW(toRadians(angle))

    override fun rotateYZ(angle: Float) = camera.rotateYZ(toRadians(angle))

    override fun rotateYW(angle: Float) = camera.rotateYW(toRadians(angle))

    override fun rotateZW(angle: Float) = camera.rotateZW(toRadians(angle))
}