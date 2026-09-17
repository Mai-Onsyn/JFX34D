module;
#include <ostream>
export module Vectors;
import Types;

using std::ostream;

export struct Vector2D {
    Float x, y;

    Vector2D() = default;
    Vector2D(Float x, Float y);

    Vector2D operator+(const Vector2D& other) const;
    Vector2D operator-(const Vector2D& v) const;
    Vector2D operator*(const Vector2D& v) const;
    Vector2D operator*(float scalar) const;
    Vector2D operator/(const Vector2D& v) const;
    Vector2D operator/(float scalar) const;
    Vector2D& operator+=(const Vector2D& v);
    Vector2D& operator-=(const Vector2D& v);
    Vector2D& operator*=(const Vector2D& v);
    Vector2D& operator*=(float scalar);
    Vector2D& operator/=(const Vector2D& v);
    Vector2D& operator/=(float scalar);
    Boolean operator==(const Vector2D &v) const;

    [[nodiscard]] Float dot(const Vector2D& v) const;
    [[nodiscard]] Float length() const;
    [[nodiscard]] Vector2D normalize() const;

    [[nodiscard]] String toString() const;
    friend ostream& operator<<(ostream & lhs, const Vector2D & pos);
};

export struct VectorInt2D {
    Int64 x, y;

    VectorInt2D() = default;
    VectorInt2D(Int64 x, Int64 y);
    explicit operator Vector2D() const;

    VectorInt2D operator+(const VectorInt2D& other) const;
    VectorInt2D operator-(const VectorInt2D& v) const;
    VectorInt2D operator*(const VectorInt2D& v) const;
    VectorInt2D operator*(Float scalar) const;
    VectorInt2D operator/(const VectorInt2D& v) const;
    VectorInt2D operator/(Float scalar) const;
    VectorInt2D& operator+=(const VectorInt2D& v);
    VectorInt2D& operator-=(const VectorInt2D& v);
    VectorInt2D& operator*=(const VectorInt2D& v);
    VectorInt2D& operator*=(Float scalar);
    VectorInt2D& operator/=(const VectorInt2D& v);
    VectorInt2D& operator/=(Float scalar);
    Boolean operator==(const VectorInt2D &v) const;

    [[nodiscard]] Int64 dot(const VectorInt2D& v) const;
    [[nodiscard]] Int64 length() const;
    [[nodiscard]] VectorInt2D normalize() const;

    [[nodiscard]] String toString() const;
    friend ostream& operator<<(ostream & lhs, const VectorInt2D & pos);
};

export struct Vector3D {
    Float x, y, z;

    Vector3D() = default;
    Vector3D(Float x, Float y, Float z);
    explicit Vector3D(const Vector2D& v2, Float z = 1.0f);
    explicit operator Vector2D() const;

    Vector3D operator+(const Vector3D& other) const;
    Vector3D operator-(const Vector3D& other) const;
    Vector3D operator*(const Vector3D& other) const;
    Vector3D operator*(Float scalar) const;
    Vector3D operator^(const Vector3D& other) const;
    Vector3D operator/(const Vector3D& other) const;
    Vector3D operator/(Float scalar) const;
    Vector3D& operator+=(const Vector3D& other);
    Vector3D& operator-=(const Vector3D& other);
    Vector3D& operator*=(const Vector3D& other);
    Vector3D& operator*=(Float scalar);
    Vector3D& operator/=(const Vector3D& other);
    Vector3D& operator/=(Float scalar);
    Boolean operator==(const Vector3D& other) const;
    [[nodiscard]] Float dot(const Vector3D& other) const;
    [[nodiscard]] Vector3D cross(const Vector3D& other) const;
    [[nodiscard]] Float length() const;
    [[nodiscard]] Vector3D normalize() const;

    Vector3D rotate(Float angle, const Vector3D& axis) const;

    [[nodiscard]] String toString() const;
    friend ostream& operator<<(ostream& lhs, const Vector3D& v);
};

export struct Vector4D {
    Float x, y, z, w;

    Vector4D() = default;
    Vector4D(Float x, Float y, Float z, Float w);
    explicit Vector4D(const Vector3D& v3, Float w = 1.0f);
    explicit operator Vector3D() const;

    Vector4D operator+(const Vector4D& other) const;
    Vector4D operator-(const Vector4D& other) const;
    Vector4D operator*(const Vector4D& other) const;
    Vector4D operator*(Float scalar) const;
    Vector4D operator/(const Vector4D& other) const;
    Vector4D operator/(Float scalar) const;
    Vector4D& operator+=(const Vector4D& other);
    Vector4D& operator-=(const Vector4D& other);
    Vector4D& operator*=(const Vector4D& other);
    Vector4D& operator*=(Float scalar);
    Vector4D& operator/=(const Vector4D& other);
    Vector4D& operator/=(Float scalar);
    Boolean operator==(const Vector4D& other) const;

    [[nodiscard]] Float dot(const Vector4D& other) const;
    [[nodiscard]] Float length() const;
    [[nodiscard]] Vector4D normalize() const;

    [[nodiscard]] String toString() const;
    friend ostream& operator<<(ostream& lhs, const Vector4D& v);
};