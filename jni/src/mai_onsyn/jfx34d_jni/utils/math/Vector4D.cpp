#include <cmath>
#include <string>
import Vectors;
import Types;
import Format;

using std::sqrt;
using std::ostream;

Vector4D::Vector4D(const Float x, const Float y, const Float z, const Float w) : x(x), y(y), z(z), w(w) {}

Vector4D::Vector4D(const Vector3D& v3, const Float w) : x(v3.x), y(v3.y), z(v3.z), w(w) {}

Vector4D::operator Vector3D() const {
    return {x, y, z};
}

Vector4D Vector4D::operator+(const Vector4D& other) const {
    return {x + other.x, y + other.y, z + other.z, w + other.w};
}

Vector4D Vector4D::operator-(const Vector4D& other) const {
    return {x - other.x, y - other.y, z - other.z, w - other.w};
}

Vector4D Vector4D::operator*(const Vector4D& other) const {
    return {x * other.x, y * other.y, z * other.z, w * other.w};
}

Vector4D Vector4D::operator*(const Float scalar) const {
    return {x * scalar, y * scalar, z * scalar, w * scalar};
}

Vector4D Vector4D::operator/(const Vector4D& other) const {
    return {x / other.x, y / other.y, z / other.z, w / other.w};
}

Vector4D Vector4D::operator/(const Float scalar) const {
    return {x / scalar, y / scalar, z / scalar, w / scalar};
}

Vector4D& Vector4D::operator+=(const Vector4D& other) {
    x += other.x;
    y += other.y;
    z += other.z;
    w += other.w;
    return *this;
}

Vector4D& Vector4D::operator-=(const Vector4D& other) {
    x -= other.x;
    y -= other.y;
    z -= other.z;
    w -= other.w;
    return *this;
}

Vector4D& Vector4D::operator*=(const Vector4D& other) {
    x *= other.x;
    y *= other.y;
    z *= other.z;
    w *= other.w;
    return *this;
}

Vector4D& Vector4D::operator*=(const Float scalar) {
    x *= scalar;
    y *= scalar;
    z *= scalar;
    w *= scalar;
    return *this;
}

Vector4D& Vector4D::operator/=(const Vector4D& other) {
    x /= other.x;
    y /= other.y;
    z /= other.z;
    w /= other.w;
    return *this;
}

Vector4D& Vector4D::operator/=(const Float scalar) {
    x /= scalar;
    y /= scalar;
    z /= scalar;
    w /= scalar;
    return *this;
}

Boolean Vector4D::operator==(const Vector4D& other) const {
    return x == other.x && y == other.y && z == other.z && w == other.w;
}

Float Vector4D::dot(const Vector4D& other) const {
    return x * other.x + y * other.y + z * other.z + w * other.w;
}

Float Vector4D::length() const {
    return sqrt(x * x + y * y + z * z + w * w);
}

Vector4D Vector4D::normalize() const {
    if (const Float len = length(); len > 0) return {x / len, y / len, z / len, w / len};
    return *this;
}

String Vector4D::toString() const {
    return format("(%.5f, %.5f, %.5f, %.5f)", x, y, z, w);
}

ostream& operator<<(ostream& lhs, const Vector4D& v) {
    return lhs << v.toString();
}