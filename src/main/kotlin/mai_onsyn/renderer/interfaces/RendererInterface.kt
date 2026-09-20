package mai_onsyn.renderer.interfaces

interface RendererInterface {

    companion object {
        val INSTANCE = RendererInterfaceImpl()
    }

    /**
     * 摄像机相关的操作
     */
    val camera: CameraInterface

    /**
     * 基本模型管理操作，比如创建或者删除
     */
    val model: ModelInterface

    /**
     * 模型变换，作用到整个模型上的操作
     */
    val transform: TransformInterface

    /**
     * 模型编辑，具体操作模型上的顶点
     */
    val geometry: GeometryInterface

    /**
     * 快速添加形状
     */
    val shape: ShapeInterface

    /**
     * 管理模型的合并与拆分
     */
    val composition: CompositionInterface

    /**
     * 操作模型与文件的交互
     */
    val io: IOInterface
}