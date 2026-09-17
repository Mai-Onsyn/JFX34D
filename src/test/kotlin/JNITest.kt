import mai_onsyn.renderer.cpu4dkt.JNIRasterizer
import mai_onsyn.renderer.cpu4dkt.Matrix5f
import org.junit.jupiter.api.Test

class JNITest {
    @Test
    fun rasterizerTransmitTest() {
        val inputArray = floatArrayOf(0f, 3f, 4f, 1.1f)
        val I = Matrix5f.IDENTITY.data
        val outArray = JNIRasterizer.process(inputArray, 1, I, I, I, I)
        println(outArray.joinToString(prefix = "[", postfix = "]"))
    }
}