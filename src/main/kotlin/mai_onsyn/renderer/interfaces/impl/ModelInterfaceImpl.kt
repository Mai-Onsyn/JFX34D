package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.ModelInterface
import mai_onsyn.renderer.interfaces.requireContains
import mai_onsyn.renderer.interfaces.requireNotContains

class ModelInterfaceImpl(
    private val scene: SimpleScene4D,
): ModelInterface {
    override fun listModel(): List<String> = scene.getMeshes().map { it.name.trim() }

    override fun addModel(name: String) {
        scene.requireNotContains(name)
        scene.meshList.add(Mesh4D(name = name))
    }

    override fun removeModel(name: String) {
        scene.requireContains(name)
        scene.meshList.removeIf { it.name.trim() == name.trim() }
    }

    override fun copyModel(srcName: String, dstName: String) {
        scene.requireNotContains(dstName)
        scene.meshList.add(scene.requireContains(srcName).copy(newName = dstName))
    }

    override fun mergeModel(src1Name: String, src2Name: String, dstName: String) {
        TODO("Not yet implemented")
    }

    override fun applyTransformToVertex(srcName: String, dstName: String) {
        TODO("Not yet implemented")
    }
}