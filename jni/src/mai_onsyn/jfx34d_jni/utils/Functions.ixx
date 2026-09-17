module;
#include <string>
#include <immintrin.h>
export module Functions;
import Types;
// import Color;

using std::to_string;

export String toString(const Int32 value) {
    return to_string(value);
}

export String toString(const UInt32 value) {
    return to_string(value);
}

export String toString(const Float value) {
    return to_string(value);
}

export void avx2Fill(UInt8* buffer, const UInt32 start, const Int32 value, const Int32 size) {
    if (size <= 0) return;
    const __m256i fill = _mm256_set1_epi32(value);
    auto* pixels = reinterpret_cast<UInt32*>(buffer + (start << 2));
    for (UInt32 i = 0; i + 8 <= size; i += 8) {
        _mm256_storeu_si256(reinterpret_cast<__m256i*>(pixels + i), fill);
    }
    for (UInt32 i = (size / 8) * 8; i < size; i++) {
        pixels[i] = value;
    }
}

// export void avx2Fill(UInt8* buffer, const UInt32 start, const Color c, const UInt32 size) {
//     avx2Fill(buffer, start, c.a << 24 | c.b << 16 | c.g << 8 | c.r, size);
// }

export Boolean startWith(const String& str, const String& prefix) {
    return str.substr(0, prefix.length()) == prefix;
}

export List<String> split(const String& str, const String& delimiter) {
    List<String> result;
    if (str.empty()) {
        return result;
    }
    if (delimiter.empty()) {
        result.push_back(str);
        return result;
    }

    UInt64 start = 0;
    UInt64 end = 0;
    while ((end = str.find(delimiter, start)) != String::npos) {
        result.push_back(str.substr(start, end - start));
        start = end + delimiter.length();
    }
    result.push_back(str.substr(start));
    return result;
}

export String trim(const String& str) {
    const UInt64 start = str.find_first_not_of(" \t\n\r");
    if (start == String::npos) {
        return "";
    }
    const UInt64 end = str.find_last_not_of(" \t\n\r");
    return str.substr(start, end - start + 1);
}

export void collapseSpaces(String& str) {
    if (str.empty()) return;

    UInt64 writeIndex = 0;
    Boolean inSpace = false;

    for (UInt64 i = 0; i < str.length(); ++i) {
        if (str[i] == ' ') {
            if (!inSpace) {
                str[writeIndex++] = ' ';
                inSpace = true;
            }
        } else {
            str[writeIndex++] = str[i];
            inSpace = false;
        }
    }

    str.resize(writeIndex);
}