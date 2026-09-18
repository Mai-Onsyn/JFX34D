import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage
import mai_onsyn.jfx_tools.layout.Box
import mai_onsyn.jfx_tools.layout.Column
import mai_onsyn.jfx_tools.layout.modifier
import mai_onsyn.renderer.cpu4dkt.*
import mai_onsyn.renderer.ogl3d.GL3DRegion
import mai_onsyn.renderer.ogl3d.data.*
import mai_onsyn.renderer.utils.toRowMajorFloatArray
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class MainApp : Application() {
    override fun start(stage: Stage?) {
        stage!!
        val box = Box()

        val scene = SimpleScene3D()
        scene.meshList.addAll(listOf(makeTestMesh(), makeTest4DMesh()))
//        scene.meshList.add(OBJLoader.load("D:\\Users\\Desktop\\Files\\Projects\\Cpp\\Renderer4\\assets\\meshes\\mika\\mika test.obj"))

        val gL3DRegion = GL3DRegion(scene)
        box.add(gL3DRegion, modifier.fillMaxSize())

        val infoColumn = Column()
        val fpsLabel = Label("FPS")
        val low1percentLabel = Label("Low 1%")
        val posLabel = Label("Pos")
        infoColumn.add(fpsLabel)
        infoColumn.add(low1percentLabel)
        infoColumn.add(posLabel)

        Thread.ofVirtual().start {
            fpsLabel.textFill = Color.WHITE
            low1percentLabel.textFill = Color.WHITE
            posLabel.textFill = Color.WHITE
            fpsLabel.font = Font(18.0)
            low1percentLabel.font = Font(18.0)
            posLabel.font = Font(18.0)
            while (!Thread.currentThread().isInterrupted) {
                Platform.runLater {
                    fpsLabel.text = "FPS = %.2f".format(gL3DRegion.newFPSCounter.getAverageFrequency())
                    low1percentLabel.text = "1%% Low PFS = %.2f".format(gL3DRegion.newFPSCounter.getOnePercentLowFrequency())
                }
                Thread.sleep(1000)
            }
        }
        Thread.ofVirtual().start {
            while (!Thread.currentThread().isInterrupted) {
                Platform.runLater {
                    val pos = gL3DRegion.scene.getCamera().pos
                    posLabel.text = "Pos = (%.2f, %.2f, %.2f)".format(pos.x, pos.y, pos.z)
                }
                Thread.sleep(50)
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
        Vertex(Vector3f(0.6f, -0.5f, 4f), ColorARGB(r = 1f), Vector3f(-1f, 0f, 0f), Vector2f(0f, 0f)),
        Vertex(Vector3f(-0.6f, -0.5f, 4f), ColorARGB(g = 1f), Vector3f(-1f, 0f, 0f), Vector2f(0f, 0f)),
        Vertex(Vector3f(0f, 0.5f, 4f), ColorARGB(b = 1f), Vector3f(-1f, 0f, 0f), Vector2f(0f, 0f))
    )
    mesh.triangles.add(triangle)
    return mesh
}

fun makeTest4DMesh(): Mesh {
    val cube = Mesh4D(constructHypercube(edgeLength = 4f))
    val flattened = JNIRasterizer.packMesh4D(cube)
    val camera = Camera4D(pos = Vector4f(8f, 0f, 0f, -15f))
    val I = Matrix5f.IDENTITY.data
    val outArray = JNIRasterizer.process(flattened, cube.tetrahedrons.size, I, camera.viewMatrix.data, camera.projectionMatrix().data,
        Matrix4f().scale(5f).toRowMajorFloatArray())
    val tetrahedrons = JNIRasterizer.extractTetrahedrons(outArray)
    return tetrahedrons.toMesh()
}

fun List<Tetrahedron3D>.toMesh(): Mesh {
    val mesh = Mesh()
    this.forEach {
        mesh.triangles.add(Triangle(it.v0, it.v1, it.v2))
        mesh.triangles.add(Triangle(it.v0, it.v1, it.v3))
        mesh.triangles.add(Triangle(it.v0, it.v2, it.v3))
        mesh.triangles.add(Triangle(it.v1, it.v2, it.v3))
    }
    return mesh
}