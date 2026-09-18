import mai_onsyn.renderer.cpu4dkt.Camera4D
import mai_onsyn.renderer.cpu4dkt.JNIRasterizer
import mai_onsyn.renderer.cpu4dkt.Matrix5f
import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.constructHypercube
import org.joml.Vector4f
import org.junit.jupiter.api.Test

class HypercubeTest {
    @Test
    fun hypercubeTest() {
        val cube = Mesh4D(constructHypercube())
        println(cube.tetrahedrons.size)
        val flattened = JNIRasterizer.packMesh4D(cube)
        for (i in flattened.indices step 36) {
            println(flattened.copyOfRange(i, i + 36).joinToString { "%.2f".format(it) })
        }

        println("\n".repeat(4))

        val rebuilt = JNIRasterizer.extractMesh4D(flattened)
        rebuilt.tetrahedrons.forEach {
            println(it)
        }

        println("\n".repeat(4))

        val camera = Camera4D(pos = Vector4f(0f, 0f, 0f, -5f))
        val I = Matrix5f.IDENTITY.data
        val outArray = JNIRasterizer.project(flattened, cube.tetrahedrons.size, I, camera.viewMatrix.data, camera.projectionMatrix().data, I)
        for (i in outArray.indices step 28) {
            println(outArray.copyOfRange(i, i + 28).joinToString { "%.2f".format(it) })
        }
    }
}