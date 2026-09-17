import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage
import mai_onsyn.jfx_tools.layout.Alignment
import mai_onsyn.jfx_tools.layout.Box
import mai_onsyn.jfx_tools.layout.Column
import mai_onsyn.jfx_tools.layout.modifier
import mai_onsyn.renderer.data.ColorARGB
import mai_onsyn.renderer.data.Mesh
import mai_onsyn.renderer.data.OBJLoader
import mai_onsyn.renderer.data.SimpleScene3D
import mai_onsyn.renderer.data.Triangle
import mai_onsyn.renderer.data.Vertex
import mai_onsyn.renderer.ogl3d.GL3DRegion
import org.joml.Vector2f
import org.joml.Vector3f

class MainApp : Application() {
    override fun start(stage: Stage?) {
        stage!!
        val box = Box()

        val scene = SimpleScene3D()
        scene.meshList.addAll(listOf(makeTestMesh(), OBJLoader.load("D:\\Users\\Desktop\\Files\\Projects\\Cpp\\Renderer4\\assets\\meshes\\mika\\mika test.obj")))
        val gL3DRegion = GL3DRegion(scene)
        box.add(gL3DRegion, modifier.fillMaxSize())
        val infoColumn = Column()
        val fpsLabel = Label("FPS")
        val low1percentLabel = Label("Low 1%")
        infoColumn.add(fpsLabel)
        infoColumn.add(low1percentLabel)

        Thread.ofVirtual().start {
            fpsLabel.textFill = Color.WHITE
            low1percentLabel.textFill = Color.WHITE
            fpsLabel.font = Font(18.0)
            low1percentLabel.font = Font(18.0)
            while (!Thread.currentThread().isInterrupted) {
                Platform.runLater {
                    fpsLabel.text = "FPS = %.2f".format(gL3DRegion.newFPSCounter.getAverageFrequency())
                    low1percentLabel.text = "1%% Low PFS = %.2f".format(gL3DRegion.newFPSCounter.getOnePercentLowFrequency())
                }
                Thread.sleep(1000)
            }
        }

        box.add(infoColumn, modifier.padding(top = 24.0, left = 24.0))

        stage.scene = Scene(box, 640.0, 480.0)

        stage.show()
    }
}

fun makeTestMesh(): Mesh {
    val mesh = Mesh()
    val triangle = Triangle(
        Vertex(Vector3f(0.6f, -0.5f, 4f), ColorARGB(0x8FF0000u), Vector3f(-1f, 0f, 0f), Vector2f(0f, 0f)),
        Vertex(Vector3f(-0.6f, -0.5f, 4f), ColorARGB(0xFF00FF00u), Vector3f(-1f, 0f, 0f), Vector2f(0f, 0f)),
        Vertex(Vector3f(0f, 0.5f, 4f), ColorARGB(0xFF0000FFu), Vector3f(-1f, 0f, 0f), Vector2f(0f, 0f))
    )
    mesh.triangles.add(triangle)
    return mesh
}