#include <cmath>
#include <cstring>
#include <sstream>
import Matrix;
import Vectors;
import Types;

using std::cos;
using std::sin;

namespace {

    // 高斯-约当消元求逆, 顺带把行列式一起算出来 (主元乘积)。
    // 5x5 用不着更聪明的做法, 也不值得手写伴随矩阵 (那个抄错的概率太高)。
    Boolean invertInPlace(Float* a, const Int32 n, Float& det) {
        Float d = 1;

        for (Int32 i = 0; i < n; i++) {
            // 选主元
            Int32 pivot = i;
            for (Int32 r = i + 1; r < n; r++) {
                if (std::fabs(a[r * n + i]) > std::fabs(a[pivot * n + i])) pivot = r;
            }
            if (a[pivot * n + i] == 0) return false; // 奇异

            if (pivot != i) {
                for (Int32 c = 0; c < 2 * n; c++) {
                    const Float tmp = a[i * n + c];
                    a[i * n + c] = a[pivot * n + c];
                    a[pivot * n + c] = tmp;
                }
                d = -d;
            }

            const Float inv = 1.0f / a[i * n + i];
            d *= a[i * n + i];
            for (Int32 c = 0; c < 2 * n; c++) a[i * n + c] *= inv;

            for (Int32 r = 0; r < n; r++) {
                if (r == i) continue;
                const Float factor = a[r * n + i];
                if (factor == 0) continue;
                for (Int32 c = 0; c < 2 * n; c++) a[r * n + c] -= factor * a[i * n + c];
            }
        }

        det = d;
        return true;
    }

} // namespace

Matrix5x5 Matrix5x5::rotate(const Float angle, const Int32 a, const Int32 b) {
    Matrix5x5 matrix = I();
    if (a < 0 || a > 4 || b < 0 || b > 4 || a == b) return matrix;

    const Float c = cos(angle);
    const Float s = sin(angle);

    // 行主序: [row * 5 + col]。a 轴那一行只留 col == a, b 轴同理。
    matrix.m[a * 5 + a] = c;
    matrix.m[a * 5 + b] = -s;
    matrix.m[b * 5 + a] = s;
    matrix.m[b * 5 + b] = c;
    return matrix;
}

Vector4D Matrix5x5::transform(const Vector4D& p) const {
    return {
        m[0] * p.x + m[1] * p.y + m[2] * p.z + m[3] * p.w + m[4],
        m[5] * p.x + m[6] * p.y + m[7] * p.z + m[8] * p.w + m[9],
        m[10] * p.x + m[11] * p.y + m[12] * p.z + m[13] * p.w + m[14],
        m[15] * p.x + m[16] * p.y + m[17] * p.z + m[18] * p.w + m[19]
    };
}

Float Matrix5x5::calcDet() const {
    Float a[25 * 2];
    memcpy(a, m, sizeof(m));
    memset(a + 25, 0, sizeof(m));
    for (Int32 i = 0; i < 5; i++) a[i * 5 + 5 + i] = 1;

    Float det = 0;
    if (!invertInPlace(a, 5, det)) return 0;
    return det; // 主元乘积, 消元过程中顺便拿到的
}

Matrix5x5 Matrix5x5::inverse() const {
    Float a[25 * 2];
    memcpy(a, m, sizeof(m));
    memset(a + 25, 0, sizeof(m));
    for (Int32 i = 0; i < 5; i++) a[i * 5 + 5 + i] = 1;

    Float det = 0;
    if (!invertInPlace(a, 5, det)) throw RuntimeError("Matrix5x5::inverse() - 行列式为 0, 不可逆");

    Matrix5x5 result;
    for (Int32 r = 0; r < 5; r++) {
        for (Int32 c = 0; c < 5; c++) result.m[r * 5 + c] = a[r * 5 + 5 + c];
    }
    return result;
}

String Matrix5x5::toString() const {
    std::stringstream ss;
    ss << "Matrix5x5{";
    for (Int32 r = 0; r < 5; r++) {
        ss << (r == 0 ? "[" : ", [");
        for (Int32 c = 0; c < 5; c++) {
            ss << m[r * 5 + c];
            if (c < 4) ss << ", ";
        }
        ss << "]";
    }
    ss << "}";
    return ss.str();
}
