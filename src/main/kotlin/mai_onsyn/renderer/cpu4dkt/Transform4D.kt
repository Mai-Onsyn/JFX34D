package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector4f
import org.joml.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class Transform4D @JvmOverloads constructor (
    var matrix: Matrix5f = Matrix5f.IDENTITY
) {
    private var tT = Matrix5f.IDENTITY      // T        变换到局部坐标
    private var tTn1 = Matrix5f.IDENTITY    // T^(-1)   变换到全局坐标

    fun move(v: Vector4f) {
        val new = Matrix5f(
            1f, 0f, 0f, 0f, v.x,
            0f, 1f, 0f, 0f, v.y,
            0f, 0f, 1f, 0f, v.z,
            0f, 0f, 0f, 1f, v.w,
            0f, 0f, 0f, 0f, 1f
        )
        this.matrix *= tT
        this.matrix *= new
        this.matrix *= tTn1
    }

    fun scale(x: Float, y: Float = x, z: Float = x, w: Float = x) {
        val new = Matrix5f(
            x, 0f, 0f, 0f, 0f,
            0f, y, 0f, 0f, 0f,
            0f, 0f, z, 0f, 0f,
            0f, 0f, 0f, w, 0f,
            0f, 0f, 0f, 0f, 1f
        )
        this.matrix *= tT
        this.matrix *= new
        this.matrix *= tTn1
    }

    fun rotate(axis: Direction.Plane, angle: Float) {
        val radians = toRadians(angle)
        val c = cos(radians)
        val s = sin(radians)
        val new = when (axis) {
            Direction.Plane.XY -> Matrix5f(
                c, -s, 0f, 0f, 0f,
                s, c, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 1f
            )
            Direction.Plane.XZ -> Matrix5f(
                c, 0f, -s, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                s, 0f, c, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 1f
            )
            Direction.Plane.XW -> Matrix5f(
                c, 0f, 0f, -s, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                s, 0f, 0f, c, 0f,
                0f, 0f, 0f, 0f, 1f
            )
            Direction.Plane.YZ -> Matrix5f(
                0f, 0f, 0f, 0f, 0f,
                0f, c, -s, 0f, 0f,
                0f, s, c, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 1f
            )
            Direction.Plane.YW -> Matrix5f(
                0f, 0f, 0f, 0f, 0f,
                0f, c, 0f, -s, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, s, 0f, c, 0f,
                0f, 0f, 0f, 0f, 1f
            )
            Direction.Plane.ZW -> Matrix5f(
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, c, -s, 0f,
                0f, 0f, s, c, 0f,
                0f, 0f, 0f, 0f, 1f
            )
        }
        this.matrix *= tT
        this.matrix *= new
        this.matrix *= tTn1
    }

    fun clip(src: Direction.Axis, dest: Direction.Axis, k: Float) {
        val new = Matrix5f.IDENTITY
        if (src == dest) {
            new[dest.ordinal * 5 + src.ordinal] = 1f + k
        } else {
            new[dest.ordinal * 5 + src.ordinal] = k
        }
        this.matrix *= tT
        this.matrix *= new
        this.matrix *= tTn1
    }

    fun setTransformCoordinate(origin: Vector4f, coordinate: Coordinate4D) {
        tT = coordinate.run {
            Matrix5f(
                vx.x, vx.y, vx.z, vx.w, -(vx.dot(origin)),
                vy.x, vy.y, vy.z, vy.w, -(vy.dot(origin)),
                vz.x, vz.y, vz.z, vz.w, -(vz.dot(origin)),
                vw.x, vw.y, vw.z, vw.w, -(vw.dot(origin)),
                0f, 0f, 0f, 0f, 1f
            )
        }
        tTn1 = coordinate.run {
            Matrix5f(
                vx.x, vy.x, vz.x, vw.x, origin.x,
                vx.y, vy.y, vz.y, vw.y, origin.y,
                vx.z, vy.z, vz.z, vw.z, origin.z,
                vx.w, vy.w, vz.w, vw.w, origin.w,
                0f, 0f, 0f, 0f, 1f
            )
        }
    }
}