package mai_onsyn.renderer.interfaces.impl

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.MeshKind
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vector5f
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.interfaces.GeometryInterface
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f

class GeometryInterfaceImpl(
    private val scene: SimpleScene4D
): GeometryInterface {

    // ---------------------------------------------------------------- 查询

    override fun getModelInfos(name: String): String {
        val mesh = scene.findMesh(name)
        if (mesh != null) return meshInfos(mesh)

        // name 是一个"分组路径"（没有实体，只有子模型），给出汇总
        val sub = scene.subTree(name)
        if (sub.isEmpty()) scene.requireContains(name)   // 抛带候选的异常

        return buildString {
            append("## \"").append(name).append("\" (group path, no geometry itself)\n")
            append("Sub models: ").append(sub.size).append('\n')
            sub.forEach {
                append("- ").append(it.name).append("  ").append(it.kind)
                    .append("  tets=").append(it.tetrahedrons.size).append('\n')
            }
            append("Total tetrahedrons: ").append(sub.sumOf { it.tetrahedrons.size })
        }
    }

    override fun getTetrahedronInfos(name: String, tetIndex: Int): String {
        val mesh = requireGeometry(name)
        val tet = requireTet(mesh, tetIndex)
        return buildString {
            append("## ").append(mesh.name).append('#').append(tetIndex).append('\n')
            tet.vertices.forEachIndexed { i, v ->
                append('v').append(i).append(": pos=").append(formatVector(v.pos))
                    .append(" color=").append(v.color)
                    .append(" normal=").append(formatVector(v.normal))
                    .append('\n')
            }
        }.trimEnd()
    }

    // ---------------------------------------------------------------- 编辑

    override fun setVertex(
        name: String,
        tetIndex: Int,
        vertexNum: Int,
        pos: Vector4f,
        color: ColorARGB?,
        normal: Vector4f?
    ) {
        require(vertexNum in 0..3) { "Vertex number must be in [0, 3], got $vertexNum" }
        val mesh = requireGeometry(name)
        val tet = requireTet(mesh, tetIndex)

        val old = tet.vertices[vertexNum]
        val updated = Vertex4D(
            Vector4f(pos),
            color ?: old.color,
            normal?.let { Vector4f(it) } ?: old.normal
        )
        mesh.tetrahedrons[tetIndex] = when (vertexNum) {
            0 -> tet.copy(v0 = updated)
            1 -> tet.copy(v1 = updated)
            2 -> tet.copy(v2 = updated)
            else -> tet.copy(v3 = updated)
        }
        markEdited(mesh, "vertex edited")
    }

    override fun transformTetrahedrons(name: String, tetIndices: List<Int>, transform: Matrix5f) {
        val mesh = requireGeometry(name)
        val targets = if (tetIndices.isEmpty()) mesh.tetrahedrons.indices.toList() else tetIndices
        targets.forEach { requireTet(mesh, it) }
        targets.forEach { mesh.tetrahedrons[it] = mesh.tetrahedrons[it].transform(transform) }
        markEdited(mesh, "tetrahedrons transformed")
    }

    override fun addTetrahedron(name: String, tetrahedron: Tetrahedron): Int {
        val mesh = requireGeometry(name)
        mesh.tetrahedrons.add(tetrahedron)
        markEdited(mesh, "tetrahedron appended")
        return mesh.tetrahedrons.size - 1
    }

    override fun removeTetrahedron(name: String, tetIndex: Int) {
        val mesh = requireGeometry(name)
        requireTet(mesh, tetIndex)
        mesh.tetrahedrons.removeAt(tetIndex)
        markEdited(mesh, "tetrahedron removed")
    }

    override fun sliceModel(name: String, plane: Tetrahedron, pathA: String, pathB: String) {
        val source = requireGeometry(name)
        require(pathA != pathB) { "Slice destinations must differ, both are \"$pathA\"" }
        scene.requireNotContains(pathA)
        scene.requireNotContains(pathB)

        val hyperplane = TetrahedronSlicer.planeOf(
            plane.v0.pos, plane.v1.pos, plane.v2.pos, plane.v3.pos
        )
        val (sideA, sideB) = TetrahedronSlicer.split(source.tetrahedrons, hyperplane)

        scene.meshList.add(sliced(pathA, sideA, "A", source.name, plane, hyperplane.normal))
        scene.meshList.add(sliced(pathB, sideB, "B", source.name, plane, hyperplane.normal))
    }

    // ---------------------------------------------------------------- 内部

    private fun sliced(
        path: String,
        tets: List<Tetrahedron>,
        side: String,
        sourceName: String,
        plane: Tetrahedron,
        normal: Vector4f
    ): Mesh4D = Mesh4D(tetrahedrons = tets.toMutableList(), name = path).apply {
        kind = MeshKind.CARVED
        params = JSONObject(mapOf(
            "type" to "Sliced",
            "source" to sourceName,
            "side" to side,
            "normal" to formatVector(normal),
            "plane" to plane.vertices.joinToString(" ") { formatVector(it.pos) }
        ))
        dirty = true
    }

    private fun markEdited(mesh: Mesh4D, reason: String) {
        mesh.markCarved(reason)
        mesh.dirty = true
    }

    /** 取一个能承载几何的模型：分组路径、只当容器的模型都不接受 */
    private fun requireGeometry(name: String): Mesh4D {
        val mesh = scene.findMesh(name)
        if (mesh == null) {
            val sub = scene.subTree(name)
            if (sub.isNotEmpty()) {
                throw IllegalArgumentException(
                    "\"$name\" is a group path and holds no geometry, " +
                        "pick one of: ${sub.joinToString(", ") { it.name }}"
                )
            }
            throw NoSuchElementException(scene.noSuchMeshMessage(name))
        }
        val children = scene.childrenOf(name)
        if (children.isNotEmpty()) {
            throw IllegalArgumentException(
                "\"$name\" is a group of ${children.size} sub models and cannot hold geometry, " +
                    "pick one of: ${children.joinToString(", ") { it.name }}"
            )
        }
        return mesh
    }

    private fun requireTet(mesh: Mesh4D, tetIndex: Int): Tetrahedron {
        if (tetIndex !in mesh.tetrahedrons.indices) {
            throw IndexOutOfBoundsException(
                "Tetrahedron index $tetIndex is out of range [0, ${mesh.tetrahedrons.size}) " +
                    "in \"${mesh.name}\""
            )
        }
        return mesh.tetrahedrons[tetIndex]
    }

    private fun meshInfos(mesh: Mesh4D): String = buildString {
        val children = scene.childrenOf(mesh.name)
        append("## \"").append(mesh.name).append("\"\n")
        append("Kind: ").append(mesh.kind).append('\n')
        mesh.params?.let { append("Params: ").append(JSON.toJSONString(it)).append('\n') }
        append("Tetrahedrons: ").append(mesh.tetrahedrons.size).append('\n')
        append("Bound: ").append(boundOf(mesh)).append('\n')
        if (mesh.tetrahedrons.isNotEmpty()) {
            append("Vertex list omitted, use GET_TETRAHEDRON with index in [0, ")
                .append(mesh.tetrahedrons.size).append(")\n")
        }
        append("Transform matrix:\n```text\n").append(mesh.transform).append("\n```\n")
        if (children.isNotEmpty()) {
            append("Sub models:\n")
            children.forEach {
                append("- ").append(it.name).append("  ").append(it.kind)
                    .append("  tets=").append(it.tetrahedrons.size).append('\n')
            }
        }
    }.trimEnd()

    /** 顶点经变换矩阵后的世界坐标包围盒 */
    private fun boundOf(mesh: Mesh4D): String {
        if (mesh.tetrahedrons.isEmpty()) return "empty"
        val min = Vector4f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val max = Vector4f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        val matrix = mesh.transform.matrix
        mesh.tetrahedrons.forEach { tet ->
            tet.vertices.forEach { v ->
                val p = (matrix * Vector5f(v.pos)).toVector4f()
                min.min(p)
                max.max(p)
            }
        }
        return "x[%.2f, %.2f] y[%.2f, %.2f] z[%.2f, %.2f] w[%.2f, %.2f]".format(
            min.x, max.x, min.y, max.y, min.z, max.z, min.w, max.w
        )
    }

    /** 打印用：+0f 是为了把 -0.0 归一成 0.0，不然会出现 "(1.00, -0.00, 0.00, -0.00)" 这种噪声 */
    private fun formatVector(v: Vector4f): String =
        "(%.2f, %.2f, %.2f, %.2f)".format(v.x + 0f, v.y + 0f, v.z + 0f, v.w + 0f)
}
