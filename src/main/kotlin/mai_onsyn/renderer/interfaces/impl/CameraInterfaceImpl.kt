package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.animation.AnimatedValue
import mai_onsyn.renderer.animation.Interpolator
import mai_onsyn.renderer.cpu4dkt.Camera4D
import mai_onsyn.renderer.interfaces.CameraInterface
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.dynamicFrame
import mai_onsyn.renderer.utils.interpolator
import org.joml.Math.toRadians
import org.joml.Vector4f

/**
 * [CameraInterface] 的实现，兼作摄像机的**平滑动画门面**。
 *
 * 位置的写入统一收口到一个 [AnimatedValue]（唯一真相源），[Camera4D.pos] 降级为每帧被写回的输出缓存，
 * 因此 [Camera4D] 本身保持零改动。两条写入路径：
 *  - **API 路径**（[setPosition] / [moveRight] / [moveUp] / [moveAna] / [moveForward]）：触发平滑动画；
 *  - **键盘路径**（[nudgeRight] / [nudgeUp] / [nudgeAna] / [nudgeForward]）：动画进行中直接丢弃（禁用键盘），
 *    否则即时同步进 [AnimatedValue] 并立刻写回 [Camera4D]。
 *
 * 坐标系（视角）：仅 [setView] 做动画，插值用四元数 SLERP（[Coordinate4D.interpolate]），
 * 保证中间帧始终是正交基；动画起点在触发瞬间从相机实时基向量快照（[snapshotView]），
 * 所以键盘直接旋转后的姿态也能被平滑接续、不会跳变。其余六个 rotate* 仍直接透传给 [Camera4D]。
 *
 * 一个后台 tick 线程按 [tickHz] 推进动画；动画按真实时间计算进度，与帧率解耦。
 *
 * @param camera 被包装的原始摄像机（整个项目里应只有这一个实例被驱动）
 * @param tickHz 动画推进频率（Hz）
 */
class CameraInterfaceImpl(
    val camera: Camera4D,
    var tickHz: Int = 120,
): CameraInterface {

    private val posValue = AnimatedValue(Vector4f(camera.pos), VEC4_LERP)

    /**
     * 坐标系（视角）动画。起点由 [snapshotView] 在 [setView] 触发时即时抓取，
     * 因此键盘直接旋转（绕过本类）造成的姿态变化也能被正确接续，不会跳变。
     * 插值用四元数 SLERP（见 [Coordinate4D.interpolate]），保证中间帧始终是正交基。
     */
    private val viewValue = AnimatedValue(snapshotView(), Coordinate4D.interpolator())

    private var tickThread: Thread? = null

    init {
        startTickThread()
    }

    // ---------------------------------------------------------------- API 路径（平滑动画）

    override fun getPosition(): Vector4f = camera.pos

    override fun getView(): Coordinate4D = Coordinate4D(
        camera.vx,
        camera.vy,
        camera.vz,
        camera.vw
    )

    override fun setPosition(pos: Vector4f) {
        posValue.animateTo(Vector4f(pos))
    }

    override fun setView(v: Coordinate4D) {
        // 先采纳"屏幕上的实时姿态"作为动画起点，再平滑过渡到目标坐标系
        viewValue.snapTo(snapshotView())
        viewValue.animateTo(v)
    }

    override fun moveRight(distance: Float) = animateAlong(camera.vx, distance)

    override fun moveUp(distance: Float) = animateAlong(camera.vy, distance)

    override fun moveAna(distance: Float) = animateAlong(camera.vz, distance)

    override fun moveForward(distance: Float) = animateAlong(camera.vw, distance)

    override fun rotateXY(angle: Float) = camera.rotateXY(toRadians(angle))

    override fun rotateXZ(angle: Float) = camera.rotateXZ(toRadians(angle))

    override fun rotateXW(angle: Float) = camera.rotateXW(toRadians(angle))

    override fun rotateYZ(angle: Float) = camera.rotateYZ(toRadians(angle))

    override fun rotateYW(angle: Float) = camera.rotateYW(toRadians(angle))

    override fun rotateZW(angle: Float) = camera.rotateZW(toRadians(angle))

    // ---------------------------------------------------------------- 键盘路径（即时同步）

    /** 键盘右移：动画中忽略；否则即时写入 [AnimatedValue] 与 [Camera4D] */
    fun nudgeRight(distance: Float) = nudgeAlong(camera.vx, distance)

    /** 键盘上移：动画中忽略；否则即时写入 [AnimatedValue] 与 [Camera4D] */
    fun nudgeUp(distance: Float) = nudgeAlong(camera.vy, distance)

    /** 键盘 z+ 移动：动画中忽略；否则即时写入 [AnimatedValue] 与 [Camera4D] */
    fun nudgeAna(distance: Float) = nudgeAlong(camera.vz, distance)

    /** 键盘 w+ 移动：动画中忽略；否则即时写入 [AnimatedValue] 与 [Camera4D] */
    fun nudgeForward(distance: Float) = nudgeAlong(camera.vw, distance)

    /** 当前位置是否正处于动画过渡中（动画期间键盘移动被禁用） */
    val isAnimating: Boolean
        get() = posValue.isAnimating

    /** 停止后台 tick 线程，画布释放时调用 */
    fun dispose() {
        tickThread?.interrupt()
    }

    // ---------------------------------------------------------------- 内部

    /** API 语义：把当前位置沿 [dir] 平移 [distance] 后的结果作为动画目标 */
    private fun animateAlong(dir: Vector4f, distance: Float) {
        posValue.animateTo(Vector4f(posValue.value).fma(distance, dir))
    }

    /** 键盘语义：动画中丢弃；否则在当前位置上即时平移并写回 */
    private fun nudgeAlong(dir: Vector4f, distance: Float) {
        if (posValue.isAnimating) return
        posValue.snapTo(Vector4f(posValue.value).fma(distance, dir))
        camera.pos.set(posValue.value)
    }

    /** 从相机当前基向量拷贝一份坐标系快照（用于让 [setView] 动画从"屏幕上的实时姿态"出发） */
    private fun snapshotView(): Coordinate4D = Coordinate4D(
        Vector4f(camera.vx),
        Vector4f(camera.vy),
        Vector4f(camera.vz),
        Vector4f(camera.vw)
    )

    /** 把插值得到的坐标系写回相机基向量 */
    private fun applyView(c: Coordinate4D) {
        camera.vx = c.vx
        camera.vy = c.vy
        camera.vz = c.vz
        camera.vw = c.vw
    }

    private fun startTickThread() {
        tickThread = Thread.ofVirtual().name("Camera Animation Tick").start {
            dynamicFrame({ tickHz }, { !Thread.currentThread().isInterrupted }) {
                if (posValue.tick()) {
                    camera.pos.set(posValue.value)
                }
                if (viewValue.tick()) {
                    applyView(viewValue.value)
                }
            }
        }
    }
}

/** 四维向量的线性插值器（逐分量 lerp） */
private val VEC4_LERP = object : Interpolator<Vector4f> {
    override fun interpolate(frac: Float, start: Vector4f, end: Vector4f): Vector4f = Vector4f(
        start.x + (end.x - start.x) * frac,
        start.y + (end.y - start.y) * frac,
        start.z + (end.z - start.z) * frac,
        start.w + (end.w - start.w) * frac
    )
}