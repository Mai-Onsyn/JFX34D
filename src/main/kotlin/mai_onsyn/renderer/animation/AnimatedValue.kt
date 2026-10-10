package mai_onsyn.renderer.animation

/**
 * 一个可平滑过渡到目标值的容器。它自己带时间基准，按真实时间推进（与帧率 / 推进频率解耦），
 * 由外部循环（如 [mai_onsyn.renderer.interfaces.impl.CameraInterfaceImpl] 的 tick 线程）反复调用 [tick] 驱动。
 *
 * 语义：
 *  - [animateTo] 启动一段缓动动画，[isAnimating] 置 true；
 *  - [snapTo] 立即到值并取消动画（键盘那种"直接同步"的写入走这里）；
 *  - [tick] 每次推进一帧，动画结束的那一帧也会把 [value] 精确落到 [target]。
 */
class AnimatedValue<T>(
    initial: T,
    private val interpolator: Interpolator<T>,
    var easing: Easing = Easing.Smooth
) {
    var isAnimating: Boolean = false
        private set

    var value: T = initial
        private set

    /** 当前动画的目标值；未动画时等于 [value] */
    var target: T = initial
        private set

    private var startValue: T = initial
    private var startNanos: Long = 0L
    private var durationMs: Long = 0L

    /**
     * 平滑过渡到 [target]。
     *
     * @param durationMs 时长（毫秒），<= 0 时退化为 [snapTo]
     * @param easing 缓动曲线，null 时取全局默认 [DEFAULT_EASING]
     */
    fun animateTo(target: T, durationMs: Long = DEFAULT_DURATION_MS, easing: Easing? = null) {
        if (durationMs <= 0L) {
            snapTo(target)
            return
        }
        this.startValue = this.value
        this.target = target
        this.startNanos = System.nanoTime()
        this.durationMs = durationMs
        this.easing = easing ?: DEFAULT_EASING
        this.isAnimating = true
    }

    /** 立即写入并取消当前动画，[value] 与 [target] 都对齐到 [v] */
    fun snapTo(v: T) {
        this.value = v
        this.target = v
        this.isAnimating = false
    }

    /**
     * 推进一帧。
     *
     * @return 本次调用是否更新了 [value]（动画中的每一帧，含结束帧，都返回 true）
     */
    fun tick(): Boolean {
        if (!isAnimating) return false

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
        val frac = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        value = interpolator.interpolate(easing.ease(frac), startValue, target)

        if (frac >= 1f) {
            value = target
            isAnimating = false
        }
        return true
    }

    companion object {
        /** 全局默认动画时长（毫秒） */
        @Volatile
        var DEFAULT_DURATION_MS: Long = 1000L

        /** 全局默认缓动曲线 */
        @Volatile
        var DEFAULT_EASING: Easing = Easing.FastOutSlowIn
    }
}
