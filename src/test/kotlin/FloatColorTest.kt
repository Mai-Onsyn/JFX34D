import kotlin.test.Test

class FloatColorTest {
    @Test
    fun floatColorTest() {
        val multiplier = 0.003921569f
        require(multiplier * 255f == 1.0f)
    }
}