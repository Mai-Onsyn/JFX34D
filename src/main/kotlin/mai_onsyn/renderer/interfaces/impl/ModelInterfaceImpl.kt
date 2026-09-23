package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.ModelInterface

class ModelInterfaceImpl(
    private val scene: SimpleScene4D,
): ModelInterface {
    override fun listModel(): List<String> = scene.getMeshes().map { it.name.trim() }

    override fun addModel(name: String) {
        if (scene.meshList.find { it.name == name } != null) throw IllegalArgumentException("Model \"$name\" already exists")
        scene.meshList.add(Mesh4D(name = name))
    }

    override fun removeModel(name: String) {
        if (scene.meshList.find { it.name == name } == null) throw IllegalArgumentException("Model \"$name\" does not exist")
        scene.meshList.removeIf { it.name.trim() == name.trim() }
    }

    override fun copyModel(srcName: String, dstName: String) {
        val source = scene.meshList.find { it.name == srcName }
        if (source == null) throw IllegalArgumentException("Source model \"$srcName\" does not exist")
        if (scene.meshList.find { it.name == dstName } != null) throw IllegalArgumentException("Destination model \"$dstName\" already exists")
        scene.meshList.add(source.copy(newName = dstName))
    }

    override fun mergeModel(src1Name: String, src2Name: String, dstName: String) {
        TODO("Not yet implemented")
    }

    override fun applyTransformToVertex(srcName: String, dstName: String) {
        TODO("Not yet implemented")
    }
}