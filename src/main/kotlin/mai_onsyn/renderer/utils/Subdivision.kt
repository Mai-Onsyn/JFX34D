package mai_onsyn.renderer.utils

import kotlin.math.ceil

const val DEFAULT_MAX_SUBDIVISIONS: Int = 64

fun densityToSubdivisions(density: Float, max: Int = DEFAULT_MAX_SUBDIVISIONS): Int {
    require(density > 0f) { "density must be > 0, got $density" }
    val cap = if (max < 1) 1 else max
    return ceil(density).toInt().coerceIn(1, cap)
}
