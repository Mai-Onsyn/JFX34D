package mai_onsyn.renderer.ogl3d.data

interface Scene3D {
    fun getMeshes(): List<Mesh>
    fun getCamera(): Camera
}