package mai_onsyn.renderer.utils



@JvmInline
value class ColorARGB(val hex: Int) {
    companion object {
        operator fun invoke(r: Float = 0f, g: Float = 0f, b: Float = 0f, a: Float = 1f): ColorARGB {
            val ai = (a.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val ri = (r.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val gi = (g.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val bi = (b.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            return ColorARGB((ai shl 24) or (ri shl 16) or (gi shl 8) or bi)
        }
    }

    val alpha: Int
        get() = ((hex ushr 24) and 0xff)

    val red: Int
        get() = ((hex ushr 16) and 0xff)

    val green: Int
        get() = ((hex ushr 8) and 0xff)

    val blue: Int
        get() = ((hex ushr 0) and 0xff)

    val alphaF: Float
        get() = ((hex ushr 24) and 0xff).toFloat() * 0.003921569f

    val redF: Float
        get() = ((hex ushr 16) and 0xff).toFloat() * 0.003921569f

    val greenF: Float
        get() = ((hex ushr 8) and 0xff).toFloat() * 0.003921569f

    val blueF: Float
        get() = ((hex ushr 0) and 0xff).toFloat() * 0.003921569f

    override fun toString(): String {
        return "#%02X%02X%02X%02X".format(alpha, red, green, blue)
    }
}