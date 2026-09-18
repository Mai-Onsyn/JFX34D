package mai_onsyn.renderer.cpu4dkt

import mai_onsyn.renderer.ogl3d.data.Camera

interface Scene4D {
    fun getMeshes(): List<Mesh4D>
    fun getCamera(): Camera4D
}