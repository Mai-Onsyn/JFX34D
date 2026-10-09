package mai_onsyn.renderer.interfaces.impl

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.MeshKind
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.ModelInterface

class ModelInterfaceImpl(
    private val scene: SimpleScene4D,
): ModelInterface {
    override fun listModel(): List<String> = scene.getMeshes().map { it.rootPath }.toSet().toList()

    override fun listAllModels(): List<String> = scene.getMeshes().map { it.name }

    override fun createEmptyModel(name: String) {
        scene.requireNotContains(name)
        scene.meshList.add(Mesh4D(name = name).apply { this.kind = MeshKind.GROUP })
    }

    /** 删除模型及其全部子模型（路径前缀） */
    override fun removeModel(name: String) {
        val removed = scene.meshList.removeIf { it.name == name || it.name.startsWith("$name/") }
        if (!removed) throw NoSuchElementException(scene.noSuchMeshMessage(name))
    }

    /** 重命名模型（连整棵子树一起改名，保持父子关系）；newName 是叶名，不是路径。
     *  path 可以是真实模型，也可以是中间/分组路径（按前缀改名整段）。 */
    override fun renameModel(path: String, newName: String) {
        val exact = scene.findMesh(path)
        if (exact == null && scene.subTree(path).isEmpty()) throw NoSuchElementException(scene.noSuchMeshMessage(path))
        val parent = path.substringBeforeLast('/', "")
        val newPath = if (parent.isEmpty()) newName else "$parent/$newName"
        if (newPath == path) return
        scene.requireNotContains(newPath)                                  // 同名节点已存在
        if (scene.subTree(newPath).isNotEmpty()) {                         // 已有子树占用该路径
            throw IllegalArgumentException("Model \"$newPath\" already exists")
        }
        exact?.name = newPath
        val oldPrefix = "$path/"
        scene.meshList.filter { it.name.startsWith(oldPrefix) }.forEach {
            it.name = "$newPath/" + it.name.removePrefix(oldPrefix)
        }
    }

    /** 复制模型：连同其整棵子树一起复制（src/... → dst/...）；src 可真实也可为中间路径，dst 必须不存在 */
    override fun copyModel(srcName: String, dstName: String) {
        scene.requireNotContains(dstName)
        val src = scene.findMesh(srcName)
        val oldPrefix = "$srcName/"
        val descendants = scene.meshList.filter { it.name.startsWith(oldPrefix) }
            .map { it to (dstName + "/" + it.name.removePrefix(oldPrefix)) }
        if (src == null && descendants.isEmpty()) throw NoSuchElementException(scene.noSuchMeshMessage(srcName))
        src?.let { scene.meshList.add(it.copy(newName = dstName)) }
        descendants.forEach { (mesh, newPath) -> scene.meshList.add(mesh.copy(newName = newPath)) }
    }

    override fun mergeModel(src1Name: String, src2Name: String, dstName: String) {
        val originMesh1 = scene.requireContains(src1Name)
        val originMesh2 = scene.requireContains(src2Name)
        scene.requireNotContains(dstName)

        scene.meshList.add(Mesh4D(name = dstName).apply {
            val m1 = originMesh1.applyTransform()
            val m2 = originMesh2.applyTransform()
            this.tetrahedrons.addAll(m1.tetrahedrons)
            this.tetrahedrons.addAll(m2.tetrahedrons)
            this.kind = MeshKind.MERGED
            this.params = JSONObject(mapOf(
                "type" to "Merged",
                "source A" to JSONObject(mapOf(
                    "params" to originMesh1.params,
                    "transform matrix" to originMesh1.transform.toString()
                )),
                "source B" to JSONObject(mapOf(
                    "params" to originMesh2.params,
                    "transform matrix" to originMesh2.transform.toString()
                ))
            ))
        })
    }

    override fun applyTransformToVertex(srcName: String, dstName: String) {
        scene.requireNotContains(dstName)
        val sourceMesh = scene.requireContains(srcName)
        val applied = sourceMesh.applyTransform().apply {
            name = dstName
            params = JSONObject(mapOf(
                "type" to "Transformed",
                "source" to sourceMesh.params,
                "transformed matrix" to sourceMesh.transform.toString()
            ))
        }
        scene.meshList.add(applied)
    }

    /** 隐藏/显示模型及其子模型；name 可以是分组/中间路径（作用于自身 + 全部后代） */
    override fun setModelVisible(name: String, visible: Boolean) {
        scene.targets(name).forEach { it.visible = visible }
    }

    override fun getModel(name: String): Mesh4D = scene.requireContains(name)

    override fun mergeAllSubModels(srcName: String, dstName: String) {
        scene.requireNotContains(dstName)
        val resMesh = Mesh4D(name = dstName)
        resMesh.kind = MeshKind.MERGED
        val sourceJson = JSONArray()
        resMesh.params = JSONObject(mapOf(
            "type" to "Merged",
            "sources" to sourceJson
        ))
        scene.targets(srcName).forEach { mesh ->
            resMesh.tetrahedrons.addAll(mesh.applyTransform().tetrahedrons)
            sourceJson.add(JSONObject(mapOf(
                "name" to mesh.name,
                "transform matrix" to mesh.transform.toString(),
                "params" to mesh.params
            )))
        }
        scene.meshList.add(resMesh)
    }
}