package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.interfaces.SystemInterface

class SystemInterfaceImpl(
    private val region: GL4DRegion
): SystemInterface {
    override fun set3DMaxFPS(fps: Float) {
        TODO("Not yet implemented")
    }

    override fun get3DFPS(): Float = region.newFPSCounter.getAverageFrequency()

    override fun get3D1PercentLowFPS(): Float = region.newFPSCounter.getOnePercentLowFrequency()

    override fun set4DMaxFPS(fps: Float) {
        TODO("Not yet implemented")
    }

    override fun get4DFPS(): Float {
        TODO("Not yet implemented")
    }

    override fun enableTriangleLineRendering(enable: Boolean) = region.setOutlineRendering(enable)

    override fun enableLightRendering(enable: Boolean) {
        TODO("Not yet implemented")
    }
}