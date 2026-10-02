package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.cpu4dkt.Vertex4D
import mai_onsyn.renderer.interfaces.IOInterface
import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector4f
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class IOInterfaceImpl(
    private val scene: SimpleScene4D
): IOInterface {
    
    private data class TetVertexReference(
        val pos: Int,
        val color: Int,
        val normal: Int
    )

    private data class TetReference(
        val v0: TetVertexReference,
        val v1: TetVertexReference,
        val v2: TetVertexReference,
        val v3: TetVertexReference
    )

    private fun Vector4f.roundForIndex(): Vector4f = Vector4f(
        (x * 10000f).roundToInt() / 10000f,
        (y * 10000f).roundToInt() / 10000f,
        (z * 10000f).roundToInt() / 10000f,
        (w * 10000f).roundToInt() / 10000f
    )
    
    override fun saveModel(name: String, fileName: String) {
        val file = File(fileName)
        val prefix = name.substringAfterLast('/', "")
        file.bufferedWriter().use { fos ->
            val space = ' '.code
            val wrap = '\n'.code
            scene.targets(name).forEach { model ->
                if (model.tetrahedrons.isEmpty()) return@forEach
                fos.write('g'.code)
                fos.write(space)
                fos.write(model.name.removePrefix(prefix))
                fos.write(wrap)
                fos.write(wrap)

                val applied = model.applyTransform()
                
                val posIndexMap = LinkedHashMap<Vector4f, Int>()
                val colorIndexMap = LinkedHashMap<ColorARGB, Int>()
                val normalIndexMap = LinkedHashMap<Vector4f, Int>()
                fun pushIntoMap(vertex: Vertex4D): TetVertexReference {
                    return TetVertexReference(
                        posIndexMap.getOrPut(vertex.pos.roundForIndex()) { posIndexMap.size },
                        colorIndexMap.getOrPut(vertex.color) { colorIndexMap.size },
                        normalIndexMap.getOrPut(vertex.normal.roundForIndex()) { normalIndexMap.size }
                    )
                }
                val tets = ArrayList<TetReference>(applied.tetrahedrons.size)
                applied.tetrahedrons.forEach { tetrahedron ->
                    tets.add(TetReference(
                        pushIntoMap(tetrahedron.v0),
                        pushIntoMap(tetrahedron.v1),
                        pushIntoMap(tetrahedron.v2),
                        pushIntoMap(tetrahedron.v3)
                    ))
                }

                posIndexMap.keys.forEach {
                    fos.write("v %.4f %.4f %.4f %.4f\n".format(
                        it.x, it.y, it.z, it.w
                    ))
                }
                fos.write(wrap)

                colorIndexMap.keys.forEach {
                    fos.write("c $it\n")
                }
                fos.write(wrap)

                normalIndexMap.keys.forEach {
                    fos.write("n %.4f %.4f %.4f %.4f\n".format(
                        it.x, it.y, it.z, it.w
                    ))
                }
                fos.write(wrap)

                tets.forEach {
                    fos.write("t %d/%d/%d %d/%d/%d %d/%d/%d %d/%d/%d\n".format(
                        it.v0.pos, it.v0.color, it.v0.normal,
                        it.v1.pos, it.v1.color, it.v1.normal,
                        it.v2.pos, it.v2.color, it.v2.normal,
                        it.v3.pos, it.v3.color, it.v3.normal
                    ))
                }
                fos.write(wrap)
            }

            fos.flush()
        }
    }

    override fun loadModel(name: String, file: File) {
        TODO("Not yet implemented")
    }
}