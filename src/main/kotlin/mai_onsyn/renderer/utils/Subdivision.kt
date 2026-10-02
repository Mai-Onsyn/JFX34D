package mai_onsyn.renderer.utils

import kotlin.math.ceil

/**
 * "密度" → 细分段数的**安全上限**兜底。
 *
 * cubed-sphere 方案的代价是 O(k³)：四维超球每个密度级别要生成 `8 · k³ · 6` 个四面体
 * （`k = 64` 时约 1260 万，`k = 100` 就近 4800 万），所以必须有个上限，
 * 否则调用方随手填个 `density = 500` 就会把内存打爆。
 *
 * 需要更细的细分时，由调用方把 `max` 显式传进来（`constructBall4` / `createSphere`
 * 都带了 `maxSubdivisions` 参数），而不是把这里改大。
 */
const val DEFAULT_MAX_SUBDIVISIONS: Int = 64

/**
 * 把"密度"映射成细分段数 `k` —— 也就是 cubed-sphere 里**每条边的分段数**。
 *
 * - 用 `ceil` 而不是 `round`：单调递增，`density` 每加 1 就多一级细分，
 *   不会出现 `12.0 ~ 12.4` 都被算成 12 这种"加了没用"的情况，细分尽可能可控；
 * - 结果恒满足 `1 <= k <= max`：`density = 1` 时 `k = 1`（即立方体 / 超立方体那一级）。
 *
 * @param density 密度，必须 `> 0`
 * @param max     细分段数上限，默认 [DEFAULT_MAX_SUBDIVISIONS]，至少按 1 处理
 * @throws IllegalArgumentException `density <= 0`（含 NaN）
 */
fun densityToSubdivisions(density: Float, max: Int = DEFAULT_MAX_SUBDIVISIONS): Int {
    require(density > 0f) { "density must be > 0, got $density" }
    val cap = if (max < 1) 1 else max
    return ceil(density).toInt().coerceIn(1, cap)
}
