import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.MeshKind
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vector5f
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.interfaces.impl.CameraInterfaceImpl
import mai_onsyn.renderer.interfaces.impl.GeometryInterfaceImpl
import mai_onsyn.renderer.interfaces.impl.ModelInterfaceImpl
import mai_onsyn.renderer.interfaces.impl.ShapeInterfaceImpl
import mai_onsyn.renderer.interfaces.impl.TransformInterfaceImpl
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.generator.createCube
import mai_onsyn.renderer.utils.ColorARGB
import mai_onsyn.renderer.utils.Coordinate4D
import mai_onsyn.renderer.utils.Direction
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 聚合测试：把所有对外接口（除 IOInterface，按约定不实现）跑一遍。
 *
 * 只能靠窗口看出来的部分（背景色 / 线框 / 光照 / 光源 / 视口尺寸 / 帧率上限）
 * 依赖 GL 上下文和 JavaFX，单元测试里造不出 GL4DRegion，放到
 * `VisualAllInterfacesApp` 那个可视化测试里做（那边同样会断言并打印结果）。
 * 其余接口全部在这里用数学方法验证。
 */
class AllInterfacesTest {

    // ------------------------------------------------------------ 测试脚手架

    private class Api {
        val scene = SimpleScene4D()
        val model = ModelInterfaceImpl(scene)
        val shape = ShapeInterfaceImpl(scene)
        val transform = TransformInterfaceImpl(scene)
        val geometry = GeometryInterfaceImpl(scene)
        val camera = CameraInterfaceImpl(scene.camera4D)

        fun mesh(path: String): Mesh4D = scene.findMesh(path) ?: fail("模型 \"$path\" 不存在")
        fun tets(path: String): List<Tetrahedron> = mesh(path).tetrahedrons
        fun positions(path: String): List<Vector4f> = tets(path).flatMap { it.vertices }.map { it.pos }
        fun uniquePositions(path: String): List<Vector4f> = positions(path).distinctBy { posKey(it) }
    }

    // ------------------------------------------------------------ 摄像机

    @Test
    fun cameraInterface() {
        val cam = Api().camera

        // 位置读写
        assertVec(Vector4f(0f, 0f, 0f, 0f), cam.getPosition(), "初始位置")
        cam.setPosition(Vector4f(1f, 2f, 3f, 4f))
        assertVec(Vector4f(1f, 2f, 3f, 4f), cam.getPosition(), "setPosition")

        // 移动：沿当前摄像机基向量
        cam.setPosition(zero4())
        cam.moveRight(2f)
        assertVec(Vector4f(2f, 0f, 0f, 0f), cam.getPosition(), "moveRight")
        cam.moveUp(3f)
        assertVec(Vector4f(2f, 3f, 0f, 0f), cam.getPosition(), "moveUp")
        cam.moveAna(-1f)
        assertVec(Vector4f(2f, 3f, -1f, 0f), cam.getPosition(), "moveAna")
        cam.moveForward(5f)
        assertVec(Vector4f(2f, 3f, -1f, 5f), cam.getPosition(), "moveForward")

        // 视角读写
        cam.setView(Coordinate4D())
        assertVec(Vector4f(1f, 0f, 0f, 0f), cam.getView().vx, "初始 vx")
        assertVec(Vector4f(0f, 1f, 0f, 0f), cam.getView().vy, "初始 vy")
        assertVec(Vector4f(0f, 0f, 1f, 0f), cam.getView().vz, "初始 vz")
        assertVec(Vector4f(0f, 0f, 0f, 1f), cam.getView().vw, "初始 vw")

        // 旋转：接口收角度、内部转弧度。XY 90° -> vx 转到 +vy
        cam.setView(Coordinate4D())
        cam.rotateXY(90f)
        assertVec(Vector4f(0f, 1f, 0f, 0f), cam.getView().vx, "rotateXY(90) vx")
        assertVec(Vector4f(-1f, 0f, 0f, 0f), cam.getView().vy, "rotateXY(90) vy")

        // rotateZW 只动 vz / vw
        cam.setView(Coordinate4D())
        cam.rotateZW(37f)
        assertVec(Vector4f(1f, 0f, 0f, 0f), cam.getView().vx, "rotateZW 不该动 vx")
        assertVec(Vector4f(0f, 1f, 0f, 0f), cam.getView().vy, "rotateZW 不该动 vy")

        // 六个平面各转一遍，基向量必须保持标准正交
        cam.setView(Coordinate4D())
        cam.rotateXY(31f); cam.rotateXZ(-17f); cam.rotateXW(53f)
        cam.rotateYZ(11f); cam.rotateYW(-42f); cam.rotateZW(29f)
        val basis = listOf(cam.getView().vx, cam.getView().vy, cam.getView().vz, cam.getView().vw)
        basis.forEachIndexed { i, b ->
            assertEquals(1f, b.length(), 1e-4f, "旋转后基向量 $i 不是单位向量")
        }
        for (i in 0..3) for (j in i + 1..3) {
            assertEquals(0f, basis[i].dot(basis[j]), 1e-4f, "旋转后基向量 $i·$j 不正交")
        }

        // 每个平面转 360° 应该回到原位
        cam.setView(Coordinate4D())
        val rotations: List<(Float) -> Unit> = listOf(
            cam::rotateXY, cam::rotateXZ, cam::rotateXW,
            cam::rotateYZ, cam::rotateYW, cam::rotateZW
        )
        rotations.forEach { it(360f) }
        assertVec(Vector4f(1f, 0f, 0f, 0f), cam.getView().vx, "转一圈 vx")
        assertVec(Vector4f(0f, 1f, 0f, 0f), cam.getView().vy, "转一圈 vy")
        assertVec(Vector4f(0f, 0f, 0f, 1f), cam.getView().vw, "转一圈 vw")

        // 移动跟随旋转后的坐标系：XW 转 90° 后右方向变成 +w
        cam.setPosition(zero4())
        cam.setView(Coordinate4D())
        cam.rotateXW(90f)
        cam.moveRight(2f)
        assertVec(Vector4f(0f, 0f, 0f, 2f), cam.getPosition(), "旋转后 moveRight 应沿新 vx")
    }

    // ------------------------------------------------------------ 模型

    @Test
    fun modelInterface() {
        val api = Api()

        api.model.createEmptyModel("Tower")
        assertTrue("Tower" in api.model.listModel(), "listModel 应包含 Tower")
        assertEquals(MeshKind.GROUP, api.mesh("Tower").kind, "空模型应标记为 GROUP")

        // 部件挂在模型下面
        api.shape.createTesseract("Tower", "Base", zero4(), 2f)
        assertEquals(listOf("Tower"), api.model.listModel(), "listModel 只给根路径")
        assertEquals(listOf("Tower/Base"), api.scene.subModels("Tower"), "subModels")
        assertEquals("Tower", api.mesh("Tower/Base").parentPath, "parentPath")
        assertEquals("Base", api.mesh("Tower/Base").leafName, "leafName")
        assertTrue(!api.mesh("Tower/Base").isRoot, "Tower/Base 不是根路径")

        // 多级路径：中间层不需要真实存在
        api.shape.createTesseract("Tower/Upper", "Lid", Vector4f(0f, 0f, 0f, 3f), 1f)
        assertEquals("Tower/Upper/Lid", api.mesh("Tower/Upper/Lid").name, "二级路径名字")
        assertEquals(listOf("Tower/Upper/Lid"), api.scene.subModels("Tower/Upper"), "二级路径的子模型")
        assertEquals(2, api.scene.subTree("Tower").size, "subTree 应含隔代")
        assertEquals(listOf("Tower"), api.model.listModel(), "多级路径仍然只有 Tower 一个根")

        // 复制：几何、kind、params、矩阵都要带过去
        api.transform.move("Tower/Base", Vector4f(1f, 2f, 3f, 4f))
        api.model.copyModel("Tower/Base", "Tower/BaseCopy")
        assertEquals(api.tets("Tower/Base").size, api.tets("Tower/BaseCopy").size, "copyModel 四面体数")
        assertEquals(MeshKind.SHAPE, api.mesh("Tower/BaseCopy").kind, "copyModel 应带上 kind")
        assertNotNull(api.mesh("Tower/BaseCopy").params, "copyModel 应带上 params")
        assertMatrix(
            api.mesh("Tower/Base").transform.matrix,
            api.mesh("Tower/BaseCopy").transform.matrix,
            "copyModel 应带上变换矩阵"
        )

        // 合并：顶点烘焙进新模型，且不能动到源模型
        val baseMatrixBefore = Matrix5f(api.mesh("Tower/Base").transform.matrix.data.copyOf())
        val lidMatrixBefore = Matrix5f(api.mesh("Tower/Upper/Lid").transform.matrix.data.copyOf())
        val mergedCount = api.tets("Tower/Base").size + api.tets("Tower/Upper/Lid").size
        api.model.mergeModel("Tower/Base", "Tower/Upper/Lid", "Tower/All")
        assertEquals(MeshKind.MERGED, api.mesh("Tower/All").kind, "mergeModel kind")
        assertEquals(mergedCount, api.tets("Tower/All").size, "mergeModel 四面体数")
        assertMatrix(baseMatrixBefore, api.mesh("Tower/Base").transform.matrix, "merge 不该动源模型矩阵")
        assertMatrix(lidMatrixBefore, api.mesh("Tower/Upper/Lid").transform.matrix, "merge 不该动源模型矩阵")

        // 把模型矩阵烘焙到顶点：顶点 = 矩阵 × 原顶点，源模型本身不变
        val srcTets = api.tets("Tower/Base").toList()
        api.model.applyTransformToVertex("Tower/Base", "Tower/Baked")
        assertEquals(srcTets.size, api.tets("Tower/Baked").size, "烘焙后四面体数")
        srcTets.forEachIndexed { i, tet ->
            tet.vertices.forEachIndexed { j, v ->
                val baked = (baseMatrixBefore * Vector5f(v.pos)).toVector4f()
                assertVec(baked, api.tets("Tower/Baked")[i].vertices[j].pos, "烘焙顶点 $i/$j", 1e-3f)
                assertVec(v.pos, api.tets("Tower/Base")[i].vertices[j].pos, "烘焙不该改源顶点 $i/$j", 1e-3f)
            }
        }

        // 错误：重名 / 不存在（带候选）
        assertFailsWith<IllegalArgumentException> { api.model.createEmptyModel("Tower") }
        val missing = assertFailsWith<NoSuchElementException> { api.model.removeModel("Tower/Bass") }
        assertTrue(
            missing.message!!.contains("Candidates") && missing.message!!.contains("Tower/Base"),
            "找不到模型时应给候选，实际：${missing.message}"
        )

        // 删除模型 = 删整棵子树
        api.model.removeModel("Tower")
        assertTrue(api.model.listModel().isEmpty(), "删掉 Tower 后不该还有模型")
        assertTrue(api.scene.meshList.isEmpty(), "删掉 Tower 后不该还有残留部件")
    }

    // ------------------------------------------------------------ 形状

    @Test
    fun shapeInterface() {
        val api = Api()
        val cube = cubeBase()
        api.model.createEmptyModel("Gallery")

        api.shape.createTetrahedron("Gallery", "Tet", zero4(), 1f)
        api.shape.create5Cell("Gallery", "Five", zero4(), 2f)
        api.shape.create16Cell("Gallery", "Sixteen", zero4(), 1.5f)
        api.shape.createTesseract("Gallery", "Cube", zero4(), 2f)
        api.shape.createPrism4("Gallery", "Prism", cube, -1f, 1f)
        api.shape.createCone4("Gallery", "Cone", cube, Vector4f(0f, 0f, 0f, 2.5f))
        api.shape.createBall4("Gallery", "Ball", zero4(), 2f, 2f)

        // 全部是 SHAPE，且都记录了参数
        listOf("Tet", "Five", "Sixteen", "Cube", "Prism", "Cone", "Ball").forEach { name ->
            val mesh = api.mesh("Gallery/$name")
            assertEquals(MeshKind.SHAPE, mesh.kind, "$name 应该是 SHAPE")
            assertNotNull(mesh.params, "$name 应该记录 params")
            assertEquals(name, mesh.leafName, "$name 名字")
        }

        // 胞（四面体）数量
        val triCount = cube.triangles.size
        assertEquals(1, api.tets("Gallery/Tet").size, "正四面超平面 1 个胞")
        assertEquals(5, api.tets("Gallery/Five").size, "5-cell 5 个胞")
        assertEquals(16, api.tets("Gallery/Sixteen").size, "16-cell 16 个胞")
        assertEquals(48, api.tets("Gallery/Cube").size, "超立方体 48 个胞")
        assertEquals(3 * triCount, api.tets("Gallery/Prism").size, "超棱柱 = 3 × 底面三角形")
        assertEquals(triCount, api.tets("Gallery/Cone").size, "超锥 = 底面三角形")
        assertEquals(8 * 2 * 2 * 2 * 6, api.tets("Gallery/Ball").size, "超球 = 8 胞 × k³ × 6")

        // 正四面超平面：4 个顶点都在坐标轴上，距中心 radius
        val tetPos = api.uniquePositions("Gallery/Tet")
        assertEquals(4, tetPos.size, "正四面超平面顶点数")
        tetPos.forEach { assertEquals(1f, it.length(), 1e-4f, "顶点应在半径 1 上") }

        // 5-cell：所有棱长都等于 size
        val fivePos = api.uniquePositions("Gallery/Five")
        assertEquals(5, fivePos.size, "5-cell 顶点数")
        for (i in 0 until 5) for (j in i + 1 until 5) {
            assertEquals(
                2f, Vector4f(fivePos[i]).sub(fivePos[j]).length(), 1e-3f,
                "5-cell 棱长应等于 size"
            )
        }

        // 16-cell：8 个顶点，都在半径 1.5 上
        val sixteen = api.uniquePositions("Gallery/Sixteen")
        assertEquals(8, sixteen.size, "16-cell 顶点数")
        sixteen.forEach { assertEquals(1.5f, it.length(), 1e-4f, "16-cell 顶点半径") }

        // 超立方体：16 个顶点，坐标都是 ±1
        val cubePos = api.uniquePositions("Gallery/Cube")
        assertEquals(16, cubePos.size, "超立方体顶点数")
        cubePos.forEach {
            assertEquals(1f, abs(it.x), 1e-4f, "超立方体顶点 x")
            assertEquals(1f, abs(it.y), 1e-4f, "超立方体顶点 y")
            assertEquals(1f, abs(it.z), 1e-4f, "超立方体顶点 z")
            assertEquals(1f, abs(it.w), 1e-4f, "超立方体顶点 w")
        }

        // 超棱柱：所有顶点只在 w = ±1 两个超平面上；开口面恰好是两端的 base 三角形
        api.positions("Gallery/Prism").forEach {
            assertEquals(1f, abs(it.w), 1e-4f, "超棱柱顶点应该只在两个 w 平面上")
        }
        val prismBoundary = boundaryFaces(api.tets("Gallery/Prism"))
        assertEquals(2 * triCount, prismBoundary.size, "超棱柱的开口面数")
        assertTrue(
            prismBoundary.all { face -> face.all { abs(abs(it.w) - 1f) < 1e-4f } },
            "超棱柱的开口面必须都在两端的 w 平面上"
        )

        // 超锥：每个胞都含 apex；顶点只来自底面(w = 0)或 apex
        val apex = Vector4f(0f, 0f, 0f, 2.5f)
        api.tets("Gallery/Cone").forEach { tet ->
            assertTrue(tet.vertices.any { posKey(it.pos) == posKey(apex) }, "超锥每个胞都该含 apex")
        }
        api.positions("Gallery/Cone").forEach {
            assertTrue(
                abs(it.w) < 1e-4f || posKey(it) == posKey(apex),
                "超锥顶点只能来自底面(w=0)或 apex，实际 $it"
            )
        }

        // 超球：所有顶点都在半径 2 的 3-球面上
        api.uniquePositions("Gallery/Ball").forEach {
            assertEquals(2f, it.length(), 1e-4f, "超球顶点应在半径 2 的球面上")
        }

        // 闭合性：3-流形表面的每个三角面都该被两个胞共用
        assertClosed(api.tets("Gallery/Ball"), "超球")
        assertClosed(api.tets("Gallery/Cube"), "超立方体")
        assertClosed(api.tets("Gallery/Sixteen"), "16-cell")
        assertClosed(api.tets("Gallery/Five"), "5-cell")
        // 单个正四面超平面是 3-胞，正好 4 个面
        assertEquals(4, boundaryFaces(api.tets("Gallery/Tet")).size, "单个胞应该有 4 个面")

        // 参数校验
        assertFailsWith<IllegalArgumentException> { api.shape.createTetrahedron("Gallery", "Bad", zero4(), -1f) }
        assertFailsWith<IllegalArgumentException> { api.shape.createBall4("Gallery", "Bad", zero4(), 1f, 0f) }
        assertFailsWith<IllegalArgumentException> { api.shape.createPrism4("Gallery", "Bad", cube, 1f, 1f) }
        // 路径已存在要报错，避免两个形状挤进同一个 SHAPE 把参数弄脏
        assertFailsWith<IllegalArgumentException> {
            api.shape.createTesseract("Gallery", "Cube", zero4(), 2f)
        }
        assertFailsWith<IllegalArgumentException> {
            api.shape.createTesseract("Gallery", "  ", zero4(), 2f)
        }
        // ws > we 自动交换
        api.shape.createPrism4("Gallery", "Swap", cube, 3f, -1f)
        val range = api.mesh("Gallery/Swap").params!!.getString("w range")
        assertTrue(range.contains("-1") && range.contains("3"), "w 范围应自动交换，实际 $range")
    }

    // ------------------------------------------------------------ 变换

    @Test
    fun transformInterface() {
        val api = Api()
        api.model.createEmptyModel("Tower")
        api.shape.createTesseract("Tower", "Base", zero4(), 2f)
        api.shape.createTesseract("Tower", "Lid", Vector4f(0f, 0f, 0f, 4f), 1f)

        // 矩阵读写
        assertMatrix(Matrix5f.IDENTITY, api.transform.getModelMatrix("Tower/Base"), "初始应为单位阵")
        val custom = Matrix5f(
            1f, 0f, 0f, 0f, 5f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
            0f, 0f, 0f, 0f, 1f
        )
        api.transform.setModelMatrix("Tower/Base", custom)
        assertMatrix(custom, api.transform.getModelMatrix("Tower/Base"), "setModelMatrix")
        assertMatrix(custom, api.mesh("Tower/Base").transform.matrix, "变换应写在 Mesh4D.transform 上")

        // move：平移量写在第 5 列
        api.transform.setModelMatrix("Tower/Base", Matrix5f.IDENTITY)
        api.transform.move("Tower/Base", Vector4f(1f, 2f, 3f, 4f))
        val moved = api.transform.getModelMatrix("Tower/Base")
        assertEquals(1f, moved[4], 1e-5f, "平移 x")
        assertEquals(2f, moved[9], 1e-5f, "平移 y")
        assertEquals(3f, moved[14], 1e-5f, "平移 z")
        assertEquals(4f, moved[19], 1e-5f, "平移 w")

        // transform(m)：左乘，而且必须真的生效（曾经是空操作）
        api.transform.setModelMatrix("Tower/Lid", Matrix5f.IDENTITY)
        val scale2 = Matrix5f(
            2f, 0f, 0f, 0f, 0f,
            0f, 2f, 0f, 0f, 0f,
            0f, 0f, 2f, 0f, 0f,
            0f, 0f, 0f, 2f, 0f,
            0f, 0f, 0f, 0f, 1f
        )
        api.transform.transform("Tower/Lid", scale2)
        assertMatrix(scale2, api.transform.getModelMatrix("Tower/Lid"), "transform 应左乘生效")

        // 分组路径：作用到模型自己和全部子模型
        api.transform.setModelMatrix("Tower/Base", Matrix5f.IDENTITY)
        api.transform.setModelMatrix("Tower/Lid", Matrix5f.IDENTITY)
        api.transform.rotate("Tower", Direction.Plane.XW, 90f)
        val rotBase = api.transform.getModelMatrix("Tower/Base")
        assertVec(
            Vector4f(0f, 0f, 0f, 1f),
            (rotBase * Vector5f(Vector4f(1f, 0f, 0f, 0f))).toVector4f(),
            "XW 转 90° 后 x 轴应转到 +w"
        )
        assertVec(
            Vector4f(-1f, 0f, 0f, 0f),
            (rotBase * Vector5f(Vector4f(0f, 0f, 0f, 1f))).toVector4f(),
            "XW 转 90° 后 w 轴应转到 -x"
        )
        assertMatrix(rotBase, api.transform.getModelMatrix("Tower/Lid"), "分组路径应作用到所有子模型")

        // scale / clip / setCoordinate
        api.transform.setModelMatrix("Tower/Base", Matrix5f.IDENTITY)
        api.transform.scale("Tower/Base", 2f, 3f, 4f, 5f)
        val scaled = api.transform.getModelMatrix("Tower/Base")
        assertEquals(2f, scaled[0], 1e-5f, "scale x")
        assertEquals(3f, scaled[6], 1e-5f, "scale y")
        assertEquals(4f, scaled[12], 1e-5f, "scale z")
        assertEquals(5f, scaled[18], 1e-5f, "scale w")

        api.transform.setModelMatrix("Tower/Base", Matrix5f.IDENTITY)
        api.transform.clip("Tower/Base", Direction.Axis.X, Direction.Axis.Y, 0.5f)
        assertTrue(
            abs(api.transform.getModelMatrix("Tower/Base")[5] - 0.5f) < 1e-5f,
            "clip 应把 x 的偏移量写进 y 轴"
        )

        api.transform.setCoordinate("Tower/Base", Vector4f(1f, 0f, 0f, 0f), Coordinate4D())
        api.transform.move("Tower/Base", Vector4f(0f, 0f, 0f, 0f))
        assertNotNull(api.transform.getModelMatrix("Tower/Base"), "setCoordinate 后矩阵仍可读")

        // 错误
        assertFailsWith<NoSuchElementException> { api.transform.move("Tower/Nope", zero4()) }
        assertFailsWith<NoSuchElementException> { api.transform.getModelMatrix("Tower/Nope") }
    }

    // ------------------------------------------------------------ 几何编辑

    @Test
    fun geometryQueryAndEdit() {
        val api = Api()
        api.model.createEmptyModel("Tower")
        api.shape.createTesseract("Tower", "Base", zero4(), 2f)

        // getModelInfos：按 kind 分派，不 dump 顶点列表
        val info = api.geometry.getModelInfos("Tower/Base")
        assertTrue(info.contains("Kind: SHAPE"), "应给出 kind：\n$info")
        assertTrue(info.contains("Params:"), "应给出 params：\n$info")
        assertTrue(info.contains("Tetrahedrons: 48"), "应给出四面体数：\n$info")
        assertTrue(info.contains("Vertex list omitted"), "不该 dump 顶点：\n$info")
        assertTrue(info.contains("Transform matrix:"), "应给出变换矩阵：\n$info")

        // "Tower" 是一个真实存在的 GROUP 节点：给 kind 与子模型列表
        val groupInfo = api.geometry.getModelInfos("Tower")
        assertTrue(groupInfo.contains("Kind: GROUP"), "GROUP 节点应给出 kind：\n$groupInfo")
        assertTrue(groupInfo.contains("Sub models:"), "应列出子模型：\n$groupInfo")
        assertTrue(groupInfo.contains("Tower/Base"), "应列出 Tower/Base：\n$groupInfo")

        // 没有实体的纯"分组路径"（只有子模型）也要能查
        api.shape.createTesseract("Deep/Mid", "Leaf", zero4(), 1f)
        val pureGroup = api.geometry.getModelInfos("Deep/Mid")
        assertTrue(pureGroup.contains("group path"), "纯分组路径应给汇总：\n$pureGroup")
        assertTrue(pureGroup.contains("Deep/Mid/Leaf"), "纯分组路径应列出子模型：\n$pureGroup")

        // getTetrahedronInfos
        val tetInfo = api.geometry.getTetrahedronInfos("Tower/Base", 0)
        assertTrue(tetInfo.contains("Tower/Base#0"), "四面体信息抬头：\n$tetInfo")
        listOf("v0:", "v1:", "v2:", "v3:").forEach {
            assertTrue(tetInfo.contains(it), "四面体信息应含 $it：\n$tetInfo")
        }

        // setVertex：只给位置时颜色与法向保持不变
        val before = api.tets("Tower/Base")[0].v2
        api.geometry.setVertex("Tower/Base", 0, 2, Vector4f(9f, 8f, 7f, 6f))
        val after = api.tets("Tower/Base")[0].v2
        assertVec(Vector4f(9f, 8f, 7f, 6f), after.pos, "setVertex 位置")
        assertEquals(before.color, after.color, "setVertex 不该动颜色")
        assertVec(before.normal, after.normal, "setVertex 不该动法向")
        assertEquals(MeshKind.CARVED, api.mesh("Tower/Base").kind, "雕刻后应降级为 CARVED")
        assertEquals("Carved", api.mesh("Tower/Base").params!!.getString("type"), "雕刻后 params 应记录")

        // color / normal 可选参数
        api.geometry.setVertex(
            "Tower/Base", 0, 2, Vector4f(1f, 1f, 1f, 1f),
            ColorARGB(1f, 0f, 0f, 1f), Vector4f(0f, 0f, 0f, 1f)
        )
        val recolored = api.tets("Tower/Base")[0].v2
        assertEquals(ColorARGB(1f, 0f, 0f, 1f), recolored.color, "setVertex 颜色")
        assertVec(Vector4f(0f, 0f, 0f, 1f), recolored.normal, "setVertex 法向")

        // transformTetrahedrons：指定子集
        api.shape.createTesseract("Tower", "Lid", Vector4f(0f, 0f, 0f, 4f), 1f)
        val shift = Matrix5f(
            1f, 0f, 0f, 0f, 3f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
            0f, 0f, 0f, 0f, 1f
        )
        val lidBefore = api.tets("Tower/Lid").map { it.v0.pos }
        api.geometry.transformTetrahedrons("Tower/Lid", listOf(0, 2), shift)
        api.tets("Tower/Lid").forEachIndexed { i, tet ->
            val expected = if (i == 0 || i == 2) {
                Vector4f(lidBefore[i]).add(Vector4f(3f, 0f, 0f, 0f))
            } else {
                lidBefore[i]
            }
            assertVec(expected, tet.v0.pos, "transformTetrahedrons 第 $i 个胞")
        }
        // 空列表 = 全部
        val allBefore = api.tets("Tower/Lid").map { it.v0.pos }
        api.geometry.transformTetrahedrons("Tower/Lid", emptyList(), shift)
        api.tets("Tower/Lid").forEachIndexed { i, tet ->
            assertVec(
                Vector4f(allBefore[i]).add(Vector4f(3f, 0f, 0f, 0f)),
                tet.v0.pos, "空列表应变换全部"
            )
        }

        // addTetrahedron / removeTetrahedron：索引语义
        api.model.createEmptyModel("Sculpt")
        val idx = api.geometry.addTetrahedron(
            "Sculpt",
            tetra(
                Vector4f(0f, 0f, 0f, 0f), Vector4f(1f, 0f, 0f, 0f),
                Vector4f(0f, 1f, 0f, 0f), Vector4f(0f, 0f, 1f, 1f)
            )
        )
        assertEquals(0, idx, "addTetrahedron 应返回新索引")
        assertEquals(1, api.tets("Sculpt").size, "addTetrahedron 数量")
        assertEquals(MeshKind.CARVED, api.mesh("Sculpt").kind, "往空模型里加胞后应是 CARVED")
        api.geometry.removeTetrahedron("Sculpt", 0)
        assertEquals(0, api.tets("Sculpt").size, "removeTetrahedron 数量")

        // 错误路径
        assertFailsWith<IndexOutOfBoundsException> { api.geometry.getTetrahedronInfos("Tower/Base", 999) }
        assertFailsWith<IllegalArgumentException> { api.geometry.setVertex("Tower/Base", 0, 7, zero4()) }
        assertFailsWith<IllegalArgumentException> { api.geometry.removeTetrahedron("Tower", 0) }
        val notFound = assertFailsWith<NoSuchElementException> { api.geometry.getModelInfos("Tower/Basee") }
        assertTrue(notFound.message!!.contains("Candidates"), "应给候选：${notFound.message}")
    }

    // ------------------------------------------------------------ 超平面切割

    private data class SliceCase(val title: String, val points: List<Vector4f>, val twoSided: Boolean)

    @Test
    fun geometrySlice() {
        // 超立方体（边长 2，8 个胞共 64 的 3-体积）在各种超平面下切割，必须体积守恒
        val cases = listOf(
            SliceCase(
                "x = 0",
                listOf(
                    Vector4f(0f, 0f, 0f, 0f), Vector4f(0f, 1f, 0f, 0f),
                    Vector4f(0f, 0f, 1f, 0f), Vector4f(0f, 0f, 0f, 1f)
                ), true
            ),
            SliceCase(
                "x+y+z+w = 0",
                listOf(
                    Vector4f(1f, 0f, 0f, -1f), Vector4f(0f, 1f, 0f, -1f),
                    Vector4f(0f, 0f, 1f, -1f), Vector4f(0f, 0f, 0f, 0f)
                ), true
            ),
            SliceCase(
                "x+y+z+w = 1（4 个顶点共面）",
                listOf(
                    Vector4f(1f, 0f, 0f, 0f), Vector4f(0f, 1f, 0f, 0f),
                    Vector4f(0f, 0f, 1f, 0f), Vector4f(0f, 0f, 0f, 1f)
                ), true
            ),
            SliceCase(
                "x = 1（与 +X 胞共面）",
                listOf(
                    Vector4f(1f, 0f, 0f, 0f), Vector4f(1f, 1f, 0f, 0f),
                    Vector4f(1f, 0f, 1f, 0f), Vector4f(1f, 0f, 0f, 1f)
                ), true
            ),
            SliceCase(
                "z = w（8 个顶点共面）",
                listOf(
                    Vector4f(0f, 0f, 0f, 0f), Vector4f(1f, 0f, 0f, 0f),
                    Vector4f(0f, 1f, 0f, 0f), Vector4f(0f, 0f, 1f, 1f)
                ), true
            ),
            SliceCase(
                "x = 2（完全不相交）",
                listOf(
                    Vector4f(2f, 0f, 0f, 0f), Vector4f(2f, 1f, 0f, 0f),
                    Vector4f(2f, 0f, 1f, 0f), Vector4f(2f, 0f, 0f, 1f)
                ), false
            ),
            SliceCase(
                "斜切 x+2y-z+0.5w = 0.3",
                listOf(
                    Vector4f(0.3f, 0f, 0f, 0f), Vector4f(-1.7f, 1f, 0f, 0f),
                    Vector4f(1.3f, 0f, 1f, 0f), Vector4f(-0.2f, 0f, 0f, 1f)
                ), true
            )
        )

        cases.forEach { case ->
            val api = Api()
            api.model.createEmptyModel("Carved")
            api.shape.createTesseract("Carved", "Box", zero4(), 2f)
            val before = volume3(api.tets("Carved/Box"))

            api.geometry.sliceModel(
                "Carved/Box", planeOf(case.points), "Carved/A", "Carved/B"
            )

            val a = api.tets("Carved/A")
            val b = api.tets("Carved/B")
            val after = volume3(a) + volume3(b)
            val rel = abs(after - before) / before
            assertTrue(rel < 1e-4, "${case.title}: 体积不守恒，相对误差 $rel")
            assertEquals(MeshKind.CARVED, api.mesh("Carved/A").kind, "${case.title}: 切片产物应是 CARVED")
            assertEquals("Sliced", api.mesh("Carved/A").params!!.getString("type"), "${case.title}: 切片参数")
            if (case.twoSided) {
                assertTrue(a.isNotEmpty() && b.isNotEmpty(), "${case.title}: 两侧都该非空")
            } else {
                assertTrue(a.isEmpty() || b.isEmpty(), "${case.title}: 不相交的平面应整块归一侧")
            }
        }

        // x = 1 这一侧正好是 +X 胞的 6 个四面体（f = x-1 < 0 归 A，所以 A 是里面那 42 个）
        val api = Api()
        api.model.createEmptyModel("Carved")
        api.shape.createTesseract("Carved", "Box", zero4(), 2f)
        api.geometry.sliceModel(
            "Carved/Box",
            planeOf(
                listOf(
                    Vector4f(1f, 0f, 0f, 0f), Vector4f(1f, 1f, 0f, 0f),
                    Vector4f(1f, 0f, 1f, 0f), Vector4f(1f, 0f, 0f, 1f)
                )
            ),
            "Carved/A", "Carved/B"
        )
        assertEquals(42, api.tets("Carved/A").size, "x<1 的部分应归 A")
        assertEquals(6, api.tets("Carved/B").size, "+X 胞整体在 f >= 0 一侧，应归 B")

        // 错误路径
        assertFailsWith<IllegalArgumentException> {
            api.geometry.sliceModel(
                "Carved/Box",
                planeOf(
                    listOf(
                        Vector4f(0f, 0f, 0f, 0f), Vector4f(0f, 0f, 0f, 0f),
                        Vector4f(0f, 0f, 0f, 0f), Vector4f(0f, 0f, 0f, 0f)
                    )
                ),
                "Carved/C", "Carved/D"
            )
        }
        val valid = planeOf(
            listOf(
                Vector4f(0f, 0f, 0f, 0f), Vector4f(0f, 1f, 0f, 0f),
                Vector4f(0f, 0f, 1f, 0f), Vector4f(0f, 0f, 0f, 1f)
            )
        )
        assertFailsWith<IllegalArgumentException> {
            api.geometry.sliceModel("Carved/Box", valid, "Carved/A", "Carved/E")   // A 已存在
        }
        assertFailsWith<IllegalArgumentException> {
            api.geometry.sliceModel("Carved/Box", valid, "Carved/F", "Carved/F")   // 目标相同
        }
    }
}

// ---------------------------------------------------------------- 工具

private const val EPS = 1e-4f

/** 顶点位置做 key：+0f 是为了把 -0.0 归一成 0.0，避免同一个点算出两个 key */
private fun posKey(v: Vector4f): String =
    "%.5f|%.5f|%.5f|%.5f".format(v.x + 0f, v.y + 0f, v.z + 0f, v.w + 0f)

private fun assertVec(expected: Vector4f, actual: Vector4f, what: String, eps: Float = EPS) {
    val d = Vector4f(expected).sub(actual).length()
    assertTrue(d < eps, "$what: 期望 $expected，实际 $actual（差 $d）")
}

private fun assertMatrix(expected: Matrix5f, actual: Matrix5f, what: String, eps: Float = EPS) {
    for (i in 0 until 25) {
        assertTrue(
            abs(expected[i] - actual[i]) < eps,
            "$what: 矩阵第 $i 项 期望 ${expected[i]} 实际 ${actual[i]}"
        )
    }
}

/** 4D 中一个四面体（3-单纯形）的 3-体积 = sqrt(det(Gram)) / 6 */
private fun volume3(tets: List<Tetrahedron>): Double = tets.sumOf { tet ->
    val p = tet.vertices.map { it.pos }
    val e = (1..3).map { Vector4f(p[it]).sub(p[0]) }
    val g = Array(3) { i ->
        DoubleArray(3) { j ->
            (e[i].x * e[j].x + e[i].y * e[j].y + e[i].z * e[j].z + e[i].w * e[j].w).toDouble()
        }
    }
    val det = g[0][0] * (g[1][1] * g[2][2] - g[1][2] * g[2][1]) -
        g[0][1] * (g[1][0] * g[2][2] - g[1][2] * g[2][0]) +
        g[0][2] * (g[1][0] * g[2][1] - g[1][1] * g[2][0])
    if (det <= 0.0) 0.0 else sqrt(det) / 6.0
}

private val TET_FACES = listOf(
    intArrayOf(0, 1, 2), intArrayOf(0, 1, 3), intArrayOf(0, 2, 3), intArrayOf(1, 2, 3)
)

/** 只被一个四面体用到的三角面，也就是这个模型的"表面" */
private fun boundaryFaces(tets: List<Tetrahedron>): List<List<Vector4f>> {
    val counts = HashMap<String, Int>()
    val byKey = HashMap<String, List<Vector4f>>()
    tets.forEach { tet ->
        val v = tet.vertices.map { it.pos }
        TET_FACES.forEach { f ->
            val pts = f.map { v[it] }
            if (pts.map { posKey(it) }.distinct().size < 3) return@forEach   // 退化面不算
            val k = pts.map { posKey(it) }.sorted().joinToString("/")
            counts[k] = (counts[k] ?: 0) + 1
            byKey[k] = pts
        }
    }
    return counts.filterValues { it == 1 }.keys.map { byKey.getValue(it) }
}

/** 闭合的 3-流形：每个三角面都被恰好两个胞共用 */
private fun assertClosed(tets: List<Tetrahedron>, what: String) {
    val boundary = boundaryFaces(tets)
    assertTrue(boundary.isEmpty(), "$what: 表面不闭合，有 ${boundary.size} 个只出现一次的面")
}

private fun vertex(p: Vector4f): Vertex4D =
    Vertex4D(Vector4f(p), ColorARGB(1f, 1f, 1f, 1f), Vector4f(0f, 0f, 0f, 1f))

private fun tetra(v0: Vector4f, v1: Vector4f, v2: Vector4f, v3: Vector4f): Tetrahedron =
    Tetrahedron(vertex(v0), vertex(v1), vertex(v2), vertex(v3))

private fun planeOf(points: List<Vector4f>): Tetrahedron =
    tetra(points[0], points[1], points[2], points[3])

private fun cubeBase(size: Float = 2f): Mesh = Mesh(createCube(size = size))

/**
 * 四维零向量。
 * JOML 的 `Vector4f()` 是 `(0,0,0,1)`（齐次坐标），当"原点"用会悄悄把 w 抬到 1，
 * 所以测试里一律用这个工厂，而且每次返回新实例（避免共享可变对象被改）。
 */
private fun zero4(): Vector4f = Vector4f(0f, 0f, 0f, 0f)
