package HCloudbyte.ui.viewport

import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.ogl3d.data.Mesh
import mai_onsyn.renderer.ogl3d.generator.CubeFace
import mai_onsyn.renderer.ogl3d.generator.createCube
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Matrix4f

/**
 * 视口区域指示方块。
 *
 * <p>一个半透明正方体（六个面各自染色，alpha = [ALPHA]），边长 = 4D 视口的显示边长，
 * 挂在 [GL4DRegion.scene3D] 里，用来在 3D 视图里直观标出「4D 视口」的空间范围。
 *
 * <p>说明：
 * - 顶点只在构造时按 [DEFAULT_SIZE] 生成一次；实际边长靠 mesh 的模型矩阵缩放实现，
 *   所以改 [size] 不需要重建顶点、也没有跨线程改三角形列表的风险；
 * - 显示 / 隐藏只是往 scene3D 的 mesh 列表里加 / 删**同一个** [Mesh] 实例
 *   （复用实例 = 不重复申请 GPU 资源；meshList 是 CopyOnWriteArrayList，跨线程安全）。
 */
class ViewportBox @JvmOverloads constructor(
    private val region: GL4DRegion,
    size: Float = DEFAULT_SIZE,
) {

    /** 边长（= 4D 视口显示边长，对应 SceneInterface.setDisplaySize）。 */
    var size: Float = size
        set(value) {
            if (value <= 0f || value == field) return
            field = value
            applyScale()
        }

    private val mesh: Mesh = Mesh(createCube(size = DEFAULT_SIZE, colorOf = { face ->
        when (face) {
            CubeFace.TOP    -> ColorARGB(1f, 0f, 0f, ALPHA)
            CubeFace.BOTTOM -> ColorARGB(0f, 1f, 0f, ALPHA)
            CubeFace.LEFT   -> ColorARGB(0f, 0f, 1f, ALPHA)
            CubeFace.RIGHT  -> ColorARGB(1f, 1f, 0f, ALPHA)
            CubeFace.FRONT  -> ColorARGB(0f, 1f, 1f, ALPHA)
            CubeFace.BACK   -> ColorARGB(1f, 0f, 1f, ALPHA)
        }
    }))

    init {
        applyScale()
    }

    /** 当前是否显示。 */
    val isVisible: Boolean
        get() = region.scene3D.meshList.contains(mesh)

    /** 显示 / 隐藏。 */
    fun setVisible(visible: Boolean) {
        val list = region.scene3D.meshList
        if (visible) {
            if (!list.contains(mesh)) list.add(mesh)
        } else {
            list.remove(mesh)
        }
    }

    /** 切换显示状态。 */
    fun toggle() = setVisible(!isVisible)

    /** 按当前 [size] 设置模型矩阵缩放（单位方块 -> 目标边长）。 */
    private fun applyScale() {
        mesh.transform.matrix = Matrix4f().scale(size / DEFAULT_SIZE)
    }

    companion object {
        /** 默认边长，与 4D 视口显示边长的默认值一致。 */
        const val DEFAULT_SIZE = 8f

        /** 每个面的透明度。 */
        private const val ALPHA = 0.1f
    }
}
