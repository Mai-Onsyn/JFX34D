module;
#include <immintrin.h>
#include <cmath>

#if defined(_MSC_VER)
#define FORCE_INLINE __forceinline
#elif defined(__GNUC__) || defined(__clang__)
#define FORCE_INLINE __attribute__((always_inline)) inline
#else
#define FORCE_INLINE inline
#endif
export module SIMD;
import Types;
import Matrix;
import Vectors;

export class Vec8i;

export class alignas(32) Vec8f {
    __m256 val;

public:
    FORCE_INLINE Vec8f(__m256 v) : val(v) {}
    FORCE_INLINE Vec8f(const Float v) : val(_mm256_set1_ps(v)) {}
    FORCE_INLINE Vec8f(const Float v1, const Float v2, const Float v3, const Float v4, const Float v5, const Float v6,
                 const Float v7, const Float v8) : val(_mm256_set_ps(v8, v7, v6, v5, v4, v3, v2, v1)) {}
    FORCE_INLINE Vec8f(const Float* v) : val(_mm256_loadu_ps(v)) {}

    FORCE_INLINE operator Vec8i() const;
    FORCE_INLINE operator __m256() const { return val; }

    FORCE_INLINE Vec8f operator+(const Vec8f& o) const { return _mm256_add_ps(val, o.val); }
    FORCE_INLINE Vec8f operator-(const Vec8f& o) const { return _mm256_sub_ps(val, o.val); }
    FORCE_INLINE Vec8f operator*(const Vec8f& o) const { return _mm256_mul_ps(val, o.val); }
    FORCE_INLINE Vec8f operator/(const Vec8f& o) const { return _mm256_div_ps(val, o.val); }

    FORCE_INLINE Boolean operator>(const Vec8f& o) const { return _mm256_movemask_ps(_mm256_cmp_ps(val, o.val, _CMP_GT_OQ)) == 0xFF; }
    FORCE_INLINE Boolean operator>=(const Vec8f& o) const { return _mm256_movemask_ps(_mm256_cmp_ps(val, o.val, _CMP_GE_OQ)) == 0xFF; }
    FORCE_INLINE Boolean operator<(const Vec8f& o) const { return _mm256_movemask_ps(_mm256_cmp_ps(val, o.val, _CMP_LT_OQ)) == 0xFF; }
    FORCE_INLINE Boolean operator<=(const Vec8f& o) const { return _mm256_movemask_ps(_mm256_cmp_ps(val, o.val, _CMP_LE_OQ)) == 0xFF; }

    FORCE_INLINE void operator+=(const Vec8f& o) { val = _mm256_add_ps(val, o.val); }
    FORCE_INLINE void operator-=(const Vec8f& o) { val = _mm256_sub_ps(val, o.val); }
    FORCE_INLINE void operator*=(const Vec8f& o) { val = _mm256_mul_ps(val, o.val); }
    FORCE_INLINE void operator/=(const Vec8f& o) { val = _mm256_div_ps(val, o.val); }

    FORCE_INLINE void store(Float* ptr) const { _mm256_storeu_ps(ptr, val); }
    FORCE_INLINE Vec8f sqrt() const { return _mm256_sqrt_ps(val); }

    FORCE_INLINE Vec8f clamp(const Vec8f& min, const Vec8f& max) const {
        return _mm256_min_ps(_mm256_max_ps(val, min.val), max.val);
    }

    FORCE_INLINE Vec8f clamp(const Float lo, const Float hi) const {
        return _mm256_min_ps(_mm256_max_ps(val, _mm256_set1_ps(lo)), _mm256_set1_ps(hi));
    }

    FORCE_INLINE Vec8f floor() const { return _mm256_floor_ps(val); }

    // ~= 1 / A
    FORCE_INLINE Vec8f inv() const {
        __m256 r = _mm256_rcp_ps(val);
        __m256 t = _mm256_fnmadd_ps(val, r, _mm256_set1_ps(2.0f));
        return _mm256_mul_ps(r, t);
    }
    // FORCE_INLINE Vec8f inv() const { return _mm256_div_ps(_mm256_set1_ps(1), this->val); }

    // ~= 1 / sqrt(A)
    FORCE_INLINE Vec8f invSqrt() const { return _mm256_rsqrt_ps(val); }

    FORCE_INLINE Vec8f cmpBigger(const Vec8f& o) const { return _mm256_cmp_ps(val, o.val, _CMP_GT_OS); }
    FORCE_INLINE Vec8f cmpSmaller(const Vec8f& o) const { return _mm256_cmp_ps(val, o.val, _CMP_LT_OS); }

    FORCE_INLINE static Vec8f andNot(const Vec8f& a, const Vec8f& b) { return _mm256_andnot_ps(a.val, b.val); }
    FORCE_INLINE static Vec8f max(const Vec8f& a, const Vec8f& b) { return _mm256_max_ps(a.val, b.val); }
    FORCE_INLINE static Vec8f min(const Vec8f& a, const Vec8f& b) { return _mm256_min_ps(a.val, b.val); }

    FORCE_INLINE static Vec8f dot3D(const Vec8f& ax, const Vec8f& ay, const Vec8f& az, const Vec8f& bx, const Vec8f& by, const Vec8f& bz) {
        return ax * bx + ay * by + az * bz;
    }

    // A * B + C
    FORCE_INLINE static Vec8f fma(const Vec8f& a, const Vec8f& b, const Vec8f& c) {
        return _mm256_fmadd_ps(a.val, b.val, c.val);
    }

    // A * B - C
    FORCE_INLINE static Vec8f fms(const Vec8f& a, const Vec8f& b, const Vec8f& c) {
        return _mm256_fmsub_ps(a.val, b.val, c.val);
    }

    // m * v
    FORCE_INLINE static void matrixMul(const Matrix4x4& m, Vec8f& vx, Vec8f& vy, Vec8f& vz, Vec8f& vw) {
        Vec8f x = fma(vx, m[0], fma(vy, m[1], fma(vz, m[2], vw * m[3])));
        Vec8f y = fma(vx, m[4], fma(vy, m[5], fma(vz, m[6], vw * m[7])));
        Vec8f z = fma(vx, m[8], fma(vy, m[9], fma(vz, m[10], vw * m[11])));
        Vec8f w = fma(vx, m[12], fma(vy, m[13], fma(vz, m[14], vw * m[15])));
        vx = x;
        vy = y;
        vz = z;
        vw = w;
    }
    FORCE_INLINE static void matrixMul(const Matrix3x3& m, Vec8f& vx, Vec8f& vy, Vec8f& vz) {
        Vec8f x = fma(vx, m[0], fma(vy, m[1], vz * m[2]));
        Vec8f y = fma(vx, m[3], fma(vy, m[4], vz * m[5]));
        Vec8f z = fma(vx, m[6], fma(vy, m[7], vz * m[8]));
        vx = x;
        vy = y;
        vz = z;
    }
};

export class alignas(32) Vec8i {
    __m256i val;

public:
    FORCE_INLINE Vec8i(__m256i v) : val(v) {}
    FORCE_INLINE Vec8i(const Int32 v) : val(_mm256_set1_epi32(v)) {}
    FORCE_INLINE Vec8i(const Int32 v1, const Int32 v2, const Int32 v3, const Int32 v4, const Int32 v5, const Int32 v6, const Int32 v7, const Int32 v8) :
        val(_mm256_set_epi32(v8, v7, v6, v5, v4, v3, v2, v1)) {}
    FORCE_INLINE Vec8i(const Int32* ptr) : val(_mm256_loadu_si256((__m256i*)ptr)) {}

    FORCE_INLINE operator Vec8f() const;
    FORCE_INLINE operator __m256i() const { return val; }

    FORCE_INLINE Vec8i operator+(const Vec8i& o) const { return Vec8i(_mm256_add_epi32(val, o.val)); }
    FORCE_INLINE Vec8i operator-(const Vec8i& o) const { return Vec8i(_mm256_sub_epi32(val, o.val)); }
    FORCE_INLINE Vec8i operator*(const Vec8i& o) const { return Vec8i(_mm256_mullo_epi32(val, o.val)); }

    FORCE_INLINE void store(Int32* buffer) const { _mm256_storeu_si256(reinterpret_cast<__m256i *>(buffer), val); }

    FORCE_INLINE Vec8i clamp(const Int32 lo, const Int32 hi) const {
        return _mm256_min_epi32(_mm256_max_epi32(val, _mm256_set1_epi32(lo)), _mm256_set1_epi32(hi));
    }

    FORCE_INLINE Vec8i clamp(const Vec8i& lo, const Vec8i& hi) const {
        return _mm256_min_epi32(_mm256_max_epi32(val, lo.val), hi.val);
    }

    FORCE_INLINE static Vec8i max(const Vec8i& a, const Vec8i& b) { return Vec8i(_mm256_max_epi32(a.val, b.val)); }
    FORCE_INLINE static Vec8i min(const Vec8i& a, const Vec8i& b) { return Vec8i(_mm256_min_epi32(a.val, b.val)); }
};

FORCE_INLINE Vec8f::operator Vec8i() const { return {_mm256_cvttps_epi32(val)}; }

FORCE_INLINE Vec8i::operator Vec8f() const { return {_mm256_cvtepi32_ps(val)}; }


export FORCE_INLINE void storeColorVec(const __m256i& colorVec, UInt8* ptr) {
    __m128i lo = _mm256_castsi256_si128(colorVec);
    __m128i hi = _mm256_extracti128_si256(colorVec, 1);

    __m128i pack16 = _mm_packus_epi32(lo, hi);
    __m128i pack8 = _mm_packus_epi16(pack16, _mm_setzero_si128());

    _mm_storel_epi64(reinterpret_cast<__m128i*>(ptr), pack8);
}


export FORCE_INLINE void transpose8_ps(Vec8f &row0, Vec8f &row1, Vec8f &row2, Vec8f &row3,
                                 Vec8f &row4, Vec8f &row5, Vec8f &row6, Vec8f &row7) {
    __m256 t0, t1, t2, t3, t4, t5, t6, t7;
    __m256 tt0, tt1, tt2, tt3, tt4, tt5, tt6, tt7;

    // 1. 解包（Unpack）：将相邻两行的元素交叉配对
    //    例如 t0 = row0[0], row1[0], row0[1], row1[1], ...
    t0 = _mm256_unpacklo_ps(row0, row1);
    t1 = _mm256_unpackhi_ps(row0, row1);
    t2 = _mm256_unpacklo_ps(row2, row3);
    t3 = _mm256_unpackhi_ps(row2, row3);
    t4 = _mm256_unpacklo_ps(row4, row5);
    t5 = _mm256_unpackhi_ps(row4, row5);
    t6 = _mm256_unpacklo_ps(row6, row7);
    t7 = _mm256_unpackhi_ps(row6, row7);

    // 2. 混洗（Shuffle）：将解包后的寄存器组进一步交叉组合
    //    _MM_SHUFFLE(3,2,3,2) 和 _MM_SHUFFLE(1,0,1,0) 用于选择特定的通道组合
    tt0 = _mm256_shuffle_ps(t0, t2, _MM_SHUFFLE(1,0,1,0));
    tt1 = _mm256_shuffle_ps(t0, t2, _MM_SHUFFLE(3,2,3,2));
    tt2 = _mm256_shuffle_ps(t1, t3, _MM_SHUFFLE(1,0,1,0));
    tt3 = _mm256_shuffle_ps(t1, t3, _MM_SHUFFLE(3,2,3,2));
    tt4 = _mm256_shuffle_ps(t4, t6, _MM_SHUFFLE(1,0,1,0));
    tt5 = _mm256_shuffle_ps(t4, t6, _MM_SHUFFLE(3,2,3,2));
    tt6 = _mm256_shuffle_ps(t5, t7, _MM_SHUFFLE(1,0,1,0));
    tt7 = _mm256_shuffle_ps(t5, t7, _MM_SHUFFLE(3,2,3,2));

    // 3. 置换（Permute）：将混洗后的结果跨128位通道重新组合，形成最终的行
    //    常量 0x20 表示取低128位，0x31 表示取高128位
    row0 = _mm256_permute2f128_ps(tt0, tt4, 0x20);
    row1 = _mm256_permute2f128_ps(tt1, tt5, 0x20);
    row2 = _mm256_permute2f128_ps(tt2, tt6, 0x20);
    row3 = _mm256_permute2f128_ps(tt3, tt7, 0x20);
    row4 = _mm256_permute2f128_ps(tt0, tt4, 0x31);
    row5 = _mm256_permute2f128_ps(tt1, tt5, 0x31);
    row6 = _mm256_permute2f128_ps(tt2, tt6, 0x31);
    row7 = _mm256_permute2f128_ps(tt3, tt7, 0x31);
}



// 绝对安全的工业级 SIMD exp 近似（纯浮点运算，无位移风险）
FORCE_INLINE __m256 _mm256_exp_ps_safe(__m256 x) {
    // 强行把输入限制在安全范围内，防止发生浮点下溢或上溢
    x = _mm256_max_ps(_mm256_set1_ps(-80.0f), _mm256_min_ps(_mm256_set1_ps(80.0f), x));

    // e^x = [1 + x/256]^256
    // 采用 Exponentiation by squaring (连续平方8次代替256次方)
    x = _mm256_fmadd_ps(x, _mm256_set1_ps(1.0f / 256.0f), _mm256_set1_ps(1.0f));
    x = _mm256_mul_ps(x, x); // 2
    x = _mm256_mul_ps(x, x); // 4
    x = _mm256_mul_ps(x, x); // 8
    x = _mm256_mul_ps(x, x); // 16
    x = _mm256_mul_ps(x, x); // 32
    x = _mm256_mul_ps(x, x); // 64
    x = _mm256_mul_ps(x, x); // 128
    return _mm256_mul_ps(x, x); // 256
}

// 绝对安全的 SIMD log 近似（泰勒展开级数）
FORCE_INLINE __m256 _mm256_log_ps_safe(__m256 x) {
    // 确保底数绝对大于0，防止逼近 -inf
    x = _mm256_max_ps(x, _mm256_set1_ps(1e-4f));

    // 使用 Pade 近似或泰勒级数改善 [0.5, 2.0] 之间的精度
    // 这里采用高效逼近：ln(x)
    __m256 num = _mm256_sub_ps(x, _mm256_set1_ps(1.0f));
    __m256 den = _mm256_add_ps(x, _mm256_set1_ps(1.0f));
    __m256 t = _mm256_div_ps(num, den); // t = (x-1)/(x+1)

    __m256 t2 = _mm256_mul_ps(t, t);

    // 多项式逼近 2 * (t + t^3/3 + t^5/5)
    __m256 p = _mm256_fmadd_ps(t2, _mm256_set1_ps(0.2f), _mm256_set1_ps(0.3333333f));
    p = _mm256_fmadd_ps(t2, p, _mm256_set1_ps(1.0f));
    return _mm256_mul_ps(_mm256_set1_ps(2.0f), _mm256_mul_ps(t, p));
}

// 最终稳定的 pow
export FORCE_INLINE __m256 _mm256_pow_ps_approx(__m256 base, __m256 exp) {
    // 如果高光项 dot 接近 0，直接斩断，不让它进 log 计算
    __m256 mask = _mm256_cmp_ps(base, _mm256_set1_ps(1e-4f), _CMP_GT_OQ);

    __m256 log_base = _mm256_log_ps_safe(base);
    __m256 mul = _mm256_mul_ps(log_base, exp);
    __m256 res = _mm256_exp_ps_safe(mul);

    // 如果本来 base 就小于等于 0，强行将其高光归零，防止 NaN 污染
    return _mm256_and_ps(res, mask);
}