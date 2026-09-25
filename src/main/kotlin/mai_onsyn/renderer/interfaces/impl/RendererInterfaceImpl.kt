package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.interfaces.RendererInterface

class RendererInterfaceImpl(
    region: GL4DRegion
): RendererInterface {
    override val scene = SceneInterfaceImpl(region)
    override val camera = CameraInterfaceImpl(region.scene4D.getCamera())
    override val model = ModelInterfaceImpl(region.scene4D)
    override val transform = TransformInterfaceImpl(region.scene4D)
    override val geometry = GeometryInterfaceImpl()
    override val shape = ShapeInterfaceImpl(region.scene4D)
    override val io = IOInterfaceImpl()
}