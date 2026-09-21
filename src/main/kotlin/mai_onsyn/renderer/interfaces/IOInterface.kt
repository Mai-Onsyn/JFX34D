package mai_onsyn.renderer.interfaces

import java.io.File

interface IOInterface {

    /**
     * 保存模型到指定文件
     * @param fileName 不包含后缀的文件名，用相对路径
     */
    fun saveModel(name: String, fileName: String)

    /**
     * 加载模型
     * @param file 绝对路径，带后缀
     */
    fun loadModel(name: String, file: File)
}