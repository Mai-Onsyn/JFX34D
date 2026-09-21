package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.interfaces.RendererInterface

class RendererInterfaceImpl: RendererInterface {
    override val camera = CameraInterfaceImpl()
    override val model = ModelInterfaceImpl()
    override val transform = TransformInterfaceImpl()
    override val geometry = GeometryInterfaceImpl()
    override val shape = ShapeInterfaceImpl()
    override val io = IOInterfaceImpl()
}