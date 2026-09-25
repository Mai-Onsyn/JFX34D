package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.SimpleScene4D
import mai_onsyn.renderer.interfaces.IOInterface
import mai_onsyn.renderer.ogl3d.data.SimpleScene3D
import java.io.File

class IOInterfaceImpl(
    scene3D: SimpleScene3D,
    scene4D: SimpleScene4D
): IOInterface {
    override fun saveModel(name: String, fileName: String) {
        TODO("Not yet implemented")
    }

    override fun loadModel(name: String, file: File) {
        TODO("Not yet implemented")
    }
}