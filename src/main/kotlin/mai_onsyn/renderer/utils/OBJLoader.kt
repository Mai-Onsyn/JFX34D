package mai_onsyn.renderer.utils

import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.data.Texture
import mai_onsyn.renderer.ogl3d.data.Triangle
import mai_onsyn.renderer.ogl3d.data.Vertex
import org.joml.Vector2f
import org.joml.Vector3f
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.collections.iterator

//private val random
// ==================== 中间结构 ====================

data class MtlObject(
    var name: String = "",
    var ka: ColorARGB = ColorARGB(1f, 0.25f, 0.25f, 0.25f),   // 环境光 (默认 64,64,64)
    var kd: ColorARGB = ColorARGB(1f, 0.75f, 0.75f, 0.75f),   // 漫反射 (默认 200,200,200)
    var ks: ColorARGB = ColorARGB(),   // 镜面反射 (默认 0,0,0)
    var ns: Float = 1f,
    var d: Float = 1f,
    var mapKd: String = "",
    var mapKs: String = "",
    var mapD: String = "",
    var mapBump: String = "",
)

data class OBJFace(
    val vertexIndices: MutableList<Int> = mutableListOf(),
    val uvIndices: MutableList<Int> = mutableListOf(),
    val normalIndices: MutableList<Int> = mutableListOf(),
)

data class FaceMtlRange(
    var start: Int = 0,
    var end: Int = 0,
    var mtlName: String = "",
)

data class OBJFile(
    val mtls: MutableMap<String, MtlObject> = LinkedHashMap(),
    val vertices: MutableList<Vector3f> = mutableListOf(),
    val normals: MutableList<Vector3f> = mutableListOf(),
    val uvs: MutableList<Vector2f> = mutableListOf(),
    val faces: MutableList<OBJFace> = mutableListOf(),
    val faceMtls: MutableList<FaceMtlRange> = mutableListOf(),
)

/** 顶点去重键: (v, uv, n) 三元组 */
private data class IndexTuple(val v: Int, val uv: Int, val n: Int)

// ==================== 加载器 ====================

object OBJLoader {

    private val WHITE = ColorARGB(1f, 1f, 1f, 1f)
    private val DEFAULT_NORMAL = Vector3f(0f, 1f, 0f)
    private val ZERO_UV = Vector2f(0f, 0f)
    private val WS = Regex("\\s+")
    private val FLIP_RE = Regex("\"flipUV_Y\"\\s*:\\s*(true|false)")

    /** 一步到位: 解析并转换成 Mesh */
    fun load(path: Path): Mesh = toMesh(parse(path))

    fun load(path: String): Mesh = load(Paths.get(path))

    // ---------------- 解析 .obj ----------------

    fun parse(path: Path): OBJFile {
        val obj = OBJFile()
        val dir = path.parent ?: Paths.get("")

        // 读取 properties.json 里的 flipUV_Y 开关 (缺失则忽略)
        var flipUVY = false
        val configFile = dir.resolve("properties.json").toFile()
        if (configFile.isFile) {
            FLIP_RE.find(configFile.readText())?.let {
                flipUVY = it.groupValues[1].toBoolean()
            }
        }

        val file = path.toFile()
        if (!file.isFile) return obj

        var mtlRange = FaceMtlRange()
        var firstUseMtl = true

        file.forEachLine { rawLine ->
            val line = rawLine.trim().replace(WS, " ")
            if (line.isEmpty()) return@forEachLine

            val tokens = line.split(' ')
            when (tokens[0]) {
                "v" -> if (tokens.size >= 4) {
                    obj.vertices.add(
                        Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat())
                    )
                }

                "vt" -> if (tokens.size >= 3) {
                    val v = tokens[2].toFloat()
                    obj.uvs.add(Vector2f(tokens[1].toFloat(), if (flipUVY) 1f - v else v))
                }

                "vn" -> if (tokens.size >= 4) {
                    obj.normals.add(
                        Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat())
                    )
                }

                "f" -> {
                    val face = OBJFace()
                    for (i in 1 until tokens.size) {
                        val ft = tokens[i].split('/')
                        if (ft.isNotEmpty() && ft[0].isNotBlank())
                            face.vertexIndices.add(ft[0].toInt() - 1)
                        if (ft.size >= 2 && ft[1].isNotBlank())
                            face.uvIndices.add(ft[1].toInt() - 1)
                        if (ft.size >= 3 && ft[2].isNotBlank())
                            face.normalIndices.add(ft[2].toInt() - 1)
                    }
                    obj.faces.add(face)
                }

                "mtllib" -> if (tokens.size >= 2) {
                    val mtlPath = dir.resolve(line.substring(7).trim())
                    obj.mtls.putAll(parseMtl(mtlPath))
                }

                "usemtl" -> if (tokens.size >= 2) {
                    if (firstUseMtl) {
                        mtlRange.start = obj.faces.size
                        firstUseMtl = false
                    } else {
                        mtlRange.end = obj.faces.size
                        obj.faceMtls.add(mtlRange)
                        mtlRange = FaceMtlRange(start = obj.faces.size)
                    }
                    mtlRange.mtlName = line.substring(7).trim()
                }
            }
        }

        if (!firstUseMtl) {
            mtlRange.end = obj.faces.size
            obj.faceMtls.add(mtlRange)
        }

        return obj
    }

    // ---------------- 解析 .mtl ----------------

    fun parseMtl(path: Path): Map<String, MtlObject> {
        val mtls = LinkedHashMap<String, MtlObject>()
        val file = path.toFile()
        if (!file.isFile) return mtls

        val dir = path.parent ?: Paths.get("")
        var current: MtlObject? = null

        file.forEachLine { rawLine ->
            val line = rawLine.trim().replace(WS, " ")
            if (line.isEmpty()) return@forEachLine

            val tokens = line.split(' ')
            val c = current
            when (tokens[0]) {
                "newmtl" -> if (tokens.size >= 2) {
                    val name = line.substring(7).trim()
                    val mtl = MtlObject(name = name)
                    mtls[name] = mtl
                    current = mtl
                }

                "Ka" -> if (c != null && tokens.size >= 4)
                    c.ka = rgb(stocp(tokens[1]), stocp(tokens[2]), stocp(tokens[3]))

                "Kd" -> if (c != null && tokens.size >= 4)
                    c.kd = rgb(stocp(tokens[1]), stocp(tokens[2]), stocp(tokens[3]))

                "Ks" -> if (c != null && tokens.size >= 4)
                    c.ks = rgb(stocp(tokens[1]), stocp(tokens[2]), stocp(tokens[3]))

                "Ns" -> if (c != null && tokens.size >= 2)
                    c.ns = tokens[1].toFloat()

                "d" -> if (c != null && tokens.size >= 2)
                    c.d = tokens[1].toFloat()

                "map_Kd" -> if (c != null && tokens.size >= 2)
                    c.mapKd = dir.resolve(line.substring(7).trim()).toString()

                "map_Ks" -> if (c != null && tokens.size >= 2)
                    c.mapKs = dir.resolve(line.substring(7).trim()).toString()

                "map_d" -> if (c != null && tokens.size >= 2)
                    c.mapD = dir.resolve(line.substring(6).trim()).toString()

                "map_bump" -> if (c != null && tokens.size >= 2)
                    c.mapBump = dir.resolve(line.substring(9).trim()).toString()
            }
        }

        return mtls
    }

    // ---------------- OBJFile -> Mesh ----------------

    fun toMesh(obj: OBJFile): Mesh {
        val mesh = Mesh()

        // --- 材质 -> Texture; 相同贴图路径的 BufferedImage 复用 ---
        val textures = HashMap<String, Texture>(obj.mtls.size)
        val imageCache = HashMap<String, BufferedImage>()
        for ((name, mtl) in obj.mtls) {
            textures[name] = Texture(
                ka = mtl.ka,
                kd = mtl.kd,
                ks = mtl.ks,
                ns = mtl.ns,
                d = mtl.d,
                mapKd = loadImage(mtl.mapKd, imageCache),
                mapKs = loadImage(mtl.mapKs, imageCache),
                mapD = loadImage(mtl.mapD, imageCache),
                mapBump = loadImage(mtl.mapBump, imageCache),
            )
        }
        val defaultTexture = Texture(mapBump = null)

        val ranges: List<FaceMtlRange> =
            if (obj.faceMtls.isEmpty() && obj.faces.isNotEmpty())
                listOf(FaceMtlRange(0, obj.faces.size, ""))
            else obj.faceMtls

        // (v, uv, n) -> 共享的 Vertex 实例
        val vertexCache = HashMap<IndexTuple, Vertex>(obj.faces.size * 3)

        for (range in ranges) {
            val tex: Texture = textures[range.mtlName] ?: defaultTexture

            for (fi in range.start until range.end) {
                val face = obj.faces[fi]
                val count = face.vertexIndices.size
                if (count < 3) continue

                // 收集本面用到的 Vertex 引用（去重共享）
                val faceVerts = ArrayList<Vertex>(count)
                for (j in 0 until count) {
                    val vi = face.vertexIndices[j]
                    val hasUV = j < face.uvIndices.size
                    val hasN = j < face.normalIndices.size
                    val uvi = if (hasUV) face.uvIndices[j] else 0
                    val ni = if (hasN) face.normalIndices[j] else 0

                    val key = IndexTuple(vi, uvi, ni)
                    val v = vertexCache.getOrPut(key) {
                        Vertex(
                            pos = obj.vertices[vi],
                            color = WHITE,//WHITE,
                            normal = if (hasN && ni in obj.normals.indices) obj.normals[ni]
                            else DEFAULT_NORMAL,
                            uv = if (hasUV && uvi in obj.uvs.indices) obj.uvs[uvi]
                            else ZERO_UV,
                        )
                    }
                    faceVerts.add(v)
                }

                // 扇形三角化 (n 边形 -> n-2 个三角形)
                for (t in 1 until count - 1) {
                    mesh.triangles.add(
                        Triangle(
                            v0 = faceVerts[0],
                            v1 = faceVerts[t],
                            v2 = faceVerts[t + 1],
                            texture = tex,
                        )
                    )
                }
            }
        }

        return mesh
    }

    // ---------------- 工具 ----------------

    private fun loadImage(path: String, cache: MutableMap<String, BufferedImage>): BufferedImage? {
        if (path.isEmpty()) return null
        cache[path]?.let { return it }
        val img = try {
            ImageIO.read(File(path))
        } catch (_: Exception) {
            null
        }
        if (img != null) cache[path] = img
        return img
    }

    /** 与 C++ 中 static_cast<UInt8>(stof(x) * 255) 行为一致（截断 + 夹取） */
    private fun stocp(s: String): Int = (s.toFloat() * 255f).toInt().coerceIn(0, 255)

    private fun rgb(r: Int, g: Int, b: Int, a: Int = 255): ColorARGB =
        ColorARGB(r / 255f, g / 255f, b / 255f, a / 255f)
}