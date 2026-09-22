package mai_onsyn.renderer.core

import mai_onsyn.renderer.interfaces.RendererInterface

object RendererInterfaceInitializer {
    fun initialize(
        region: GL4DRegion
    ) {
        RendererInterface.init(region.scene4D.getCamera())
    }
}