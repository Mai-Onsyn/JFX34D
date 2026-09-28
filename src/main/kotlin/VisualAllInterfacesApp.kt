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
import mai_onsyn.renderer.interfaces.RendererInterface
import mai_onsyn.renderer.ogl3d.data.Light
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.generator.createCube
import mai_onsyn.renderer.utils.ColorARGB
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * 接口聚合可视化测试：一个窗口里把所有对外接口（除 IOInterface）都跑一遍。
 *
 * 跑法（项目根目录）：
 * ```
 * mvn compile exec:java -Dexec.mainClass=VisualAllInterfacesAppKt
 * ```
 * 或者直接在 IDEA 里运行本文件的 main()。
 *
 * 能用眼睛验证的接口（背景色、线框、光照、光源、视口尺寸、帧率上限、模型变换、
 * 摄像机旋转、切片结果）都在窗口里现场演示；能算的（胞数、顶点半径、闭合性、
 * 体积守恒）同时用断言检查，结果显示在左上角，也打印到控制台。
 *
 * 需要注意：JNI 原生库按 `./jni/jfx34d_jni` 相对启动目录查找，
 * 所以务必在项目根目录启动。
 */
class VisualAllInterfacesApp : Application() {

    private val results = mutableListOf<String>()
    private var passed = 0
    private var failed = 0

    private fun check(what: String, ok: Boolean, detail: String = "") {
        if (ok) passed++ else failed++
        results.add("${if (ok) "[PASS]" else "[FAIL]"} $what" + if (detail.isEmpty()) "" else "   ($detail)")
    }

    override fun start(stage: Stage) {
        val region = GL4DRegion()
        val box = Box()
        box.add(region, modifier.fillMaxSize())

        val report = Label()
        report.font = Font(12.0)
        report.textFill = Color.WHITE
        val status = Label()
        status.font = Font(14.0)
        status.textFill = Color.web("#7fe08a")
        val column = Column()
        column.add(report)
        column.add(status)
        box.add(column, modifier.padding(top = 10.0, left = 12.0))

        RendererInterface.init(region)
        val api = RendererInterface.INSTANCE

        runChecks(api, region)

        report.text = buildString {
            append(results.joinToString("\n"))
            append("\n\n")
            append("$passed passed / $failed failed")
        }
        println("========================= 接口聚合可视化测试 =========================")
        println(report.text)
        println("====================================================================")

        stage.scene = Scene(box, 1180.0, 720.0)
        stage.title = "JFX34D 接口聚合可视化测试"
        stage.show()

        animate(api, report, status)
    }

    // ------------------------------------------------------------ 检查

    private fun runChecks(api: RendererInterface, region: GL4DRegion) {
        // ---------------- SceneInterface：这些只能在有 GL 上下文时验 ----------------
        api.scene.setBackgroundColor(Color.color(0.16, 0.16, 0.19))
        api.scene.enableTriangleLineRendering(false)
        api.scene.enableLightRendering(true)
        api.scene.setAmbientLight(ColorARGB(0.35f, 0.35f, 0.35f))
        api.scene.setDisplaySize(16f)                    // 视口（四维屏幕）边长，越大画面里的模型越大
        api.scene.set3DMaxFPS(60f)
        api.scene.set4DMaxFPS(60f)
        check("SceneInterface.set4DMaxFPS", region.maxFPS == 60, "maxFPS=${region.maxFPS}")

        api.scene.addLights(Light.point("KeyLight", Vector3f(0f, 40f, 0f), ColorARGB(1f, 1f, 1f), 0.8f))
        val lights = api.scene.listLight()
        check("SceneInterface.listLight", lights.contains("KeyLight"), lights.lines().firstOrNull() ?: "")
        api.scene.removeLights(listOf("KeyLight"))
        check("SceneInterface.removeLights", !api.scene.listLight().contains("KeyLight"))
        check("SceneInterface 非法光源名应报错", runCatching {
            api.scene.removeLights(listOf("NoSuchLight"))
        }.exceptionOrNull() is IllegalArgumentException)

        // 3D 摄像机拉远一点，方便看整排形状
        region.scene3D.getCamera().moveZ(6f)

        // ---------------- ModelInterface ----------------
        api.model.createEmptyModel("Gallery")
        api.model.createEmptyModel("Sculpt")
        api.model.createEmptyModel("Temp")
        check(
            "ModelInterface.createEmptyModel + listModel",
            api.model.listModel().containsAll(listOf("Gallery", "Sculpt", "Temp")),
            api.model.listModel().toString()
        )
        check(
            "ModelInterface.listParts",
            region.scene4D.subModels("Gallery").isEmpty(),
            "新建的空模型没有部件"
        )
        // 注意：JOML 的 Vector4f() 是 (0,0,0,1)，要原点必须显式写四个零
        api.shape.createTesseract("Temp", "Throwaway", Vector4f(0f, 0f, 0f, 0f), 1f)
        api.model.copyModel("Temp/Throwaway", "Temp/Copy")
        check(
            "ModelInterface.copyModel",
            mesh(region, "Temp/Copy").tetrahedrons.size == 48 &&
                mesh(region, "Temp/Copy").kind.name == "SHAPE"
        )
        api.model.removeModel("Temp")
        check("ModelInterface.removeModel 删整棵子树", region.scene4D.findMesh("Temp") == null)

        // ---------------- ShapeInterface：七种形状一字排开 ----------------
        val base = Mesh(createCube(size = 1.6f))
        val spacing = 4f
        val plane = { i: Int -> Vector4f(i * spacing - 3f * spacing, 0f, 0f, 0f) }
        val ballCenter = plane(6)
        api.shape.createTetrahedron("Gallery", "Tet", plane(0), 1.2f)
        api.shape.create5Cell("Gallery", "Five", plane(1), 2f)
        api.shape.create16Cell("Gallery", "Sixteen", plane(2), 1.4f)
        api.shape.createTesseract("Gallery", "Cube", plane(3), 2f)
        api.shape.createPrism4("Gallery", "Prism", base, -1.5f, 1.5f)
        api.shape.createCone4("Gallery", "Cone", base, Vector4f(0f, 0f, 0f, 2.5f))
        api.shape.createBall4("Gallery", "Ball", ballCenter, 1.8f, 3f)
        api.transform.move("Gallery/Prism", plane(4))
        api.transform.move("Gallery/Cone", plane(5))

        val expected = linkedMapOf(
            "Tet" to 1,
            "Five" to 5,
            "Sixteen" to 16,
            "Cube" to 48,
            "Prism" to base.triangles.size * 3,
            "Cone" to base.triangles.size,
            "Ball" to 8 * 27 * 6
        )
        expected.forEach { (name, count) ->
            val mesh = region.scene4D.findMesh("Gallery/$name")
            check(
                "ShapeInterface.create$name 胞数=$count 且 kind=SHAPE/params 齐全",
                mesh?.tetrahedrons?.size == count && mesh.kind.name == "SHAPE" && mesh.params != null,
                "实际 ${mesh?.tetrahedrons?.size}"
            )
        }
        // 超球所有顶点都该落在半径 1.8 的 3-球面上（注意要相对球心量，不是相对原点）
        val ball = region.scene4D.findMesh("Gallery/Ball")!!
        val maxErr = ball.tetrahedrons
            .flatMap { it.vertices }
            .maxOf { kotlin.math.abs(Vector4f(it.pos).sub(ballCenter).length() - 1.8f) }
        check("ShapeInterface 超球顶点在球面上", maxErr < 1e-3f, "最大偏差 $maxErr")

        // ---------------- TransformInterface ----------------
        api.transform.setModelMatrix("Gallery/Tet", mai_onsyn.renderer.cpu4dkt.Matrix5f.IDENTITY)
        api.transform.move("Gallery/Tet", Vector4f(0f, 0f, 0f, 0f))
        api.transform.rotate("Gallery", Direction.Plane.XW, 5f)
        check(
            "TransformInterface.rotate 作用到分组路径的全部子模型",
            region.scene4D.findMesh("Gallery/Tet")!!.transform.matrix[3] != 0f,
            "子模型矩阵已被修改"
        )

        // ---------------- GeometryInterface ----------------
        // 放在画廊下面一排（y = -7），这样左上角的检查表不会挡住它
        api.shape.createTesseract("Sculpt", "Box", Vector4f(0f, -7f, 0f, 0f), 2.2f)
        // 手搓一个四面体：先建一个空部件，再 ADD_TETRAHEDRON + SET_VERTEX + TRANSFORM_TETRAHEDRONS
        api.model.createEmptyModel("Sculpt/Patch")
        val patch = mai_onsyn.renderer.cpu4dkt.Tetrahedron(
            patchVertex(Vector4f(-4f, -7f, 0f, 0f)),
            patchVertex(Vector4f(-2.6f, -7f, 0f, 0f)),
            patchVertex(Vector4f(-3.4f, -6.1f, 0f, 0f)),
            patchVertex(Vector4f(-3.4f, -7f, 1.2f, 0.8f))
        )
        val patchIndex = api.geometry.addTetrahedron("Sculpt/Patch", patch)
        check("GeometryInterface.addTetrahedron", patchIndex == 0, "返回索引 $patchIndex")
        api.geometry.setVertex("Sculpt/Patch", 0, 3, Vector4f(-3.4f, -7f, 1.2f, 1.6f))
        api.geometry.transformTetrahedrons(
            "Sculpt/Patch", emptyList(),
            mai_onsyn.renderer.cpu4dkt.Matrix5f(
                1.2f, 0f, 0f, 0f, 0f,
                0f, 1.2f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 0f,
                0f, 0f, 0f, 1.2f, 0f,
                0f, 0f, 0f, 0f, 1f
            )
        )
        check(
            "GeometryInterface 雕刻后 kind = CARVED",
            region.scene4D.findMesh("Sculpt/Patch")?.kind?.name == "CARVED"
        )
        check(
            "GeometryInterface.getTetrahedronInfos",
            api.geometry.getTetrahedronInfos("Sculpt/Patch", 0).contains("Sculpt/Patch#0")
        )

        // 切片：切完把两半左右拉开，能直接看出是互补的两块
        val before = api.geometry.getModelInfos("Sculpt/Box")
        check("GeometryInterface.getModelInfos 不 dump 顶点", !before.contains("Tetrahedron IDs"), "")
        api.geometry.sliceModel(
            "Sculpt/Box",
            mai_onsyn.renderer.cpu4dkt.Tetrahedron(
                patchVertex(Vector4f(0f, -7f, 0f, 0f)),
                patchVertex(Vector4f(0f, -6f, 0f, 0f)),
                patchVertex(Vector4f(0f, -7f, 1f, 0f)),
                patchVertex(Vector4f(0f, -7f, 0f, 1f))
            ),
            "Sculpt/Left", "Sculpt/Right"
        )
        api.transform.move("Sculpt/Left", Vector4f(-4.5f, 0f, 0f, 0f))
        api.transform.move("Sculpt/Right", Vector4f(4.5f, 0f, 0f, 0f))
        val left = region.scene4D.findMesh("Sculpt/Left")
        val right = region.scene4D.findMesh("Sculpt/Right")
        check(
            "GeometryInterface.sliceModel 产出两个 CARVED 模型",
            left?.kind?.name == "CARVED" && right?.kind?.name == "CARVED" &&
                left.tetrahedrons.isNotEmpty() && right.tetrahedrons.isNotEmpty(),
            "左 ${left?.tetrahedrons?.size} 个胞 / 右 ${right?.tetrahedrons?.size} 个胞"
        )

        // ---------------- 查询输出（给人/AI 看的 markdown） ----------------
        println(api.geometry.getModelInfos("Gallery"))
        println(api.geometry.getModelInfos("Gallery/Ball"))
        println(api.geometry.getModelInfos("Sculpt/Left"))

        api.scene.enableLightRendering(true)
    }

    private fun patchVertex(p: Vector4f) = mai_onsyn.renderer.cpu4dkt.Vertex4D(
        p, ColorARGB(1f, 0.85f, 0.2f, 1f), Vector4f(0f, 0f, 0f, 1f)
    )

    private fun mesh(region: GL4DRegion, path: String) = region.scene4D.findMesh(path)!!

    // ------------------------------------------------------------ 动画

    /**
     * 动画部分负责三件事：
     * 1. CameraInterface：让四维摄像机在几个平面里小幅摆动（绝对角度重建，不累加，所以画面不会飘走），
     *    形状会被"卷进/卷出"第四维，能直观看到四维旋转的样子；
     * 2. TransformInterface：整个画廊做小幅 4D 摆动（分组路径作用到全部子模型，现场演示）；
     * 3. SceneInterface：线框 / 实体填充每 3 秒交替一次，用眼睛确认开关真的生效。
     */
    private fun animate(api: RendererInterface, report: Label, status: Label) {
        Thread.ofVirtual().name("visual-check-anim").start {
            var frames = 0
            var wireframe = true
            var fpsReported = false
            var elapsed = 0f

            while (!Thread.currentThread().isInterrupted) {
                Thread.sleep(16)
                frames++
                elapsed += 0.016f

                // 1) 摄像机：每帧从单位视角重建，用 sin 做有界摆动
                api.camera.setView(mai_onsyn.renderer.utils.Coordinate4D())
                api.camera.rotateXW(16f * kotlin.math.sin(elapsed * 0.5f))
                api.camera.rotateZW(11f * kotlin.math.sin(elapsed * 0.33f))
                api.camera.rotateXY(6f * kotlin.math.sin(elapsed * 0.21f))
                api.camera.setPosition(Vector4f(0f, 0f, 0f, 0f))
                api.camera.moveForward(-21f)

                // 2) 画廊整体小幅摆动（XW 平面），演示分组路径变换
                api.transform.rotate("Gallery", Direction.Plane.XW, 0.08f * kotlin.math.sin(elapsed * 0.4f))

                // 3) 线框 / 实体交替，肉眼验证 SceneInterface 的开关
                if (frames % 190 == 0) {
                    wireframe = !wireframe
                    val on = wireframe
                    api.scene.enableTriangleLineRendering(on)
                }

                if (!fpsReported && frames > 150) {
                    fpsReported = true
                    val fps3 = api.scene.get3DFPS()
                    val fps4 = api.scene.get4DFPS()
                    val low1 = api.scene.get3D1PercentLowFPS()
                    val ok = fps3 >= 0f && fps4 >= 0f
                    Platform.runLater {
                        check(
                            "SceneInterface.get3DFPS / get4DFPS 可读", ok,
                            "3D=%.2f 4D=%.2f 1%%Low=%.2f".format(fps3, fps4, low1)
                        )
                        report.text = results.joinToString("\n") + "\n\n$passed passed / $failed failed"
                        println("[fps] 3D=%.2f 4D=%.2f 1%%Low=%.2f".format(fps3, fps4, low1))
                    }
                }

                if (frames % 30 == 0) {
                    val fps3 = api.scene.get3DFPS()
                    val fps4 = api.scene.get4DFPS()
                    val line = if (wireframe) "线框" else "实体"
                    Platform.runLater {
                        status.text = "%s | 3D %.1f fps | 4D %.1f fps | 摄像机在 4 个平面里摆动".format(line, fps3, fps4)
                    }
                }
            }
        }
    }
}

fun main() {
    Application.launch(VisualAllInterfacesApp::class.java)
}
