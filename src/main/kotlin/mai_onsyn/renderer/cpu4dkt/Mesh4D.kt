package mai_onsyn.renderer.cpu4dkt

class Mesh4D(
    val tetrahedrons: MutableList<Tetrahedron> = mutableListOf(),
    var name: String = "New Mesh"
) {
    var dirty: Boolean = true

    fun copy(newName: String) : Mesh4D {
        return Mesh4D(name = newName).also { mesh4D ->
            this.tetrahedrons.forEach { tetrahedron ->
                // 这里是浅拷贝 Vertex是复用的 但是操作最小对象是Vertex 所以应该没问题
                mesh4D.tetrahedrons.add(tetrahedron.copy())
            }
        }
    }
}