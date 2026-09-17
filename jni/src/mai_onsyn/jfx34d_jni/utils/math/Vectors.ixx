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

export struct Vector5D {
    Float x, y, z, w, v;

    Vector5D() = default;
    Vector5D(Float x, Float y, Float z, Float w, Float v);
    explicit Vector5D(const Vector4D& v4, Float v = 1.0f);
    explicit operator Vector4D() const;

    Vector5D operator+(const Vector5D& other) const;
    Vector5D operator-(const Vector5D& other) const;
    Vector5D operator*(const Vector5D& other) const;
    Vector5D operator*(Float scalar) const;
    Vector5D operator/(const Vector5D& other) const;
    Vector5D operator/(Float scalar) const;
    Vector5D& operator+=(const Vector5D& other);
    Vector5D& operator-=(const Vector5D& other);
    Vector5D& operator*=(const Vector5D& other);
    Vector5D& operator*=(Float scalar);
    Vector5D& operator/=(const Vector5D& other);
    Vector5D& operator/=(Float scalar);
    Boolean operator==(const Vector5D& other) const;

    [[nodiscard]] Float dot(const Vector5D& other) const;
    [[nodiscard]] Float length() const;
    [[nodiscard]] Vector5D normalize() const;

    /** 按 0..4 取分量 (x y z w v), 给四维叉积的排列枚举用。 */
    [[nodiscard]] Float component(Int32 index) const;

    /**
     * 四维叉积: 同时垂直于 a, b, c, d 的那个向量。
     * 五维空间里 "垂直于 4 个向量" 只剩一个方向, 所以结果唯一 (差一个符号/长度)。
     * 用在 4D 网格上就是: 四面体/超平面求法向量。
     */
    [[nodiscard]] static Vector5D cross(
        const Vector5D& a, const Vector5D& b, const Vector5D& c, const Vector5D& d);

    [[nodiscard]] String toString() const;
    friend ostream& operator<<(ostream& lhs, const Vector5D& v);
};

/** 标量在左边的乘法: 2.0f * v */
export Vector5D operator*(Float scalar, const Vector5D& v);