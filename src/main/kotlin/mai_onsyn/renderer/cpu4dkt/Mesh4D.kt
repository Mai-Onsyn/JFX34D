package mai_onsyn.renderer.cpu4dkt

class Mesh4D @JvmOverloads constructor (
    val tetrahedrons: MutableList<Tetrahedron> = mutableListOf(),
    var name: String = "Unnamed Mesh",
    val transform: Transform4D = Transform4D()
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

    fun applyTransform(): Mesh4D {
        return Mesh4D(name = "$name-applied").also { mesh4D ->
            this.tetrahedrons.forEach { tetrahedron ->
                mesh4D.tetrahedrons.add(tetrahedron.transform(transform.matrix))
            }
            this.transform.matrix = Matrix5f.IDENTITY
        }
    }
}