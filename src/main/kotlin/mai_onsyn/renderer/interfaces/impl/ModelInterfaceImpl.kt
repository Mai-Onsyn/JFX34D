package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.ModelInterface
import mai_onsyn.renderer.interfaces.requireContains
import mai_onsyn.renderer.interfaces.requireNotContains

class ModelInterfaceImpl(
    private val scene: SimpleScene4D,
): ModelInterface {
    override fun listModel(): List<String> = scene.getMeshes().map { it.name }

    override fun createModel(name: String) {
        scene.requireNotContains(name)
        scene.meshList.add(Mesh4D(name = name))
    }

    override fun removeModel(name: String) {
        scene.requireContains(name)
        scene.meshList.removeIf { it.name == name }
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
        })
    }

    override fun applyTransformToVertex(srcName: String, dstName: String) {
        scene.requireNotContains(dstName)
        val applied = scene.requireContains(srcName).applyTransform()
        applied.name = dstName
        scene.meshList.add(applied)
    }
}