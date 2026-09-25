package mai_onsyn.renderer.cpu4dkt

class Matrix5f(
    val data: FloatArray = FloatArray(25)
) {
    operator fun times(m: Matrix5f): Matrix5f {
        val buffer = FloatArray(25)
        for (i in 0 until 5) {
            val ri = i * 5
            for (k in 0 until 5) {
                val aik = data[ri + k]
                if (aik == 0f) continue
                val rk = k * 5
                for (j in 0 until 5) {
                    buffer[ri + j] += aik * m[rk + j]
                }
            }
        }
        return Matrix5f(buffer)
    }

//    operator fun timesAssign(m: Matrix5f) {
//        val f = this.times(m)
//        System.arraycopy(f.data, 0, data, 0, f.data.size)
//    }

    operator fun times(v: Vector5f): Vector5f {
        val res = Vector5f()
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                res[i] += data[i * 5 + j] * v[j]
            }
        }
        return res
    }

    operator fun get(i: Int): Float {
        return data[i]
    }

    operator fun set(i: Int, v: Float) {
        data[i] = v
    }

    /**
     * 行主序，与joml(列主序)不同
     */
    constructor(
        a1: Float = 0f, a2: Float = 0f, a3: Float = 0f, a4: Float = 0f, a5: Float = 0f,
        b1: Float = 0f, b2: Float = 0f, b3: Float = 0f, b4: Float = 0f, b5: Float = 0f,
        c1: Float = 0f, c2: Float = 0f, c3: Float = 0f, c4: Float = 0f, c5: Float = 0f,
        d1: Float = 0f, d2: Float = 0f, d3: Float = 0f, d4: Float = 0f, d5: Float = 0f,
        e1: Float = 0f, e2: Float = 0f, e3: Float = 0f, e4: Float = 0f, e5: Float = 0f
    ): this(floatArrayOf(
        a1, a2, a3, a4, a5,
        b1, b2, b3, b4, b5,
        c1, c2, c3, c4, c5,
        d1, d2, d3, d4, d5,
        e1, e2, e3, e4, e5
    ))

    companion object {
        val IDENTITY = Matrix5f(
            a1 = 1f, b2 = 1f, c3 = 1f, d4 = 1f, e5 = 1f
        )
    }
}