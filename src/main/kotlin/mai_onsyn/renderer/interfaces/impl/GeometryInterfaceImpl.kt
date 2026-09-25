package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.interfaces.GeometryInterface

class GeometryInterfaceImpl(
    private val scene: SimpleScene4D
): GeometryInterface {
    override fun getModelInfos(name: String): String =
        scene.requireContains(name).run { """
            ## Information of %s
            Tetrahedron count = %d 
            
            Model matrix:
            ```text
            %s
            ```
            
            Tetrahedron IDs:
            ```text
            %s
            ```
        """.trimIndent().format(
            name, tetrahedrons.size, transform.toString(),
            StringBuilder().apply {
                for ((idx, i) in this@run.tetrahedrons.withIndex()) {
                    if (idx != 0) {
                        if (idx % 10 == 0) append('\n')
                        else append(' ')
                    }
                    append(i.id)
                }
            }.toString()
        ) }

    override fun getTetrahedronInfos(name: String, tetId: Long): String =
        scene.requireContains(name, tetId).run { """
            ## Information of tetrahedron %d in %s
            v0: %s
            v1: %s
            v2: %s
            v3: %s
        """.trimIndent().format(
            tetId, name,
            this.v0, this.v1, this.v2, this.v3
        ) }

    override fun setVertex(
        name: String,
        tetId: Long,
        vertexNum: Int,
        vertex: Vertex4D
    ) {
        TODO("Not yet implemented")
    }

    override fun transformMatrix(
        name: String,
        tets: List<Int>,
        transform: Matrix5f
    ) {
        TODO("Not yet implemented")
    }

    override fun addTetrahedron(name: String, tetrahedron: Tetrahedron): Int {
        TODO("Not yet implemented")
    }

    override fun removeTetrahedron(name: String, tetId: Long) {
        TODO("Not yet implemented")
    }

    override fun sliceModel(
        name: String,
        plane: Tetrahedron,
        destA: String,
        destB: String
    ) {
        TODO("Not yet implemented")
    }

    private fun SimpleScene4D.requireContains(name: String, tetId: Long): Tetrahedron {
        val mesh = this.requireContains(name)
        val find = mesh.tetrahedrons.find { it.id == tetId }
        if (find == null) throw NoSuchElementException("No tetrahedron with ID $tetId is found in $name")
        return find
    }
}