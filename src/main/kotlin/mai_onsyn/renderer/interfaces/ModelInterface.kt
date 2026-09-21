package mai_onsyn.renderer.interfaces

interface ModelInterface {

    /** 列出模型，返 */
    fun listModel(): List<String>

    /** 创建模型，需要校验名称是否合法 */
    fun addModel(name: String)

    /** 删除模型，必须存在 */
    fun removeModel(name: String)

    /** 复制模型 */
    fun copyModel(srcName: String, dstName: String)
}