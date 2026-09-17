import mai_onsyn.renderer.cpu4dkt.JNIRasterizer
import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.constructHypercube
import org.junit.jupiter.api.Test

class HypercubeTest {
    @Test
    fun hypercubeTest() {
        val cube = Mesh4D(constructHypercube())
        println(cube.tetrahedrons.size)
        val flattened = JNIRasterizer.packMesh4D(cube)
        for (i in flattened.indices step 20) {
            println(flattened.copyOfRange(i, i + 20).joinToString { "%.2f".format(it) })
        }

        println("\n".repeat(4))

        val rebuilt = JNIRasterizer.extractMesh4D(flattened)
        rebuilt.tetrahedrons.forEach {
            println(it)
        }

        println("\n".repeat(4))

        val I = Matrix5f.IDENTITY.data
        val outArray = JNIRasterizer.process(flattened, cube.tetrahedrons.size, I, I, I, I)
        for (i in outArray.indices step 16) {
            println(outArray.copyOfRange(i, i + 16).joinToString { "%.2f".format(it) })
        }
    }
}