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
import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.cpu4dkt.*
import mai_onsyn.renderer.cpu4dkt.generator.constructHypercube
import mai_onsyn.renderer.cpu4dkt.generator.constructHypercubeWithCellColors
import mai_onsyn.renderer.utils.OBJLoader
import mai_onsyn.renderer.interfaces.RendererInterface
import mai_onsyn.renderer.ogl3d.GL3DRegion
import mai_onsyn.renderer.ogl3d.data.*
import mai_onsyn.renderer.ogl3d.generator.CubeFace
import mai_onsyn.renderer.ogl3d.generator.createCube
import mai_onsyn.renderer.utils.ColorARGB
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import mai_onsyn.renderer.utils.fixedFrame
import mai_onsyn.renderer.utils.toRowMajorFloatArray
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.concurrent.locks.LockSupport
import kotlin.math.sin

class MainApp : Application() {
    override fun start(stage: Stage?) {
        stage!!
        start4DTest2(stage)
//        start3DTest(stage)
    }
}

fun start4DTest2(stage: Stage) {
    val box = Box()

    val region = GL4DRegion()
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

    val pos3Label = Label("pos")
    val pos4Label = Label("pos")
    val cam4Label = Label("cam")
    pos3Label.font = Font(18.0)
    pos4Label.font = Font(18.0)
    cam4Label.font = Font(18.0)
    pos3Label.textFill = Color.WHITE
    pos4Label.textFill = Color.WHITE
    cam4Label.textFill = Color.WHITE
    Thread.ofVirtual().start {
        while (!Thread.currentThread().isInterrupted) {
            Platform.runLater {
                val pos3 = region.scene3D.getCamera().pos
                val pos4 = region.scene4D.getCamera().pos
                pos3Label.text = "3D Pos = (%.2f, %.2f, %.2f)".format(pos3.x, pos3.y, pos3.z)
                pos4Label.text = "4D Pos = (%.2f, %.2f, %.2f, %.2f)".format(pos4.x, pos4.y, pos4.z, pos4.w)
                cam4Label.text = "4D Cam = ${region.scene4D.getCamera().getCameraOrientation()}"
            }
            Thread.sleep(100)
        }
    }

    box.add(region, modifier.fillMaxSize())
    val column = Column()
    column.add(pos3Label)
    column.add(pos4Label)
    column.add(cam4Label)
    box.add(column, modifier.padding(top = 16.0, left = 16.0))

    RendererInterface.init(region)

    val i = RendererInterface.INSTANCE
    i.camera.moveForward(-5f)
    i.scene.enableLightRendering(false)
    i.scene.enableTriangleLineRendering(true)

    i.model.createModel("TestModel")
    i.shape.createTesseract("TestModel", Vector4f(0f, 0f, 0f, 0f), 1f)
    i.shape.createTesseract("TestModel", Vector4f(1f, 0f, 0f, 0f), 1f)
    i.shape.createTesseract("TestModel", Vector4f(-1f, 0f, 0f, 0f), 1f)
    i.shape.createTesseract("TestModel", Vector4f(0f, 0f, 0f, 1f), 1f)
    i.shape.createTesseract("TestModel", Vector4f(0f, 1f, 0f, 0f), 1f)
//    i.transform.setCoordinate("TestModel", Vector4f(0f, 0f, 0f, 0f), Coordinate4D())
    Thread.ofVirtual().start {
        fixedFrame(100, { !Thread.currentThread().isInterrupted }) {
            i.transform.rotate("TestModel", Direction.Plane.ZW, 0.25f)
//            i.transform.rotate("TestModel", Direction.Plane.YW, 0.25f)
        }
    }

    println(i.geometry.getModelInfos("TestModel"))

    stage.scene = Scene(box, 800.0, 600.0)
    stage.show()
}

fun start4DTest(stage: Stage) {
    val box = Box()

    val scene3d = SimpleScene3D()
    val scene4d = SimpleScene4D()
    scene3d.addMesh(makeTestMesh())
    scene4d.meshList.add(Mesh4D(constructHypercubeWithCellColors(edgeLength = 1f)))

    val gl4dRegion = GL4DRegion(scene3d, scene4d)
    box.add(gl4dRegion, modifier.fillMaxSize())

    gl4dRegion.setOutlineRendering(true)

//    gl3dRegion.addEventFilter(KeyEvent.KEY_PRESSED) {
//        when (it.code) {
//            KeyCode.I -> gl3dRegion.enableInput = !gl3dRegion.enableInput
//            else -> {}
//        }
//    }

//    val renderer4D = Renderer4D(scene4d, scene3D)
//    renderer4D.start()
    gl4dRegion.scene4D.getCamera().startTestTrajectory()

    stage.scene = Scene(box, 800.0, 600.0)
    stage.show()
}

fun start3DTest(stage: Stage) {
    val box = Box()

    val scene = SimpleScene3D()
    scene.addMesh(makeTestMesh())
    scene.addMesh(makeTest4DMesh())
    scene.addMesh(OBJLoader.load("D:\\Users\\Desktop\\Files\\Projects\\Cpp\\Renderer4\\assets\\meshes\\mika\\mika test.obj").apply { transform.move(Vector3f(0f, 0f, 15f)) })
    scene.addMesh(OBJLoader.load("D:\\Users\\Desktop\\Files\\Projects\\Cpp\\Renderer4\\assets\\meshes\\Sponza Palace\\scene.obj").apply { transform.move(Vector3f(30f, 0f, 0f)) })

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
                low1percentLabel.text =
                    "1%% Low PFS = %.2f".format(gL3DRegion.newFPSCounter.getOnePercentLowFrequency())
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

    scene.addLight(Light("Green", Vector3f(0f, 0f, 0f), ColorARGB(1f, 1f, 1f)))

    stage.scene = Scene(box, 640.0, 480.0)
    stage.show()
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
    val outArray = JNIRasterizer.project(flattened, cube.tetrahedrons.size, I, camera.viewMatrix.data, camera.projectionMatrix().data,
        Matrix4f().scale(5f).toRowMajorFloatArray())
    val tetrahedrons = JNIRasterizer.extractTetrahedrons(outArray)
    return tetrahedrons.toMesh()
}

fun Camera4D.startTestTrajectory(
    speed: Float = 1f,             // 速度倍率，1f = 12 秒一圈
    radius: Float = 8f,            // 相机到原点的距离（超立方体半边长 ≤2.5，8 足够远）
    secondsPerLoop: Float = 12f,   // speed=1 时一圈的秒数
): Thread = Thread.ofVirtual().name("camera4d-test").start {
    val twoPi  = (2.0 * Math.PI).toFloat()
    val dtMs   = 4L
    val dt     = dtMs / 1000f
    val omega  = twoPi / secondsPerLoop
    val tickNs = dtMs * 1_000_000L

    var u    = 0f
    var next = System.nanoTime() + tickNs

    while (!Thread.currentThread().isInterrupted) {

        // 1) 从单位阵重建 —— 不做任何"累加"，无漂移
        vx.set(1f, 0f, 0f, 0f)
        vy.set(0f, 1f, 0f, 0f)
        vz.set(0f, 0f, 1f, 0f)
        vw.set(0f, 0f, 0f, 1f)

        // 2) 六个平面各转一点，绝对角度，sin 波形保证 2π 严格闭合
        rotateXY(0.20f * sin(u))
        rotateXZ(0.50f * sin(u + 1.5f))
        rotateXW(0.40f * sin(u + 3.0f))
        rotateYZ(0.45f * sin(u + 2.3f))
        rotateYW(0.35f * sin(u + 4.1f))
        rotateZW(0.30f * sin(u + 5.6f))

        // 3) 定位 —— 相机永远在 -vw 方向 radius 处
        //    pos = -radius * vw  ⇒  从 pos 指向原点的方向 = vw
        //    而 vw 是相机看向方向 ⇒ 视线永远穿过 (0,0,0,0)
        pos.set(-vw.x * radius, -vw.y * radius, -vw.z * radius, -vw.w * radius)

        u += speed * omega * dt
        if (u >= twoPi) u -= twoPi

        next += tickNs
        val wait = next - System.nanoTime()
        if (wait > 0) LockSupport.parkNanos(wait)
        else next = System.nanoTime() + tickNs
    }
}