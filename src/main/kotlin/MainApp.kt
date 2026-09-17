import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
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
        val box = Column()
        box.modifier = modifier
            .fillMaxSize()

        val scene = SimpleScene3D()
        scene.meshList.addAll(listOf(makeTestMesh(), OBJLoader.load("D:\\Users\\Desktop\\Files\\Projects\\Cpp\\Renderer4\\assets\\meshes\\Sponza Palace\\scene.obj")))
        box.add(Label("3D Space"), modifier.align(Alignment.Horizontal.CENTER))
        box.add(GL3DRegion(scene), modifier.fillMaxWidth().weight(1.0))

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