import mai_onsyn.renderer.cpu4dkt.JNIRasterizer
import org.junit.jupiter.api.Test

class JNITest {
    @Test
    fun rasterizerTransmitTest() {
        val inputArray = floatArrayOf(0f, 3f, 4f, 1.1f)
        val outArray = JNIRasterizer.process(inputArray, 1)
        println(outArray.joinToString(prefix = "[", postfix = "]"))
    }
}