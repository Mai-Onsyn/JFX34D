#include <algorithm>
#include <cmath>
#include <string>
import Vectors;
import Types;
import Format;

using std::sqrt;
using std::ostream;

Vector5D::Vector5D(const Float x, const Float y, const Float z, const Float w, const Float v)
    : x(x), y(y), z(z), w(w), v(v) {}

Vector5D::Vector5D(const Vector4D& v4, const Float v) : x(v4.x), y(v4.y), z(v4.z), w(v4.w), v(v) {}

Vector5D::operator Vector4D() const {
    return {x, y, z, w};
}

Vector5D Vector5D::operator+(const Vector5D& other) const {
    return {x + other.x, y + other.y, z + other.z, w + other.w, v + other.v};
}

Vector5D Vector5D::operator-(const Vector5D& other) const {
    return {x - other.x, y - other.y, z - other.z, w - other.w, v - other.v};
}

Vector5D Vector5D::operator*(const Vector5D& other) const {
    return {x * other.x, y * other.y, z * other.z, w * other.w, v * other.v};
}

Vector5D Vector5D::operator*(const Float scalar) const {
    return {x * scalar, y * scalar, z * scalar, w * scalar, v * scalar};
}

Vector5D Vector5D::operator/(const Vector5D& other) const {
    return {x / other.x, y / other.y, z / other.z, w / other.w, v / other.v};
}

Vector5D Vector5D::operator/(const Float scalar) const {
    return {x / scalar, y / scalar, z / scalar, w / scalar, v / scalar};
}

Vector5D& Vector5D::operator+=(const Vector5D& other) {
    x += other.x;
    y += other.y;
    z += other.z;
    w += other.w;
    v += other.v;
    return *this;
}

Vector5D& Vector5D::operator-=(const Vector5D& other) {
    x -= other.x;
    y -= other.y;
    z -= other.z;
    w -= other.w;
    v -= other.v;
    return *this;
}

Vector5D& Vector5D::operator*=(const Vector5D& other) {
    x *= other.x;
    y *= other.y;
    z *= other.z;
    w *= other.w;
    v *= other.v;
    return *this;
}

Vector5D& Vector5D::operator*=(const Float scalar) {
    x *= scalar;
    y *= scalar;
    z *= scalar;
    w *= scalar;
    v *= scalar;
    return *this;
}

Vector5D& Vector5D::operator/=(const Vector5D& other) {
    x /= other.x;
    y /= other.y;
    z /= other.z;
    w /= other.w;
    v /= other.v;
    return *this;
}

Vector5D& Vector5D::operator/=(const Float scalar) {
    x /= scalar;
    y /= scalar;
    z /= scalar;
    w /= scalar;
    v /= scalar;
    return *this;
}

Boolean Vector5D::operator==(const Vector5D& other) const {
    return x == other.x && y == other.y && z == other.z && w == other.w && v == other.v;
}

Float Vector5D::dot(const Vector5D& other) const {
    return x * other.x + y * other.y + z * other.z + w * other.w + v * other.v;
}

Float Vector5D::length() const {
    return sqrt(dot(*this));
}

Vector5D Vector5D::normalize() const {
    if (const Float len = length(); len > 0) return {x / len, y / len, z / len, w / len, v / len};
    return *this;
}

Float Vector5D::component(const Int32 index) const {
    switch (index) {
        case 0: return x;
        case 1: return y;
        case 2: return z;
        case 3: return w;
        case 4: return v;
        default: return 0;
    }
}

Vector5D Vector5D::cross(const Vector5D& a, const Vector5D& b, const Vector5D& c, const Vector5D& d) {
    const Vector5D* src[5] = {&a, &b, &c, &d, nullptr};

    Float out[5];
    // 第 k 个分量 = 把 "除 k 之外的 4 个分量" 展开得到的 4x4 行列式。
    // 展开式 = 对那 4 个下标的全排列求和: sum( sign(perm) * sum_i src[i][perm[i]] )。
    // 起点 {0,1,2,3} 是偶排列, 所以每走一步 next_permutation (必定是奇数次相邻换位)
    // 就把符号翻一次 —— 这样不用手写 120 项, 也不会抄错正负号。
    for (Int32 k = 0; k < 5; k++) {
        const Vector5D* vectors[4];
        Int32 n = 0;
        for (Int32 i = 0; i < 5; i++) {
            if (i != k) vectors[n++] = src[i];
        }

        Int32 perm[4] = {0, 1, 2, 3};
        Float sum = 0;
        Int32 sign = 1;
        do {
            sum += static_cast<Float>(sign)
                * vectors[0]->component(perm[0])
                * vectors[1]->component(perm[1])
                * vectors[2]->component(perm[2])
                * vectors[3]->component(perm[3]);
            sign = -sign;
        } while (std::next_permutation(perm, perm + 4));

        out[k] = sum;
    }

    return {out[0], out[1], out[2], out[3], out[4]};
}

String Vector5D::toString() const {
    return format("(%.5f, %.5f, %.5f, %.5f, %.5f)", x, y, z, w, v);
}

ostream& operator<<(ostream& lhs, const Vector5D& v) {
    return lhs << v.toString();
}

Vector5D operator*(const Float scalar, const Vector5D& v) {
    return v * scalar;
}
