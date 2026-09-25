package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.cpu4dkt.Vertex4D

interface GeometryInterface {
    /**
     * 获取模型信息
     * @return 包含模型顶点、四面体数量、编辑次数等信息的markdown，注意顶点信息可能不全
     */
    fun getModelInfos(name: String): String

    /**
     * 获取单个四面体
     * @param id 四面体的id
     * @return 格式化的四面体markdown
     */
    fun getTetrahedronInfos(name: String, tetId: Long): String

    /**
     * 设置四面体的一个顶点
     * @param tetId 四面体的id
     * @param vertexNum 顶点编号，可选[0, 1, 2, 3]
     * @param vertex 包含了颜色、位置的顶点
     */
    fun setVertex(name: String, tetId: Long, vertexNum: Int, vertex: Vertex4D)

    /**
     * 用矩阵对指定四面体进行变换
     * @param tets 目标四面体列表
     * @param transform 变换矩阵
     */
    fun transformMatrix(name: String, tets: List<Int>, transform: Matrix5f)

    /**
     * 添加四面体
     * 与Shape接口的createTetrahedron不同，这个是直接指定四面体的顶点
     * 顶点颜色可用统一指定，也可以每个顶点单独指定，这里只接收完成后的四面体对象
     * @return 新加的四面体的id
     */
    fun addTetrahedron(name: String, tetrahedron: Tetrahedron): Int

    /** 删除四面体 */
    fun removeTetrahedron(name: String, tetId: Long)

    /**
     * 用超平面切片模型
     * @param plane 超平面，无限延伸
     * @param destA 切片后的一半模型名称
     * @param destB 切片后的另一半模型名称
     */
    fun sliceModel(name: String, plane: Tetrahedron, destA: String, destB: String)

}