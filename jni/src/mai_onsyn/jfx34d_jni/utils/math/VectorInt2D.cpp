#include <cmath>
#include <string>
import Vectors;
import Types;
import Format;

using std::sqrt;
using std::ostream;

VectorInt2D::VectorInt2D(const Int64 x, const Int64 y) : x(x), y(y) {}

VectorInt2D::operator Vector2D() const {
    return {static_cast<Float>(x), static_cast<Float>(y)};
}

VectorInt2D VectorInt2D::operator+(const VectorInt2D &other) const {
    return {x + other.x, y + other.y};
}

VectorInt2D VectorInt2D::operator-(const VectorInt2D &v) const {
    return {x - v.x, y - v.y};
}

VectorInt2D VectorInt2D::operator*(const Float scalar) const {
    return {static_cast<Int64>(x * scalar), static_cast<Int64>(y * scalar)};
}

VectorInt2D VectorInt2D::operator*(const VectorInt2D &v) const {
    return {x * v.x, y * v.y};
}

VectorInt2D VectorInt2D::operator/(const Float scalar) const {
    return {static_cast<Int64>(x / scalar), static_cast<Int64>(y / scalar)};
}

VectorInt2D VectorInt2D::operator/(const VectorInt2D &v) const {
    return {x / v.x, y / v.y};
}

VectorInt2D &VectorInt2D::operator+=(const VectorInt2D &v) {
    x += v.x;
    y += v.y;
    return *this;
}

VectorInt2D &VectorInt2D::operator-=(const VectorInt2D &v) {
    x -= v.x;
    y -= v.y;
    return *this;
}

VectorInt2D &VectorInt2D::operator*=(const Float scalar) {
    x *= scalar;
    y *= scalar;
    return *this;
}

VectorInt2D &VectorInt2D::operator*=(const VectorInt2D &v) {
    x *= v.x;
    y *= v.y;
    return *this;
}

VectorInt2D &VectorInt2D::operator/=(const Float scalar) {
    x /= scalar;
    y /= scalar;
    return *this;
}

VectorInt2D &VectorInt2D::operator/=(const VectorInt2D &v) {
    x /= v.x;
    y /= v.y;
    return *this;
}

Boolean VectorInt2D::operator==(const VectorInt2D &v) const {
    return x == v.x && y == v.y;
}

Int64 VectorInt2D::dot(const VectorInt2D &v) const {
    return x * v.x + y * v.y;
}

Int64 VectorInt2D::length() const {
    return sqrt(x * x + y * y);
}

VectorInt2D VectorInt2D::normalize() const {
    if (const Int64 len = length(); len > 0) {
        return {x / len, y / len};
    }
    return *this;
}

String VectorInt2D::toString() const {
    return format("(%d, %d)", x, y);
}

ostream& operator<<(ostream& lhs, const VectorInt2D& pos) {
    return lhs << pos.toString();
}