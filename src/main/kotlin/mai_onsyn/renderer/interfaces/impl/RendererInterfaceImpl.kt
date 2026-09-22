package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Camera4D
import mai_onsyn.renderer.interfaces.RendererInterface

class RendererInterfaceImpl(
    _IN_CAMERA: Camera4D
): RendererInterface {
    override val camera = CameraInterfaceImpl(_IN_CAMERA)
    override val model = ModelInterfaceImpl()
    override val transform = TransformInterfaceImpl()
    override val geometry = GeometryInterfaceImpl()
    override val shape = ShapeInterfaceImpl()
    override val io = IOInterfaceImpl()
}