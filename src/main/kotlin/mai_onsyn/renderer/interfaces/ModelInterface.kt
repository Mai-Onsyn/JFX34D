package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Mesh4D

interface ModelInterface {

    /** 列出模型，返回所有模型名称的列表 */
    fun listModel(): List<String>

    /** 创建模型，需要校验名称是否合法 */
    fun createEmptyModel(name: String)

    /** 删除模型，必须存在 */
    fun removeModel(name: String)

    /** 复制模型 */
    fun copyModel(srcName: String, dstName: String)

    /** 将两个模型合并成一个新模型 */
    fun mergeModel(src1Name: String, src2Name: String, dstName: String)

    /** 将模型变换矩阵应用至模型顶点 */
    fun applyTransformToVertex(srcName: String, dstName: String)

    /** 隐藏指定模型及其子模型 */
    fun setModelVisible(name: String, visible: Boolean)

    /** 获取模型实例，AI不使用，其他地方使用 */
    fun getModel(name: String): Mesh4D
}