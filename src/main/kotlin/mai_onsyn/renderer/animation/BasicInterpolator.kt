package mai_onsyn.renderer.animation

import org.joml.Vector4f
import kotlin.math.roundToInt

fun Float.Companion.interpolator(): Interpolator<Float> {
    return object: Interpolator<Float> {
        override fun interpolate(frac: Float, start: Float, end: Float): Float = start + (end - start) * frac
    }
}

fun Double.Companion.interpolator(): Interpolator<Double> {
    return object: Interpolator<Double> {
        override fun interpolate(frac: Float, start: Double, end: Double): Double = start + (end - start) * frac
    }
}

fun Int.Companion.interpolator(): Interpolator<Int> {
    return object: Interpolator<Int> {
        override fun interpolate(frac: Float, start: Int, end: Int): Int = start + ((end - start) * frac).roundToInt()
    }
}

fun Vector4f.interpolator(): Interpolator<Vector4f> {
    return object: Interpolator<Vector4f> {
        override fun interpolate(frac: Float, start: Vector4f, end: Vector4f): Vector4f {
            return Vector4f(
                start.x + (end.x - start.x) * frac,
                start.y + (end.y - start.y) * frac,
                start.z + (end.z - start.z) * frac,
                start.w + (end.w - start.w) * frac
            )
        }
    }
}