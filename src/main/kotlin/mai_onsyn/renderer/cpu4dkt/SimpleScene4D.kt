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

    /** 精确命中一个模型；路径只做前缀的"分组路径"不要用这个 */
    fun findMesh(path: String): Mesh4D? = meshList.find { it.name == path }

    /** 直接子模型（Tower -> Tower/Base, Tower/Lid） */
    fun childrenOf(parent: String): List<Mesh4D> = meshList.filter { it.parentPath == parent }

    /** 全部后代，不含自身 */
    fun subTree(path: String): List<Mesh4D> = meshList.filter { it.name.startsWith("$path/") }

    /** 直接子模型的路径名 */
    fun subModels(parent: String): List<String> {
        return childrenOf(parent).map { it.name }
    }
}
