package mai_onsyn.renderer.core

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.impl.CameraInterfaceImpl
import mai_onsyn.renderer.ogl3d.GL3DRegion
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import mai_onsyn.renderer.ogl4d.DirectGL4DEngine
import mai_onsyn.renderer.utils.fixedFrame

/**
 * 4D 场景的画布 —— 用 compute shader 直接变换 4D 顶点, 取代 [mai_onsyn.renderer.core.deprecated.GL4DRegion]
 * 里那条「CPU(C++) 串行变换 + JNI 回传 + 重建 Mesh」的路径。
 *
 * 用法和 GL4DRegion 基本一样:
 * ```
 * val region = DirectGL4DRegion()
 * region.scene4D.meshList.add(Mesh4D(constructHypercube()))
 * box.add(region, modifier.fillMaxSize())
 * ```
 * 输入也一样: 默认鼠标键盘控制 3D 相机, 按 I 切到 4D 相机 (WASD/空格/Shift/EQ 移动,
 * 方向键 + R/Alt 做六个平面的旋转)。
 *
 * 与 GL4DRegion 的差别:
 *  - 没有后台投影线程, 4D 数据只在使用时才打包上传 (改 transform 不会重传顶点)。
 *  - 4D 网格由 [mai_onsyn.renderer.ogl4d.DirectGL4DEngine] 用同一个 basic shader 在 3D 场景之后画一遍,
 *    共用颜色/深度缓冲, 所以背景色、光照、透明、线框的语义都跟老路径一致。
 *  - 因为 GL3DRegion.setOutlineRendering 是 final 的且只作用于 3D 引擎, 4D 的线框开关
 *    走 [set4DOutlineRendering]。光照开关继续用继承来的 enableLightRendering (同一个 uniform)。
 *  - 它不继承 GL4DRegion, 所以不能传给 RendererInterface.init (那个只收 GL4DRegion)。
 */
class GL4DRegion(
    val scene3D: SimpleScene3D = SimpleScene3D(),
    val scene4D: SimpleScene4D = SimpleScene4D()
) : GL3DRegion(scene3D) {

    private val engine = DirectGL4DEngine(scene4D, scene3D)

    /**
     * 摄像机接口实现，包装同一个 [scene4D.camera4D]，也是本画布的摄像机动画门面。
     * Agent 走它的 [CameraInterface] 方法（平滑动画），键盘走 nudge* 方法（动画中被禁用，否则即时同步），
     * 两边共用一个 [mai_onsyn.renderer.animation.AnimatedValue]。
     */
    val cameraInterface = CameraInterfaceImpl(scene4D.camera4D)

    private var enable4DInput = false
    private val movement = MovementState()

    private var switchPressed = false
    private var inputThread: Thread? = null

    var onEnable4DInputChanged: ((Boolean) -> Unit)? = null

    @Volatile var move4DSpeed = 1f
    @Volatile var mouse4DSpeed = 1f

    /** 4D 侧 (compute + 上传) 的帧率上限, <= 0 不限制 */
    var maxFPS: Int
        get() = engine.maxFPS
        set(value) {
            engine.maxFPS = value
        }

    /** 当前上下文能不能跑 compute; false 时画布只剩 3D 场景 */
    val isComputeSupported: Boolean
        get() = engine.isSupported

    fun get4DFPS(): Float = engine.fpsCounter.getAverageFrequency()

    fun get4D1PercentLowFPS(): Float = engine.fpsCounter.getOnePercentLowFrequency()

    fun setViewPortLength(length: Float) {
        engine.viewPortLength = length
    }

    /**
     * 当开启时关闭3D输入，两者互斥
     */
    fun enable4DInput(enable: Boolean) {
        enable4DInput = enable
        enableInput = !enable
        onEnable4DInputChanged?.invoke(enable)
    }

    /** 4D 网格的线框开关 (3D 的那个是继承来的 setOutlineRendering, 只作用于 3D 引擎) */
    fun set4DOutlineRendering(enable: Boolean) {
        engine.useOutlineRendering = enable
    }

    init {
        // 父类 init 先把 3D 引擎的事件挂上, 所以这里的 handler 一定在它之后跑:
        // 3D 引擎先清屏 + 画 3D 场景, 本引擎再往同一个 framebuffer 上画 4D 网格。
        this.addOnInitEvent(engine::init)
        this.addOnReshapeEvent(engine::reshape)
        this.addOnRenderEvent(engine::render)
        this.addOnDisposeEvent { onDispose() }

        this.addEventHandler(KeyEvent.KEY_PRESSED) {
            if (it.code == KeyCode.I) {
                this.enableInput = !this.enableInput
                enable4DInput = !this.enableInput
                onEnable4DInputChanged?.invoke(this.enableInput)
            }

            if (!enable4DInput) return@addEventHandler

            var consume = true
            when (it.code) {
                KeyCode.W -> movement.forward = 1f
                KeyCode.S -> movement.back = 1f
                KeyCode.A -> movement.left = 1f
                KeyCode.D -> movement.right = 1f
                KeyCode.SPACE -> movement.up = 1f
                KeyCode.SHIFT -> movement.down = 1f
                KeyCode.E -> movement.ana = 1f
                KeyCode.Q -> movement.negAna = 1f

                KeyCode.COMMA -> if (switchPressed) movement.nzw = 1f else movement.nxy = 1f
                KeyCode.PERIOD -> if (switchPressed) movement.zw = 1f else movement.xy = 1f

                KeyCode.LEFT -> if (switchPressed) movement.nxw = 1f else movement.nxz = 1f
                KeyCode.RIGHT -> if (switchPressed) movement.xw = 1f else movement.xz = 1f

                KeyCode.UP -> if (switchPressed) movement.nyw = 1f else movement.nyz = 1f
                KeyCode.DOWN -> if (switchPressed) movement.yw = 1f else movement.yz = 1f

                KeyCode.R, KeyCode.ALT -> {
                    switchPressed = true
                    movement.nxy = 0f
                    movement.xy = 0f
                    movement.nxw = 0f
                    movement.xw = 0f
                    movement.nyw = 0f
                    movement.yw = 0f
                }
                else -> consume = false
            }
            if (consume) it.consume()
        }

        this.addEventHandler(KeyEvent.KEY_RELEASED) {
            var consume = true
            when (it.code) {
                KeyCode.W -> movement.forward = 0f
                KeyCode.S -> movement.back = 0f
                KeyCode.A -> movement.left = 0f
                KeyCode.D -> movement.right = 0f
                KeyCode.SPACE -> movement.up = 0f
                KeyCode.SHIFT -> movement.down = 0f
                KeyCode.E -> movement.ana = 0f
                KeyCode.Q -> movement.negAna = 0f

                KeyCode.COMMA -> if (switchPressed) movement.nzw = 0f else movement.nxy = 0f
                KeyCode.PERIOD -> if (switchPressed) movement.zw = 0f else movement.xy = 0f

                KeyCode.LEFT -> if (switchPressed) movement.nxw = 0f else movement.nxz = 0f
                KeyCode.RIGHT -> if (switchPressed) movement.xw = 0f else movement.xz = 0f

                KeyCode.UP -> if (switchPressed) movement.nyw = 0f else movement.nyz = 0f
                KeyCode.DOWN -> if (switchPressed) movement.yw = 0f else movement.yz = 0f

                KeyCode.R, KeyCode.ALT -> {
                    switchPressed = false
                    movement.nzw = 0f
                    movement.zw = 0f
                    movement.nxz = 0f
                    movement.xz = 0f
                    movement.nyz = 0f
                    movement.yz = 0f
                }
                else -> consume = false
            }
            if (consume) it.consume()
        }

        startKeyEventHandlerThread()
    }

    private fun onDispose() {
        inputThread?.interrupt()
        cameraInterface.dispose()
    }

    private fun startKeyEventHandlerThread() {
        inputThread = Thread.ofVirtual().name("Direct 4D Region Key Event Handler").start {
            fixedFrame(1000, { !Thread.currentThread().isInterrupted }) {
                if (!enable4DInput) {
                    Thread.sleep(500)
                    return@fixedFrame
                }
                val moveSpeed = 0.004f * move4DSpeed
                val mouseSpeed = 0.0005f * mouse4DSpeed

                val deltaX = moveSpeed * (movement.right - movement.left)
                val deltaY = moveSpeed * (movement.up - movement.down)
                val deltaZ = moveSpeed * (movement.ana - movement.negAna)
                val deltaW = moveSpeed * (movement.forward - movement.back)

                if (deltaX != 0f) cameraInterface.nudgeRight(deltaX)
                if (deltaY != 0f) cameraInterface.nudgeUp(deltaY)
                if (deltaZ != 0f) cameraInterface.nudgeAna(deltaZ)
                if (deltaW != 0f) cameraInterface.nudgeForward(deltaW)

                val deltaXY = mouseSpeed * (movement.xy - movement.nxy)
                val deltaXZ = mouseSpeed * (movement.xz - movement.nxz)
                val deltaXW = mouseSpeed * (movement.xw - movement.nxw)
                val deltaYZ = mouseSpeed * (movement.yz - movement.nyz)
                val deltaYW = mouseSpeed * (movement.yw - movement.nyw)
                val deltaZW = mouseSpeed * (movement.zw - movement.nzw)

                if (deltaXY != 0f) scene4D.camera4D.rotateXY(deltaXY)
                if (deltaXZ != 0f) scene4D.camera4D.rotateXZ(deltaXZ)
                if (deltaXW != 0f) scene4D.camera4D.rotateXW(deltaXW)
                if (deltaYZ != 0f) scene4D.camera4D.rotateYZ(deltaYZ)
                if (deltaYW != 0f) scene4D.camera4D.rotateYW(deltaYW)
                if (deltaZW != 0f) scene4D.camera4D.rotateZW(deltaZW)
            }
        }
    }
}

private class MovementState(
    var left: Float = 0f,
    var right: Float = 0f,
    var down: Float = 0f,
    var up: Float = 0f,
    var negAna: Float = 0f,
    var ana: Float = 0f,
    var back: Float = 0f,
    var forward: Float = 0f,

    var xy: Float = 0f,
    var xz: Float = 0f,
    var xw: Float = 0f,
    var yz: Float = 0f,
    var yw: Float = 0f,
    var zw: Float = 0f,

    var nxy: Float = 0f,
    var nxz: Float = 0f,
    var nxw: Float = 0f,
    var nyz: Float = 0f,
    var nyw: Float = 0f,
    var nzw: Float = 0f
)
