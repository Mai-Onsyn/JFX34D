package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Tetrahedron
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f

/**
 * 模型几何编辑：直接操作某个模型路径上的四面体与顶点。
 *
 * 寻址约定（与 [ShapeInterface] 一致）：
 * - name 一律是 "模型/部件" 形式的完整路径，如 "Tower/Base"；
 * - 四面体用**该模型内的索引**寻址，从 0 开始按加入顺序，不用 Tetrahedron.id；
 *   删除后其后的索引顺移，但只在同一个模型内部，不会影响别的部件；
 * - 顶点/四面体级编辑会把目标从 SHAPE 降级为 CARVED（params 不再能描述它）。
 *
 * 这些是"雕刻"级的接口，AI 的主路径应该是 [ShapeInterface] 的部件级操作。
 */
interface GeometryInterface {

    /**
     * 模型整体信息：kind / params / 四面体数 / 世界包围盒 / 变换矩阵 / 子模型。
     * name 可以是一个"分组路径"（如 "Tower"），此时返回它下面所有子模型的汇总。**不列出顶点**。
     */
    fun getModelInfos(name: String): String

    /**
     * 获取单个四面体的四个顶点
     * @param tetIndex 四面体在该模型内的索引，从 0 开始
     */
    fun getTetrahedronInfos(name: String, tetIndex: Int): String

    /**
     * 设置四面体的一个顶点
     * @param tetIndex 四面体索引
     * @param vertexNum 顶点编号，可选 [0, 1, 2, 3]
     * @param pos 新的四维位置
     * @param color 新颜色，null 表示保持原值
     * @param normal 新法向量，null 表示保持原值
     */
    fun setVertex(
        name: String,
        tetIndex: Int,
        vertexNum: Int,
        pos: Vector4f,
        color: ColorARGB? = null,
        normal: Vector4f? = null
    )

    /**
     * 用矩阵变换指定四面体
     * @param tetIndices 目标四面体索引列表，**空列表表示该模型内全部四面体**
     */
    fun transformTetrahedrons(name: String, tetIndices: List<Int>, transform: Matrix5f)

    /**
     * 添加四面体
     * 与 [ShapeInterface] 的 createXxx 不同，这个是直接指定四面体的四个顶点
     * @return 新四面体在该模型内的索引
     */
    fun addTetrahedron(name: String, tetrahedron: Tetrahedron): Int

    /** 按索引删除四面体 */
    fun removeTetrahedron(name: String, tetIndex: Int)

    /**
     * 用超平面精确切割模型，产出两个新模型（都是 CARVED）
     * @param plane 超平面，由四个仿射无关的顶点定义，坐标取源模型的**局部坐标系**
     * @param pathA 切割后 f(p) < 0 那一半的模型路径
     * @param pathB 切割后 f(p) > 0 那一半的模型路径
     */
    fun sliceModel(name: String, plane: Tetrahedron, pathA: String, pathB: String)
}
