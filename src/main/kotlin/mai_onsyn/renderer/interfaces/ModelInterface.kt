package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Mesh4D

interface ModelInterface {

    /** 列出模型的根路径（去重后的第一段），按添加顺序 */
    fun listModel(): List<String>

    /** 列出场景中全部模型的完整路径（即场景 Map 的 keySet），按添加顺序。UI 建树用 */
    fun listAllModels(): List<String>

    /** 创建模型，需要校验名称是否合法 */
    fun createEmptyModel(name: String)

    /** 删除模型及其整棵子树（路径前缀匹配），必须存在 */
    fun removeModel(name: String)

    /**
     * 重命名模型，连整棵子树一起改名（子模型路径前缀同步跟随）。
     * [path] 可以是真实模型，也可以是**中间/分组路径**（此时按前缀改名整段）。
     * [newName] 是**叶名**，不是路径。
     */
    fun renameModel(path: String, newName: String)

    /**
     * 复制模型：连同其**整棵子树**一起复制（src/... → dst/...）。
     * [srcName] 可以是真实模型或中间/分组路径，[dstName] 必须不存在。
     */
    fun copyModel(srcName: String, dstName: String)

    /** 将两个模型合并成一个新模型 */
    fun mergeModel(src1Name: String, src2Name: String, dstName: String)

    /** 将模型变换矩阵应用至模型顶点 */
    fun applyTransformToVertex(srcName: String, dstName: String)

    /**
     * 隐藏/显示模型及其子模型。
     * [name] 可以是真实模型，也可以是**中间/分组路径**（作用于自身 + 全部后代）。
     */
    fun setModelVisible(name: String, visible: Boolean)

    /** 获取模型实例，AI不使用，其他地方使用 */
    fun getModel(name: String): Mesh4D

    /** 合并当前节点下的所有模型 */
    fun mergeAllSubModels(srcName: String, dstName: String)
}