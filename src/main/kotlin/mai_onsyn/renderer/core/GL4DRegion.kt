package mai_onsyn.renderer.core

import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import mai_onsyn.renderer.cpu4dkt.Renderer4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.ogl3d.GL3DRegion
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import mai_onsyn.renderer.utils.fixedFrame
import kotlin.math.roundToLong

class GL4DRegion(
    val scene3d: SimpleScene3D = SimpleScene3D(),
    val scene4D: SimpleScene4D = SimpleScene4D()
) : GL3DRegion(scene3d) {

    private val renderer = Renderer4D(scene4D, scene3d)

    private var enable4DInput = false
    private val movement = MovementState()

    init {
        renderer.start()

        this.addEventFilter(KeyEvent.KEY_PRESSED) {
            if (it.code == KeyCode.I) {
                this.enableInput = !this.enableInput
                enable4DInput = !this.enableInput
            }

            if (!enable4DInput) return@addEventFilter

            when (it.code) {
                KeyCode.W -> movement.forward = 1f
                KeyCode.S -> movement.back = 1f
                KeyCode.A -> movement.left = 1f
                KeyCode.D -> movement.right = 1f
                KeyCode.SPACE -> movement.up = 1f
                KeyCode.SHIFT -> movement.down = 1f
                KeyCode.E -> movement.ana = 1f
                KeyCode.Q -> movement.negAna = 1f
                else -> {}
            }
        }
        
        this.addEventFilter(KeyEvent.KEY_RELEASED) {
            when (it.code) {
                KeyCode.W -> movement.forward = 0f
                KeyCode.S -> movement.back = 0f
                KeyCode.A -> movement.left = 0f
                KeyCode.D -> movement.right = 0f
                KeyCode.SPACE -> movement.up = 0f
                KeyCode.SHIFT -> movement.down = 0f
                KeyCode.E -> movement.ana = 0f
                KeyCode.Q -> movement.negAna = 0f
                else -> {}
            }
        }

        startKeyEventHandlerThread()
    }

    private fun startKeyEventHandlerThread() {
        Thread.ofVirtual().name("4D Region Key Event Handler").start {
            val moveSpeed = 0.001f
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
    var zw: Float = 0f
)