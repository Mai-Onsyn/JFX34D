package mai_onsyn.renderer.animation

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

abstract class Easing {
    abstract fun ease(f: Float) : Float

    companion object {
        fun easeBy(f: (Float) -> Float): Easing {
            return object : Easing() {
                override fun ease(f: Float): Float {
                    return f(f)
                }
            }
        }

        private fun dampedSpring(
            damping: Float = 4f,
            frequency: Float = 4.5f,
        ): (Float) -> Float {
            val raw: (Float) -> Float = { t -> 1f - exp(-damping * t) * cos(frequency * t) }

            val tail = raw(1f)

            return { t -> raw(t) / tail }
        }

        val Linear: Easing = easeBy { it }
        val Smooth: Easing = easeBy { t -> t * t * (3f - 2f * t) }
        val DampedSpring: Easing = easeBy(dampedSpring())
        val FastOutSlowIn: Easing = easeBy { t -> 1f - (1f - t).pow(3) }
    }
}