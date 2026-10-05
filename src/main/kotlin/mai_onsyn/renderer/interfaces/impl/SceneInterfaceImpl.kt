package mai_onsyn.renderer.interfaces.impl

import javafx.scene.paint.Color
import mai_onsyn.renderer.core.GL4DRegion
import mai_onsyn.renderer.cpu4dkt.CameraOrientation
import mai_onsyn.renderer.interfaces.SceneInterface
import mai_onsyn.renderer.ogl3d.data.Light
import mai_onsyn.renderer.utils.ColorARGB
import kotlin.math.roundToInt

class SceneInterfaceImpl(
    private val region: GL4DRegion
): SceneInterface {
    override fun set3DMaxFPS(fps: Float) {
        region.fps = fps.toDouble()
    }

    override fun get3DFPS(): Float = region.newFPSCounter.getAverageFrequency()

    override fun get3D1PercentLowFPS(): Float = region.newFPSCounter.getOnePercentLowFrequency()

    override fun set4DMaxFPS(fps: Float) {
        region.maxFPS = fps.roundToInt()
    }

    override fun get4DFPS(): Float = region.get4DFPS()

    override fun get4D1PercentLowFPS(): Float = region.get4D1PercentLowFPS()

    override fun enableTriangleLineRendering(enable: Boolean) = region.set4DOutlineRendering(enable)

    override fun setBackgroundColor(color: Color) = region.setBackgroundColor(color)

    override fun enableLightRendering(enable: Boolean) = region.enableLightRendering(enable)

    override fun listLight(): String = StringBuilder("## Lights in 3D Scene").apply {
        for (l in region.scene3D.lightList) {
            append("\n- ")
            // 名字必须显式打出来：removeLights 是按名字删的，
            // 而 Light.toString() 里没有名字，光靠它 AI 永远不知道光源叫什么
            append(l.name)
            append(' ')
            append(l)
        }
    }.toString()

    override fun listLights(): List<Light> = region.scene3D.lightList

    override fun addLights(light: Light) = region.scene3D.addLight(light)

    override fun removeLights(names: List<String>) {
        val unContained = names.filter { region.scene3D.lightList.find { l -> l.name == it } == null }
        if (unContained.isNotEmpty()) {
            throw IllegalArgumentException("There is no lights named [${unContained.joinToString()}]")
        }
        region.scene3D.lightList.removeIf { it.name in names }
    }

    override fun setAmbientLight(ambientLight: ColorARGB) = region.scene3D.setAmbient(ambientLight)

    override fun setDisplaySize(edgeLength: Float) = region.setViewPortLength(edgeLength)

    override fun setOnEnable4DInputChanged(eventHandler: (Boolean) -> Unit) {
        region.onEnable4DInputChanged = eventHandler
    }

    override fun setEnable4DInput(enable: Boolean) = region.enable4DInput(enable)

    override fun set4DCameraMoveSpeed(speed: Float) {
        region.move4DSpeed = speed
    }

    override fun set4DCameraRotateSpeed(speed: Float) {
        region.mouse4DSpeed = speed
    }

    override fun set4DCameraFov(fov: Float) {
        region.scene4D.camera4D.fov = fov
    }

    override fun get4DCameraOrientation(): CameraOrientation = region.scene4D.getCamera().getCameraOrientation()

    override fun set3DCameraMoveSpeed(speed: Float) {
        region.move3DSpeed = speed
    }

    override fun set3DCameraRotateSpeed(speed: Float) {
        region.mouse3DSpeed = speed
    }

    override fun set3DCameraFov(fov: Float) {
        region.scene3D.getCamera().fov = fov
    }
}