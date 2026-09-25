package mai_onsyn.renderer.core

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import mai_onsyn.renderer.cpu4dkt.Renderer4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.ogl3d.GL3DRegion
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import mai_onsyn.renderer.utils.fixedFrame

class GL4DRegion(
    val scene3D: SimpleScene3D = SimpleScene3D(),
    val scene4D: SimpleScene4D = SimpleScene4D()
) : GL3DRegion(scene3D) {

    private val renderer = Renderer4D(scene4D, scene3D)

    private var enable4DInput = false
    private val movement = MovementState()

    private var switchPressed = false

    var maxFPS: Int
        get() = renderer.maxFPS
        set(value) {
            renderer.maxFPS = value
        }

    fun get4DFPS(): Float = renderer.fpsCounter.getAverageFrequency()

    fun get4D1PercentLowFPS(): Float = renderer.fpsCounter.getOnePercentLowFrequency()

    fun setViewPortLength(length: Float) { renderer.viewPortLength = length }

    init {
        renderer.start()

        this.addEventHandler(KeyEvent.KEY_PRESSED) {
            if (it.code == KeyCode.I) {
                this.enableInput = !this.enableInput
                enable4DInput = !this.enableInput
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

    private fun startKeyEventHandlerThread() {
        Thread.ofVirtual().name("4D Region Key Event Handler").start {
            val moveSpeed = 0.004f
            val mouseSpeed = 0.0005f
            fixedFrame(1000, { !Thread.currentThread().isInterrupted }) {
                if (!enable4DInput) {
                    Thread.sleep(500)
                    return@fixedFrame
                }

                val deltaX = moveSpeed * (movement.right - movement.left)
                val deltaY = moveSpeed * (movement.up - movement.down)
                val deltaZ = moveSpeed * (movement.ana - movement.negAna)
                val deltaW = moveSpeed * (movement.forward - movement.back)

                if (deltaX != 0f) scene4D.camera4D.moveRight(deltaX)
                if (deltaY != 0f) scene4D.camera4D.moveUp(deltaY)
                if (deltaZ != 0f) scene4D.camera4D.moveAna(deltaZ)
                if (deltaW != 0f) scene4D.camera4D.moveForward(deltaW)

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