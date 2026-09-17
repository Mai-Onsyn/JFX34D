package mai_onsyn.renderer.data

import org.lwjgl.opengl.GL30.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

data class Texture(
    val ka: ColorARGB = ColorARGB(0xFFFFFFFFu),     // 环境光颜色
    val kd: ColorARGB = ColorARGB(0xFF808080u),     // 漫反射颜色
    val ks: ColorARGB = ColorARGB(0xFFFFFFFFu),     // 镜面反射颜色
    val ns: Float = 1f,                 // 镜面反射指数
    val d: Float = 0f,                  // 透明度
    val mapKd: BufferedImage? = null,   // 漫反射纹理/主材质
    val mapKs: BufferedImage? = null,   // 镜面反射纹理
    val mapD: BufferedImage? = null,    // 透明度纹理
    val mapBump: BufferedImage?,        // 法线纹理
)

//class GpuMaterial {
//    var mapKdId: Int = 0
//        private set
//    var uploaded: Boolean = false
//        private set
//
//    // 如果以后加 mapKs/mapD/mapBump，就在这里继续加 id
//
//    fun upload(texture: Texture) {
//        if (uploaded) return
//        texture.mapKd?.let { mapKdId = uploadImage(it) }
//        uploaded = true
//    }
//
//    private fun uploadImage(image: BufferedImage): Int {
//        val w = image.width
//        val h = image.height
//        val buf = ByteBuffer.allocateDirect(w * h * 4)
//        val row = IntArray(w)
//        for (y in 0 until h) {
//            image.getRGB(0, y, w, 1, row, 0, w)
//            for (x in 0 until w) {
//                val argb = row[x]
//                buf.put(((argb shr 16) and 0xFF).toByte())
//                buf.put(((argb shr 8)  and 0xFF).toByte())
//                buf.put(( argb        and 0xFF).toByte())
//                buf.put(((argb shr 24) and 0xFF).toByte())
//            }
//        }
//        buf.flip()
//
//        val id = glGenTextures()
//        glBindTexture(GL_TEXTURE_2D, id)
//        glTexImage2D(
//            GL_TEXTURE_2D, 0, GL_RGBA,
//            w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf
//        )
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
//        glBindTexture(GL_TEXTURE_2D, 0)
//        return id
//    }
//
//    fun bindKd(unit: Int) {
//        glActiveTexture(GL_TEXTURE0 + unit)
//        glBindTexture(GL_TEXTURE_2D, mapKdId)
//    }
//
//    fun dispose() {
//        if (mapKdId != 0) glDeleteTextures(mapKdId)
//    }
//}