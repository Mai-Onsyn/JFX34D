package mai_onsyn.renderer.interfaces

import mai_onsyn.renderer.ogl3d.data.Mesh
import org.joml.Vector4f

interface ShapeInterface {

    /** 创建正四面超平面（四个顶点在坐标轴上radius位置） */
    fun createTetrahedron(target: String, center: Vector4f, radius: Float)

    /** 创建四维单纯形 */
    fun create5Cell(target: String, center: Vector4f, size: Float)

    /** 创建超八面体 */
    fun create16Cell(target: String, center: Vector4f, radius: Float)

    /** 创建超立方体 */
    fun createTesseract(target: String, center: Vector4f, edgeLength: Float)

    /** 创建超棱柱 */
    fun createPrism4(target: String, base: Mesh, ws: Float, we: Float)

    /** 创建超锥 */
    fun createCone4(target: String, base: Mesh, apex: Vector4f)

    /** 创建超球 */
    fun createBall4(target: String, center: Vector4f, radius: Float, density: Float)
}