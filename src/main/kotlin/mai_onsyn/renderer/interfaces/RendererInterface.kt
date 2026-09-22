package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.cpu4dkt.Camera4D
import mai_onsyn.renderer.interfaces.impl.RendererInterfaceImpl

interface RendererInterface {

    companion object {
        @Volatile
        private var _instance: RendererInterface? = null

        val INSTANCE: RendererInterface
            get() = _instance ?: error("RendererInterface is NOT initialized yet, please invoke init() first")

        fun init(
            camera: Camera4D
        ) {
            if (_instance == null) {
                synchronized(this) {
                    if (_instance == null) {
                        _instance = RendererInterfaceImpl(camera)
                    }
                }
            }
        }
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
     * 操作模型与文件的交互
     */
    val io: IOInterface
}