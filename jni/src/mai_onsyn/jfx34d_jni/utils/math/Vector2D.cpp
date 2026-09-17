#include <cmath>
#include <string>
import Vectors;
import Types;
import Format;

using std::sqrt;
using std::ostream;

Vector2D::Vector2D(const Float x, const Float y) : x(x), y(y) {}

Vector2D Vector2D::operator+(const Vector2D &other) const {
    return {x + other.x, y + other.y};
}

Vector2D Vector2D::operator-(const Vector2D &v) const {
    return {x - v.x, y - v.y};
}

Vector2D Vector2D::operator*(const Float scalar) const {
    return {x * scalar, y * scalar};
}

Vector2D Vector2D::operator*(const Vector2D &v) const {
    return {x * v.x, y * v.y};
}

Vector2D Vector2D::operator/(const Float scalar) const {
    return {x / scalar, y / scalar};
}

Vector2D Vector2D::operator/(const Vector2D &v) const {
    return {x / v.x, y / v.y};
}

Vector2D &Vector2D::operator+=(const Vector2D &v) {
    x += v.x;
    y += v.y;
    return *this;
}

Vector2D &Vector2D::operator-=(const Vector2D &v) {
    x -= v.x;
    y -= v.y;
    return *this;
}

Vector2D &Vector2D::operator*=(const Float scalar) {
    x *= scalar;
    y *= scalar;
    return *this;
}

Vector2D &Vector2D::operator*=(const Vector2D &v) {
    x *= v.x;
    y *= v.y;
    return *this;
}

Vector2D &Vector2D::operator/=(const Float scalar) {
    x /= scalar;
    y /= scalar;
    return *this;
}

Vector2D &Vector2D::operator/=(const Vector2D &v) {
    x /= v.x;
    y /= v.y;
    return *this;
}

Boolean Vector2D::operator==(const Vector2D &v) const {
    return x == v.x && y == v.y;
}

Float Vector2D::dot(const Vector2D &v) const {
    return x * v.x + y * v.y;
}

Float Vector2D::length() const {
    return sqrt(x * x + y * y);
}

Vector2D Vector2D::normalize() const {
    if (const Float len = length(); len > 0) {
        return {x / len, y / len};
    }
    return *this;
}

String Vector2D::toString() const {
    return format("(%.5f, %.5f)", x, y);
}

ostream& operator<<(ostream& lhs, const Vector2D& pos) {
    return lhs << pos.toString();
}