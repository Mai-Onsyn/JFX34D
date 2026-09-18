package mai_onsyn.renderer.cpu4dkt

class SimpleScene4D(
    val meshList: MutableList<Mesh4D> = mutableListOf(),
    val camera4D: Camera4D = Camera4D()
): Scene4D {
    override fun getMeshes(): List<Mesh4D> {
        return meshList
    }

    override fun getCamera(): Camera4D {
        return camera4D
    }
}