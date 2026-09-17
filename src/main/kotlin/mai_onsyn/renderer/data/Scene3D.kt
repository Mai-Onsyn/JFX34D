package mai_onsyn.renderer.data

interface Scene3D {
    fun getMeshes(): List<Mesh>
    fun getCamera(): Camera
}