import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import mai_onsyn.jfx_tools.layout.Box
import mai_onsyn.jfx_tools.layout.modifier
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.generator.constructBall4
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.generator.CubeFace
import mai_onsyn.renderer.ogl3d.generator.createCube
import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.utils.ColorARGB
import mai_onsyn.renderer.utils.fixedFrame

class GPU4DTest : Application() {
    override fun start(stage: Stage) {
        val box = Box()

        val region = GL4DRegion()
        region.scene4D.meshList.add(Mesh4D(constructBall4(
            density = 12f
        )))
        region.set4DOutlineRendering(true)
        region.scene4D.camera4D.moveForward(-5f)
        box.add(region, modifier.fillMaxSize())

        region.scene3D.meshList.add(Mesh(createCube(colorOf = { face ->
            when (face) {
                CubeFace.TOP    -> ColorARGB(1f, 0f, 0f, 0.1f)
                CubeFace.BOTTOM -> ColorARGB(0f, 1f, 0f, 0.1f)
                CubeFace.LEFT   -> ColorARGB(0f, 0f, 1f, 0.1f)
                CubeFace.RIGHT  -> ColorARGB(1f, 1f, 0f, 0.1f)
                CubeFace.FRONT  -> ColorARGB(0f, 1f, 1f, 0.1f)
                CubeFace.BACK   -> ColorARGB(1f, 0f, 1f, 0.1f)
            }
        }, size = 8f)))

        Thread.ofVirtual().start {
            fixedFrame(1, { !Thread.currentThread().isInterrupted }) {
                println("3D: %.2f FPS, 4D: %.2f FPS".format(region.newFPSCounter.getAverageFrequency(), region.get4DFPS()))
            }
        }

        stage.scene = Scene(box, 800.0, 600.0)
        stage.show()
    }
}

fun main() {
    Application.launch(GPU4DTest::class.java)
}