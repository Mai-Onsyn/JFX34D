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

    override fun createEmptyModel(name: String) {
        scene.requireNotContains(name)
        scene.meshList.add(Mesh4D(name = name).apply { this.kind = MeshKind.GROUP })
    }

    /** 删除模型及其全部子模型（路径前缀） */
    override fun removeModel(name: String) {
        val removed = scene.meshList.removeIf { it.name == name || it.name.startsWith("$name/") }
        if (!removed) throw NoSuchElementException(scene.noSuchMeshMessage(name))
    }

    override fun renameModel(path: String, newName: String) {
        val srcMesh = scene.requireContains(path)
        srcMesh.name = srcMesh.parentPath?.let {
            val newPath = "$path/$newName"
            scene.requireNotContains(path)
            newPath
        } ?: newName
    }

    override fun copyModel(srcName: String, dstName: String) {
        scene.requireNotContains(dstName)
        scene.meshList.add(scene.requireContains(srcName).copy(newName = dstName))
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

    override fun setModelVisible(name: String, visible: Boolean) {
        scene.requireContains(name).visible = visible
    }

    override fun getModel(name: String): Mesh4D = scene.requireContains(name)

    override fun mergeAllSubModels(srcName: String, dstName: String) {
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