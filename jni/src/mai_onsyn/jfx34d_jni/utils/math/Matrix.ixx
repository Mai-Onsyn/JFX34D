module;
#include <cstring>
#include <sstream>
#include <immintrin.h>
#include <cmath>
export module Matrix;
import Types;
import Vectors;
// import SIMD;

using std::sin;
using std::cos;

export class Matrix3x3 {
public:
    Float m[9]{};
    Matrix3x3() = default;

    Matrix3x3(const Float n0, const Float n1, const Float n2,
              const Float n3, const Float n4, const Float n5,
              const Float n6, const Float n7, const Float n8) {
        m[0] = n0;
        m[1] = n1;
        m[2] = n2;
        m[3] = n3;
        m[4] = n4;
        m[5] = n5;
        m[6] = n6;
        m[7] = n7;
        m[8] = n8;
    }

    Float& operator[](const Int32 idx) {
        return m[idx];
    }

    const Float& operator[](const Int32 idx) const {
        return m[idx];
    }

    Matrix3x3 inverse() const {
        const Float det = calcDet();
        if (det == 0.0f) {
            throw RuntimeError("Matrix3x3::inverse() - determinant is 0");
        }
        const Float invDet = 1.0f / det;

        return Matrix3x3{
            (m[4] * m[8] - m[5] * m[7]) * invDet,
            (m[2] * m[7] - m[1] * m[8]) * invDet,
            (m[1] * m[5] - m[2] * m[4]) * invDet,

            (m[5] * m[6] - m[3] * m[8]) * invDet,
            (m[0] * m[8] - m[2] * m[6]) * invDet,
            (m[2] * m[3] - m[0] * m[5]) * invDet,

            (m[3] * m[7] - m[4] * m[6]) * invDet,
            (m[1] * m[6] - m[0] * m[7]) * invDet,
            (m[0] * m[4] - m[1] * m[3]) * invDet
        };
    }

    Matrix3x3 transpose() const {
        return Matrix3x3{
            m[0], m[3], m[6],
            m[1], m[4], m[7],
            m[2], m[5], m[8]
        };
    }

    Matrix3x3 operator*(const Matrix3x3 &other) const {
        return Matrix3x3{
            m[0] * other[0] + m[1] * other[3] + m[2] * other[6],
            m[0] * other[1] + m[1] * other[4] + m[2] * other[7],
            m[0] * other[2] + m[1] * other[5] + m[2] * other[8],
            m[3] * other[0] + m[4] * other[3] + m[5] * other[6],
            m[3] * other[1] + m[4] * other[4] + m[5] * other[7],
            m[3] * other[2] + m[4] * other[5] + m[5] * other[8],
            m[6] * other[0] + m[7] * other[3] + m[8] * other[6],
            m[6] * other[1] + m[7] * other[4] + m[8] * other[7],
            m[6] * other[2] + m[7] * other[5] + m[8] * other[8]
        };
    }

    Vector3D operator*(const Vector3D &other) const {
        return Vector3D{
            m[0] * other.x + m[1] * other.y + m[2] * other.z,
            m[3] * other.x + m[4] * other.y + m[5] * other.z,
            m[6] * other.x + m[7] * other.y + m[8] * other.z
        };
    }

    [[nodiscard]] Float calcDet() const {
        return m[0] * (m[4] * m[8] - m[5] * m[7]) - m[1] * (m[3] * m[8] - m[5] * m[6]) + m[2] * (m[3] * m[7] - m[4] * m[6]);
    }
};

export class alignas(32) Matrix4x4 {
    alignas(32) Float m[16]{};
public:
    Matrix4x4() {
        memset(m, 0, sizeof(m));
    };

    Matrix4x4(const Float* arr) {
        memcpy(m, arr, sizeof(m));
    }

    Matrix4x4(Matrix4x4&& other) noexcept {
        memcpy(this->m, other.m, sizeof(m));
    }
    Matrix4x4(const Matrix4x4&& other) noexcept {
        memcpy(this->m, other.m, sizeof(m));
    }
    Matrix4x4(const Matrix4x4& other) noexcept {
        memcpy(this->m, other.m, sizeof(m));
    }
    Matrix4x4& operator=(Matrix4x4&& other) noexcept {
        memcpy(this->m, other.m, sizeof(m));
        return *this;
    }

    Matrix4x4& operator=(const Matrix4x4& other) {
        memcpy(this->m, other.m, sizeof(m));
        return *this;
    }

    static Matrix4x4 I() {
        Matrix4x4 matrix4;
        matrix4[0] = 1;
        matrix4[5] = 1;
        matrix4[10] = 1;
        matrix4[15] = 1;
        return matrix4;
    }

    static Matrix4x4 scale(const Float s) {
        Matrix4x4 matrix4;
        matrix4[0] = s;
        matrix4[5] = s;
        matrix4[10] = s;
        matrix4[15] = 1;
        return matrix4;
    }

    static Matrix4x4 translate(const Vector3D& v) {
        Matrix4x4 matrix = I();
        matrix[3] = v.x;
        matrix[7] = v.y;
        matrix[11] = v.z;
        return matrix;
    }

    static Matrix4x4 rotate(const Float angle, const Vector3D& axis) {
        Vector3D v = axis.normalize();

        Float c = cos(angle);
        Float s = sin(angle);
        Float t = 1 - c; // (1 - cos)

        Matrix4x4 matrix = I(); // 初始为单位矩阵

        // 列主序填充 3x3 旋转部分
        // 第 0 列 (索引 0,1,2)
        matrix[0] = t * v.x * v.x + c;
        matrix[1] = t * v.x * v.y + s * v.z;
        matrix[2] = t * v.x * v.z - s * v.y;

        // 第 1 列 (索引 4,5,6)
        matrix[4] = t * v.x * v.y - s * v.z;
        matrix[5] = t * v.y * v.y + c;
        matrix[6] = t * v.y * v.z + s * v.x;

        // 第 2 列 (索引 8,9,10)
        matrix[8] = t * v.x * v.z + s * v.y;
        matrix[9] = t * v.y * v.z - s * v.x;
        matrix[10] = t * v.z * v.z + c;

        // 第 3 列 (平移部分，保持 0,0,0,1)
        matrix[3] = 0;
        matrix[7] = 0;
        matrix[11] = 0;

        return matrix;
    }

    explicit operator Matrix3x3() const {
        return {m[0], m[1], m[2], m[4], m[5], m[6], m[8], m[9], m[10]};
    }

    Float& operator[](const Int32 idx) {
        return m[idx];
    }

    const Float& operator[](const Int32 idx) const {
        return m[idx];
    }

    Matrix4x4 operator*(const Matrix4x4 &other) const {
        Matrix4x4 result;
        // result.m[0] = fma(m[0], other[0], fma(m[1], other[4], fma(m[2], other[8], m[3] * other[12])));
        // result.m[1] = fma(m[0], other[1], fma(m[1], other[5], fma(m[2], other[9], m[3] * other[13])));
        // result.m[2] = fma(m[0], other[2], fma(m[1], other[6], fma(m[2], other[10], m[3] * other[14])));
        // result.m[3] = fma(m[0], other[3], fma(m[1], other[7], fma(m[2], other[11], m[3] * other[15])));
        //
        // result.m[4] = fma(m[4], other[0], fma(m[5], other[4], fma(m[6], other[8], m[7] * other[12])));
        // result.m[5] = fma(m[4], other[1], fma(m[5], other[5], fma(m[6], other[9], m[7] * other[13])));
        // result.m[6] = fma(m[4], other[2], fma(m[5], other[6], fma(m[6], other[10], m[7] * other[14])));
        // result.m[7] = fma(m[4], other[3], fma(m[5], other[7], fma(m[6], other[11], m[7] * other[15])));
        //
        // result.m[8] = fma(m[8], other[0], fma(m[9], other[4], fma(m[10], other[8], m[11] * other[12])));
        // result.m[9] = fma(m[8], other[1], fma(m[9], other[5], fma(m[10], other[9], m[11] * other[13])));
        // result.m[10] = fma(m[8], other[2], fma(m[9], other[6], fma(m[10], other[10], m[11] * other[14])));
        // result.m[11] = fma(m[8], other[3], fma(m[9], other[7], fma(m[10], other[11], m[11] * other[15])));
        //
        // result.m[12] = fma(m[12], other[0], fma(m[13], other[4], fma(m[14], other[8], m[15] * other[12])));
        // result.m[13] = fma(m[12], other[1], fma(m[13], other[5], fma(m[14], other[9], m[15] * other[13])));
        // result.m[14] = fma(m[12], other[2], fma(m[13], other[6], fma(m[14], other[10], m[15] * other[14])));
        // result.m[15] = fma(m[12], other[3], fma(m[13], other[7], fma(m[14], other[11], m[15] * other[15])));

        result.m[0] = m[0] * other[0] + m[1] * other[4] + m[2] * other[8] + m[3] * other[12];
        result.m[1] = m[0] * other[1] + m[1] * other[5] + m[2] * other[9] + m[3] * other[13];
        result.m[2] = m[0] * other[2] + m[1] * other[6] + m[2] * other[10] + m[3] * other[14];
        result.m[3] = m[0] * other[3] + m[1] * other[7] + m[2] * other[11] + m[3] * other[15];

        result.m[4] = m[4] * other[0] + m[5] * other[4] + m[6] * other[8] + m[7] * other[12];
        result.m[5] = m[4] * other[1] + m[5] * other[5] + m[6] * other[9] + m[7] * other[13];
        result.m[6] = m[4] * other[2] + m[5] * other[6] + m[6] * other[10] + m[7] * other[14];
        result.m[7] = m[4] * other[3] + m[5] * other[7] + m[6] * other[11] + m[7] * other[15];

        result.m[8] = m[8] * other[0] + m[9] * other[4] + m[10] * other[8] + m[11] * other[12];
        result.m[9] = m[8] * other[1] + m[9] * other[5] + m[10] * other[9] + m[11] * other[13];
        result.m[10] = m[8] * other[2] + m[9] * other[6] + m[10] * other[10] + m[11] * other[14];
        result.m[11] = m[8] * other[3] + m[9] * other[7] + m[10] * other[11] + m[11] * other[15];

        result.m[12] = m[12] * other[0] + m[13] * other[4] + m[14] * other[8] + m[15] * other[12];
        result.m[13] = m[12] * other[1] + m[13] * other[5] + m[14] * other[9] + m[15] * other[13];
        result.m[14] = m[12] * other[2] + m[13] * other[6] + m[14] * other[10] + m[15] * other[14];
        result.m[15] = m[12] * other[3] + m[13] * other[7] + m[14] * other[11] + m[15] * other[15];
        return result;
    }

    Vector4D operator*(const Vector4D &other) const {
        return {
            m[0] * other.x + m[1] * other.y + m[2] * other.z + m[3] * other.w,
            m[4] * other.x + m[5] * other.y + m[6] * other.z + m[7] * other.w,
            m[8] * other.x + m[9] * other.y + m[10] * other.z + m[11] * other.w,
            m[12] * other.x + m[13] * other.y + m[14] * other.z + m[15] * other.w
        };
    }

    Vector4D operator*(const Vector3D &other) const {
        return {
            m[0] * other.x + m[1] * other.y + m[2] * other.z + m[3],
            m[4] * other.x + m[5] * other.y + m[6] * other.z + m[7],
            m[8] * other.x + m[9] * other.y + m[10] * other.z + m[11],
            m[12] * other.x + m[13] * other.y + m[14] * other.z + m[15]
        };
    }

    [[nodiscard]] String toString() const {
        std::stringstream ss;
        ss << "Matrix{";
        for (int i = 0; i < 15; i++) {
            ss << m[i] << ", ";
        }
        ss << m[15] << "}";
        return ss.str();
    }
};

/**
 * 5x5 矩阵, 行主序: m[row * 5 + col]。
 *
 * 注意和 Matrix4x4 的区别: 那个是列主序 (跟 joml 对齐), 这个跟 Kotlin 侧的
 * Matrix5f 一样是行主序。两者不要互相赋值。
 *
 * 4D 齐次变换: (x, y, z, w, 1) -> M * v, 最后一行管平移。
 */
export class Matrix5x5 {
public:
    Float m[25]{};

    Matrix5x5() = default;

    Matrix5x5(const Float n0, const Float n1, const Float n2, const Float n3, const Float n4,
              const Float n5, const Float n6, const Float n7, const Float n8, const Float n9,
              const Float n10, const Float n11, const Float n12, const Float n13, const Float n14,
              const Float n15, const Float n16, const Float n17, const Float n18, const Float n19,
              const Float n20, const Float n21, const Float n22, const Float n23, const Float n24) {
        const Float values[25] = {
            n0, n1, n2, n3, n4,
            n5, n6, n7, n8, n9,
            n10, n11, n12, n13, n14,
            n15, n16, n17, n18, n19,
            n20, n21, n22, n23, n24
        };
        memcpy(m, values, sizeof(m));
    }

    Matrix5x5(const Float* arr) {
        memcpy(m, arr, sizeof(m));
    }

    static Matrix5x5 I() {
        Matrix5x5 matrix;
        for (Int32 i = 0; i < 5; i++) matrix.m[i * 5 + i] = 1;
        return matrix;
    }

    static Matrix5x5 scale(const Vector4D& s) {
        Matrix5x5 matrix = I();
        matrix.m[0] = s.x;
        matrix.m[6] = s.y;
        matrix.m[12] = s.z;
        matrix.m[18] = s.w;
        return matrix;
    }

    /** 4D 平移: 最后一行 (第 4 行) 的 0..3 列 */
    static Matrix5x5 translate(const Vector4D& t) {
        Matrix5x5 matrix = I();
        matrix.m[20] = t.x;
        matrix.m[21] = t.y;
        matrix.m[22] = t.z;
        matrix.m[23] = t.w;
        return matrix;
    }

    /**
     * 在 a, b 两张坐标轴张成的平面内旋转 angle。
     * a, b ∈ [0, 4] 依次是 x, y, z, w, v。
     *
     * 4D 里 "绕 w 旋转" 就是 rotate(angle, 0, 3) 这种平面旋转 ——
     * 4D 没有绕一根轴的旋转 (那需要两根轴), 只有绕平面的旋转。
     */
    static Matrix5x5 rotate(Float angle, Int32 a, Int32 b);

    Float& operator[](const Int32 idx) { return m[idx]; }
    const Float& operator[](const Int32 idx) const { return m[idx]; }

    Float& at(const Int32 row, const Int32 col) { return m[row * 5 + col]; }
    [[nodiscard]] const Float& at(const Int32 row, const Int32 col) const { return m[row * 5 + col]; }

    Matrix5x5 operator*(const Matrix5x5& other) const {
        Matrix5x5 result;
        for (Int32 r = 0; r < 5; r++) {
            for (Int32 c = 0; c < 5; c++) {
                Float sum = 0;
                for (Int32 k = 0; k < 5; k++) sum += m[r * 5 + k] * other.m[k * 5 + c];
                result.m[r * 5 + c] = sum;
            }
        }
        return result;
    }

    /** 齐次变换: 结果是 5D 量, 最后一个分量通常还是 1 (仿射), 否则要除以它。 */
    Vector5D operator*(const Vector5D& other) const {
        return {
            m[0] * other.x + m[1] * other.y + m[2] * other.z + m[3] * other.w + m[4] * other.v,
            m[5] * other.x + m[6] * other.y + m[7] * other.z + m[8] * other.w + m[9] * other.v,
            m[10] * other.x + m[11] * other.y + m[12] * other.z + m[13] * other.w + m[14] * other.v,
            m[15] * other.x + m[16] * other.y + m[17] * other.z + m[18] * other.w + m[19] * other.v,
            m[20] * other.x + m[21] * other.y + m[22] * other.z + m[23] * other.w + m[24] * other.v
        };
    }

    /** 把 (x, y, z, w) 当仿射点变换 (v = 1), 返回前四个分量。 */
    [[nodiscard]] Vector4D transform(const Vector4D& p) const;

    [[nodiscard]] Matrix5x5 transpose() const {
        Matrix5x5 result;
        for (Int32 r = 0; r < 5; r++) {
            for (Int32 c = 0; c < 5; c++) result.m[r * 5 + c] = m[c * 5 + r];
        }
        return result;
    }

    [[nodiscard]] Float calcDet() const;

    [[nodiscard]] Matrix5x5 inverse() const;

    [[nodiscard]] String toString() const;
};