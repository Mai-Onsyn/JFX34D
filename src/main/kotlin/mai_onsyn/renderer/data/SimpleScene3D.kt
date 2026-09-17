package mai_onsyn.renderer.data

class SimpleScene3D(
    val meshList: MutableList<Mesh> = mutableListOf<Mesh>(),
    val mainCamera: Camera = Camera()
) : Scene3D {
    override fun getMeshes(): List<Mesh> {
        return meshList
    }

    override fun getCamera(): Camera = mainCamera
}