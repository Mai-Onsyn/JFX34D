package mai_onsyn.renderer.interfaces

class RendererInterfaceImpl: RendererInterface {
    override val camera = CameraInterfaceImpl()
    override val model = ModelInterfaceImpl()
    override val transform = TransformInterfaceImpl()
    override val geometry = GeometryInterfaceImpl()
    override val shape = ShapeInterfaceImpl()
    override val composition = CompositionInterfaceImpl()
    override val io = IOInterfaceImpl()
}