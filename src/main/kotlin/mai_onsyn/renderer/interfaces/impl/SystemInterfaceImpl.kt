package mai_onsyn.renderer.interfaces.impl

import javafx.scene.paint.Color
import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.interfaces.SystemInterface
import kotlin.math.roundToInt

class SystemInterfaceImpl(
    private val region: GL4DRegion
): SystemInterface {
    override fun set3DMaxFPS(fps: Float) {
        region.fps = fps.toDouble()
    }

    override fun get3DFPS(): Float = region.newFPSCounter.getAverageFrequency()

    override fun get3D1PercentLowFPS(): Float = region.newFPSCounter.getOnePercentLowFrequency()

    override fun set4DMaxFPS(fps: Float) {
        region.maxFPS = fps.roundToInt()
    }

    override fun get4DFPS(): Float = region.get4DFPS()

    override fun get4D1PercentLowFPS(): Float = region.get4D1PercentLowFPS()

    override fun enableTriangleLineRendering(enable: Boolean) = region.setOutlineRendering(enable)

    override fun setBackgroundColor(color: Color) = region.setBackgroundColor(color)

    override fun enableLightRendering(enable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setDisplaySize(edgeLength: Float) = region.setViewPortLength(edgeLength)
}