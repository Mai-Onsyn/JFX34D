package mai_onsyn.renderer.ogl3d

import com.huskerdev.openglfx.canvas.GLCanvas
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor
import javafx.scene.Cursor
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.robot.Robot
import mai_onsyn.renderer.ogl3d.data.Scene3D
import mai_onsyn.renderer.utils.FrequencyCounter
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.roundToLong

open class GL3DRegion(val scene: Scene3D) : GLCanvas(
    executor = LWJGLExecutor.LWJGL_MODULE,
    fps = 10000.0,
    msaa = 4
) {
    var _enableInput: Boolean = true
    var enableInput: Boolean
        get() = _enableInput
        set(value) {
            _enableInput = value
            if (!value) this.cursor = Cursor.DEFAULT
            mouseCatched = false
        }

    private val engine = GL3DEngine(scene)
    @Volatile private var moveW: Float = 0f
    @Volatile private var moveA: Float = 0f
    @Volatile private var moveS: Float = 0f
    @Volatile private var moveD: Float = 0f
    @Volatile private var moveUp: Float = 0f
    @Volatile private var moveDown: Float = 0f
    @Volatile private var verticalDelta: Float = 0f
    @Volatile private var horizontalDelta: Float = 0f
    @Volatile private var mouseUp: Float = 0f
    @Volatile private var mouseDown: Float = 0f
    @Volatile private var mouseLeft: Float = 0f
    @Volatile private var mouseRight: Float = 0f

    @Volatile private var sprint: Float = 1f

    @Volatile private var mouseCatched: Boolean = false

    private var centerX: Double = 100.0
    private var centerY: Double = 100.0

    @Volatile private var lastX: Double = 0.0
    @Volatile private var lastY: Double = 0.0
    @Volatile private var robotJustMoved: Boolean = false

    private val robot = Robot()

    val newFPSCounter: FrequencyCounter
        get() = engine.fpsCounter

    fun setOutlineRendering(b: Boolean) { engine.useOutlineRendering = b }

    init {
        this.addOnInitEvent(engine::init)
        this.addOnReshapeEvent(engine::reshape)
        this.addOnRenderEvent(engine::render)

        this.addEventHandler(MouseEvent.MOUSE_CLICKED) {
            this.requestFocus()
            if (!enableInput) return@addEventHandler

            mouseCatched = true
            lastX = it.x
            lastY = it.y
            robot.mouseMove(this.localToScreen(centerX, centerY))
            robotJustMoved = true
            this.cursor = Cursor.NONE
        }

        this.addEventHandler(MouseEvent.MOUSE_MOVED) {
            if (!this.isFocused || !mouseCatched || !enableInput) {
                return@addEventHandler
            }

            if (robotJustMoved &&
                abs(it.x - centerX) < 5.0 &&
                abs(it.y - centerY) < 5.0
            ) {
                lastX = it.x
                lastY = it.y
                robotJustMoved = false
                return@addEventHandler
            }
            robotJustMoved = false
            horizontalDelta += (it.x - lastX).toFloat()
            verticalDelta   += (it.y - lastY).toFloat()
            lastX = it.x
            lastY = it.y

            robot.mouseMove(this.localToScreen(centerX, centerY))
            robotJustMoved = true
        }

        this.widthProperty().addListener { _, _, _ ->
            centerX = this.width / 2
            centerY = this.height / 2
        }
        this.heightProperty().addListener { _, _, _ ->
            centerX = this.width / 2
            centerY = this.height / 2
        }

        this.addEventHandler(KeyEvent.KEY_PRESSED) {
            if (!enableInput) return@addEventHandler
            when (it.code) {
                KeyCode.W -> moveW = 1f
                KeyCode.A -> moveA = 1f
                KeyCode.S -> moveS = 1f
                KeyCode.D -> moveD = 1f
                KeyCode.SHIFT -> moveDown = 1f
                KeyCode.SPACE -> moveUp = 1f
                KeyCode.UP -> mouseUp = 1f
                KeyCode.DOWN -> mouseDown = 1f
                KeyCode.LEFT -> mouseLeft = 1f
                KeyCode.RIGHT -> mouseRight = 1f

                KeyCode.ESCAPE -> {
                    mouseCatched = false
                    this.cursor = Cursor.DEFAULT
                }

                KeyCode.CONTROL -> sprint = 4f
                KeyCode.TAB -> sprint = 0.2f
                else -> {}
            }
        }
        this.addEventHandler(KeyEvent.KEY_RELEASED) {
            if (!enableInput) return@addEventHandler
            when (it.code) {
                KeyCode.W -> moveW = 0f
                KeyCode.A -> moveA = 0f
                KeyCode.S -> moveS = 0f
                KeyCode.D -> moveD = 0f
                KeyCode.SHIFT -> moveDown = 0f
                KeyCode.SPACE -> moveUp = 0f
                KeyCode.UP -> mouseUp = 0f
                KeyCode.DOWN -> mouseDown = 0f
                KeyCode.LEFT -> mouseLeft = 0f
                KeyCode.RIGHT -> mouseRight = 0f

                KeyCode.CONTROL -> sprint = 1f
                KeyCode.TAB -> sprint = 1f
                else -> {}
            }
        }

        launchBackHandlerThread()
    }

    private var handlerThread: Thread? = null
    private fun launchBackHandlerThread() {
        handlerThread = Thread.ofVirtual().name("3D Region Key Event Handler").start {
            val moveSpeed = 0.004f
            val mouseKeySpeed = 0.0015f
            val mouseMoveSpeed = 0.001f
            val smoothTimeMs = 5f
            var lastTime = System.nanoTime()

            var targetMouseX = 0f
            var targetMouseY = 0f
            var currentMouseX = 0f
            var currentMouseY = 0f

            while (!Thread.currentThread().isInterrupted) {
                if (!enableInput) {
                    Thread.sleep(100)
                    continue
                }
                val deltaX = moveSpeed * sprint * (moveD - moveA)
                val deltaY = moveSpeed * sprint * (moveUp - moveDown)
                val deltaZ = moveSpeed * sprint * (moveW - moveS)
                if (deltaX != 0f) scene.getCamera().moveX(deltaX)
                if (deltaY != 0f) scene.getCamera().moveY(deltaY)
                if (deltaZ != 0f) scene.getCamera().moveZ(deltaZ)

                // 累积鼠标旋转的目标量
                targetMouseX += (mouseRight - mouseLeft) * mouseKeySpeed +
                        horizontalDelta * mouseMoveSpeed
                targetMouseY += (mouseDown - mouseUp) * mouseKeySpeed +
                        verticalDelta * mouseMoveSpeed
                horizontalDelta = 0f
                verticalDelta = 0f

                val now = System.nanoTime()
                val passedTime = now - lastTime
                lastTime = now
                val dtMs = passedTime / 1_000_000f

                val t = (dtMs / smoothTimeMs).coerceIn(0f, 1f)

                val newMouseX = currentMouseX + (targetMouseX - currentMouseX) * t
                val newMouseY = currentMouseY + (targetMouseY - currentMouseY) * t

                val applyX = newMouseX - currentMouseX
                val applyY = newMouseY - currentMouseY
                if (applyX != 0f) scene.getCamera().rotateX(applyX)
                if (applyY != 0f) scene.getCamera().rotateY(applyY)

                currentMouseX = newMouseX
                currentMouseY = newMouseY

                val waitingTime = ((1_000_000L - passedTime).toFloat() / 1_000_000f).roundToLong()
                if (waitingTime > 0) {
                    Thread.sleep(waitingTime)
                }
            }
        }
    }
}