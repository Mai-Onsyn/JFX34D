package mai_onsyn.renderer.ogl3d.data

import mai_onsyn.renderer.utils.ColorARGB
import org.joml.Vector3f

/**
 * 光源, 纯数据, 不碰 GL。
 *
 * 位置/方向都是**世界空间**, 和相机 viewPos 一个坐标系。
 * 颜色用 [ColorARGB] (提交给 shader 时 /255 转成 0..1 的浮点色)。
 * 环境光不在光源上, 在场景上 ([Scene3D.getAmbient])。
 *
 * 两种类型, 靠 [direction] 区分 (和 C++ 侧 LightType::Point / Face 对应):
 *   - 点光源: [direction] == null, 各向均匀
 *   - 面光源: [direction] != null, 只照它朝向的反方向那一侧,
 *             方向衰减 = max(0, -dot(dir, L)), L 是片元指向光源的单位向量
 */
class Light @JvmOverloads constructor(
    /** 光源名称, 用来区分是哪个光源 */
    var name: String = "Light",
    /** 光源位置 (世界空间) */
    var pos: Vector3f = Vector3f(0f, 50f, 0f),
    /** 光源颜色 */
    var color: ColorARGB = ColorARGB(1f, 1f, 1f, 1f),
    /** 面光源朝向 (世界空间); null = 点光源 */
    var direction: Vector3f? = null,
    /** 光源强度, 乘在颜色上 */
    var intensity: Float = 0.6f,
    /** 有效距离: 超过这个距离的片元不参与这个光源的照明 */
    var range: Float = 600f,
    /** 距离衰减系数: 1 / (1 + a*d + b*d*d) */
    var attenuationA: Float = 0.00007f,
    var attenuationB: Float = 0.00003f,
) {
    val isFace: Boolean
        get() = direction != null

    /** 把光源挪到世界空间里的某个位置 */
    fun moveTo(x: Float, y: Float, z: Float): Light {
        pos.set(x, y, z)
        return this
    }

    fun moveTo(position: Vector3f): Light = moveTo(position.x, position.y, position.z)

    fun setColor(color: ColorARGB): Light {
        this.color = color
        return this
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float = 1f): Light =
        setColor(ColorARGB(r, g, b, a))

    /** 变成面光源, 朝向 (x, y, z); 只照这个朝向的反方向那一侧 */
    fun faceTo(x: Float, y: Float, z: Float): Light {
        direction = Vector3f(x, y, z)
        return this
    }

    fun faceTo(direction: Vector3f): Light = faceTo(direction.x, direction.y, direction.z)

    /** 变回点光源 */
    fun point(): Light {
        direction = null
        return this
    }

    fun setIntensity(intensity: Float, range: Float = this.range): Light {
        this.intensity = intensity
        this.range = range
        return this
    }

    fun setAttenuation(a: Float, b: Float): Light {
        attenuationA = a
        attenuationB = b
        return this
    }

    override fun toString(): String =
        "Light{pos=(%.2f, %.2f, %.2f), color=%s, direction=%s, intensity=%.2f, range=%.2f, att.A=%.2f, att.B=%.2f)}".format(
            pos.x, pos.y, pos.z, color.toString(), direction?.let { "(%.2f, %.2f, %.2f)".format(it.x, it.y, it.z) } ?: "None",
            intensity, range, attenuationA, attenuationB
        )

    companion object {
        /** 点光源 */
        fun point(
            name: String = "PointLight",
            position: Vector3f = Vector3f(0f, 50f, 0f),
            color: ColorARGB = ColorARGB(1f, 1f, 1f, 1f),
            intensity: Float = 0.6f,
            range: Float = 600f,
        ): Light = Light(name, position, color, null, intensity, range)

        /** 面光源 */
        fun face(
            name: String = "FaceLight",
            position: Vector3f,
            direction: Vector3f,
            color: ColorARGB = ColorARGB(1f, 1f, 1f, 1f),
            intensity: Float = 0.6f,
            range: Float = 600f,
        ): Light = Light(name, position, color, direction, intensity, range)
    }
}
