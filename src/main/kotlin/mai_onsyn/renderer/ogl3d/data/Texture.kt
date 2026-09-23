package mai_onsyn.renderer.ogl3d.data

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL30.*
import java.awt.image.BufferedImage

data class Texture(
    val ka: ColorARGB = ColorARGB(1f, 1f, 1f, 1f),     // 环境光颜色
    val kd: ColorARGB = ColorARGB(1f, 0.5f, 0.5f, 0.5f),     // 漫反射颜色
    val ks: ColorARGB = ColorARGB(1f, 1f, 1f, 1f),     // 镜面反射颜色
    val ns: Float = 1f,                 // 镜面反射指数
    val d: Float = 0f,                  // 透明度
    val mapKd: BufferedImage? = null,   // 漫反射纹理/主材质
    val mapKs: BufferedImage? = null,   // 镜面反射纹理
    val mapD: BufferedImage? = null,    // 透明度纹理
    val mapBump: BufferedImage?,        // 法线纹理
) {
    /** 贴图/材质在 GPU 上的那份资源, 同一个 Texture 只传一次 */
    val gl: GLMaterial by lazy { GLMaterial(this) }

    companion object {
        /** 没有材质的三角形 (比如测试三角形) 共用的兜底材质 */
        val DEFAULT = Texture(mapBump = null)
    }
}

/** 一个材质在 GPU 上的资源: 贴图 id。材质参数本身作为 uniform 每帧绑定。 */
class GLMaterial(
    val texture: Texture
) {
    var mapKdId: Int = 0
        private set
    var mapKsId: Int = 0
        private set
    var mapDId: Int = 0
        private set
    var mapBumpId: Int = 0
        private set

    var uploaded: Boolean = false
        private set

    /**
     * 材质带镂空/半透明 (贴图里有 alpha < 255 的像素)。
     * 这种组要放到透明那一遍里画: 关深度写入 + 开混合,
     * 让边缘像素按自己的 alpha 和后面的像素混合。
     */
    var transparent: Boolean = false
        private set

    private var alphaChecked = false

    /** 不碰 GL, 只扫贴图的 alpha 通道; 建组时要用 (这时还没上传) */
    fun checkTransparent(): Boolean {
        if (!alphaChecked) {
            val img = texture.mapKd
            transparent = img != null && hasAlpha(img)
            alphaChecked = true
        }
        return transparent
    }

    fun upload() {
        if (uploaded) return
        checkTransparent()
        texture.mapKd?.let { mapKdId = uploadImage(it) }
        texture.mapKs?.let { mapKsId = uploadImage(it) }
        texture.mapD?.let { mapDId = uploadImage(it) }
        texture.mapBump?.let { mapBumpId = uploadImage(it) }
        uploaded = true
    }

    /** 只查 alpha 通道, 有一像素不是 255 就算这个材质是透明的 */
    private fun hasAlpha(image: BufferedImage): Boolean {
        val w = image.width
        val row = IntArray(w)
        for (y in 0 until image.height) {
            image.getRGB(0, y, w, 1, row, 0, w)
            for (x in 0 until w) {
                if ((row[x] ushr 24) != 0xFF) return true
            }
        }
        return false
    }

    private fun uploadImage(image: BufferedImage): Int {
        val w = image.width
        val h = image.height
        val buf = BufferUtils.createByteBuffer(w * h * 4)
        val row = IntArray(w)
        for (y in 0 until h) {
            image.getRGB(0, y, w, 1, row, 0, w)
            for (x in 0 until w) {
                val argb = row[x]
                buf.put(((argb shr 16) and 0xFF).toByte())
                buf.put(((argb shr 8) and 0xFF).toByte())
                buf.put((argb and 0xFF).toByte())
                buf.put(((argb shr 24) and 0xFF).toByte())
            }
        }
        buf.flip()

        val id = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, id)
        glTexImage2D(
            GL_TEXTURE_2D, 0, GL_RGBA,
            w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf
        )
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glBindTexture(GL_TEXTURE_2D, 0)
        return id
    }

    /** 把贴图绑到 texture unit 0..3, 并把 id 写给对应 sampler */
    fun bindTextures() {
        bindTexture(GL_TEXTURE0, 0, mapKdId, uMapKd)
        bindTexture(GL_TEXTURE0, 1, mapKsId, uMapKs)
        bindTexture(GL_TEXTURE0, 2, mapDId, uMapD)
        bindTexture(GL_TEXTURE0, 3, mapBumpId, uMapBump)
    }

    private fun bindTexture(base: Int, unit: Int, id: Int, samplerPtr: Int) {
        glActiveTexture(base + unit)
        glBindTexture(GL_TEXTURE_2D, id)
        glUniform1i(samplerPtr, unit)
    }

    fun dispose() {
        if (mapKdId != 0) glDeleteTextures(mapKdId)
        if (mapKsId != 0) glDeleteTextures(mapKsId)
        if (mapDId != 0) glDeleteTextures(mapDId)
        if (mapBumpId != 0) glDeleteTextures(mapBumpId)
        mapKdId = 0
        mapKsId = 0
        mapDId = 0
        mapBumpId = 0
        uploaded = false
    }

    companion object {
        // 材质 uniform 位置 (program link 后固定不变, 由 GL3DEngine.init 写入)
        var uKa: Int = -1
        var uKd: Int = -1
        var uKs: Int = -1
        var uNs: Int = -1
        var uD: Int = -1
        var uUseTexture: Int = -1
        var uMapKd: Int = -1
        var uMapKs: Int = -1
        var uMapD: Int = -1
        var uMapBump: Int = -1
    }
}
