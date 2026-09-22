package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.interfaces.GeometryInterface

class GeometryInterfaceImpl: GeometryInterface {
    override fun getModelInfos(name: String): String {
        TODO("Not yet implemented")
    }

    override fun getTetrahedronInfos(name: String, tetId: Int): String {
        TODO("Not yet implemented")
    }

    override fun setVertex(
        name: String,
        tetId: Int,
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

    override fun removeTetrahedron(name: String, tetId: Int) {
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
}