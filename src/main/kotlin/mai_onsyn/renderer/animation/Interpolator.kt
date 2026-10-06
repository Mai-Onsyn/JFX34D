package mai_onsyn.renderer.animation

interface Interpolator<T> {
    fun interpolate(frac: Float, start: T, end: T): T
}