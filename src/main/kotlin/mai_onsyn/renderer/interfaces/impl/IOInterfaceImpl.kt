package mai_onsyn.renderer.interfaces.impl

import com.alibaba.fastjson2.JSONObject
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.MeshKind
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.interfaces.IOInterface
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class IOInterfaceImpl(
    private val scene: SimpleScene4D
): IOInterface {
    
    private data class TetVertexReference(
        val pos: Int,
        val color: Int,
        val normal: Int
    )

    private data class TetReference(
        val v0: TetVertexReference,
        val v1: TetVertexReference,
        val v2: TetVertexReference,
        val v3: TetVertexReference
    )

    private fun Vector4f.roundForIndex(): Vector4f = Vector4f(
        (x * 10000f).roundToInt() / 10000f,
        (y * 10000f).roundToInt() / 10000f,
        (z * 10000f).roundToInt() / 10000f,
        (w * 10000f).roundToInt() / 10000f
    )
    
    override fun saveModel(name: String, fileName: String) {
        val file = File(fileName)
        // 组名 = 模型完整名字去掉「传入 name 的父路径」；name 没有父级时就是完整名字。
        // 例：name = "TestModel"        -> g TestModel / g TestModel/subA
        //     name = "Scene1/TestModel" -> g TestModel / g TestModel/subA
        val parentPrefix = if (name.contains('/')) name.substringBeforeLast('/') + "/" else ""
        file.bufferedWriter().use { fos ->
            val space = ' '.code
            val wrap = '\n'.code
            scene.targets(name).forEach { model ->
                if (model.tetrahedrons.isEmpty()) return@forEach
                fos.write('g'.code)
                fos.write(space)
                fos.write(model.name.removePrefix(parentPrefix))
                fos.write(wrap)
                fos.write(wrap)

                val applied = model.applyTransform()
                
                val posIndexMap = LinkedHashMap<Vector4f, Int>()
                val colorIndexMap = LinkedHashMap<ColorARGB, Int>()
                val normalIndexMap = LinkedHashMap<Vector4f, Int>()
                fun pushIntoMap(vertex: Vertex4D): TetVertexReference {
                    return TetVertexReference(
                        posIndexMap.getOrPut(vertex.pos.roundForIndex()) { posIndexMap.size },
                        colorIndexMap.getOrPut(vertex.color) { colorIndexMap.size },
                        normalIndexMap.getOrPut(vertex.normal.roundForIndex()) { normalIndexMap.size }
                    )
                }
                val tets = ArrayList<TetReference>(applied.tetrahedrons.size)
                applied.tetrahedrons.forEach { tetrahedron ->
                    tets.add(TetReference(
                        pushIntoMap(tetrahedron.v0),
                        pushIntoMap(tetrahedron.v1),
                        pushIntoMap(tetrahedron.v2),
                        pushIntoMap(tetrahedron.v3)
                    ))
                }

                posIndexMap.keys.forEach {
                    fos.write("v %.4f %.4f %.4f %.4f\n".format(
                        it.x, it.y, it.z, it.w
                    ))
                }
                fos.write(wrap)

                colorIndexMap.keys.forEach {
                    fos.write("c $it\n")
                }
                fos.write(wrap)

                normalIndexMap.keys.forEach {
                    fos.write("n %.4f %.4f %.4f %.4f\n".format(
                        it.x, it.y, it.z, it.w
                    ))
                }
                fos.write(wrap)

                tets.forEach {
                    fos.write("t %d/%d/%d %d/%d/%d %d/%d/%d %d/%d/%d\n".format(
                        it.v0.pos, it.v0.color, it.v0.normal,
                        it.v1.pos, it.v1.color, it.v1.normal,
                        it.v2.pos, it.v2.color, it.v2.normal,
                        it.v3.pos, it.v3.color, it.v3.normal
                    ))
                }
                fos.write(wrap)
            }

            fos.flush()
        }
    }

    /** 一个 g 分组：组内 v / c / n 三张表各自编号，遇到新的 g 全部从 0 重算 */
    private class TetGroup(val name: String) {
        val posTable = ArrayList<Vector4f>()
        val colorTable = ArrayList<ColorARGB>()
        val normalTable = ArrayList<Vector4f>()
        val tets = ArrayList<Tetrahedron>()
    }

    /**
     * 读取 .4do：每个 g 分组还原成一个 Mesh4D。
     * @param name 模型前缀，把文件里的组名挂到它下面；传 "" 表示挂到顶级（沿用文件里的组名）。
     *             不允许以 '/' 结尾。
     */
    override fun loadModel(name: String, file: File) {
        require(name.isEmpty() || !name.endsWith('/')) {
            "Model prefix must not end with '/', got \"$name\""
        }
        require(file.isFile) { "Model file does not exist: ${file.absolutePath}" }

        var group: TetGroup? = null

        /** 结算当前分组：算出完整路径 -> 校验不重名 -> 加进场景 */
        fun flushGroup() {
            val g = group ?: return
            val path = if (name.isEmpty()) g.name else "$name/${g.name}"
            scene.requireNotContains(path)
            scene.meshList.add(Mesh4D(tetrahedrons = g.tets, name = path).apply {
                kind = MeshKind.CARVED            // 顶点已烘焙，参数已丢失
                params = JSONObject(mapOf(
                    "type" to "Loaded",
                    "source" to file.name
                ))
                dirty = true
            })
            group = null
        }

        file.bufferedReader().useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEach

                val tag = line.substringBefore(' ')
                val body = line.substringAfter(' ', "").trim()

                when (tag) {
                    "g" -> {
                        flushGroup()
                        require(body.isNotEmpty()) { "Empty group name in \"${file.name}\"" }
                        group = TetGroup(body)
                    }

                    "v" -> {
                        val t = group ?: return@forEach
                        val f = body.split(' ')
                        t.posTable.add(Vector4f(f[0].toFloat(), f[1].toFloat(), f[2].toFloat(), f[3].toFloat()))
                    }

                    "n" -> {
                        val t = group ?: return@forEach
                        val f = body.split(' ')
                        t.normalTable.add(Vector4f(f[0].toFloat(), f[1].toFloat(), f[2].toFloat(), f[3].toFloat()))
                    }

                    "c" -> {
                        // "#AARRGGBB"，用 Long 中转避免 0xFF...... 溢出
                        val t = group ?: return@forEach
                        t.colorTable.add(ColorARGB(body.removePrefix("#").toLong(16).toInt()))
                    }

                    "t" -> {
                        val t = group ?: return@forEach
                        val vertices = body.split(' ').map { ref ->
                            val idx = ref.split('/')
                            Vertex4D(
                                t.posTable[idx[0].toInt()],
                                t.colorTable[idx[1].toInt()],
                                t.normalTable[idx[2].toInt()]
                            )
                        }
                        t.tets.add(Tetrahedron(vertices[0], vertices[1], vertices[2], vertices[3]))
                    }
                }
            }
        }

        flushGroup()
    }
}