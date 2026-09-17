module;
#include <algorithm>
#include <string>
export module Color;
import Types;
import Format;

inline UInt8 multUint8(const UInt8 a, const UInt8 b) {
    const UInt32 prod = static_cast<UInt32>(a) * static_cast<UInt32>(b);
    UInt32 val = (prod << 8) + prod;
    val += 32768;
    return static_cast<UInt8>(val >> 16);
}

export struct Color {
    UInt8 r, g, b, a = 255;

    constexpr Color(const UInt8 r, const UInt8 g, const UInt8 b, const UInt8 a = 255) noexcept
        : r(r), g(g), b(b), a(a) {}

    constexpr Color() = default;

    Color operator*(const Color& other) const noexcept {
        return {
            multUint8(r, other.r),
            multUint8(g, other.g),
            multUint8(b, other.b),
            multUint8(a, other.a)
        };
    }

    Color operator*(const Float f) const noexcept {
        return {
            static_cast<UInt8>(r * f),
            static_cast<UInt8>(g * f),
            static_cast<UInt8>(b * f),
            a
        };
    }


    String toString() const {
        return format("Color{%d, %d, %d, %d}", r, g, b, a);
    }

    static const Color Transparent;
    static const Color Red;
    static const Color Green;
    static const Color Blue;
    static const Color Yellow;
    static const Color Magenta;
    static const Color Cyan;
    static const Color White;
    static const Color Black;

    static const Color SkyBlue;
};

export constexpr Color Color::Transparent{255, 255, 255, 0};
export constexpr Color Color::Red{255, 0, 0};
export constexpr Color Color::Green{0, 255, 0};
export constexpr Color Color::Blue{0, 0, 255};
export constexpr Color Color::Yellow{255, 255, 0};
export constexpr Color Color::Magenta{255, 0, 255};
export constexpr Color Color::Cyan{0, 255, 255};
export constexpr Color Color::White{255, 255, 255};
export constexpr Color Color::Black{0, 0, 0};

export constexpr Color Color::SkyBlue{135, 206, 250};


export struct FColor {
    Float r, g, b, a = 1.0f;

    FColor() = default;

    FColor(const Float all): r(all), g(all), b(all), a(255) {}

    FColor(const Float r, const Float g, const Float b, const Float a = 1.0f): r(r), g(g), b(b), a(a) {}

    explicit operator Color() const {
        return Color{static_cast<UInt8>(r * 255), static_cast<UInt8>(g * 255), static_cast<UInt8>(b * 255), static_cast<UInt8>(a * 255)};
    }

    FColor(const Color& color): r(color.r / 255.0f), g(color.g / 255.0f), b(color.b / 255.0f), a(color.a / 255.0f) {}


    FColor operator*(const FColor& other) const {
        return {r * other.r, g * other.g, b * other.b};
    }

    FColor operator*(const Float& other) const {
        return {r * other, g * other, b * other};
    }

    FColor operator+(const FColor& other) const {
        return {r + other.r, g + other.g, b + other.b};
    }

    FColor operator+=(const FColor& other) {
        return *this = *this + other;
    }

    FColor clamp() const {
        return {std::clamp(r, 0.0f, 1.0f), std::clamp(g, 0.0f, 1.0f), std::clamp(b, 0.0f, 1.0f), std::clamp(a, 0.0f, 1.0f)};
    }
};