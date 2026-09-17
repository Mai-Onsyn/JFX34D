#include <cmath>
#include <string>
import Vectors;
import Types;
import Format;

using std::sqrt;
using std::ostream;

Vector3D::Vector3D(const Float x, const Float y, const Float z) : x(x), y(y), z(z) {}

Vector3D::Vector3D(const Vector2D& v2, const Float z) : x(v2.x), y(v2.y), z(z) {}

Vector3D::operator Vector2D() const {
    return {x, y};
}

Vector3D Vector3D::operator+(const Vector3D& other) const {
    return {x + other.x, y + other.y, z + other.z};
}

Vector3D Vector3D::operator-(const Vector3D& other) const {
    return {x - other.x, y - other.y, z - other.z};
}

Vector3D Vector3D::operator*(const Vector3D& other) const {
    return {x * other.x, y * other.y, z * other.z};
}

Vector3D Vector3D::operator*(const Float scalar) const { return
    {x * scalar, y * scalar, z * scalar};
}

Vector3D Vector3D::operator^(const Vector3D &other) const {
    return cross(other);
}

Vector3D Vector3D::operator/(const Vector3D& other) const {
    return {x / other.x, y / other.y, z / other.z};
}

Vector3D Vector3D::operator/(const Float scalar) const {
    return {x / scalar, y / scalar, z / scalar};
}

Vector3D& Vector3D::operator+=(const Vector3D& other) {
    x += other.x;
    y += other.y;
    z += other.z;
    return *this;
}

Vector3D& Vector3D::operator-=(const Vector3D& other) {
    x -= other.x;
    y -= other.y;
    z -= other.z;
    return *this;
}

Vector3D& Vector3D::operator*=(const Vector3D& other) {
    x *= other.x;
    y *= other.y;
    z *= other.z;
    return *this;
}

Vector3D& Vector3D::operator*=(Float scalar) {
    x *= scalar;
    y *= scalar;
    z *= scalar;
    return *this;
}

Vector3D& Vector3D::operator/=(const Vector3D& other) {
    x /= other.x;
    y /= other.y;
    z /= other.z;
    return *this;
}

Vector3D& Vector3D::operator/=(Float scalar) {
    x /= scalar;
    y /= scalar;
    z /= scalar;
    return *this;
}

Boolean Vector3D::operator==(const Vector3D& other) const {
    return x == other.x && y == other.y && z == other.z;
}

Float Vector3D::dot(const Vector3D& other) const {
    return x * other.x + y * other.y + z * other.z;
}

Vector3D Vector3D::cross(const Vector3D& other) const {
    return {
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    };
}

Float Vector3D::length() const {
    return sqrt(x * x + y * y + z * z);
}

Vector3D Vector3D::normalize() const {
    if (const Float len = length(); len > 0)
        return {x / len, y / len, z / len};
    return *this;
}

Vector3D Vector3D::rotate(const Float angle, const Vector3D &axis) const {
    // k 必须为单位向量，若不是，则需要进行归一化
    const Vector3D kr = axis.normalize();

    // 计算 v 和 k 的点积
    const Float dp = dot(kr);

    // 计算 v 和 k 的叉积
    const Vector3D cp = kr ^ *this;

    // 计算旋转后的向量
    const auto cosTheta = static_cast<Float>(cos(angle));
    const auto sinTheta = static_cast<Float>(sin(angle));

    Float vxT = x * cosTheta + (1 - cosTheta) * dp * kr.x + sinTheta * cp.x;
    Float vyT = y * cosTheta + (1 - cosTheta) * dp * kr.y + sinTheta * cp.y;
    Float vzT = z * cosTheta + (1 - cosTheta) * dp * kr.z + sinTheta * cp.z;

    return {vxT, vyT, vzT};
}


String Vector3D::toString() const {
    return format("(%.5f, %.5f, %.5f)", x, y, z);
}

ostream& operator<<(ostream& lhs, const Vector3D& v) {
    return lhs << v.toString();
}